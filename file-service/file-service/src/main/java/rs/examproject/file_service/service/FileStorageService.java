package rs.examproject.file_service.service;

import io.minio.GetObjectArgs;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.errors.MinioException;
import jakarta.annotation.PostConstruct;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import rs.examproject.file_service.dto.FileMetadataResponse;
import rs.examproject.file_service.dto.FileUploadedEvent;
import rs.examproject.file_service.model.FileMetadata;
import rs.examproject.file_service.repository.FileMetadataRepository;

import java.io.InputStream;
import java.util.List;
import java.util.UUID;

@Service
public class FileStorageService {

    private final MinioClient minioClient;
    private final String bucket;
    private final FileMetadataRepository repository;
    private final RabbitTemplate rabbitTemplate;
    private final String queueName;

    public FileStorageService(
            MinioClient minioClient,
            @Value("${minio.bucket}") String bucket,
            FileMetadataRepository repository,
            RabbitTemplate rabbitTemplate,
            @Value("${app.file.queue:file.process}") String queueName
    ) {
        this.minioClient = minioClient;
        this.bucket = bucket;
        this.repository = repository;
        this.rabbitTemplate = rabbitTemplate;
        this.queueName = queueName;
    }

    @PostConstruct
    void ensureBucket() {
        try {
            boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
            if (!exists) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
            }
        } catch (Exception e) {
            throw new IllegalStateException("Cannot ensure MinIO bucket: " + bucket, e);
        }
    }

    @Transactional
    public FileMetadataResponse store(MultipartFile file, String uploadedBy) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        String contentType = file.getContentType() != null ? file.getContentType() : MediaType.APPLICATION_OCTET_STREAM_VALUE;
        String objectKey = UUID.randomUUID() + "/" + sanitize(file.getOriginalFilename());

        try (InputStream is = file.getInputStream()) {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectKey)
                    .contentType(contentType)
                    .stream(is, file.getSize(), -1)
                    .build());
        } catch (MinioException e) {
            throw new IllegalStateException("MinIO error while storing file", e);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to store file", e);
        }

        FileMetadata meta = new FileMetadata();
        meta.setOriginalFilename(file.getOriginalFilename());
        meta.setObjectKey(objectKey);
        meta.setContentType(contentType);
        meta.setSize(file.getSize());
        meta.setUploadedBy(uploadedBy);

        FileMetadata saved = repository.save(meta);
        publishEvent(saved);
        return toResponse(saved);
    }

    public List<FileMetadataResponse> list() {
        return repository.findAll().stream().map(this::toResponse).toList();
    }

    public FileMetadataResponse getMetadata(long id) {
        return repository.findById(id).map(this::toResponse)
                .orElseThrow(() -> new IllegalArgumentException("File not found: " + id));
    }

    public FileDownload download(long id) {
        FileMetadata meta = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("File not found: " + id));

        try {
            InputStream stream = minioClient.getObject(GetObjectArgs.builder()
                    .bucket(bucket)
                    .object(meta.getObjectKey())
                    .build());
            return new FileDownload(meta, stream);
        } catch (Exception e) {
            throw new IllegalStateException("Cannot download file " + meta.getObjectKey(), e);
        }
    }

    @Transactional
    public void delete(long id) {
        FileMetadata meta = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("File not found: " + id));

        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(bucket)
                    .object(meta.getObjectKey())
                    .build());
        } catch (Exception e) {
            throw new IllegalStateException("Cannot delete file from MinIO: " + meta.getObjectKey(), e);
        }

        repository.deleteById(id);
    }

    private void publishEvent(FileMetadata meta) {
        FileUploadedEvent event = new FileUploadedEvent(
                meta.getId(),
                meta.getObjectKey(),
                meta.getOriginalFilename(),
                meta.getContentType(),
                meta.getSize(),
                meta.getUploadedBy()
        );
        rabbitTemplate.convertAndSend(queueName, event);
    }

    private FileMetadataResponse toResponse(FileMetadata meta) {
        return new FileMetadataResponse(
                meta.getId(),
                meta.getOriginalFilename(),
                meta.getObjectKey(),
                meta.getContentType(),
                meta.getSize(),
                meta.getUploadedBy(),
                meta.getCreatedAt()
        );
    }

    private String sanitize(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) {
            return "file.bin";
        }
        return originalFilename.replaceAll("[^a-zA-Z0-9\\.\\-_]", "_");
    }

    public record FileDownload(FileMetadata meta, InputStream stream) {}
}

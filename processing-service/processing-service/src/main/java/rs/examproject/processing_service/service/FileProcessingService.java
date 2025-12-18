package rs.examproject.processing_service.service;

import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import rs.examproject.processing_service.dto.FileUploadedEvent;

import java.io.InputStream;

@Service
public class FileProcessingService {

    private static final Logger log = LoggerFactory.getLogger(FileProcessingService.class);

    private final MinioClient minioClient;
    private final String bucket;

    public FileProcessingService(MinioClient minioClient,
                                 @Value("${minio.bucket}") String bucket) {
        this.minioClient = minioClient;
        this.bucket = bucket;
    }

    @RabbitListener(queues = "${app.file.queue:file.process}")
    public void handleFileUploaded(FileUploadedEvent event) {
        log.info("Received file event id={} key={} size={} type={}",
                event.id(), event.objectKey(), event.size(), event.contentType());
        try (InputStream stream = minioClient.getObject(GetObjectArgs.builder()
                .bucket(bucket)
                .object(event.objectKey())
                .build())) {

            long bytes = stream.transferTo(java.io.OutputStream.nullOutputStream());
            log.info("Processed file {} ({} bytes) uploadedBy={}", event.filename(), bytes, event.uploadedBy());
        } catch (Exception e) {
            log.error("Failed to process file {}", event.objectKey(), e);
        }
    }
}

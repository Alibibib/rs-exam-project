package rs.examproject.file_service.service;

import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.GetObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.errors.MinioException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import rs.examproject.file_service.config.RabbitMQConfig;
import rs.examproject.file_service.dto.FileProcessingMessage;

import java.io.InputStream;
import java.util.UUID;

@Service
public class FileService {

    private final MinioClient minioClient;
    private final String bucketName;
    private final RabbitTemplate rabbitTemplate;

    public FileService(MinioClient minioClient,
                       @Value("${minio.bucket}") String bucketName,
                       RabbitTemplate rabbitTemplate) {
        this.minioClient = minioClient;
        this.bucketName = bucketName;
        this.rabbitTemplate = rabbitTemplate;
    }

    public String uploadFile(MultipartFile file, String userId) {
        try {
            String fileName = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
            
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(fileName)
                            .stream(file.getInputStream(), file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build()
            );
            
            // Отправляем сообщение в очередь для обработки
            FileProcessingMessage message = new FileProcessingMessage(fileName, "PROCESS", userId);
            rabbitTemplate.convertAndSend(RabbitMQConfig.FILE_PROCESSING_QUEUE, message);
            
            return fileName;
        } catch (Exception e) {
            throw new RuntimeException("Failed to upload file: " + e.getMessage(), e);
        }
    }

    public InputStream downloadFile(String fileName) {
        try {
            return minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucketName)
                            .object(fileName)
                            .build()
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to download file: " + e.getMessage(), e);
        }
    }

    public void deleteFile(String fileName) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucketName)
                            .object(fileName)
                            .build()
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete file: " + e.getMessage(), e);
        }
    }
}


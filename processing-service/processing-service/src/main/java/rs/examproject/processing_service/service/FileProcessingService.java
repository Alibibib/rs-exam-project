package rs.examproject.processing_service.service;

import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.StatObjectArgs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.InputStream;

@Service
public class FileProcessingService {

    private static final Logger logger = LoggerFactory.getLogger(FileProcessingService.class);

    private final MinioClient minioClient;
    private final String bucketName;

    public FileProcessingService(MinioClient minioClient,
                                  @Value("${minio.bucket}") String bucketName) {
        this.minioClient = minioClient;
        this.bucketName = bucketName;
    }

    public void processFile(String fileName, String operation) {
        try {
            logger.info("Processing file: {} with operation: {}", fileName, operation);

            switch (operation) {
                case "upload" -> processUploadedFile(fileName);
                case "delete" -> processDeletedFile(fileName);
                default -> logger.warn("Unknown operation: {}", operation);
            }
        } catch (Exception e) {
            logger.error("Error processing file: {}", fileName, e);
            throw new RuntimeException("Failed to process file: " + fileName, e);
        }
    }

    private void processUploadedFile(String fileName) throws Exception {
        logger.info("Processing uploaded file: {}", fileName);

        // Проверяем существование файла
        if (!fileExists(fileName)) {
            logger.warn("File not found: {}", fileName);
            return;
        }

        // Получаем информацию о файле
        var stat = minioClient.statObject(
            StatObjectArgs.builder()
                .bucket(bucketName)
                .object(fileName)
                .build()
        );

        logger.info("File info - Name: {}, Size: {}, ContentType: {}", 
            fileName, stat.size(), stat.contentType() != null ? stat.contentType() : "unknown");

        // Читаем файл для обработки
        try (InputStream stream = minioClient.getObject(
            GetObjectArgs.builder()
                .bucket(bucketName)
                .object(fileName)
                .build()
        )) {
            // Здесь можно добавить логику обработки файла
            // Например, генерация превью, извлечение метаданных и т.д.
            logger.info("File {} processed successfully", fileName);
        }
    }

    private void processDeletedFile(String fileName) {
        logger.info("Processing deleted file: {}", fileName);
        // Здесь можно добавить логику очистки связанных данных
        // Например, удаление превью, метаданных и т.д.
    }

    private boolean fileExists(String fileName) {
        try {
            minioClient.statObject(
                StatObjectArgs.builder()
                    .bucket(bucketName)
                    .object(fileName)
                    .build()
            );
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}


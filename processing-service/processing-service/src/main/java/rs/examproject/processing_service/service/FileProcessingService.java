package rs.examproject.processing_service.service;

import io.minio.MinioClient;
import io.minio.GetObjectArgs;
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
        logger.info("Processing file: {} with operation: {}", fileName, operation);
        
        try {
            // Получаем файл из Minio
            InputStream inputStream = minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucketName)
                            .object(fileName)
                            .build()
            );

            // Здесь можно добавить различную обработку файла
            // Например: конвертация, изменение размера изображения, извлечение метаданных и т.д.
            switch (operation) {
                case "PROCESS":
                    processFileContent(inputStream, fileName);
                    break;
                case "ANALYZE":
                    analyzeFile(inputStream, fileName);
                    break;
                default:
                    logger.warn("Unknown operation: {}", operation);
            }

            inputStream.close();
            logger.info("File processing completed: {}", fileName);
            
        } catch (Exception e) {
            logger.error("Error processing file: {}", fileName, e);
            throw new RuntimeException("Failed to process file: " + e.getMessage(), e);
        }
    }

    private void processFileContent(InputStream inputStream, String fileName) {
        logger.info("Processing content of file: {}", fileName);
        // Здесь можно добавить логику обработки содержимого файла
        // Например: парсинг, валидация, преобразование и т.д.
    }

    private void analyzeFile(InputStream inputStream, String fileName) {
        logger.info("Analyzing file: {}", fileName);
        // Здесь можно добавить логику анализа файла
        // Например: определение типа файла, извлечение метаданных и т.д.
    }
}


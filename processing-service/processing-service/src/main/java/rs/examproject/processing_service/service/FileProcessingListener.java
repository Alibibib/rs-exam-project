package rs.examproject.processing_service.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import rs.examproject.processing_service.dto.FileProcessingRequest;

@Component
public class FileProcessingListener {

    private static final Logger logger = LoggerFactory.getLogger(FileProcessingListener.class);

    private final FileProcessingService fileProcessingService;

    public FileProcessingListener(FileProcessingService fileProcessingService) {
        this.fileProcessingService = fileProcessingService;
    }

    @RabbitListener(queues = "file.processing")
    public void processFile(FileProcessingRequest request) {
        logger.info("Received processing request: fileName={}, operation={}", 
            request.fileName(), request.operation());
        
        try {
            fileProcessingService.processFile(request.fileName(), request.operation());
            logger.info("Successfully processed file: {}", request.fileName());
        } catch (Exception e) {
            logger.error("Failed to process file: {}", request.fileName(), e);
            // В реальном приложении здесь можно добавить retry логику или отправку в DLQ
        }
    }
}


package rs.examproject.processing_service.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import rs.examproject.processing_service.dto.FileProcessingMessage;
import rs.examproject.processing_service.service.FileProcessingService;

@Component
public class FileProcessingListener {

    private static final Logger logger = LoggerFactory.getLogger(FileProcessingListener.class);

    private final FileProcessingService fileProcessingService;

    public FileProcessingListener(FileProcessingService fileProcessingService) {
        this.fileProcessingService = fileProcessingService;
    }

    @RabbitListener(queues = "file.processing.queue")
    public void handleFileProcessing(FileProcessingMessage message) {
        logger.info("Received file processing message: fileName={}, operation={}, userId={}", 
                message.getFileName(), message.getOperation(), message.getUserId());
        
        try {
            fileProcessingService.processFile(message.getFileName(), message.getOperation());
            logger.info("Successfully processed file: {}", message.getFileName());
        } catch (Exception e) {
            logger.error("Error processing file: {}", message.getFileName(), e);
            // Здесь можно добавить логику повторной попытки или отправки в очередь ошибок
        }
    }
}


package rs.examproject.file_service.service;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import rs.examproject.file_service.dto.FileProcessingRequest;

@Service
public class FileProcessingProducer {

    private final RabbitTemplate rabbitTemplate;
    private final String queueName;

    public FileProcessingProducer(RabbitTemplate rabbitTemplate,
                                  @Value("${file.processing.queue:file.processing}") String queueName) {
        this.rabbitTemplate = rabbitTemplate;
        this.queueName = queueName;
    }

    public void sendProcessingTask(String fileName, String operation) {
        FileProcessingRequest request = new FileProcessingRequest(fileName, operation);
        rabbitTemplate.convertAndSend(queueName, request);
    }
}


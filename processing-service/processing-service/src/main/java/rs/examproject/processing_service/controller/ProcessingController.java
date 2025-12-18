package rs.examproject.processing_service.controller;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import rs.examproject.processing_service.config.RabbitMQConfig;
import rs.examproject.processing_service.dto.FileProcessingMessage;

@RestController
@RequestMapping("/processing")
public class ProcessingController {

    private final RabbitTemplate rabbitTemplate;

    public ProcessingController(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @PostMapping("/queue")
    public ResponseEntity<String> queueFileProcessing(@RequestBody FileProcessingMessage message) {
        rabbitTemplate.convertAndSend(RabbitMQConfig.FILE_PROCESSING_QUEUE, message);
        return ResponseEntity.ok("File processing queued: " + message.getFileName());
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Processing service is running");
    }
}


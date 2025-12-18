package rs.examproject.processing_service.dto;

public record FileUploadedEvent(
        Long id,
        String objectKey,
        String filename,
        String contentType,
        Long size,
        String uploadedBy
) {
}

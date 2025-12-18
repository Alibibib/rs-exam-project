package rs.examproject.file_service.dto;

import java.time.Instant;

public record FileMetadataResponse(
        Long id,
        String filename,
        String objectKey,
        String contentType,
        Long size,
        String uploadedBy,
        Instant createdAt
) {
}

package rs.examproject.file_service.dto;

import java.time.Instant;

public record BookResponse(
        Long id,
        String title,
        String author,
        String isbn,
        Instant createdAt
) {}

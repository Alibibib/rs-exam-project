package rs.examproject.file_service.dto;

import jakarta.validation.constraints.NotBlank;

public record BookRequest(
        @NotBlank String title,
        String author,
        String isbn
) {}

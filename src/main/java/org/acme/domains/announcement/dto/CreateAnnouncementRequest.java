package org.acme.domains.announcement.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateAnnouncementRequest(
        @NotBlank(message = "Title cannot be blank")
        @Size(max = 160, message = "Title cannot exceed 160 characters")
        String title,
        @NotBlank(message = "Content cannot be blank")
        @Size(max = 4000, message = "Content cannot exceed 4000 characters")
        String content
) {
}

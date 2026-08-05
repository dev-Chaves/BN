package org.acme.domains.announcement.dto;

import java.time.LocalDateTime;

public record ManagerAnnouncement(
        Long id,
        String title,
        String content,
        String author,
        LocalDateTime publishedAt,
        long recipientCount
) {
}

package com.bnfix.ubm.domains.announcement.dto;

import java.time.LocalDateTime;

public record EmployeeAnnouncement(
        Long id,
        String title,
        String content,
        String author,
        LocalDateTime publishedAt,
        boolean read,
        LocalDateTime readAt) {}

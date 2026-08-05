package org.acme.domains.announcement.dto;

import java.util.List;

public record EmployeeAnnouncementPage(
        List<EmployeeAnnouncement> items,
        int page,
        int size,
        boolean hasMore
) {
}

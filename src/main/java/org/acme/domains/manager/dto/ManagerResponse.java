package org.acme.domains.manager.dto;

import java.time.LocalDateTime;

public record ManagerResponse(
        Long id,
        String name,
        Long companyId,
        Boolean active,
        LocalDateTime createdAt
) {
}

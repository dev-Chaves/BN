package com.bn.benefix.manager.dto;

import java.time.LocalDateTime;

public record ManagerCreationResponseDTO(
        Long id,
        String name,
        Long companyId,
        Boolean active,
        LocalDateTime createdAt
) {
}

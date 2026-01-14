package com.bn.benefix.management.dto;

import java.time.LocalDateTime;

public record ManagerCreationResponseDTO(
        Long id,
        String name,
        String cpf,
        Long companyId,
        LocalDateTime createdAt
) {
}

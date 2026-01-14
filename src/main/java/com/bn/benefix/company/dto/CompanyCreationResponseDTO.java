package com.bn.benefix.company.dto;

import java.time.LocalDateTime;

public record CompanyCreationResponseDTO(
        Long id,
        String name,
        String cnpj,
        LocalDateTime createdAt
) {
}
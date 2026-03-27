package org.acme.domains.company.dto;

import java.time.LocalDateTime;

public record CompanyResponse(
        Long id,
        String name,
        String cnpj,
        Boolean active,
        LocalDateTime createdAt
) {
}

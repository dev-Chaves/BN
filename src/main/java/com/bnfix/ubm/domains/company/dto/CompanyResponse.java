package com.bnfix.ubm.domains.company.dto;

import java.time.LocalDateTime;

public record CompanyResponse(
        Long id, String name, String cnpj, Boolean active, Boolean owner, LocalDateTime createdAt) {}

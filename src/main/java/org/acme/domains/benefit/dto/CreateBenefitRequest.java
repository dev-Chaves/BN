package org.acme.domains.benefit.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record CreateBenefitRequest(
        @NotEmpty(message = "Name cannot be null")
        String name,
        @NotEmpty(message = "Description cannot be null")
        String description,
        @NotNull(message = "Provider ID cannot be null")
        Long companyId,
        List<Long> categoryIds
) {
}

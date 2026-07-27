package org.acme.domains.benefit.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.time.LocalDateTime;

public record CreateBenefitRequest(
        @NotEmpty(message = "Name cannot be null")
        String name,
        @NotEmpty(message = "Description cannot be null")
        String description,
        @NotNull(message = "Provider ID cannot be null")
        Long companyId,
        List<Long> categoryIds,
        Boolean publiclyVisible,
        LocalDateTime validFrom,
        LocalDateTime validUntil,
        Integer maxUsesPerUser,
        String terms
) {
    public CreateBenefitRequest(String name, String description, Long companyId, List<Long> categoryIds) {
        this(name, description, companyId, categoryIds, true, null, null, 1, null);
    }
}

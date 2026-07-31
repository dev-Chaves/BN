package org.acme.domains.benefit.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.time.LocalDateTime;

public record CreateBenefitRequest(
        @NotBlank(message = "Name cannot be null") @Size(max = 120)
        String name,
        @NotBlank(message = "Description cannot be null") @Size(max = 500)
        String description,
        @NotNull(message = "Provider ID cannot be null") @Positive
        Long companyId,
        List<@Positive Long> categoryIds,
        Boolean publiclyVisible,
        LocalDateTime validFrom,
        LocalDateTime validUntil,
        @Min(1) Integer maxUsesPerUser,
        @Size(max = 4000) String terms
) {
    public CreateBenefitRequest(String name, String description, Long companyId, List<Long> categoryIds) {
        this(name, description, companyId, categoryIds, true, null, null, 1, null);
    }
}

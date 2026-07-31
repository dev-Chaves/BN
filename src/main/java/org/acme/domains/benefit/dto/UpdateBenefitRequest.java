package org.acme.domains.benefit.dto;

import java.util.List;
import java.time.LocalDateTime;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record UpdateBenefitRequest(
        @Size(max = 120) String name,
        @Size(max = 500) String description,
        List<Long> categoryIds,
        Boolean publiclyVisible,
        LocalDateTime validFrom,
        LocalDateTime validUntil,
        @Min(1) Integer maxUsesPerUser,
        @Size(max = 4000) String terms
) {
    public UpdateBenefitRequest(String name, String description, List<Long> categoryIds) {
        this(name, description, categoryIds, null, null, null, null, null);
    }
}

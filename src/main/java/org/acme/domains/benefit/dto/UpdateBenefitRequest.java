package org.acme.domains.benefit.dto;

import java.util.List;
import java.time.LocalDateTime;

public record UpdateBenefitRequest(
        String name,
        String description,
        List<Long> categoryIds,
        Boolean publiclyVisible,
        LocalDateTime validFrom,
        LocalDateTime validUntil,
        Integer maxUsesPerUser,
        String terms
) {
    public UpdateBenefitRequest(String name, String description, List<Long> categoryIds) {
        this(name, description, categoryIds, null, null, null, null, null);
    }
}

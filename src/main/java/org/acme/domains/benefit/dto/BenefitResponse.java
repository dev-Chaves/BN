package org.acme.domains.benefit.dto;

import org.acme.domains.category.dto.CategoryResponse;

import java.time.LocalDateTime;
import java.util.List;

public record BenefitResponse(
        long id,
        String benefitName,
        String description,
        String nameProvider,
        boolean status,
        boolean publiclyVisible,
        LocalDateTime validUntil,
        Integer maxUsesPerUser,
        String terms,
        LocalDateTime createdAt,
        List<CategoryResponse> categories
) {
}

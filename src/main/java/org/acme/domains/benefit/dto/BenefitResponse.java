package org.acme.domains.benefit.dto;

import org.acme.domains.category.dto.CategoryResponse;

import java.time.LocalDateTime;
import java.util.List;

public record BenefitResponse(
        long id,
        String benefitName,
        String nameProvider,
        boolean status,
        LocalDateTime createdAt,
        List<CategoryResponse> categories
) {
}

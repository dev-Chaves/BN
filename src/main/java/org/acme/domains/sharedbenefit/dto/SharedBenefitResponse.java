package org.acme.domains.sharedbenefit.dto;

import org.acme.domains.category.dto.CategoryResponse;

import java.time.LocalDateTime;
import java.util.List;

public record SharedBenefitResponse(
        Long id,
        Long subscriptionId,
        String name,
        String description,
        String providerName,
        List<CategoryResponse> categories,
        LocalDateTime validUntil,
        Integer maxUsesPerUser,
        String terms,
        String accessStatus
) {}

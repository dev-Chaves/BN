package com.bnfix.ubm.domains.sharedbenefit.dto;

import com.bnfix.ubm.domains.category.dto.CategoryResponse;
import java.time.LocalDateTime;
import java.util.List;

public record SharedBenefitResponse(
        Long benefitId,
        Long subscriptionId,
        String benefitName,
        String description,
        String providerName,
        List<CategoryResponse> categories,
        LocalDateTime validUntil,
        Integer maxUsesPerUser,
        String terms,
        String accessStatus) {}

package com.bnfix.ubm.domains.benefit.dto;

import com.bnfix.ubm.domains.category.dto.CategoryResponse;
import java.time.LocalDateTime;
import java.util.List;

public record EmployeeBenefitResponse(
        Long benefitId,
        String benefitName,
        String description,
        String providerName,
        List<CategoryResponse> categories,
        LocalDateTime validUntil,
        Integer maxUsesPerUser,
        long usedCount,
        long remainingUses,
        String terms) {}

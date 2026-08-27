package com.bnfix.ubm.domains.benefit.dto;

import jakarta.validation.constraints.*;
import java.time.LocalDateTime;
import java.util.List;

public record UpdateBenefitRequest(
        @Size(max = 120) String name,
        @Size(max = 500) String description,
        List<Long> categoryIds,
        Boolean publiclyVisible,
        LocalDateTime validFrom,
        LocalDateTime validUntil,
        @Min(1) Integer maxUsesPerUser,
        @Size(max = 4000) String terms,
        Boolean availableToProviderEmployees) {}

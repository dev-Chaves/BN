package com.bnfix.ubm.domains.partnership.dto;

import com.bnfix.ubm.domains.partnership.PartnershipStatus;
import java.time.LocalDateTime;

public record PartnershipResponse(
        Long id,
        Long clientCompanyId,
        String clientCompanyName,
        Long benefitId,
        String benefitName,
        PartnershipStatus status,
        LocalDateTime createdAt) {}

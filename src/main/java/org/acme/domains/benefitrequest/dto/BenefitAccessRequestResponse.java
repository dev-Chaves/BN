package org.acme.domains.benefitrequest.dto;

import org.acme.domains.benefitrequest.BenefitAccessRequestStatus;

import java.time.LocalDateTime;

public record BenefitAccessRequestResponse(
        Long id,
        Long benefitId,
        String benefitName,
        String providerName,
        Long employeeId,
        String employeeName,
        String employeeCompanyName,
        BenefitAccessRequestStatus status,
        LocalDateTime requestedAt,
        LocalDateTime reviewedAt,
        String rejectionReason
) {}

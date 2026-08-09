package com.bnfix.ubm.domains.benefitrequest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RejectBenefitAccessRequest(
        @NotBlank @Size(max = 500) String reason) {}

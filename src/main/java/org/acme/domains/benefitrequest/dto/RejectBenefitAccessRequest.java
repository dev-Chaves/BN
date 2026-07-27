package org.acme.domains.benefitrequest.dto;

import jakarta.validation.constraints.Size;

public record RejectBenefitAccessRequest(
        @Size(max = 500) String reason
) {}

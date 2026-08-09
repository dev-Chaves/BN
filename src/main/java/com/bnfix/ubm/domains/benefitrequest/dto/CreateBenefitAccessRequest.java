package com.bnfix.ubm.domains.benefitrequest.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreateBenefitAccessRequest(@NotNull @Positive Long benefitId) {}

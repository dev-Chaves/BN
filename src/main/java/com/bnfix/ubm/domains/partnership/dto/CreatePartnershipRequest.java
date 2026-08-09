package com.bnfix.ubm.domains.partnership.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreatePartnershipRequest(@NotNull @Positive Long benefitId) {}

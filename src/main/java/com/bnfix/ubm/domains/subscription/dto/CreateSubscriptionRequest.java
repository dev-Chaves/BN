package com.bnfix.ubm.domains.subscription.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreateSubscriptionRequest(@NotNull @Positive Long benefitId) {}

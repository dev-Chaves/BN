package com.bnfix.ubm.domains.auth.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record SwitchCompanyRequest(@NotNull @Positive Long companyId) {}

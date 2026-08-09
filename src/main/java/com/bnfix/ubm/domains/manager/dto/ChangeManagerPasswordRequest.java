package com.bnfix.ubm.domains.manager.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangeManagerPasswordRequest(
        @NotBlank(message = "Current password cannot be null") @Size(max = 72)
        String currentPassword,

        @NotBlank(message = "New password cannot be null") @Size(min = 10, max = 72)
        String newPassword) {}

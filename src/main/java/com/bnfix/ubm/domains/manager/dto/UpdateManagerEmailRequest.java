package com.bnfix.ubm.domains.manager.dto;
import jakarta.validation.constraints.*;
public record UpdateManagerEmailRequest(@NotBlank(message = "Email cannot be null") @Email @Size(max = 255) String email,
                                        @NotBlank(message = "Current password cannot be null") @Size(max = 72) String currentPassword) {}

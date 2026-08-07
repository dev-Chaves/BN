package com.bnfix.ubm.domains.benefit.dto;
import jakarta.validation.constraints.*; import java.time.LocalDateTime; import java.util.List;
public record CreateBenefitRequest(@NotBlank @Size(max=120) String name,@NotBlank @Size(max=500) String description,@NotNull @Positive Long companyId,List<@Positive Long> categoryIds,Boolean publiclyVisible,LocalDateTime validFrom,LocalDateTime validUntil,@Min(1) Integer maxUsesPerUser,@Size(max=4000) String terms) {}

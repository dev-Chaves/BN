package com.bnfix.ubm.domains.benefit.dto;
import com.bnfix.ubm.domains.category.dto.CategoryResponse; import java.time.LocalDateTime; import java.util.List;
public record BenefitResponse(Long id,String benefitName,String description,String nameProvider,Boolean status,Boolean publiclyVisible,LocalDateTime validUntil,Integer maxUsesPerUser,String terms,LocalDateTime createdAt,List<CategoryResponse> categories) {}

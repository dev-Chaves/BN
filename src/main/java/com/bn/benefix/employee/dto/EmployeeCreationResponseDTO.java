package com.bn.benefix.employee.dto;

import com.bn.benefix.employee.EmployeeStatus;

import java.time.LocalDateTime;

public record EmployeeCreationResponseDTO(
        Long id,
        String name,
        Long companyId,
        EmployeeStatus active,
        LocalDateTime createdAt
) {
}

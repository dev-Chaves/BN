package org.acme.domains.employee.dto;

import org.acme.domains.employee.EmployeeStatus;

import java.time.LocalDateTime;

public record EmployeeResponse(
        Long id,
        String name,
        Long companyId,
        EmployeeStatus active,
        LocalDateTime createdAt
) {
}

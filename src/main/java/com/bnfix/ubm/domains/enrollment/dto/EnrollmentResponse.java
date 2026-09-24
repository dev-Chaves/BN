package com.bnfix.ubm.domains.enrollment.dto;

import com.bnfix.ubm.domains.employee.EmployeeStatus;

public record EnrollmentResponse(
        Long employeeId, String name, Long companyId, String companyName, EmployeeStatus status) {}

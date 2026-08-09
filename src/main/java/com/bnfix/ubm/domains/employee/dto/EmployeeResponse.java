package com.bnfix.ubm.domains.employee.dto;

import com.bnfix.ubm.domains.employee.EmployeeStatus;
import java.time.LocalDateTime;

public record EmployeeResponse(Long id, String name, Long companyId, EmployeeStatus active, LocalDateTime createdAt) {}

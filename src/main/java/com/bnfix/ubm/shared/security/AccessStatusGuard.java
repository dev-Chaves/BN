package com.bnfix.ubm.shared.security;

import com.bnfix.ubm.domains.employee.Employee;
import com.bnfix.ubm.domains.employee.EmployeeStatus;
import com.bnfix.ubm.domains.manager.Manager;

public final class AccessStatusGuard {
    private AccessStatusGuard() {}
    public static Manager requireActive(Manager manager) {
        if (manager == null || !Boolean.TRUE.equals(manager.getActive())) throw new SecurityException("Account is disabled");
        requireCompany(manager.getCompany() == null ? null : manager.getCompany().getActive());
        return manager;
    }
    public static Employee requireActive(Employee employee) {
        if (employee == null || employee.getActive() != EmployeeStatus.ACTIVE) throw new SecurityException("Account is disabled");
        requireCompany(employee.getCompany() == null ? null : employee.getCompany().getActive());
        return employee;
    }
    private static void requireCompany(Boolean active) {
        if (!Boolean.TRUE.equals(active)) throw new SecurityException("Company is inactive");
    }
}

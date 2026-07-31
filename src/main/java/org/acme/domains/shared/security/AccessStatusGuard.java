package org.acme.domains.shared.security;

import org.acme.domains.employee.Employee;
import org.acme.domains.employee.EmployeeStatus;
import org.acme.domains.manager.Manager;

public final class AccessStatusGuard {
    private AccessStatusGuard() {
    }

    public static Employee requireActive(Employee employee) {
        if (employee == null || employee.getActive() != EmployeeStatus.ACTIVE) {
            throw new SecurityException("Account is disabled");
        }
        requireActiveCompany(employee.getCompany() == null ? null : employee.getCompany().getActive());
        return employee;
    }

    public static Manager requireActive(Manager manager) {
        if (manager == null || !Boolean.TRUE.equals(manager.getActive())) {
            throw new SecurityException("Account is disabled");
        }
        requireActiveCompany(manager.getCompany() == null ? null : manager.getCompany().getActive());
        return manager;
    }

    private static void requireActiveCompany(Boolean active) {
        if (!Boolean.TRUE.equals(active)) {
            throw new SecurityException("Company is inactive");
        }
    }
}

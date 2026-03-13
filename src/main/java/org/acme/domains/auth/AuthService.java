package org.acme.domains.auth;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.acme.domains.auth.dto.LoginRequest;
import org.acme.domains.employee.EmployeeRepository;
import org.acme.domains.manager.Manager;
import org.acme.domains.manager.ManagerRepository;

@ApplicationScoped
public class AuthService {

    private final ManagerRepository managerRepository;

    private final EmployeeRepository employeeRepository;

    public AuthService(ManagerRepository managerRepository, EmployeeRepository employeeRepository) {
        this.managerRepository = managerRepository;
        this.employeeRepository = employeeRepository;
    }

}

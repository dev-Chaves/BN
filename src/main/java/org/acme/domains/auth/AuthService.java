package org.acme.domains.auth;

import io.quarkus.elytron.security.common.BcryptUtil;
import io.smallrye.jwt.build.Jwt;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.NotFoundException;
import org.acme.domains.account.Account;
import org.acme.domains.account.AccountRepository;
import org.acme.domains.auth.dto.LoginRequest;
import org.acme.domains.auth.dto.LoginResponse;
import org.acme.domains.employee.Employee;
import org.acme.domains.employee.EmployeeRepository;
import org.acme.domains.employee.EmployeeStatus;
import org.acme.domains.manager.ManagerRepository;
import org.acme.domains.shared.enums.Role;

import java.time.Duration;
import java.util.Set;
import java.util.UUID;

@ApplicationScoped
public class AuthService {

    private final EmployeeRepository employeeRepository;
    private final AccountRepository accountRepository;
    private final ManagerRepository managerRepository;

    public AuthService(EmployeeRepository employeeRepository, AccountRepository accountRepository, ManagerRepository managerRepository) {
        this.employeeRepository = employeeRepository;
        this.accountRepository = accountRepository;
        this.managerRepository = managerRepository;
    }

    public Uni<LoginResponse> login(LoginRequest loginRequest) {

        return validateAccount(loginRequest.email())
                .flatMap(account -> validatePassword(loginRequest.password(), account).replaceWith(account))
                .flatMap(account -> switch (account.getRole()){
                    case USER -> validateEmployee(account.id)
                            .flatMap(this::verifyDisabledEmployee)
                            .replaceWith(account);
                    case ADMIN -> Uni.createFrom().item(account);
                    case MANAGER -> validateManager(account);
                    default ->  Uni.createFrom().failure(new IllegalStateException("Invalid role"));
                })
                .flatMap(account -> generateToken(account.getEmail(), account.getRole()));
    }

    private Uni<LoginResponse> generateToken(String email, Role role) {
        String token = Jwt.issuer("bn-api")
                .upn(email)
                .subject(email)
                .groups(Set.of(role.name()))
                .expiresIn(Duration.ofHours(3))
                .sign();

        return Uni.createFrom().item(token).onItem().transform(LoginResponse::new);
    }

    private Uni<Account> validateManager(Account account){
        return managerRepository.findByAccountId(account.id)
                .onItem().ifNull().failWith(() -> new NotFoundException("Manager not found"))
                .replaceWith(account);
    }

    private Uni<Account> validateAccount(String email) {
        return accountRepository.findByEmail(email).onItem().ifNull().failWith(() -> new NotFoundException("Account not found"));
    }

    private Uni<Employee> validateEmployee(UUID id ) {
        return employeeRepository.findByAccountId(id).onItem().ifNull().failWith(() -> new NotFoundException("Employee not found"));
    }

    private Uni<Employee> verifyDisabledEmployee(Employee employee) {
        if(employee.getActive().equals(EmployeeStatus.DISABLED)){
            return Uni.createFrom().failure(new NotFoundException("Employee is disabled"));
        }
        return Uni.createFrom().item(employee);
    }

    private Uni<Void> validatePassword(String password, Account account) {

        if(!BcryptUtil.matches(password, account.getPassword())){
            return Uni.createFrom().failure(new IllegalStateException("Invalid credentials"));
        }
        return Uni.createFrom().voidItem();
    }

}

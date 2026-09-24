package com.bnfix.ubm.domains.enrollment;

import com.bnfix.ubm.domains.account.Account;
import com.bnfix.ubm.domains.account.AccountRepository;
import com.bnfix.ubm.domains.company.Company;
import com.bnfix.ubm.domains.company.CompanyRepository;
import com.bnfix.ubm.domains.employee.Employee;
import com.bnfix.ubm.domains.employee.EmployeeRepository;
import com.bnfix.ubm.domains.enrollment.dto.EnrollmentRequest;
import com.bnfix.ubm.domains.enrollment.dto.EnrollmentResponse;
import com.bnfix.ubm.domains.shared.domain.CPF;
import com.bnfix.ubm.domains.shared.enums.Role;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Service
public class EnrollmentService {
    private final AccountRepository accountRepository;
    private final CompanyRepository companyRepository;
    private final EmployeeRepository employeeRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public EnrollmentService(
            AccountRepository accountRepository,
            CompanyRepository companyRepository,
            EmployeeRepository employeeRepository,
            BCryptPasswordEncoder passwordEncoder) {
        this.accountRepository = accountRepository;
        this.companyRepository = companyRepository;
        this.employeeRepository = employeeRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public EnrollmentResponse enroll(EnrollmentRequest request, Long companyId) {
        if (companyId == null || companyId <= 0) throw notFound("Enrollment not available");
        Company company = companyRepository
                .findById(companyId)
                .filter(candidate -> Boolean.TRUE.equals(candidate.getActive()))
                .orElseThrow(() -> notFound("Enrollment not available"));
        if (accountRepository.findByEmail(request.email()).isPresent()) throw conflict("Email already in use");
        if (accountRepository.findByCPF(request.cpf()).isPresent()) throw conflict("CPF already in use");
        Account account = accountRepository.save(Account.builder(
                        request.name(),
                        CPF.of(request.cpf()),
                        passwordEncoder.encode(request.password()),
                        request.email(),
                        Role.USER)
                .build());
        Employee employee = Employee.builder(request.name(), company, account).build();
        employee.active();
        employee = employeeRepository.save(employee);
        log.info("Employee {} enrolled in company {} ({})", employee.id, company.id, account.getEmail());
        return new EnrollmentResponse(
                employee.id, employee.getName(), company.id, company.getName(), employee.getActive());
    }

    private ResponseStatusException notFound(String message) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, message);
    }

    private ResponseStatusException conflict(String message) {
        return new ResponseStatusException(HttpStatus.CONFLICT, message);
    }
}

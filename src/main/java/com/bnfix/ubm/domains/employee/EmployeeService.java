package com.bnfix.ubm.domains.employee;

import com.bnfix.ubm.domains.account.*;
import com.bnfix.ubm.domains.company.*;
import com.bnfix.ubm.domains.employee.dto.*;
import com.bnfix.ubm.domains.manager.*;
import com.bnfix.ubm.domains.redemption.RedemptionTokenRepository;
import com.bnfix.ubm.domains.shared.domain.CPF;
import com.bnfix.ubm.domains.shared.enums.Role;
import com.bnfix.ubm.domains.subscription.CompanyBenefitAssignmentService;
import com.bnfix.ubm.shared.security.TenantGuard;
import jakarta.transaction.Transactional;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Service
public class EmployeeService {
    private final AccountRepository accountRepository;
    private final CompanyRepository companyRepository;
    private final ManagerRepository managerRepository;
    private final EmployeeRepository employeeRepository;
    private final TenantGuard tenantGuard;
    private final RedemptionTokenRepository redemptionTokenRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final CompanyBenefitAssignmentService companyBenefitAssignmentService;

    public EmployeeService(
            AccountRepository accountRepository,
            CompanyRepository companyRepository,
            ManagerRepository managerRepository,
            EmployeeRepository employeeRepository,
            TenantGuard tenantGuard,
            RedemptionTokenRepository redemptionTokenRepository,
            BCryptPasswordEncoder passwordEncoder,
            CompanyBenefitAssignmentService companyBenefitAssignmentService) {
        this.accountRepository = accountRepository;
        this.companyRepository = companyRepository;
        this.managerRepository = managerRepository;
        this.employeeRepository = employeeRepository;
        this.tenantGuard = tenantGuard;
        this.redemptionTokenRepository = redemptionTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.companyBenefitAssignmentService = companyBenefitAssignmentService;
    }

    @Transactional
    public EmployeeResponse create(CreateEmployeeRequest request, String email, Long tenantId) {
        if (accountRepository.findByEmail(request.email()).isPresent()) throw conflict("Email already in use");
        if (accountRepository.findByCPF(request.cpf()).isPresent()) throw conflict("CPF already in use");
        Manager manager = manager(email, tenantId);
        Company company = tenantGuard.verifyManagerCompanyAccess(manager, request.companyId());
        Account account = accountRepository.save(Account.builder(
                        request.name(),
                        CPF.of(request.cpf()),
                        passwordEncoder.encode(request.password()),
                        request.email(),
                        Role.USER)
                .build());
        Employee employee = employeeRepository.save(
                Employee.builder(request.name(), company, account).build());
        companyBenefitAssignmentService.assignActiveCompanyBenefits(employee);
        log.info("Employee {} created by manager {} in company {}", employee.id, manager.id, company.id);
        return response(employee);
    }

    @Transactional
    public EmployeeResponse disable(Long id, String email, Long company) {
        Manager manager = manager(email, company);
        Employee employee = tenantGuard.verifyManagerEmployeeAccess(manager, find(id));
        employee.disable();
        redemptionTokenRepository.revokeActiveByEmployee(employee.id);
        log.info("Employee {} disabled by manager {}", id, manager.id);
        return response(employee);
    }

    @Transactional
    public EmployeeResponse activate(Long id, String email, Long company) {
        Manager manager = manager(email, company);
        Employee employee = tenantGuard.verifyManagerEmployeeAccess(manager, find(id));
        employee.active();
        log.info("Employee {} activated by manager {}", id, manager.id);
        return response(employee);
    }

    @Transactional
    public EmployeeResponse update(Long id, UpdateEmployeeRequest request, String email, Long company) {
        Employee employee = tenantGuard.verifyManagerEmployeeAccess(manager(email, company), find(id));
        employee.update(request.name());
        log.info("Employee {} updated by manager {}", id, manager(email, company).id);
        return response(employee);
    }

    @Transactional
    public List<EmployeeResponse> list(String email, Long company, int page, int size) {
        Company companyAccess = tenantGuard.verifyManagerCompanyAccess(manager(email, company), company);
        return employeeRepository
                .findByCompanyId(companyAccess.id, Math.max(0, page), Math.max(1, Math.min(size, 100)))
                .stream()
                .map(this::response)
                .toList();
    }

    private Manager manager(String email, Long companyId) {
        return managerRepository
                .findByEmailAndCompanyId(email, companyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Manager not Found"));
    }

    private Employee find(Long id) {
        return employeeRepository
                .findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Employee not Found"));
    }

    private EmployeeResponse response(Employee employee) {
        return new EmployeeResponse(
                employee.id,
                employee.getName(),
                employee.getCompany().id,
                employee.getActive(),
                employee.getCreatedAt());
    }

    private ResponseStatusException conflict(String message) {
        return new ResponseStatusException(HttpStatus.CONFLICT, message);
    }
}

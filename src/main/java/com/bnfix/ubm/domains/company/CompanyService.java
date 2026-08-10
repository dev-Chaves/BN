package com.bnfix.ubm.domains.company;

import com.bnfix.ubm.domains.account.Account;
import com.bnfix.ubm.domains.account.AccountRepository;
import com.bnfix.ubm.domains.company.dto.*;
import com.bnfix.ubm.domains.employee.EmployeeRepository;
import com.bnfix.ubm.domains.manager.Manager;
import com.bnfix.ubm.domains.manager.ManagerRepository;
import com.bnfix.ubm.domains.partnership.PartnershipRepository;
import com.bnfix.ubm.domains.redemption.RedemptionTokenRepository;
import com.bnfix.ubm.domains.shared.domain.CNPJ;
import com.bnfix.ubm.domains.shared.enums.Role;
import com.bnfix.ubm.shared.security.AccessStatusGuard;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Service
public class CompanyService {
    private final CompanyRepository companyRepository;
    private final AccountRepository accountRepository;
    private final ManagerRepository managerRepository;
    private final EmployeeRepository employeeRepository;
    private final PartnershipRepository partnershipRepository;
    private final RedemptionTokenRepository redemptionTokenRepository;
    private final EntityManager entityManager;
    private final BCryptPasswordEncoder passwordEncoder;

    public CompanyService(
            CompanyRepository companyRepository,
            AccountRepository accountRepository,
            ManagerRepository managerRepository,
            EmployeeRepository employeeRepository,
            PartnershipRepository partnershipRepository,
            RedemptionTokenRepository redemptionTokenRepository,
            EntityManager entityManager,
            BCryptPasswordEncoder passwordEncoder) {
        this.companyRepository = companyRepository;
        this.accountRepository = accountRepository;
        this.managerRepository = managerRepository;
        this.employeeRepository = employeeRepository;
        this.partnershipRepository = partnershipRepository;
        this.redemptionTokenRepository = redemptionTokenRepository;
        this.entityManager = entityManager;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public List<CompanyResponse> listMine(String email) {
        return managerRepository.findActiveByEmail(email).stream()
                .map(this::response)
                .toList();
    }

    @Transactional
    public CompanyResponse getMine(String email, Long companyId) {
        return response(activeManager(email, companyId));
    }

    @Transactional
    public CompanyResponse create(CreateCompanyRequest request, String email) {
        Account account = accountRepository.findByEmail(email).orElseThrow(() -> notFound("Account not found"));
        if (account.getRole() != Role.MANAGER)
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Account cannot manage companies");
        if (companyRepository.findByCNPJ(request.cnpj()).isPresent())
            throw new ResponseStatusException(HttpStatus.CONFLICT, "CNPJ already registered");
        Company company = companyRepository.save(
                Company.builder(request.name(), CNPJ.of(request.cnpj())).build());
        Manager manager = managerRepository.save(Manager.builder(account.getName(), company, account)
                .companyOwner()
                .build());
        log.info("Company {} created by manager {}", company.id, manager.id);
        return response(manager);
    }

    @Transactional
    public CompanyResponse update(String email, Long id, UpdateCompanyRequest request) {
        Manager manager = activeManager(email, id);
        manager.getCompany().update(request.name());
        log.info("Company {} updated by manager {}", id, manager.id);
        return response(manager);
    }

    @Transactional
    public CompanyResponse deactivate(String email, Long id, DeactivateCompanyRequest request) {
        Manager manager = activeManager(email, id);
        if (!Boolean.TRUE.equals(manager.getCompanyOwner()))
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Only the company owner can deactivate the company");
        if (!passwordEncoder.matches(request.password(), manager.getAccount().getPassword()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password is incorrect");
        Company company = manager.getCompany();
        company.deactivateCompany();
        entityManager
                .createQuery(
                        "update BenefitAccessRequest r set r.status = com.bnfix.ubm.domains.benefitrequest.BenefitAccessRequestStatus.CANCELLED, r.reviewedAt = :now where r.employee.company.id = :id and r.status = com.bnfix.ubm.domains.benefitrequest.BenefitAccessRequestStatus.PENDING")
                .setParameter("now", LocalDateTime.now())
                .setParameter("id", id)
                .executeUpdate();
        managerRepository.deactivateByCompanyId(id);
        employeeRepository.disableByCompanyId(id);
        entityManager
                .createQuery("update Benefit b set b.active = false where b.provider.id = :id")
                .setParameter("id", id)
                .executeUpdate();
        partnershipRepository.disableByCompanyId(id);
        redemptionTokenRepository.revokeActiveByCompanyId(id);
        log.info("Company {} deactivated by manager {}", id, manager.id);
        return response(manager);
    }

    private Manager activeManager(String email, Long companyId) {
        Manager manager = managerRepository
                .findByEmailAndCompanyId(email, companyId)
                .orElseThrow(() -> notFound("Company not found"));
        return AccessStatusGuard.requireActive(manager);
    }

    private CompanyResponse response(Manager manager) {
        Company company = manager.getCompany();
        return new CompanyResponse(
                company.id,
                company.getName(),
                company.getCnpj().getValue(),
                company.getActive(),
                manager.getCompanyOwner(),
                company.getCreatedAt());
    }

    private ResponseStatusException notFound(String message) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, message);
    }
}

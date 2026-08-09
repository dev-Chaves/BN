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
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CompanyService {
    private final CompanyRepository companies;
    private final AccountRepository accounts;
    private final ManagerRepository managers;
    private final EmployeeRepository employees;
    private final PartnershipRepository partnerships;
    private final RedemptionTokenRepository tokens;
    private final EntityManager entityManager;
    private final BCryptPasswordEncoder passwords;

    public CompanyService(
            CompanyRepository companies,
            AccountRepository accounts,
            ManagerRepository managers,
            EmployeeRepository employees,
            PartnershipRepository partnerships,
            RedemptionTokenRepository tokens,
            EntityManager entityManager,
            BCryptPasswordEncoder passwords) {
        this.companies = companies;
        this.accounts = accounts;
        this.managers = managers;
        this.employees = employees;
        this.partnerships = partnerships;
        this.tokens = tokens;
        this.entityManager = entityManager;
        this.passwords = passwords;
    }

    @Transactional
    public List<CompanyResponse> listMine(String email) {
        return managers.findActiveByEmail(email).stream().map(this::response).toList();
    }

    @Transactional
    public CompanyResponse getMine(String email, Long companyId) {
        return response(activeManager(email, companyId));
    }

    @Transactional
    public CompanyResponse create(CreateCompanyRequest request, String email) {
        Account account = accounts.findByEmail(email).orElseThrow(() -> notFound("Account not found"));
        if (account.getRole() != Role.MANAGER)
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Account cannot manage companies");
        if (companies.findByCNPJ(request.cnpj()).isPresent())
            throw new ResponseStatusException(HttpStatus.CONFLICT, "CNPJ already registered");
        Company company = companies.save(
                Company.builder(request.name(), CNPJ.of(request.cnpj())).build());
        return response(managers.save(Manager.builder(account.getName(), company, account)
                .companyOwner()
                .build()));
    }

    @Transactional
    public CompanyResponse update(String email, Long id, UpdateCompanyRequest request) {
        Manager manager = activeManager(email, id);
        manager.getCompany().update(request.name());
        return response(manager);
    }

    @Transactional
    public CompanyResponse deactivate(String email, Long id, DeactivateCompanyRequest request) {
        Manager manager = activeManager(email, id);
        if (!Boolean.TRUE.equals(manager.getCompanyOwner()))
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Only the company owner can deactivate the company");
        if (!passwords.matches(request.password(), manager.getAccount().getPassword()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password is incorrect");
        Company company = manager.getCompany();
        company.deactivateCompany();
        entityManager
                .createQuery(
                        "update BenefitAccessRequest r set r.status = com.bnfix.ubm.domains.benefitrequest.BenefitAccessRequestStatus.CANCELLED, r.reviewedAt = :now where r.employee.company.id = :id and r.status = com.bnfix.ubm.domains.benefitrequest.BenefitAccessRequestStatus.PENDING")
                .setParameter("now", LocalDateTime.now())
                .setParameter("id", id)
                .executeUpdate();
        managers.deactivateByCompanyId(id);
        employees.disableByCompanyId(id);
        entityManager
                .createQuery("update Benefit b set b.active = false where b.provider.id = :id")
                .setParameter("id", id)
                .executeUpdate();
        partnerships.disableByCompanyId(id);
        tokens.revokeActiveByCompanyId(id);
        return response(manager);
    }

    private Manager activeManager(String email, Long companyId) {
        Manager manager =
                managers.findByEmailAndCompanyId(email, companyId).orElseThrow(() -> notFound("Company not found"));
        return AccessStatusGuard.requireActive(manager);
    }

    private CompanyResponse response(Manager manager) {
        Company c = manager.getCompany();
        return new CompanyResponse(
                c.id, c.getName(), c.getCnpj().getValue(), c.getActive(), manager.getCompanyOwner(), c.getCreatedAt());
    }

    private ResponseStatusException notFound(String message) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, message);
    }
}

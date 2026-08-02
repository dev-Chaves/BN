package org.acme.domains.company;

import io.quarkus.elytron.security.common.BcryptUtil;
import io.quarkus.hibernate.reactive.panache.common.WithSession;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import org.acme.domains.account.Account;
import org.acme.domains.account.AccountRepository;
import org.acme.domains.benefit.BenefitRepository;
import org.acme.domains.benefitrequest.BenefitAccessRequestRepository;
import org.acme.domains.company.dto.CompanyResponse;
import org.acme.domains.company.dto.CreateCompanyRequest;
import org.acme.domains.company.dto.DeactivateCompanyRequest;
import org.acme.domains.employee.EmployeeRepository;
import org.acme.domains.manager.Manager;
import org.acme.domains.manager.ManagerRepository;
import org.acme.domains.partnership.PartnershipRepository;
import org.acme.domains.redemption.RedemptionTokenRepository;
import org.acme.domains.shared.domain.CNPJ;
import org.acme.domains.shared.enums.Role;
import org.acme.domains.shared.security.AccessStatusGuard;

import java.time.LocalDateTime;
import java.util.List;

@ApplicationScoped
public class CompanyService {

    private final CompanyRepository companyRepository;
    private final AccountRepository accountRepository;
    private final ManagerRepository managerRepository;
    private final EmployeeRepository employeeRepository;
    private final BenefitRepository benefitRepository;
    private final BenefitAccessRequestRepository benefitAccessRequestRepository;
    private final PartnershipRepository partnershipRepository;
    private final RedemptionTokenRepository redemptionTokenRepository;

    public CompanyService(
            CompanyRepository companyRepository,
            AccountRepository accountRepository,
            ManagerRepository managerRepository,
            EmployeeRepository employeeRepository,
            BenefitRepository benefitRepository,
            BenefitAccessRequestRepository benefitAccessRequestRepository,
            PartnershipRepository partnershipRepository,
            RedemptionTokenRepository redemptionTokenRepository
    ) {
        this.companyRepository = companyRepository;
        this.accountRepository = accountRepository;
        this.managerRepository = managerRepository;
        this.employeeRepository = employeeRepository;
        this.benefitRepository = benefitRepository;
        this.benefitAccessRequestRepository = benefitAccessRequestRepository;
        this.partnershipRepository = partnershipRepository;
        this.redemptionTokenRepository = redemptionTokenRepository;
    }

    @WithSession
    public Uni<List<CompanyResponse>> listActiveByManagerEmail(String managerEmail) {
        return managerRepository.findActiveByEmail(managerEmail)
                .map(managers -> managers.stream()
                        .map(this::toResponse)
                        .toList());
    }

    @WithSession
    public Uni<CompanyResponse> getByManagerEmailAndCompanyId(String managerEmail, Long companyId) {
        return managerRepository.findByEmailAndCompanyId(managerEmail, companyId)
                .onItem().ifNull().failWith(() -> new NotFoundException("Company not found"))
                .map(AccessStatusGuard::requireActive)
                .map(this::toResponse);
    }

    @WithTransaction
    public Uni<CompanyResponse> createForCurrentAccount(CreateCompanyRequest request, String accountEmail) {
        return accountRepository.findByEmail(accountEmail)
                .onItem().ifNull().failWith(() -> new NotFoundException("Account not found"))
                .flatMap(account -> requireManagerAccount(account)
                        .replaceWith(account))
                .flatMap(account -> companyRepository.findByCNPJ(request.cnpj())
                        .flatMap(existing -> {
                            if (existing != null) {
                                return Uni.createFrom().failure(new IllegalStateException("CNPJ already registered"));
                            }

                            Company company = Company.builder(request.name(), CNPJ.of(request.cnpj())).build();
                            return companyRepository.persist(company)
                                    .flatMap(persistedCompany -> managerRepository.persist(
                                            Manager.builder(account.getName(), persistedCompany, account)
                                                    .companyOwner()
                                                    .build()));
                        }))
                .map(this::toResponse);
    }

    @WithTransaction
    public Uni<CompanyResponse> deactivateCurrent(
            String managerEmail,
            Long companyId,
            DeactivateCompanyRequest request
    ) {
        return managerRepository.findByEmailAndCompanyId(managerEmail, companyId)
                .onItem().ifNull().failWith(() -> new NotFoundException("Company not found"))
                .map(AccessStatusGuard::requireActive)
                .map(this::requireCompanyOwner)
                .flatMap(manager -> requirePassword(manager, request.password()))
                .invoke(manager -> manager.getCompany().deactivateCompany())
                .call(() -> benefitAccessRequestRepository.cancelPendingByCompanyId(companyId, LocalDateTime.now()))
                .call(() -> managerRepository.deactivateByCompanyId(companyId))
                .call(() -> employeeRepository.disableByCompanyId(companyId))
                .call(() -> benefitRepository.deactivateByProviderId(companyId))
                .call(() -> partnershipRepository.disableByCompanyId(companyId))
                .call(() -> redemptionTokenRepository.revokeActiveByCompanyId(companyId))
                .map(this::toResponse);
    }

    private Manager requireCompanyOwner(Manager manager) {
        if (!Boolean.TRUE.equals(manager.getCompanyOwner())) {
            throw new SecurityException("Only the company owner can deactivate the company");
        }
        return manager;
    }

    private Uni<Void> requireManagerAccount(Account account) {
        if (account.getRole() != Role.MANAGER) {
            return Uni.createFrom().failure(new SecurityException("Account cannot manage companies"));
        }
        return Uni.createFrom().voidItem();
    }

    private Uni<Manager> requirePassword(Manager manager, String password) {
        if (!BcryptUtil.matches(password, manager.getAccount().getPassword())) {
            return Uni.createFrom().failure(new BadRequestException("Password is incorrect"));
        }
        return Uni.createFrom().item(manager);
    }

    private CompanyResponse toResponse(Manager manager) {
        Company company = manager.getCompany();
        return new CompanyResponse(
                company.id,
                company.getName(),
                company.getCnpj().getValue(),
                company.getActive(),
                manager.getCompanyOwner(),
                company.getCreatedAt()
        );
    }
}

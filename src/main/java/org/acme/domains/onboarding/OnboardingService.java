package org.acme.domains.onboarding;

import jakarta.inject.Inject;
import org.acme.domains.account.Account;
import org.acme.domains.account.AccountRepository;
import org.acme.domains.company.Company;
import org.acme.domains.company.CompanyRepository;
import org.acme.domains.manager.Manager;
import org.acme.domains.manager.ManagerRepository;
import org.acme.domains.onboarding.dto.OnboardingRequest;
import org.acme.domains.onboarding.dto.OnboardingResponse;
import org.acme.domains.shared.domain.CNPJ;
import org.acme.domains.shared.domain.CPF;
import org.acme.domains.shared.enums.Role;

import io.quarkus.elytron.security.common.BcryptUtil;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.BadRequestException;

@ApplicationScoped
public class OnboardingService {

    private final AccountRepository accountRepository;
    private final ManagerRepository managerRepository;
    private final CompanyRepository companyRepository;

    @Inject
    public OnboardingService(AccountRepository accountRepository, ManagerRepository managerRepository, CompanyRepository companyRepository) {
        this.accountRepository = accountRepository;
        this.managerRepository = managerRepository;
        this.companyRepository = companyRepository;
    }

    @WithTransaction
    public Uni<OnboardingResponse> onboardingCompany(OnboardingRequest request) {
        return validateCnpjNotRegistered(request.company().cnpj())
                .flatMap(unused -> resolveManagerAccount(request.manager()))
                .flatMap(accountResolution -> {
                    CNPJ cnpj = CNPJ.of(request.company().cnpj());
                    Company newCompany = Company.builder(request.company().name(), cnpj).build();
                    Manager manager = Manager.builder(
                            request.manager().name(),
                            newCompany,
                            accountResolution.account()
                    ).companyOwner().build();

                    return persistAccountIfNecessary(accountResolution)
                            .call(() -> persistCompany(newCompany))
                            .call(() -> persistManager(manager))
                            .map(ignored -> new OnboardingResponse(
                                    newCompany.id,
                                    manager.id,
                                    newCompany.getCnpj().getValue(),
                                    newCompany.getName(),
                                    manager.getName()
                            ));
                });
    }

    private Uni<AccountResolution> resolveManagerAccount(OnboardingRequest.ManagerRegistrationData managerData) {
        return accountRepository.findByEmail(managerData.email())
                .flatMap(byEmail -> accountRepository.findByCPF(managerData.cpf())
                        .map(byCpf -> resolveManagerAccount(managerData, byEmail, byCpf)));
    }

    private AccountResolution resolveManagerAccount(
            OnboardingRequest.ManagerRegistrationData managerData,
            Account byEmail,
            Account byCpf
    ) {
        if (byEmail == null && byCpf == null) {
            Account newAccount = Account.builder(
                    managerData.name(),
                    CPF.of(managerData.cpf()),
                    BcryptUtil.bcryptHash(managerData.password()),
                    managerData.email(),
                    Role.MANAGER
            ).build();
            return new AccountResolution(newAccount, true);
        }

        boolean sameAccount = byEmail != null
                && byCpf != null
                && byEmail.id != null
                && byEmail.id.equals(byCpf.id);
        boolean validManager = sameAccount && byEmail.getRole() == Role.MANAGER;
        boolean passwordMatches = validManager && BcryptUtil.matches(managerData.password(), byEmail.getPassword());

        if (!passwordMatches) {
            throw new BadRequestException("Manager identity or credentials are invalid");
        }

        return new AccountResolution(byEmail, false);
    }

    private Uni<Account> persistAccountIfNecessary(AccountResolution resolution) {
        if (!resolution.newAccount()) {
            return Uni.createFrom().item(resolution.account());
        }
        return persistAccount(resolution.account());
    }

    private Uni<Account> persistAccount(Account account) {
        return accountRepository.persist(account).replaceWith(account);
    }

    private Uni<Manager> persistManager(Manager manager) {
        return managerRepository.persist(manager).replaceWith(manager);
    }

    private Uni<Company> persistCompany(Company company) {
        return companyRepository.persist(company).replaceWith(company);
    }

    private Uni<Void> validateCnpjNotRegistered(String cnpj) {
        return companyRepository.findByCNPJ(cnpj)
                .flatMap(company -> company != null
                        ? Uni.createFrom().failure(new BadRequestException("CNPJ already registered"))
                        : Uni.createFrom().voidItem());
    }

    private record AccountResolution(Account account, boolean newAccount) {
    }

}

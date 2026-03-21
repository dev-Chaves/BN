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
        
        CNPJ cnpj = CNPJ.of(request.company().cnpj());

        Account newAccount = Account.builder(
            request.manager().name(), 
            CPF.of(request.manager().cpf()), 
            BcryptUtil.bcryptHash(request.manager().password()), 
            request.manager().email(), 
            Role.MANAGER).build();

        Company newCompany = Company.builder(request.company().name(), cnpj).build();

        Manager manager = Manager.builder(request.manager().name(), newCompany, newAccount).build();

        return persistManager(manager)
                .call(() -> persistCompany(newCompany))
                .call(() -> persistAccount(newAccount))
                .replaceWith(
                        new OnboardingResponse(
                        newCompany.getCnpj().getValue(),
                        newCompany.getName(),
                        manager.getName()
                ));
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

}

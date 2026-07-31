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
        return validateCpfNotRegistered(request.manager().cpf())
                .call(() -> validateEmailNotRegistered(request.manager().email()))
                .call(() -> validateCnpjNotRegistered(request.company().cnpj()))
                .flatMap(unused -> {
                    CNPJ cnpj = CNPJ.of(request.company().cnpj());

                    Account newAccount = Account.builder(
                            request.manager().name(),
                            CPF.of(request.manager().cpf()),
                            BcryptUtil.bcryptHash(request.manager().password()),
                            request.manager().email(),
                            Role.MANAGER).build();

                    Company newCompany = Company.builder(request.company().name(), cnpj).build();
                    Manager manager = Manager.builder(request.manager().name(), newCompany, newAccount).build();

                    return persistAccount(newAccount)
                            .call(() -> persistCompany(newCompany))
                            .call(() -> persistManager(manager))
                            .replaceWith(new OnboardingResponse(
                                    newCompany.getCnpj().getValue(),
                                    newCompany.getName(),
                                    manager.getName()
                            ));
                });
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

    private Uni<Void> validateCpfNotRegistered(String cpf) {
        return accountRepository.findByCPF(cpf)
                .flatMap(account -> {
                    if (account != null) {
                        return Uni.createFrom().failure(new BadRequestException("CPF already registered"));
                    }
                    return Uni.createFrom().voidItem();
                });
    }

    private Uni<Void> validateEmailNotRegistered(String email) {
        return accountRepository.findByEmail(email)
                .flatMap(account -> account != null
                        ? Uni.createFrom().failure(new BadRequestException("Email already registered"))
                        : Uni.createFrom().voidItem());
    }

    private Uni<Void> validateCnpjNotRegistered(String cnpj) {
        return companyRepository.findByCNPJ(cnpj)
                .flatMap(company -> company != null
                        ? Uni.createFrom().failure(new BadRequestException("CNPJ already registered"))
                        : Uni.createFrom().voidItem());
    }

}

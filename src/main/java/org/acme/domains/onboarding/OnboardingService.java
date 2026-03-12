package org.acme.domains.onboarding;

import org.acme.domains.account.Account;
import org.acme.domains.company.Company;
import org.acme.domains.manager.Manager;
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

        OnboardingResponse response = new OnboardingResponse(
            newCompany.getCnpj().getValue(),
            newCompany.getName(),
            manager.getName()
        );

        return Uni.createFrom().item(response);
    }

}

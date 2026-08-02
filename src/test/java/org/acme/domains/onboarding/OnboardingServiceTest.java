package org.acme.domains.onboarding;

import io.quarkus.elytron.security.common.BcryptUtil;
import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.BadRequestException;
import org.acme.domains.account.Account;
import org.acme.domains.account.AccountRepository;
import org.acme.domains.company.Company;
import org.acme.domains.company.CompanyRepository;
import org.acme.domains.company.dto.CreateCompanyRequest;
import org.acme.domains.manager.Manager;
import org.acme.domains.manager.ManagerRepository;
import org.acme.domains.onboarding.dto.OnboardingRequest;
import org.acme.domains.onboarding.dto.OnboardingResponse;
import org.acme.domains.shared.domain.CPF;
import org.acme.domains.shared.enums.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OnboardingServiceTest {

    @Mock
    AccountRepository accountRepository;
    @Mock
    ManagerRepository managerRepository;
    @Mock
    CompanyRepository companyRepository;

    private OnboardingService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new OnboardingService(accountRepository, managerRepository, companyRepository);
    }

    @Test
    void shouldReuseTheSameManagerAccountWhenIdentityAndPasswordMatch() {
        OnboardingRequest request = request("11222333000181", "manager@acme.com", "52998224725", "manager-pass-123");
        Account existing = managerAccount("manager@acme.com", "52998224725", "manager-pass-123");

        when(companyRepository.findByCNPJ(request.company().cnpj())).thenReturn(Uni.createFrom().nullItem());
        when(accountRepository.findByEmail(request.manager().email())).thenReturn(Uni.createFrom().item(existing));
        when(accountRepository.findByCPF(request.manager().cpf())).thenReturn(Uni.createFrom().item(existing));
        when(companyRepository.persist(any(Company.class))).thenAnswer(invocation -> {
            Company company = invocation.getArgument(0);
            company.id = 42L;
            company.onCreate();
            return Uni.createFrom().item(company);
        });
        when(managerRepository.persist(any(Manager.class))).thenAnswer(invocation -> {
            Manager manager = invocation.getArgument(0);
            manager.id = 99L;
            manager.onCreate();
            return Uni.createFrom().item(manager);
        });

        OnboardingResponse response = service.onboardingCompany(request).await().indefinitely();

        assertEquals(42L, response.companyId());
        assertEquals(99L, response.managerId());
        verify(accountRepository, never()).persist(any(Account.class));
        ArgumentCaptor<Manager> managerCaptor = ArgumentCaptor.forClass(Manager.class);
        verify(managerRepository).persist(managerCaptor.capture());
        assertSame(existing, managerCaptor.getValue().getAccount());
        assertTrue(managerCaptor.getValue().getCompanyOwner());
    }

    @Test
    void shouldRejectExistingIdentityWhenPasswordDoesNotMatch() {
        OnboardingRequest request = request("11222333000181", "manager@acme.com", "52998224725", "wrong-password");
        Account existing = managerAccount("manager@acme.com", "52998224725", "manager-pass-123");

        when(companyRepository.findByCNPJ(request.company().cnpj())).thenReturn(Uni.createFrom().nullItem());
        when(accountRepository.findByEmail(request.manager().email())).thenReturn(Uni.createFrom().item(existing));
        when(accountRepository.findByCPF(request.manager().cpf())).thenReturn(Uni.createFrom().item(existing));

        assertThrows(BadRequestException.class, () -> service.onboardingCompany(request).await().indefinitely());
        verify(companyRepository, never()).persist(any(Company.class));
        verify(managerRepository, never()).persist(any(Manager.class));
    }

    private Account managerAccount(String email, String cpf, String password) {
        Account account = Account.builder(
                "Manager",
                CPF.of(cpf),
                BcryptUtil.bcryptHash(password),
                email,
                Role.MANAGER
        ).build();
        account.id = UUID.randomUUID();
        return account;
    }

    private OnboardingRequest request(String cnpj, String email, String cpf, String password) {
        return new OnboardingRequest(
                new CreateCompanyRequest("ACME", cnpj),
                new OnboardingRequest.ManagerRegistrationData("Manager", cpf, email, password)
        );
    }
}

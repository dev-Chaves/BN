package org.acme.domains.company;

import io.quarkus.elytron.security.common.BcryptUtil;
import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.BadRequestException;
import org.acme.domains.account.Account;
import org.acme.domains.account.AccountRepository;
import org.acme.domains.benefit.BenefitRepository;
import org.acme.domains.benefitrequest.BenefitAccessRequestRepository;
import org.acme.domains.company.dto.CompanyResponse;
import org.acme.domains.company.dto.CreateCompanyRequest;
import org.acme.domains.company.dto.DeactivateCompanyRequest;
import org.acme.domains.company.dto.UpdateCompanyRequest;
import org.acme.domains.employee.EmployeeRepository;
import org.acme.domains.manager.Manager;
import org.acme.domains.manager.ManagerRepository;
import org.acme.domains.partnership.PartnershipRepository;
import org.acme.domains.redemption.RedemptionTokenRepository;
import org.acme.domains.shared.domain.CNPJ;
import org.acme.domains.shared.domain.CPF;
import org.acme.domains.shared.enums.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

class CompanyServiceTest {

    @Mock CompanyRepository companyRepository;
    @Mock AccountRepository accountRepository;
    @Mock ManagerRepository managerRepository;
    @Mock EmployeeRepository employeeRepository;
    @Mock BenefitRepository benefitRepository;
    @Mock BenefitAccessRequestRepository benefitAccessRequestRepository;
    @Mock PartnershipRepository partnershipRepository;
    @Mock RedemptionTokenRepository redemptionTokenRepository;

    private CompanyService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new CompanyService(
                companyRepository,
                accountRepository,
                managerRepository,
                employeeRepository,
                benefitRepository,
                benefitAccessRequestRepository,
                partnershipRepository,
                redemptionTokenRepository
        );
    }

    @Test
    void shouldCreateAnotherCompanyWithTheSameAccount() {
        Account account = managerAccount("correct-password");
        when(accountRepository.findByEmail(account.getEmail())).thenReturn(Uni.createFrom().item(account));
        when(companyRepository.findByCNPJ("12345678000195")).thenReturn(Uni.createFrom().nullItem());
        when(companyRepository.persist(any(Company.class))).thenAnswer(invocation -> {
            Company company = invocation.getArgument(0);
            company.id = 20L;
            company.onCreate();
            return Uni.createFrom().item(company);
        });
        when(managerRepository.persist(any(Manager.class))).thenAnswer(invocation -> {
            Manager manager = invocation.getArgument(0);
            manager.id = 30L;
            manager.onCreate();
            return Uni.createFrom().item(manager);
        });

        CompanyResponse response = service.createForCurrentAccount(
                new CreateCompanyRequest("Second Company", "12345678000195"),
                account.getEmail()
        ).await().indefinitely();

        assertEquals(20L, response.id());
        assertEquals("Second Company", response.name());

        ArgumentCaptor<Manager> managerCaptor = ArgumentCaptor.forClass(Manager.class);
        verify(managerRepository).persist(managerCaptor.capture());
        assertSame(account, managerCaptor.getValue().getAccount());
        assertEquals(20L, managerCaptor.getValue().getCompany().id);
        assertTrue(managerCaptor.getValue().getCompanyOwner());
        assertTrue(response.owner());
    }

    @Test
    void shouldDeactivateOnlyTheSelectedCompanyAndItsOperationalAccess() {
        Account account = managerAccount("correct-password");
        Company company = Company.builder("ACME", CNPJ.of("11222333000181")).build();
        company.id = 10L;
        company.onCreate();
        Manager manager = Manager.builder(account.getName(), company, account).companyOwner().build();
        manager.id = 11L;
        manager.onCreate();

        when(managerRepository.findByEmailAndCompanyId(account.getEmail(), 10L))
                .thenReturn(Uni.createFrom().item(manager));
        when(benefitAccessRequestRepository.cancelPendingByCompanyId(eq(10L), any()))
                .thenReturn(Uni.createFrom().item(6));
        when(managerRepository.deactivateByCompanyId(10L)).thenReturn(Uni.createFrom().item(1));
        when(employeeRepository.disableByCompanyId(10L)).thenReturn(Uni.createFrom().item(2));
        when(benefitRepository.deactivateByProviderId(10L)).thenReturn(Uni.createFrom().item(3));
        when(partnershipRepository.disableByCompanyId(10L)).thenReturn(Uni.createFrom().item(4));
        when(redemptionTokenRepository.revokeActiveByCompanyId(10L)).thenReturn(Uni.createFrom().item(5));

        CompanyResponse response = service.deactivateCurrent(
                account.getEmail(),
                10L,
                new DeactivateCompanyRequest("correct-password")
        ).await().indefinitely();

        assertFalse(response.active());
        verify(benefitAccessRequestRepository).cancelPendingByCompanyId(eq(10L), any());
        verify(managerRepository).deactivateByCompanyId(10L);
        verify(employeeRepository).disableByCompanyId(10L);
        verify(benefitRepository).deactivateByProviderId(10L);
        verify(partnershipRepository).disableByCompanyId(10L);
        verify(redemptionTokenRepository).revokeActiveByCompanyId(10L);
    }

    @Test
    void shouldUpdateTheSelectedCompanyName() {
        Account account = managerAccount("correct-password");
        Company company = Company.builder("Old name", CNPJ.of("11222333000181")).build();
        company.id = 10L;
        company.onCreate();
        Manager manager = Manager.builder(account.getName(), company, account).build();
        manager.id = 11L;
        manager.onCreate();

        when(managerRepository.findByEmailAndCompanyId(account.getEmail(), 10L))
                .thenReturn(Uni.createFrom().item(manager));

        CompanyResponse response = service.updateCurrent(
                account.getEmail(),
                10L,
                new UpdateCompanyRequest("  New company name  ")
        ).await().indefinitely();

        assertEquals("New company name", response.name());
        assertEquals("11222333000181", response.cnpj());
    }

    @Test
    void shouldNotDeactivateAnythingWhenPasswordIsWrong() {
        Account account = managerAccount("correct-password");
        Company company = Company.builder("ACME", CNPJ.of("11222333000181")).build();
        company.id = 10L;
        company.onCreate();
        Manager manager = Manager.builder(account.getName(), company, account).companyOwner().build();
        manager.id = 11L;
        manager.onCreate();

        when(managerRepository.findByEmailAndCompanyId(account.getEmail(), 10L))
                .thenReturn(Uni.createFrom().item(manager));

        assertThrows(BadRequestException.class, () -> service.deactivateCurrent(
                account.getEmail(),
                10L,
                new DeactivateCompanyRequest("wrong-password")
        ).await().indefinitely());

        verify(managerRepository, never()).deactivateByCompanyId(10L);
        verify(benefitAccessRequestRepository, never()).cancelPendingByCompanyId(eq(10L), any());
        verify(employeeRepository, never()).disableByCompanyId(10L);
        verify(benefitRepository, never()).deactivateByProviderId(10L);
        verify(partnershipRepository, never()).disableByCompanyId(10L);
        verify(redemptionTokenRepository, never()).revokeActiveByCompanyId(10L);
    }

    @Test
    void shouldRejectDeactivationByASecondaryManager() {
        Account account = managerAccount("correct-password");
        Company company = Company.builder("ACME", CNPJ.of("11222333000181")).build();
        company.id = 10L;
        company.onCreate();
        Manager manager = Manager.builder(account.getName(), company, account).build();
        manager.id = 11L;
        manager.onCreate();

        when(managerRepository.findByEmailAndCompanyId(account.getEmail(), 10L))
                .thenReturn(Uni.createFrom().item(manager));

        assertThrows(SecurityException.class, () -> service.deactivateCurrent(
                account.getEmail(),
                10L,
                new DeactivateCompanyRequest("correct-password")
        ).await().indefinitely());

        verify(benefitAccessRequestRepository, never()).cancelPendingByCompanyId(eq(10L), any());
        verify(managerRepository, never()).deactivateByCompanyId(10L);
    }

    private Account managerAccount(String password) {
        Account account = Account.builder(
                "Manager",
                CPF.of("52998224725"),
                BcryptUtil.bcryptHash(password),
                "manager@acme.com",
                Role.MANAGER
        ).build();
        account.id = UUID.randomUUID();
        return account;
    }
}

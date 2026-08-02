package org.acme.domains.auth;

import io.smallrye.mutiny.Uni;
import org.acme.domains.account.Account;
import org.acme.domains.auth.dto.LoginResponse;
import org.acme.domains.company.Company;
import org.acme.domains.manager.Manager;
import org.acme.domains.manager.ManagerRepository;
import org.acme.domains.shared.domain.CNPJ;
import org.acme.domains.shared.domain.CPF;
import org.acme.domains.shared.enums.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SwitchCompanyServiceTest {

    @Mock ManagerRepository managerRepository;
    @Mock TokenService tokenService;

    private SwitchCompanyService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new SwitchCompanyService(managerRepository, tokenService);
    }

    @Test
    void shouldIssueATokenForTheExactActiveMembership() {
        Company company = Company.builder("Company B", CNPJ.of("12345678000195")).build();
        company.id = 20L;
        company.onCreate();
        Account account = Account.builder(
                "Manager",
                CPF.of("52998224725"),
                "hash",
                "manager@acme.com",
                Role.MANAGER
        ).build();
        Manager manager = Manager.builder("Manager", company, account).build();
        manager.id = 30L;
        manager.onCreate();

        when(managerRepository.findByEmailAndCompanyId("manager@acme.com", 20L))
                .thenReturn(Uni.createFrom().item(manager));
        when(tokenService.generateToken("manager@acme.com", 20L, "MANAGER"))
                .thenReturn("company-b-token");

        LoginResponse response = service.switchCompany("manager@acme.com", 20L)
                .await().indefinitely();

        assertEquals("company-b-token", response.token());
        verify(managerRepository).findByEmailAndCompanyId("manager@acme.com", 20L);
    }
}

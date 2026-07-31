package org.acme.domains.manager;

import io.quarkus.elytron.security.common.BcryptUtil;
import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.NotFoundException;
import org.acme.domains.account.Account;
import org.acme.domains.account.AccountRepository;
import org.acme.domains.company.Company;
import org.acme.domains.company.CompanyRepository;
import org.acme.domains.manager.dto.CreateManagerRequest;
import org.acme.domains.manager.dto.ManagerResponse;
import org.acme.domains.shared.domain.CNPJ;
import org.acme.domains.shared.domain.CPF;
import org.acme.domains.shared.enums.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class ManagerServiceTest {

    @Mock
    private ManagerRepository managerRepository;

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private AccountRepository accountRepository;

    private ManagerService managerService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        managerService = new ManagerService(managerRepository, companyRepository, accountRepository);
    }

    @Test
    void shouldFailWhenCompanyNotFound() {
        CreateManagerRequest request = new CreateManagerRequest("Manager", "52998224725", "manager@acme.com", "123456", 10L);
        when(accountRepository.findByEmail(request.email())).thenReturn(Uni.createFrom().nullItem());
        when(accountRepository.findByCPF(request.cpf())).thenReturn(Uni.createFrom().nullItem());
        when(companyRepository.findById(request.companyId())).thenReturn(Uni.createFrom().nullItem());

        assertThrows(NotFoundException.class, () -> managerService.createManager(request).await().indefinitely());
    }

    @Test
    void shouldCreateManager() {
        CreateManagerRequest request = new CreateManagerRequest("Manager", "52998224725", "manager@acme.com", "123456", 10L);
        Company company = Company.builder("ACME", CNPJ.of("11222333000181")).build();
        company.id = 10L;
        Account persistedAccount = Account.builder("Manager", CPF.of("52998224725"), "pwd", "manager@acme.com", Role.MANAGER).build();
        persistedAccount.id = UUID.randomUUID();
        Manager persistedManager = Manager.builder("Manager", company, persistedAccount).build();
        persistedManager.id = 99L;

        when(companyRepository.findById(request.companyId())).thenReturn(Uni.createFrom().item(company));
        when(accountRepository.findByEmail(request.email())).thenReturn(Uni.createFrom().nullItem());
        when(accountRepository.findByCPF(request.cpf())).thenReturn(Uni.createFrom().nullItem());
        when(accountRepository.persist(any(Account.class))).thenReturn(Uni.createFrom().item(persistedAccount));
        when(managerRepository.persist(any(Manager.class))).thenReturn(Uni.createFrom().item(persistedManager));

        ManagerResponse response = managerService.createManager(request).await().indefinitely();

        assertEquals(99L, response.id());
        assertEquals("Manager", response.name());
        assertEquals(10L, response.companyId());
    }

    @Test
    void shouldGetCurrentManagerByEmail() {
        Account account = Account.builder("Manager", CPF.of("52998224725"), BcryptUtil.bcryptHash("123456"), "manager@acme.com", Role.MANAGER).build();
        account.id = UUID.randomUUID();
        Company company = Company.builder("ACME", CNPJ.of("11222333000181")).build();
        company.id = 12L;
        Manager manager = Manager.builder("Manager", company, account).build();
        manager.id = 15L;
        company.onCreate();
        manager.onCreate();

        when(managerRepository.findByEmail("manager@acme.com")).thenReturn(Uni.createFrom().item(manager));

        ManagerResponse response = managerService.getCurrentManager("manager@acme.com").await().indefinitely();
        assertEquals(15L, response.id());
        assertEquals(12L, response.companyId());
    }
}

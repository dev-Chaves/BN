package org.acme.domains.benefit;

import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.NotFoundException;
import org.acme.domains.account.Account;
import org.acme.domains.benefit.dto.BenefitResponse;
import org.acme.domains.benefit.dto.CreateBenefitRequest;
import org.acme.domains.category.Category;
import org.acme.domains.category.CategoryRepository;
import org.acme.domains.company.Company;
import org.acme.domains.company.CompanyRepository;
import org.acme.domains.manager.Manager;
import org.acme.domains.manager.ManagerRepository;
import org.acme.domains.shared.domain.CNPJ;
import org.acme.domains.shared.domain.CPF;
import org.acme.domains.shared.enums.Role;
import org.acme.domains.shared.security.TenantGuard;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class BenefitServiceTest {

    @Mock
    private ManagerRepository managerRepository;

    @Mock
    private BenefitRepository benefitRepository;

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private TenantGuard tenantGuard;

    @Mock
    private CategoryRepository categoryRepository;

    private BenefitService benefitService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        benefitService = new BenefitService(managerRepository, benefitRepository, companyRepository, categoryRepository, tenantGuard);
    }

    @Test
    void shouldFailWhenManagerNotFound() {
        CreateBenefitRequest request = new CreateBenefitRequest("Gym", "desc", 10L, null);
        when(categoryRepository.findByIds(any())).thenReturn(Uni.createFrom().item(List.of()));
        when(managerRepository.findByEmail("manager@acme.com")).thenReturn(Uni.createFrom().nullItem());

        assertThrows(RuntimeException.class, () ->
                benefitService.createBenefit(request, "manager@acme.com").await().indefinitely());
    }

    @Test
    void shouldFailWhenTenantValidationFails() {
        CreateBenefitRequest request = new CreateBenefitRequest("Gym", "desc", 99L, null);
        Manager manager = buildManager(10L);

        when(categoryRepository.findByIds(any())).thenReturn(Uni.createFrom().item(List.of()));
        when(managerRepository.findByEmail("manager@acme.com")).thenReturn(Uni.createFrom().item(manager));
        when(tenantGuard.verifyManagerCompanyAccess(manager, 99L))
                .thenReturn(Uni.createFrom().failure(new SecurityException("Unauthorized access: Tenant mismatch")));

        assertThrows(SecurityException.class, () ->
                benefitService.createBenefit(request, "manager@acme.com").await().indefinitely());
    }

    @Test
    void shouldCreateBenefit() {
        CreateBenefitRequest request = new CreateBenefitRequest("Gym", "desc", 10L, null);
        Company company = Company.builder("ACME", CNPJ.of("11222333000181")).build();
        company.id = 10L;
        Manager manager = buildManager(10L);

        when(categoryRepository.findByIds(any())).thenReturn(Uni.createFrom().item(List.of()));
        when(managerRepository.findByEmail("manager@acme.com")).thenReturn(Uni.createFrom().item(manager));
        when(tenantGuard.verifyManagerCompanyAccess(manager, 10L)).thenReturn(Uni.createFrom().item(company));
        when(benefitRepository.persist(any(Benefit.class))).thenAnswer(invocation -> {
            Benefit benefit = invocation.getArgument(0);
            benefit.id = 7L;
            benefit.activeBenefit();
            return Uni.createFrom().item(benefit);
        });

        BenefitResponse response = benefitService.createBenefit(request, "manager@acme.com").await().indefinitely();

        assertEquals(7L, response.id());
        assertEquals("Gym", response.benefitName());
        assertEquals("ACME", response.nameProvider());
    }

    @Test
    void shouldCreateBenefitWithCategories() {
        Category health = new Category("Saúde");
        health.id = 1L;
        CreateBenefitRequest request = new CreateBenefitRequest("Gym", "desc", 10L, List.of(1L));
        Company company = Company.builder("ACME", CNPJ.of("11222333000181")).build();
        company.id = 10L;
        Manager manager = buildManager(10L);

        when(categoryRepository.findByIds(List.of(1L))).thenReturn(Uni.createFrom().item(List.of(health)));
        when(managerRepository.findByEmail("manager@acme.com")).thenReturn(Uni.createFrom().item(manager));
        when(tenantGuard.verifyManagerCompanyAccess(manager, 10L)).thenReturn(Uni.createFrom().item(company));
        when(benefitRepository.persist(any(Benefit.class))).thenAnswer(invocation -> {
            Benefit benefit = invocation.getArgument(0);
            benefit.id = 7L;
            benefit.activeBenefit();
            return Uni.createFrom().item(benefit);
        });

        BenefitResponse response = benefitService.createBenefit(request, "manager@acme.com").await().indefinitely();

        assertEquals(7L, response.id());
        assertEquals("Gym", response.benefitName());
        assertEquals(1, response.categories().size());
        assertEquals("Saúde", response.categories().get(0).name());
    }

    @Test
    void shouldFailWhenCategoryNotFound() {
        CreateBenefitRequest request = new CreateBenefitRequest("Gym", "desc", 10L, List.of(99L));
        Company company = Company.builder("ACME", CNPJ.of("11222333000181")).build();
        company.id = 10L;
        Manager manager = buildManager(10L);

        when(categoryRepository.findByIds(List.of(99L))).thenReturn(Uni.createFrom().item(List.of()));
        when(managerRepository.findByEmail("manager@acme.com")).thenReturn(Uni.createFrom().item(manager));
        when(tenantGuard.verifyManagerCompanyAccess(manager, 10L)).thenReturn(Uni.createFrom().item(company));

        assertThrows(NotFoundException.class, () ->
                benefitService.createBenefit(request, "manager@acme.com").await().indefinitely());
    }

    private Manager buildManager(Long companyId) {
        Company managerCompany = Company.builder("ACME", CNPJ.of("11222333000181")).build();
        managerCompany.id = companyId;
        return Manager.builder(
                "Manager",
                managerCompany,
                Account.builder("Manager", CPF.of("52998224725"), "pwd", "manager@acme.com", Role.MANAGER).build()
        ).build();
    }
}

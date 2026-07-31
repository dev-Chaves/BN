package org.acme.domains.partnership;

import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.NotFoundException;
import org.acme.domains.account.Account;
import org.acme.domains.benefit.Benefit;
import org.acme.domains.benefit.BenefitRepository;
import org.acme.domains.company.Company;
import org.acme.domains.company.CompanyRepository;
import org.acme.domains.manager.Manager;
import org.acme.domains.manager.ManagerRepository;
import org.acme.domains.partnership.dto.PartnershipResponse;
import org.acme.domains.shared.domain.CNPJ;
import org.acme.domains.shared.domain.CPF;
import org.acme.domains.shared.enums.Role;
import org.acme.domains.shared.security.TenantGuard;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class PartnershipServiceTest {

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private ManagerRepository managerRepository;

    @Mock
    private BenefitRepository benefitRepository;

    @Mock
    private PartnershipRepository partnershipRepository;

    @Mock
    private TenantGuard tenantGuard;

    private PartnershipService partnershipService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        partnershipService = new PartnershipService(
                companyRepository,
                managerRepository,
                benefitRepository,
                partnershipRepository,
                tenantGuard
        );
    }

    @Test
    void shouldFailWhenManagerNotFound() {
        when(managerRepository.findByEmail("manager@acme.com")).thenReturn(Uni.createFrom().nullItem());

        assertThrows(NotFoundException.class, () ->
                partnershipService.requestPartnership("manager@acme.com", 1L).await().indefinitely());
    }

    @Test
    void shouldFailWhenTenantValidationFails() {
        Manager manager = buildManager(10L);
        when(managerRepository.findByEmail("manager@acme.com")).thenReturn(Uni.createFrom().item(manager));
        when(tenantGuard.verifyManagerCompanyAccess(manager, 10L))
                .thenReturn(Uni.createFrom().failure(new SecurityException("Unauthorized access: Tenant mismatch")));

        assertThrows(SecurityException.class, () ->
                partnershipService.requestPartnership("manager@acme.com", 1L).await().indefinitely());
    }

    @Test
    void shouldCreatePartnership() {
        Manager manager = buildManager(10L);
        Company provider = Company.builder("Provider", CNPJ.of("12345678000195")).build();
        provider.id = 30L;
        Benefit benefit = Benefit.builder("Gym", provider).description("desc").build();
        benefit.id = 1L;
        provider.onCreate();
        benefit.onCreate();
        benefit.activeBenefit();

        when(managerRepository.findByEmail("manager@acme.com")).thenReturn(Uni.createFrom().item(manager));
        when(tenantGuard.verifyManagerCompanyAccess(manager, 10L)).thenReturn(Uni.createFrom().item(manager.getCompany()));
        when(benefitRepository.findById(1L)).thenReturn(Uni.createFrom().item(benefit));
        when(partnershipRepository.findExistingPartnership(10L, 1L)).thenReturn(Uni.createFrom().item(false));
        when(partnershipRepository.persist(any(Partnership.class))).thenAnswer(invocation -> {
            Partnership partnership = invocation.getArgument(0);
            partnership.id = 100L;
            return Uni.createFrom().item(partnership);
        });

        PartnershipResponse response = partnershipService.requestPartnership("manager@acme.com", 1L).await().indefinitely();

        assertEquals(100L, response.id());
        assertEquals(10L, response.clientCompanyId());
        assertEquals(1L, response.benefitId());
    }

    private Manager buildManager(Long companyId) {
        Company company = Company.builder("ACME", CNPJ.of("11222333000181")).build();
        company.id = companyId;
        return Manager.builder(
                "Manager",
                company,
                Account.builder("Manager", CPF.of("52998224725"), "pwd", "manager@acme.com", Role.MANAGER).build()
        ).build();
    }
}

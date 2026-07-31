package org.acme.domains.benefit;

import org.acme.domains.company.Company;
import org.acme.domains.shared.domain.CNPJ;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BenefitAvailabilityTest {

    @Test
    void shouldPreserveAvailabilityConfiguredByBuilderOnPersist() {
        Company provider = activeCompany();
        Benefit benefit = Benefit.builder("Consulta", provider)
                .description("Atendimento com desconto")
                .availability(false, null, null, 5, "Apresente um documento")
                .build();

        benefit.onCreate();
        benefit.activeBenefit();

        assertFalse(benefit.getPubliclyVisible());
        assertEquals(5, benefit.getMaxUsesPerUser());
        assertTrue(benefit.isOperationalAt(LocalDateTime.now()));
        assertFalse(benefit.isDiscoverableAt(LocalDateTime.now()));
    }

    @Test
    void shouldRejectBenefitsFromInactiveProvider() {
        Company provider = Company.builder("Empresa", CNPJ.of("11222333000181")).build();
        Benefit benefit = Benefit.builder("Consulta", provider).description("Desconto").build();
        benefit.onCreate();
        benefit.activeBenefit();

        assertFalse(benefit.isOperationalAt(LocalDateTime.now()));
        assertFalse(benefit.isDiscoverableAt(LocalDateTime.now()));
    }

    private Company activeCompany() {
        Company company = Company.builder("Empresa", CNPJ.of("11222333000181")).build();
        company.onCreate();
        return company;
    }
}

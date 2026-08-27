package com.bnfix.ubm.domain.benefit;

import static org.assertj.core.api.Assertions.assertThat;

import com.bnfix.ubm.domains.benefit.Benefit;
import com.bnfix.ubm.domains.benefit.BenefitAccessPolicy;
import com.bnfix.ubm.domains.company.Company;
import com.bnfix.ubm.domains.employee.Employee;
import com.bnfix.ubm.domains.partnership.PartnershipRepository;
import com.bnfix.ubm.domains.shared.domain.CNPJ;
import java.lang.reflect.Proxy;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class BenefitAccessPolicyTest {
    @Test
    void ownBenefitIsBlockedByDefault() {
        Company company = company(1L, "Provider", "41549857000142");
        Employee employee = employee(1L, company);
        Benefit benefit = benefit(1L, company, false, true);

        assertThat(policy(false).isEligible(employee, benefit, LocalDateTime.now()))
                .isFalse();
    }

    @Test
    void ownBenefitIsAvailableWhenProviderEnablesEmployeeAccess() {
        Company company = company(1L, "Provider", "41549857000142");
        Employee employee = employee(1L, company);
        Benefit benefit = benefit(1L, company, true, true);

        assertThat(policy(false).isEligible(employee, benefit, LocalDateTime.now()))
                .isTrue();
    }

    @Test
    void activePartnershipGrantsAccessEvenWhenBenefitIsNotPubliclyVisible() {
        Company client = company(1L, "Client", "41549857000142");
        Company provider = company(2L, "Provider", "11222333000181");
        Employee employee = employee(1L, client);
        Benefit benefit = benefit(1L, provider, false, false);
        assertThat(policy(true).isEligible(employee, benefit, LocalDateTime.now()))
                .isTrue();
    }

    @Test
    void externalBenefitWithoutActivePartnershipIsBlocked() {
        Company client = company(1L, "Client", "41549857000142");
        Company provider = company(2L, "Provider", "11222333000181");
        Employee employee = employee(1L, client);
        Benefit benefit = benefit(1L, provider, false, true);

        assertThat(policy(false).isEligible(employee, benefit, LocalDateTime.now()))
                .isFalse();
    }

    private BenefitAccessPolicy policy(boolean activePartnership) {
        PartnershipRepository repository = (PartnershipRepository) Proxy.newProxyInstance(
                PartnershipRepository.class.getClassLoader(),
                new Class<?>[] {PartnershipRepository.class},
                (proxy, method, arguments) -> {
                    if (method.getName().equals("existsActivePartnership")) return activePartnership;
                    if (method.getReturnType().equals(boolean.class)) return false;
                    if (method.getReturnType().equals(int.class)) return 0;
                    return null;
                });
        return new BenefitAccessPolicy(repository);
    }

    private Company company(Long id, String name, String cnpj) {
        Company company = Company.builder(name, CNPJ.of(cnpj)).build();
        company.id = id;
        company.onCreate();
        return company;
    }

    private Employee employee(Long id, Company company) {
        Employee employee = Employee.builder("Employee", company, null).build();
        employee.id = id;
        employee.active();
        return employee;
    }

    private Benefit benefit(Long id, Company provider, boolean internalAccess, boolean publiclyVisible) {
        Benefit benefit = Benefit.builder("Benefit", provider)
                .description("Description")
                .availability(publiclyVisible, null, null, 1, null, internalAccess)
                .build();
        benefit.id = id;
        benefit.onCreate();
        benefit.activeBenefit();
        return benefit;
    }
}

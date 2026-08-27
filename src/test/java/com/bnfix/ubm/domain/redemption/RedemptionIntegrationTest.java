package com.bnfix.ubm.domain.redemption;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bnfix.ubm.domains.account.Account;
import com.bnfix.ubm.domains.account.AccountRepository;
import com.bnfix.ubm.domains.benefit.Benefit;
import com.bnfix.ubm.domains.benefit.BenefitRepository;
import com.bnfix.ubm.domains.company.Company;
import com.bnfix.ubm.domains.company.CompanyRepository;
import com.bnfix.ubm.domains.employee.Employee;
import com.bnfix.ubm.domains.employee.EmployeeRepository;
import com.bnfix.ubm.domains.manager.Manager;
import com.bnfix.ubm.domains.manager.ManagerRepository;
import com.bnfix.ubm.domains.partnership.Partnership;
import com.bnfix.ubm.domains.partnership.PartnershipRepository;
import com.bnfix.ubm.domains.redemption.BenefitRedemption;
import com.bnfix.ubm.domains.redemption.BenefitRedemptionRepository;
import com.bnfix.ubm.domains.redemption.RedemptionService;
import com.bnfix.ubm.domains.redemption.RedemptionTokenStatus;
import com.bnfix.ubm.domains.redemption.dto.RedemptionPreviewResponse;
import com.bnfix.ubm.domains.redemption.dto.RedemptionResponse;
import com.bnfix.ubm.domains.redemption.dto.RedemptionTokenResponse;
import com.bnfix.ubm.domains.shared.domain.CNPJ;
import com.bnfix.ubm.domains.shared.domain.CPF;
import com.bnfix.ubm.domains.shared.enums.Role;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(properties = "spring.flyway.enabled=true")
@ActiveProfiles("test")
@Transactional
class RedemptionIntegrationTest {
    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18-alpine");

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private ManagerRepository managerRepository;

    @Autowired
    private BenefitRepository benefitRepository;

    @Autowired
    private PartnershipRepository partnershipRepository;

    @Autowired
    private BenefitRedemptionRepository benefitRedemptionRepository;

    @Autowired
    private RedemptionService redemptionService;

    @Autowired
    private EntityManager entityManager;

    @Test
    void redeemsPartnerBenefitAndExposesDirectRelationships() {
        Fixture fixture = partnerFixture(1);

        RedemptionTokenResponse issued = redemptionService.issue(fixture.employeeEmail(), fixture.benefit().id);
        RedemptionPreviewResponse preview =
                redemptionService.preview(fixture.managerEmail(), fixture.provider().id, issued.token());
        RedemptionResponse consumed =
                redemptionService.consume(fixture.managerEmail(), fixture.provider().id, issued.token());

        BenefitRedemption redemption = reload(consumed.id());
        assertThat(preview.valid()).isTrue();
        assertThat(preview.benefitName()).isEqualTo(fixture.benefit().getName());
        assertThat(preview.beneficiaryName()).isEqualTo(fixture.employee().getName());
        assertThat(preview.providerName()).isEqualTo(fixture.provider().getName());
        assertThat(consumed.benefitName()).isEqualTo(fixture.benefit().getName());
        assertThat(consumed.beneficiaryName()).isEqualTo(fixture.employee().getName());
        assertThat(redemption.getEmployee().id).isEqualTo(fixture.employee().id);
        assertThat(redemption.getBenefit().id).isEqualTo(fixture.benefit().id);
        assertThat(redemption.getBeneficiaryCompany().id).isEqualTo(fixture.client().id);
        assertThat(redemption.getProviderCompany().id).isEqualTo(fixture.provider().id);
        assertThat(redemption.getRedeemedBy().id).isEqualTo(fixture.manager().id);
        assertThat(redemption.getToken().getEmployee().id).isEqualTo(fixture.employee().id);
        assertThat(redemption.getToken().getBenefit().id).isEqualTo(fixture.benefit().id);
        assertThat(redemption.getToken().getStatus()).isEqualTo(RedemptionTokenStatus.CONSUMED);
        assertThat(benefitRedemptionRepository.countByEmployeeIdAndBenefitId(
                        fixture.employee().id, fixture.benefit().id))
                .isEqualTo(1);
    }

    @Test
    void rejectsIssuanceWithoutActivePartnership() {
        Fixture fixture = fixture(1, false, false);

        assertThatThrownBy(() -> redemptionService.issue(fixture.employeeEmail(), fixture.benefit().id))
                .isInstanceOf(SecurityException.class)
                .hasMessage("Benefit not available for employee");
    }

    @Test
    void invalidatesIssuedTokenWhenPartnershipIsDisabled() {
        Fixture fixture = partnerFixture(1);
        RedemptionTokenResponse issued = redemptionService.issue(fixture.employeeEmail(), fixture.benefit().id);
        fixture.partnership().disable();
        partnershipRepository.saveAndFlush(fixture.partnership());

        assertThatThrownBy(
                        () -> redemptionService.preview(fixture.managerEmail(), fixture.provider().id, issued.token()))
                .isInstanceOf(SecurityException.class)
                .hasMessage("Benefit not available for employee");
    }

    @Test
    void enforcesLifetimeUsageLimitByEmployeeAndBenefit() {
        Fixture fixture = partnerFixture(1);
        RedemptionTokenResponse issued = redemptionService.issue(fixture.employeeEmail(), fixture.benefit().id);
        redemptionService.consume(fixture.managerEmail(), fixture.provider().id, issued.token());

        assertThatThrownBy(() -> redemptionService.issue(fixture.employeeEmail(), fixture.benefit().id))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Benefit usage limit reached");
    }

    @Test
    void allowsProviderEmployeeOnlyWhenInternalAccessIsEnabled() {
        Fixture fixture = fixture(1, true, true);

        RedemptionTokenResponse issued = redemptionService.issue(fixture.employeeEmail(), fixture.benefit().id);
        RedemptionResponse consumed =
                redemptionService.consume(fixture.managerEmail(), fixture.provider().id, issued.token());

        BenefitRedemption redemption = reload(consumed.id());
        assertThat(redemption.getBeneficiaryCompany().id).isEqualTo(fixture.provider().id);
        assertThat(redemption.getProviderCompany().id).isEqualTo(fixture.provider().id);
    }

    private Fixture partnerFixture(int maxUses) {
        return fixture(maxUses, false, true);
    }

    private BenefitRedemption reload(Long redemptionId) {
        entityManager.flush();
        entityManager.clear();
        return benefitRedemptionRepository.findById(redemptionId).orElseThrow();
    }

    private Fixture fixture(int maxUses, boolean ownEmployee, boolean grantAccess) {
        Company provider = companyRepository.saveAndFlush(
                Company.builder("Provider", CNPJ.of("41549857000142")).build());
        Company client = ownEmployee
                ? provider
                : companyRepository.saveAndFlush(
                        Company.builder("Client", CNPJ.of("11222333000181")).build());

        String employeeEmail = "employee@client.test";
        Account employeeAccount = accountRepository.saveAndFlush(
                Account.builder("Employee", CPF.of("52998224725"), "encoded", employeeEmail, Role.USER)
                        .build());
        Employee employee =
                Employee.builder("Employee", client, employeeAccount).build();
        employee.active();
        employee = employeeRepository.saveAndFlush(employee);

        String managerEmail = "manager@provider.test";
        Account managerAccount = accountRepository.saveAndFlush(
                Account.builder("Manager", CPF.of("16899535009"), "encoded", managerEmail, Role.MANAGER)
                        .build());
        Manager manager = managerRepository.saveAndFlush(
                Manager.builder("Manager", provider, managerAccount).build());

        Benefit benefit = Benefit.builder("Gym", provider)
                .description("Gym membership")
                .availability(true, null, null, maxUses, "Present the QR code", ownEmployee && grantAccess)
                .build();
        benefit.activeBenefit();
        benefit = benefitRepository.saveAndFlush(benefit);

        Partnership partnership = null;
        if (!ownEmployee) {
            partnership = partnershipRepository.saveAndFlush(
                    Partnership.builder(client, benefit).build());
            if (grantAccess) {
                partnership.activate();
                partnership = partnershipRepository.saveAndFlush(partnership);
            }
        }
        return new Fixture(provider, client, employee, manager, benefit, partnership, employeeEmail, managerEmail);
    }

    private record Fixture(
            Company provider,
            Company client,
            Employee employee,
            Manager manager,
            Benefit benefit,
            Partnership partnership,
            String employeeEmail,
            String managerEmail) {}
}

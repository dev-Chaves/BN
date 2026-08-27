package com.bnfix.ubm.domain.benefit;

import static org.assertj.core.api.Assertions.assertThat;

import com.bnfix.ubm.domains.benefit.Benefit;
import com.bnfix.ubm.domains.benefit.BenefitRepository;
import com.bnfix.ubm.domains.benefit.dto.BenefitPublicResponse;
import com.bnfix.ubm.domains.benefit.dto.BenefitSearchProjection;
import com.bnfix.ubm.domains.company.Company;
import com.bnfix.ubm.domains.company.CompanyRepository;
import com.bnfix.ubm.domains.shared.domain.CNPJ;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(properties = "spring.flyway.enabled=true")
@ActiveProfiles("test")
public class BenefitIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18-alpine");

    @Autowired
    private BenefitRepository benefitRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void clean() {
        benefitRepository.deleteAll();
        companyRepository.deleteAll();
    }

    @Test
    void hasToFindBenefitByDescription() {

        Company company = companyRepository.save(
                Company.builder("Benefix", CNPJ.of("41549857000142")).build());

        benefitRepository.save(Benefit.builder("Benefix", company)
                .description("Academia para todos")
                .build());

        benefitRepository.save(Benefit.builder("Benefix", company)
                .description("Natação e Mordomias")
                .build());

        Pageable pageable = PageRequest.of(0, 10);

        Page<Benefit> benefits = benefitRepository.searchByDescriptionFullText("Academia", pageable);

        assertThat(benefits.getTotalElements()).isEqualTo(1);

        assertThat(benefits.getContent()).extracting(Benefit::getDescription).contains("Academia para todos");
    }

    @Test
    void hasNotFindBenefitByDescription() {

        Company company = companyRepository.save(
                Company.builder("Benefix", CNPJ.of("41549857000142")).build());

        benefitRepository.save(Benefit.builder("Benefix", company)
                .description("Academia para todos")
                .build());

        benefitRepository.save(Benefit.builder("Benefix", company)
                .description("Natação e Mordomias")
                .build());

        Pageable pageable = PageRequest.of(0, 10);

        Page<Benefit> benefits = benefitRepository.searchByDescriptionFullText("Acad", pageable);

        assertThat(benefits.getTotalElements()).isEqualTo(0);

        assertThat(benefits.getContent().isEmpty());
    }

    @Test
    void shouldReturnBenefitSimilar() {

        Pageable pageable = PageRequest.of(0, 10);

        Company company = companyRepository.save(
                Company.builder("Benefix", CNPJ.of("41549857000142")).build());

        benefitRepository.save(Benefit.builder("Benefix", company)
                .description("Academia para todos")
                .build());

        benefitRepository.save(Benefit.builder("Benefix", company)
                .description("Desconto em produtos da Academia para todos")
                .build());

        benefitRepository.save(Benefit.builder("Benefix", company)
                .description("Natação e Mordomias")
                .build());

        Page<BenefitSearchProjection> benefits = benefitRepository.searchByDescriptionSimilarity("acad", pageable);

        Page<BenefitPublicResponse> response = benefits.map(this::publicResponse);

        assertThat(response.getTotalElements()).isEqualTo(2L);

        assertThat(response.getContent())
                .extracting(BenefitPublicResponse::description)
                .containsExactlyInAnyOrder("Academia para todos", "Desconto em produtos da Academia para todos");
    }

    @Test
    void shouldntReturnAnyBenefitSimiliar() {

        Pageable pageable = PageRequest.of(0, 10);

        Company company = companyRepository.save(
                Company.builder("Benefix", CNPJ.of("41549857000142")).build());

        benefitRepository.save(Benefit.builder("Benefix", company)
                .description("Nutricao para todos")
                .build());

        benefitRepository.save(Benefit.builder("Benefix", company)
                .description("Desconto em produtos da farmacia para todos")
                .build());

        benefitRepository.save(Benefit.builder("Benefix", company)
                .description("Natação e Mordomias")
                .build());

        Page<BenefitSearchProjection> benefits = benefitRepository.searchByDescriptionSimilarity("acad", pageable);

        assertThat(benefits.getTotalElements()).isEqualTo(0);
    }

    @Test
    void migrationReplacesSubscriptionBasedAccessSchema() {
        Integer removedTables = jdbcTemplate.queryForObject(
                "select count(*) from information_schema.tables where table_schema='public' and table_name in ('subscriptions', 'benefit_access_requests')",
                Integer.class);
        Integer tokenColumns = jdbcTemplate.queryForObject(
                "select count(*) from information_schema.columns where table_schema='public' and table_name='redemption_tokens' and column_name in ('employee_id', 'benefit_id')",
                Integer.class);
        Integer oldTokenColumn = jdbcTemplate.queryForObject(
                "select count(*) from information_schema.columns where table_schema='public' and table_name='redemption_tokens' and column_name='subscription_id'",
                Integer.class);
        Integer redemptionColumns = jdbcTemplate.queryForObject(
                "select count(*) from information_schema.columns where table_schema='public' and table_name='benefit_redemptions' and column_name in ('employee_id', 'benefit_id', 'beneficiary_company_id')",
                Integer.class);

        assertThat(removedTables).isZero();
        assertThat(tokenColumns).isEqualTo(2);
        assertThat(oldTokenColumn).isZero();
        assertThat(redemptionColumns).isEqualTo(3);
    }

    private BenefitPublicResponse publicResponse(Benefit benefit) {
        return new BenefitPublicResponse(
                benefit.id,
                benefit.getName(),
                benefit.getDescription(),
                benefit.getProvider().getName());
    }

    private BenefitPublicResponse publicResponse(BenefitSearchProjection benefits) {
        return new BenefitPublicResponse(
                benefits.getId(), benefits.getName(), benefits.getDescription(), benefits.getProviderName());
    }
}

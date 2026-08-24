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
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers
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

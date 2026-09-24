package com.bnfix.ubm.domain.enrollment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bnfix.ubm.domains.account.Account;
import com.bnfix.ubm.domains.account.AccountRepository;
import com.bnfix.ubm.domains.company.Company;
import com.bnfix.ubm.domains.company.CompanyRepository;
import com.bnfix.ubm.domains.employee.Employee;
import com.bnfix.ubm.domains.employee.EmployeeRepository;
import com.bnfix.ubm.domains.employee.EmployeeStatus;
import com.bnfix.ubm.domains.enrollment.EnrollmentService;
import com.bnfix.ubm.domains.enrollment.dto.EnrollmentRequest;
import com.bnfix.ubm.domains.enrollment.dto.EnrollmentResponse;
import com.bnfix.ubm.domains.shared.domain.CNPJ;
import com.bnfix.ubm.domains.shared.domain.CPF;
import com.bnfix.ubm.domains.shared.enums.Role;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(properties = "spring.flyway.enabled=true")
@ActiveProfiles("test")
@Transactional
class EnrollmentIntegrationTest {
    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18-alpine");

    @Autowired
    private EnrollmentService enrollmentService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Test
    void enrollsAndActivatesEmployeeInEventCompany() {
        Company company = companyRepository.saveAndFlush(
                Company.builder("Event", CNPJ.of("41549857000142")).build());

        EnrollmentResponse response = enrollmentService.enroll(
                new EnrollmentRequest("Attendee", "11144477735", "attendee@event.test", "strong-pass-1"), company.id);

        assertThat(response.status()).isEqualTo(EmployeeStatus.ACTIVE);
        assertThat(response.companyId()).isEqualTo(company.id);
        assertThat(response.companyName()).isEqualTo("Event");
        Employee employee = employeeRepository.findById(response.employeeId()).orElseThrow();
        assertThat(employee.getActive()).isEqualTo(EmployeeStatus.ACTIVE);
        assertThat(employee.getCompany().id).isEqualTo(company.id);
    }

    @Test
    void rejectsDuplicatedEmail() {
        Company company = companyRepository.saveAndFlush(
                Company.builder("Event", CNPJ.of("41549857000142")).build());
        accountRepository.saveAndFlush(
                Account.builder("Existing", CPF.of("12345678909"), "encoded", "attendee@event.test", Role.USER)
                        .build());

        assertThatThrownBy(() -> enrollmentService.enroll(
                        new EnrollmentRequest("Attendee", "11144477735", "attendee@event.test", "strong-pass-1"),
                        company.id))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Email already in use");
    }

    @Test
    void rejectsDuplicatedCpf() {
        Company company = companyRepository.saveAndFlush(
                Company.builder("Event", CNPJ.of("41549857000142")).build());
        accountRepository.saveAndFlush(
                Account.builder("Existing", CPF.of("11144477735"), "encoded", "existing@event.test", Role.USER)
                        .build());

        assertThatThrownBy(() -> enrollmentService.enroll(
                        new EnrollmentRequest("Attendee", "11144477735", "attendee@event.test", "strong-pass-1"),
                        company.id))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("CPF already in use");
    }

    @Test
    void rejectsInvalidCpf() {
        Company company = companyRepository.saveAndFlush(
                Company.builder("Event", CNPJ.of("41549857000142")).build());

        assertThatThrownBy(() -> enrollmentService.enroll(
                        new EnrollmentRequest("Attendee", "123", "attendee@event.test", "strong-pass-1"), company.id))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsUnknownOrDisabledCompany() {
        assertThatThrownBy(() -> enrollmentService.enroll(
                        new EnrollmentRequest("Attendee", "11144477735", "attendee@event.test", "strong-pass-1"),
                        999999L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Enrollment not available");
    }
}

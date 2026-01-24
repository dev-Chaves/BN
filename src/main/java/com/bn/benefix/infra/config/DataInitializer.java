package com.bn.benefix.infra.config;

import com.bn.benefix.account.Account;
import com.bn.benefix.account.AccountRepository;
import com.bn.benefix.benefit.Benefit;
import com.bn.benefix.benefit.BenefitRepository;
import com.bn.benefix.company.Company;
import com.bn.benefix.company.CompanyRepository;
import com.bn.benefix.employee.Employee;
import com.bn.benefix.employee.EmployeeRepository;
import com.bn.benefix.employee.EmployeeStatus;
import com.bn.benefix.manager.Manager;
import com.bn.benefix.manager.ManagerRepository;
import com.bn.benefix.partnership.Partnership;
import com.bn.benefix.partnership.PartnershipRepository;
import com.bn.benefix.partnership.PartnershipStatus;
import com.bn.benefix.shared.domain.CNPJ;
import com.bn.benefix.shared.domain.CPF;
import com.bn.benefix.shared.enums.Role;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

@Configuration
@Profile("!test") // Do not run in tests
public class DataInitializer {

    @Bean
    @Transactional
    CommandLineRunner initDatabase(
            AccountRepository accountRepository,
            CompanyRepository companyRepository,
            ManagerRepository managerRepository,
            BenefitRepository benefitRepository,
            EmployeeRepository employeeRepository,
            PartnershipRepository partnershipRepository,
            PasswordEncoder passwordEncoder
    ) {
        return args -> {
            if (accountRepository.count() > 0) {
                return; // Already initialized
            }

            // 1. Admin Account
            // CPF generated (valid format)
            Account adminAccount = new Account.Builder(
                    "Admin User",
                    CPF.of("52998224725"),
                    passwordEncoder.encode("admin123"),
                    "admin@benefix.com",
                    Role.ADMIN
            ).build();
            accountRepository.save(adminAccount);

            // 2. Provider Company (HealthCorp)
            // CNPJ generated (valid format)
            Company healthCorp = Company.builder("HealthCorp Ltda", CNPJ.of("06990590000123"))
                    .build();
            // Need to save company first to satisfy foreign key constraints if not cascaded correctly, 
            // but Manager requires Company. 
            // Let's rely on CascadeType.ALL or save explicitly.
            // Company entity has: @OneToMany(mappedBy = "company", cascade = CascadeType.ALL, orphanRemoval = true)
            // But usually we need to set the relationship on both sides.
            
            // Let's save company first.
            healthCorp = companyRepository.save(healthCorp);

            // 3. Manager for HealthCorp
            Account managerHealthAccount = new Account.Builder(
                    "Health Manager",
                    CPF.of("95118821030"),
                    passwordEncoder.encode("manager123"),
                    "manager@healthcorp.com",
                    Role.MANAGER
            ).build();
            // Account is OneToOne with Manager with CascadeType.ALL, but Account needs to be saved? 
            // Manager definition: @OneToOne(cascade = CascadeType.ALL) private Account account;
            
            Manager healthManager = new Manager.Builder("Carlos Health", healthCorp, managerHealthAccount).build();
            healthManager.activeManager(); // Activate
            
            // Add manager to company to ensure consistency if bidirectional
            // healthCorp.getManagers().add(healthManager); // logic is in builder/methods but let's save via repository
            managerRepository.save(healthManager);


            // 4. Client Company (TechSolutions)
            Company techSolutions = Company.builder("TechSolutions Inc", CNPJ.of("60701190000104"))
                    .build();
            techSolutions = companyRepository.save(techSolutions);

            // 5. Manager for TechSolutions
            Account managerTechAccount = new Account.Builder(
                    "Tech Manager",
                    CPF.of("23853004050"),
                    passwordEncoder.encode("manager123"),
                    "manager@techsolutions.com",
                    Role.MANAGER
            ).build();
            
            Manager techManager = new Manager.Builder("Ana Tech", techSolutions, managerTechAccount).build();
            techManager.activeManager();
            managerRepository.save(techManager);

            // 6. Benefits provided by HealthCorp
            Benefit dentalPlan = new Benefit.Builder("Plano Odonto Premium", healthCorp)
                    .description("Cobertura completa para tratamentos odontológicos.")
                    .build();
            dentalPlan.activeBenefit();
            benefitRepository.save(dentalPlan);

            Benefit gymPass = new Benefit.Builder("Gym Access", healthCorp)
                    .description("Acesso a academias em todo o país.")
                    .build();
            gymPass.activeBenefit();
            benefitRepository.save(gymPass);

            // 7. Employee for TechSolutions
            Account employeeAccount = new Account.Builder(
                    "João Silva",
                    CPF.of("54260232015"),
                    passwordEncoder.encode("user123"),
                    "joao.silva@techsolutions.com",
                    Role.USER
            ).build();

            Employee employee = new Employee.Builder("João Silva", techSolutions, employeeAccount).build();
            employee.activeEmployee(EmployeeStatus.ACTIVE);
            employeeRepository.save(employee);

            // 8. Partnership: TechSolutions contracts Dental Plan
            Partnership partnership = new Partnership.Builder(techSolutions, dentalPlan).build();
            partnership.updateStatus(PartnershipStatus.ACTIVE);
            partnershipRepository.save(partnership);

            System.out.println("--- Database Populated with Dev Data ---");
            System.out.println("Admin: admin@benefix.com / admin123");
            System.out.println("HealthCorp Manager: manager@healthcorp.com / manager123");
            System.out.println("TechSolutions Manager: manager@techsolutions.com / manager123");
            System.out.println("Employee: joao.silva@techsolutions.com / user123");
        };
    }
}

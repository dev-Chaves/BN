package org.acme.domains.subscription;

import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.NotFoundException;
import org.acme.domains.account.AccountRepository;
import org.acme.domains.benefit.Benefit;
import org.acme.domains.benefit.BenefitRepository;
import org.acme.domains.employee.Employee;
import org.acme.domains.employee.EmployeeRepository;
import org.acme.domains.partnership.Partnership;
import org.acme.domains.partnership.PartnershipRepository;
import org.acme.domains.shared.security.TenantGuard;
import org.acme.domains.subscription.dto.CreateSubscriptionRequest;
import org.acme.domains.subscription.dto.SubscriptionResponse;

@ApplicationScoped
public class SubscriptionService {

    private final BenefitRepository benefitRepository;
    private final EmployeeRepository employeeRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final AccountRepository accountRepository;
    private final TenantGuard tenantGuard;
    private final PartnershipRepository partnershipRepository;

    public SubscriptionService(BenefitRepository benefitRepository, EmployeeRepository employeeRepository, SubscriptionRepository subscriptionRepository, AccountRepository accountRepository, TenantGuard tenantGuard, PartnershipRepository partnershipRepository) {
        this.benefitRepository = benefitRepository;
        this.employeeRepository = employeeRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.accountRepository = accountRepository;
        this.tenantGuard = tenantGuard;
        this.partnershipRepository = partnershipRepository;
    }

    @WithTransaction
    public Uni<SubscriptionResponse> subscribeToBenefit(CreateSubscriptionRequest request, String email){

        return benefitRepository.findById(request.benefitId()).onItem().ifNull().failWith(new NotFoundException("Benefit not found"))
                .flatMap(benefit -> findEmployeeByAccountEmail(email)
                        .flatMap(employee -> tenantGuard.verifyEmployeeBenefitAccess(employee, benefit)
                                .flatMap(allowedBenefit -> createSubscription(allowedBenefit, employee))))
                .call(subscriptionRepository::persist)
                .map(subscription -> new SubscriptionResponse(
                        subscription.id,
                        subscription.getEmployee().getName(),
                        subscription.getBenefit().getName(),
                        subscription.getCreatedAt()
                ));

    }

    private Uni<Partnership> verifyActivePartnership(Benefit benefit){

    }

    private Uni<Employee> findEmployeeByAccountEmail(String email) {
        return accountRepository.findByEmail(email)
                .onItem().ifNull().failWith(() -> new NotFoundException("Account not found"))
                .flatMap(account -> employeeRepository.findByAccountId(account.id))
                .onItem().ifNull().failWith(() -> new NotFoundException("Employee not found"));
    }

    private Uni<Benefit> getPartnershipByBenefit(Long benefitId) {
        return partnershipRepository.findByBenefitId(benefitId).onItem().ifNull().failWith(() -> new NotFoundException("Partnership not found"));
    }

    private Uni<Subscription> createSubscription(Benefit benefit, Employee employee){
        return Uni.createFrom().item(Subscription.builder(benefit, employee).build());
    }
}

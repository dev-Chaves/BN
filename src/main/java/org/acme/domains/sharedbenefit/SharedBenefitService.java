package org.acme.domains.sharedbenefit;

import io.quarkus.hibernate.reactive.panache.common.WithSession;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.NotFoundException;
import org.acme.domains.account.AccountRepository;
import org.acme.domains.benefit.Benefit;
import org.acme.domains.benefit.BenefitRepository;
import org.acme.domains.benefitrequest.BenefitAccessRequestRepository;
import org.acme.domains.category.dto.CategoryResponse;
import org.acme.domains.employee.Employee;
import org.acme.domains.employee.EmployeeRepository;
import org.acme.domains.sharedbenefit.dto.SharedBenefitResponse;
import org.acme.domains.subscription.Subscription;
import org.acme.domains.subscription.SubscriptionRepository;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@ApplicationScoped
public class SharedBenefitService {
    private final AccountRepository accountRepository;
    private final EmployeeRepository employeeRepository;
    private final BenefitRepository benefitRepository;
    private final BenefitAccessRequestRepository requestRepository;
    private final SubscriptionRepository subscriptionRepository;

    public SharedBenefitService(AccountRepository accountRepository,
                                EmployeeRepository employeeRepository,
                                BenefitRepository benefitRepository,
                                BenefitAccessRequestRepository requestRepository,
                                SubscriptionRepository subscriptionRepository) {
        this.accountRepository = accountRepository;
        this.employeeRepository = employeeRepository;
        this.benefitRepository = benefitRepository;
        this.requestRepository = requestRepository;
        this.subscriptionRepository = subscriptionRepository;
    }

    @WithSession
    public Uni<List<SharedBenefitResponse>> available(String email) {
        return findEmployee(email)
                .flatMap(employee -> benefitRepository.findPublicAvailableByProviderNot(employee.getCompany().id)
                        .flatMap(benefits -> requestRepository.findByEmployee(employee.id)
                                .flatMap(requests -> subscriptionRepository.findByEmployeeWithBenefit(employee.id)
                        .map(subscriptions -> {
                            Set<Long> subscribed = subscriptions.stream()
                                    .map(s -> s.getBenefit().id).collect(Collectors.toSet());
                            var requestStatus = requests.stream().collect(Collectors.toMap(
                                    r -> r.getBenefit().id,
                                    r -> r.getStatus().name(),
                                    (first, second) -> first
                            ));
                            return benefits.stream()
                                    .filter(b -> !subscribed.contains(b.id))
                                    .map(b -> toResponse(b, null, requestStatus.getOrDefault(b.id, "AVAILABLE_TO_REQUEST")))
                                    .toList();
                        }))));
    }

    @WithSession
    public Uni<List<SharedBenefitResponse>> mine(String email) {
        return findEmployee(email)
                .flatMap(employee -> subscriptionRepository.findByEmployeeWithBenefit(employee.id))
                .map(items -> items.stream()
                        .filter(item -> item.getBenefit().isAvailableAt(java.time.LocalDateTime.now()))
                        .map(this::toResponse)
                        .toList());
    }

    private Uni<Employee> findEmployee(String email) {
        return accountRepository.findByEmail(email)
                .onItem().ifNull().failWith(() -> new NotFoundException("Account not found"))
                .flatMap(account -> employeeRepository.findByAccountId(account.id))
                .onItem().ifNull().failWith(() -> new NotFoundException("Employee not found"));
    }

    private SharedBenefitResponse toResponse(Subscription subscription) {
        return toResponse(subscription.getBenefit(), subscription.id, "APPROVED");
    }

    private SharedBenefitResponse toResponse(Benefit benefit, Long subscriptionId, String accessStatus) {
        return new SharedBenefitResponse(
                benefit.id,
                subscriptionId,
                benefit.getName(),
                benefit.getDescription(),
                benefit.getProvider().getName(),
                benefit.getCategories().stream().map(c -> new CategoryResponse(c.id, c.getName())).toList(),
                benefit.getValidUntil(),
                benefit.getMaxUsesPerUser(),
                benefit.getTerms(),
                accessStatus
        );
    }
}

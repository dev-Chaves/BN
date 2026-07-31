package org.acme.domains.benefitrequest;

import io.quarkus.hibernate.reactive.panache.common.WithSession;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.NotFoundException;
import org.acme.domains.account.AccountRepository;
import org.acme.domains.benefit.BenefitRepository;
import org.acme.domains.benefitrequest.dto.BenefitAccessRequestResponse;
import org.acme.domains.employee.Employee;
import org.acme.domains.employee.EmployeeRepository;
import org.acme.domains.manager.Manager;
import org.acme.domains.manager.ManagerRepository;
import org.acme.domains.subscription.Subscription;
import org.acme.domains.subscription.SubscriptionRepository;
import org.acme.domains.shared.security.AccessStatusGuard;

import java.time.LocalDateTime;
import java.util.List;

@ApplicationScoped
public class BenefitAccessRequestService {
    private final AccountRepository accountRepository;
    private final EmployeeRepository employeeRepository;
    private final ManagerRepository managerRepository;
    private final BenefitRepository benefitRepository;
    private final BenefitAccessRequestRepository requestRepository;
    private final SubscriptionRepository subscriptionRepository;

    public BenefitAccessRequestService(
            AccountRepository accountRepository,
            EmployeeRepository employeeRepository,
            ManagerRepository managerRepository,
            BenefitRepository benefitRepository,
            BenefitAccessRequestRepository requestRepository,
            SubscriptionRepository subscriptionRepository
    ) {
        this.accountRepository = accountRepository;
        this.employeeRepository = employeeRepository;
        this.managerRepository = managerRepository;
        this.benefitRepository = benefitRepository;
        this.requestRepository = requestRepository;
        this.subscriptionRepository = subscriptionRepository;
    }

    @WithTransaction
    public Uni<BenefitAccessRequestResponse> request(String email, Long benefitId) {
        return findEmployee(email)
                .flatMap(employee -> benefitRepository.findByIdWithProviderAndCategories(benefitId)
                        .onItem().ifNull().failWith(() -> new NotFoundException("Benefit not found"))
                        .flatMap(benefit -> {
                            if (benefit.getProvider().id.equals(employee.getCompany().id)) {
                                return Uni.createFrom().failure(new IllegalArgumentException("Own-company benefit does not require sharing"));
                            }
                            if (!benefit.isAvailableAt(LocalDateTime.now())) {
                                return Uni.createFrom().failure(new IllegalStateException("Benefit is not available"));
                            }
                            return subscriptionRepository.existsByEmployeeAndBenefit(employee.id, benefit.id)
                                    .flatMap(subscribed -> subscribed
                                            ? Uni.createFrom().failure(new IllegalStateException("Benefit is already available to this employee"))
                                            : requestRepository.findPending(employee.id, benefit.id)
                                                    .flatMap(existing -> existing != null
                                                            ? Uni.createFrom().failure(new IllegalStateException("A pending request already exists"))
                                                            : requestRepository.persist(new BenefitAccessRequest(employee, benefit))));
                        }))
                .map(this::toResponse);
    }

    @WithSession
    public Uni<List<BenefitAccessRequestResponse>> mine(String email) {
        return findEmployee(email)
                .flatMap(employee -> requestRepository.findByEmployee(employee.id))
                .map(items -> items.stream().map(this::toResponse).toList());
    }

    @WithSession
    public Uni<List<BenefitAccessRequestResponse>> providerPending(String email) {
        return findManager(email)
                .flatMap(manager -> requestRepository.findPendingByProvider(manager.getCompany().id))
                .map(items -> items.stream().map(this::toResponse).toList());
    }

    @WithTransaction
    public Uni<BenefitAccessRequestResponse> approve(String email, Long requestId) {
        return findManager(email)
                .flatMap(manager -> requestRepository.findByIdWithRelations(requestId)
                        .onItem().ifNull().failWith(() -> new NotFoundException("Request not found"))
                        .flatMap(request -> verifyProvider(manager, request)
                                .flatMap(verified -> subscriptionRepository.existsByEmployeeAndBenefit(
                                                verified.getEmployee().id, verified.getBenefit().id)
                                        .flatMap(exists -> {
                                            if (!verified.getBenefit().isAvailableAt(LocalDateTime.now())) {
                                                return Uni.createFrom().failure(new IllegalStateException("Benefit is no longer available"));
                                            }
                                            verified.approve(manager);
                                            if (exists) return Uni.createFrom().item(verified);
                                            return subscriptionRepository.persist(
                                                    Subscription.builder(verified.getBenefit(), verified.getEmployee()).build())
                                                    .replaceWith(verified);
                                        }))))
                .map(this::toResponse);
    }

    @WithTransaction
    public Uni<BenefitAccessRequestResponse> reject(String email, Long requestId, String reason) {
        return findManager(email)
                .flatMap(manager -> requestRepository.findByIdWithRelations(requestId)
                        .onItem().ifNull().failWith(() -> new NotFoundException("Request not found"))
                        .flatMap(request -> verifyProvider(manager, request)
                                .invoke(verified -> verified.reject(manager, reason))))
                .map(this::toResponse);
    }

    private Uni<Employee> findEmployee(String email) {
        return accountRepository.findByEmail(email)
                .onItem().ifNull().failWith(() -> new NotFoundException("Account not found"))
                .flatMap(account -> employeeRepository.findByAccountId(account.id))
                .onItem().ifNull().failWith(() -> new NotFoundException("Employee not found"))
                .map(AccessStatusGuard::requireActive);
    }

    private Uni<Manager> findManager(String email) {
        return managerRepository.findByEmail(email)
                .onItem().ifNull().failWith(() -> new NotFoundException("Manager not found"))
                .map(AccessStatusGuard::requireActive);
    }

    private Uni<BenefitAccessRequest> verifyProvider(Manager manager, BenefitAccessRequest request) {
        if (!request.getBenefit().getProvider().id.equals(manager.getCompany().id)) {
            return Uni.createFrom().failure(new SecurityException("Only the provider company can review this request"));
        }
        return Uni.createFrom().item(request);
    }

    private BenefitAccessRequestResponse toResponse(BenefitAccessRequest request) {
        return new BenefitAccessRequestResponse(
                request.id,
                request.getBenefit().id,
                request.getBenefit().getName(),
                request.getBenefit().getProvider().getName(),
                request.getEmployee().id,
                request.getEmployee().getName(),
                request.getEmployee().getCompany().getName(),
                request.getStatus(),
                request.getRequestedAt(),
                request.getReviewedAt(),
                request.getRejectionReason()
        );
    }
}

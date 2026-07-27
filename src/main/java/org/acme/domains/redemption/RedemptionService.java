package org.acme.domains.redemption;

import io.quarkus.hibernate.reactive.panache.common.WithSession;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.NotFoundException;
import org.acme.domains.account.AccountRepository;
import org.acme.domains.employee.Employee;
import org.acme.domains.employee.EmployeeRepository;
import org.acme.domains.manager.Manager;
import org.acme.domains.manager.ManagerRepository;
import org.acme.domains.redemption.dto.RedemptionPreviewResponse;
import org.acme.domains.redemption.dto.RedemptionResponse;
import org.acme.domains.redemption.dto.RedemptionTokenResponse;
import org.acme.domains.subscription.Subscription;
import org.acme.domains.subscription.SubscriptionRepository;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;

@ApplicationScoped
public class RedemptionService {
    private static final SecureRandom RANDOM = new SecureRandom();

    private final AccountRepository accountRepository;
    private final EmployeeRepository employeeRepository;
    private final ManagerRepository managerRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final RedemptionTokenRepository tokenRepository;
    private final BenefitRedemptionRepository redemptionRepository;

    @ConfigProperty(name = "app.public-url", defaultValue = "http://localhost:3000")
    String publicUrl;

    public RedemptionService(AccountRepository accountRepository,
                             EmployeeRepository employeeRepository,
                             ManagerRepository managerRepository,
                             SubscriptionRepository subscriptionRepository,
                             RedemptionTokenRepository tokenRepository,
                             BenefitRedemptionRepository redemptionRepository) {
        this.accountRepository = accountRepository;
        this.employeeRepository = employeeRepository;
        this.managerRepository = managerRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.tokenRepository = tokenRepository;
        this.redemptionRepository = redemptionRepository;
    }

    @WithTransaction
    public Uni<RedemptionTokenResponse> issue(String email, Long subscriptionId) {
        return findEmployee(email)
                .flatMap(employee -> subscriptionRepository.findOwnedWithBenefit(subscriptionId, employee.id)
                        .onItem().ifNull().failWith(() -> new NotFoundException("Subscription not found"))
                        .flatMap(subscription -> {
                            if (!subscription.getBenefit().isAvailableAt(LocalDateTime.now())) {
                                return Uni.createFrom().failure(new IllegalStateException("Benefit is not available"));
                            }
                            return validateUsageLimit(subscription).replaceWith(subscription);
                        }))
                .flatMap(subscription -> tokenRepository.revokeActiveBySubscription(subscription.id)
                        .replaceWith(subscription))
                .flatMap(subscription -> {
                    String rawToken = generateToken();
                    LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(3);
                    return tokenRepository.persist(new RedemptionToken(subscription, hash(rawToken), expiresAt))
                            .replaceWith(new RedemptionTokenResponse(
                                    rawToken,
                                    publicUrl.replaceAll("/$", "") + "/resgatar/" + rawToken,
                                    expiresAt
                            ));
                });
    }

    @WithSession
    public Uni<RedemptionPreviewResponse> preview(String managerEmail, String rawToken) {
        return findManager(managerEmail)
                .flatMap(manager -> findValidToken(rawToken)
                        .flatMap(token -> verifyProvider(manager, token))
                        .map(token -> new RedemptionPreviewResponse(
                                true,
                                token.getSubscription().getBenefit().getName(),
                                token.getSubscription().getEmployee().getName(),
                                token.getSubscription().getBenefit().getProvider().getName(),
                                token.getExpiresAt(),
                                "Benefit ready to redeem"
                        )));
    }

    @WithTransaction
    public Uni<RedemptionResponse> consume(String managerEmail, String rawToken) {
        LocalDateTime now = LocalDateTime.now();
        return findManager(managerEmail)
                .flatMap(manager -> findValidToken(rawToken)
                        .flatMap(token -> verifyProvider(manager, token)
                                .flatMap(valid -> validateUsageLimit(valid.getSubscription())
                                        .replaceWith(valid))
                                .flatMap(valid -> tokenRepository.consumeIfActive(valid.id, now)
                                        .flatMap(updated -> {
                                            if (updated != 1) {
                                                return Uni.createFrom().failure(new IllegalStateException("Token expired or already used"));
                                            }
                                            BenefitRedemption redemption = new BenefitRedemption(
                                                    valid.getSubscription(),
                                                    valid,
                                                    valid.getSubscription().getBenefit().getProvider(),
                                                    manager
                                            );
                                            return redemptionRepository.persist(redemption);
                                        }))))
                .map(redemption -> new RedemptionResponse(
                        redemption.id,
                        redemption.getSubscription().getBenefit().getName(),
                        redemption.getSubscription().getEmployee().getName(),
                        redemption.getRedeemedAt()
                ));
    }

    private Uni<RedemptionToken> findValidToken(String rawToken) {
        return tokenRepository.findByHashWithRelations(hash(rawToken))
                .onItem().ifNull().failWith(() -> new NotFoundException("Token not found"))
                .flatMap(token -> {
                    if (token.getStatus() != RedemptionTokenStatus.ACTIVE || !token.getExpiresAt().isAfter(LocalDateTime.now())) {
                        return Uni.createFrom().failure(new IllegalStateException("Token expired or already used"));
                    }
                    if (!token.getSubscription().getBenefit().isAvailableAt(LocalDateTime.now())) {
                        return Uni.createFrom().failure(new IllegalStateException("Benefit is not available"));
                    }
                    return Uni.createFrom().item(token);
                });
    }

    private Uni<RedemptionToken> verifyProvider(Manager manager, RedemptionToken token) {
        if (!token.getSubscription().getBenefit().getProvider().id.equals(manager.getCompany().id)) {
            return Uni.createFrom().failure(new SecurityException("This establishment cannot redeem the benefit"));
        }
        return Uni.createFrom().item(token);
    }

    private Uni<Void> validateUsageLimit(Subscription subscription) {
        return redemptionRepository.countBySubscription(subscription.id)
                .flatMap(count -> count >= subscription.getBenefit().getMaxUsesPerUser()
                        ? Uni.createFrom().failure(new IllegalStateException("Benefit usage limit reached"))
                        : Uni.createFrom().voidItem());
    }

    private Uni<Employee> findEmployee(String email) {
        return accountRepository.findByEmail(email)
                .onItem().ifNull().failWith(() -> new NotFoundException("Account not found"))
                .flatMap(account -> employeeRepository.findByAccountId(account.id))
                .onItem().ifNull().failWith(() -> new NotFoundException("Employee not found"));
    }

    private Uni<Manager> findManager(String email) {
        return managerRepository.findByEmail(email)
                .onItem().ifNull().failWith(() -> new NotFoundException("Manager not found"));
    }

    private String generateToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String token) {
        try {
            return java.util.HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8))
            );
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }
}

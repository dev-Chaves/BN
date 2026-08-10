package com.bnfix.ubm.domains.redemption;

import com.bnfix.ubm.domains.account.AccountRepository;
import com.bnfix.ubm.domains.employee.Employee;
import com.bnfix.ubm.domains.employee.EmployeeRepository;
import com.bnfix.ubm.domains.manager.Manager;
import com.bnfix.ubm.domains.manager.ManagerRepository;
import com.bnfix.ubm.domains.redemption.dto.RedemptionPreviewResponse;
import com.bnfix.ubm.domains.redemption.dto.RedemptionResponse;
import com.bnfix.ubm.domains.redemption.dto.RedemptionTokenResponse;
import com.bnfix.ubm.domains.subscription.Subscription;
import com.bnfix.ubm.domains.subscription.SubscriptionRepository;
import com.bnfix.ubm.shared.security.AccessStatusGuard;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Service
public class RedemptionService {
    private final AccountRepository accountRepository;
    private final EmployeeRepository employeeRepository;
    private final ManagerRepository managerRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final RedemptionTokenRepository redemptionTokenRepository;
    private final BenefitRedemptionRepository benefitRedemptionRepository;
    private final SecureRandom random = new SecureRandom();
    private final String publicUrl;

    public RedemptionService(
            AccountRepository accountRepository,
            EmployeeRepository employeeRepository,
            ManagerRepository managerRepository,
            SubscriptionRepository subscriptionRepository,
            RedemptionTokenRepository redemptionTokenRepository,
            BenefitRedemptionRepository benefitRedemptionRepository,
            @Value("${app.public-url:http://localhost:3000}") String publicUrl) {
        this.accountRepository = accountRepository;
        this.employeeRepository = employeeRepository;
        this.managerRepository = managerRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.redemptionTokenRepository = redemptionTokenRepository;
        this.benefitRedemptionRepository = benefitRedemptionRepository;
        this.publicUrl = publicUrl;
    }

    @Transactional
    public RedemptionTokenResponse issue(String email, Long subscriptionId) {
        Employee employee = findEmployee(email);
        Subscription subscription = subscriptionRepository
                .findOwnedWithBenefit(subscriptionId, employee.id)
                .orElseThrow(() -> notFound("Subscription not found"));
        LocalDateTime now = LocalDateTime.now();
        if (!subscription.getBenefit().isOperationalAt(now))
            throw new IllegalStateException("Benefit is not available");
        validateUsageLimit(subscription);
        redemptionTokenRepository.revokeActiveBySubscription(subscription.id);
        String rawToken = generateToken();
        LocalDateTime expiresAt = now.plusMinutes(3);
        redemptionTokenRepository.save(new RedemptionToken(subscription, hash(rawToken), expiresAt));
        log.info("Redemption token issued by employee {} for subscription {}", employee.id, subscriptionId);
        return new RedemptionTokenResponse(
                rawToken, publicUrl.replaceAll("/$", "") + "/resgatar/" + rawToken, expiresAt);
    }

    @Transactional(readOnly = true)
    public RedemptionPreviewResponse preview(String managerEmail, Long companyId, String rawToken) {
        Manager manager = findManager(managerEmail, companyId);
        RedemptionToken token = findValidToken(rawToken);
        verifyProvider(manager, token);
        return new RedemptionPreviewResponse(
                true,
                token.getSubscription().getBenefit().getName(),
                token.getSubscription().getEmployee().getName(),
                token.getSubscription().getBenefit().getProvider().getName(),
                token.getExpiresAt(),
                "Benefit ready to redeem");
    }

    @Transactional
    public RedemptionResponse consume(String managerEmail, Long companyId, String rawToken) {
        Manager manager = findManager(managerEmail, companyId);
        RedemptionToken token = findValidToken(rawToken);
        verifyProvider(manager, token);
        validateUsageLimit(token.getSubscription());
        LocalDateTime now = LocalDateTime.now();
        if (redemptionTokenRepository.consumeIfActive(token.id, now) != 1)
            throw new IllegalStateException("Token expired or already used");
        BenefitRedemption redemption = benefitRedemptionRepository.save(new BenefitRedemption(
                token.getSubscription(),
                token,
                token.getSubscription().getBenefit().getProvider(),
                manager));
        log.info(
                "Benefit redeemed by manager {} (redemption {}, benefit {}, employee {})",
                manager.id,
                redemption.id,
                token.getSubscription().getBenefit().id,
                token.getSubscription().getEmployee().id);
        return new RedemptionResponse(
                redemption.id,
                redemption.getSubscription().getBenefit().getName(),
                redemption.getSubscription().getEmployee().getName(),
                redemption.getRedeemedAt());
    }

    private RedemptionToken findValidToken(String rawToken) {
        RedemptionToken token = redemptionTokenRepository
                .findByHashWithRelations(hash(rawToken))
                .orElseThrow(() -> notFound("Token not found"));
        LocalDateTime now = LocalDateTime.now();
        if (token.getStatus() != RedemptionTokenStatus.ACTIVE
                || !token.getExpiresAt().isAfter(now)) throw new IllegalStateException("Token expired or already used");
        AccessStatusGuard.requireActive(token.getSubscription().getEmployee());
        if (!token.getSubscription().getBenefit().isOperationalAt(now))
            throw new IllegalStateException("Benefit is not available");
        return token;
    }

    private void verifyProvider(Manager manager, RedemptionToken token) {
        if (!token.getSubscription().getBenefit().getProvider().id.equals(manager.getCompany().id))
            throw new SecurityException("This establishment cannot redeem the benefit");
    }

    private void validateUsageLimit(Subscription subscription) {
        if (benefitRedemptionRepository.countBySubscriptionId(subscription.id)
                >= subscription.getBenefit().getMaxUsesPerUser())
            throw new IllegalStateException("Benefit usage limit reached");
    }

    private Employee findEmployee(String email) {
        var account = accountRepository.findByEmail(email).orElseThrow(() -> notFound("Account not found"));
        return AccessStatusGuard.requireActive(
                employeeRepository.findByAccountId(account.id).orElseThrow(() -> notFound("Employee not found")));
    }

    private Manager findManager(String email, Long companyId) {
        return AccessStatusGuard.requireActive(managerRepository
                .findByEmailAndCompanyId(email, companyId)
                .orElseThrow(() -> notFound("Manager not found")));
    }

    private String generateToken() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String token) {
        try {
            return java.util.HexFormat.of()
                    .formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    private ResponseStatusException notFound(String message) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, message);
    }
}

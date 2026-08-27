package com.bnfix.ubm.domains.redemption;

import com.bnfix.ubm.domains.account.AccountRepository;
import com.bnfix.ubm.domains.benefit.Benefit;
import com.bnfix.ubm.domains.benefit.BenefitAccessPolicy;
import com.bnfix.ubm.domains.benefit.BenefitRepository;
import com.bnfix.ubm.domains.employee.Employee;
import com.bnfix.ubm.domains.employee.EmployeeRepository;
import com.bnfix.ubm.domains.manager.Manager;
import com.bnfix.ubm.domains.manager.ManagerRepository;
import com.bnfix.ubm.domains.redemption.dto.RedemptionPreviewResponse;
import com.bnfix.ubm.domains.redemption.dto.RedemptionResponse;
import com.bnfix.ubm.domains.redemption.dto.RedemptionTokenResponse;
import com.bnfix.ubm.shared.security.AccessStatusGuard;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
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
    private final BenefitRepository benefitRepository;
    private final RedemptionTokenRepository redemptionTokenRepository;
    private final BenefitRedemptionRepository benefitRedemptionRepository;
    private final BenefitAccessPolicy benefitAccessPolicy;
    private final SecureRandom random = new SecureRandom();
    private final String publicUrl;

    public RedemptionService(
            AccountRepository accountRepository,
            EmployeeRepository employeeRepository,
            ManagerRepository managerRepository,
            BenefitRepository benefitRepository,
            RedemptionTokenRepository redemptionTokenRepository,
            BenefitRedemptionRepository benefitRedemptionRepository,
            BenefitAccessPolicy benefitAccessPolicy,
            @Value("${app.public-url:http://localhost:3000}") String publicUrl) {
        this.accountRepository = accountRepository;
        this.employeeRepository = employeeRepository;
        this.managerRepository = managerRepository;
        this.benefitRepository = benefitRepository;
        this.redemptionTokenRepository = redemptionTokenRepository;
        this.benefitRedemptionRepository = benefitRedemptionRepository;
        this.benefitAccessPolicy = benefitAccessPolicy;
        this.publicUrl = publicUrl;
    }

    @Transactional
    public RedemptionTokenResponse issue(String email, Long benefitId) {
        Employee employee = findEmployee(email);
        Benefit benefit = benefitRepository
                .findByIdWithProviderAndCategories(benefitId)
                .orElseThrow(() -> notFound("Benefit not found"));
        LocalDateTime now = LocalDateTime.now();
        benefitAccessPolicy.requireEligible(employee, benefit, now);
        validateUsageLimit(employee, benefit);
        redemptionTokenRepository.revokeActiveByEmployeeAndBenefit(employee.id, benefit.id);
        String rawToken = generateToken();
        LocalDateTime expiresAt = now.plusMinutes(3);
        redemptionTokenRepository.save(new RedemptionToken(employee, benefit, hash(rawToken), expiresAt));
        log.info("Redemption token issued by employee {} for benefit {}", employee.id, benefitId);
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
                token.getBenefit().getName(),
                token.getEmployee().getName(),
                token.getBenefit().getProvider().getName(),
                token.getExpiresAt(),
                "Benefit ready to redeem");
    }

    @Transactional
    public RedemptionResponse consume(String managerEmail, Long companyId, String rawToken) {
        Manager manager = findManager(managerEmail, companyId);
        RedemptionToken token = findValidToken(rawToken);
        verifyProvider(manager, token);
        validateUsageLimit(token.getEmployee(), token.getBenefit());
        LocalDateTime now = LocalDateTime.now();
        if (redemptionTokenRepository.consumeIfActive(token.id, now) != 1)
            throw new IllegalStateException("Token expired or already used");
        BenefitRedemption redemption = benefitRedemptionRepository.save(new BenefitRedemption(
                token.getEmployee(),
                token.getBenefit(),
                token,
                token.getBenefit().getProvider(),
                token.getEmployee().getCompany(),
                manager));
        log.info(
                "Benefit redeemed by manager {} (redemption {}, benefit {}, employee {})",
                manager.id,
                redemption.id,
                token.getBenefit().id,
                token.getEmployee().id);
        return new RedemptionResponse(
                redemption.id,
                redemption.getBenefit().getName(),
                redemption.getEmployee().getName(),
                redemption.getRedeemedAt());
    }

    private RedemptionToken findValidToken(String rawToken) {
        RedemptionToken token = redemptionTokenRepository
                .findByHashWithRelations(hash(rawToken))
                .orElseThrow(() -> notFound("Token not found"));
        LocalDateTime now = LocalDateTime.now();
        if (token.getStatus() != RedemptionTokenStatus.ACTIVE
                || !token.getExpiresAt().isAfter(now)) throw new IllegalStateException("Token expired or already used");
        benefitAccessPolicy.requireEligible(token.getEmployee(), token.getBenefit(), now);
        return token;
    }

    private void verifyProvider(Manager manager, RedemptionToken token) {
        if (!token.getBenefit().getProvider().id.equals(manager.getCompany().id))
            throw new SecurityException("This establishment cannot redeem the benefit");
    }

    private void validateUsageLimit(Employee employee, Benefit benefit) {
        if (benefitRedemptionRepository.countByEmployeeIdAndBenefitId(employee.id, benefit.id)
                >= benefit.getMaxUsesPerUser()) throw new IllegalStateException("Benefit usage limit reached");
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
            return HexFormat.of()
                    .formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    private ResponseStatusException notFound(String message) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, message);
    }
}

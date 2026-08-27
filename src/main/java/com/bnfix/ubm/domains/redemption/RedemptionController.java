package com.bnfix.ubm.domains.redemption;

import com.bnfix.ubm.domains.redemption.dto.RedemptionTokenRequest;
import com.bnfix.ubm.shared.security.JwtCompanyContext;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/redemptions")
public class RedemptionController {
    private final RedemptionService redemptionService;

    public RedemptionController(RedemptionService redemptionService) {
        this.redemptionService = redemptionService;
    }

    @PostMapping("/benefits/{benefitId}/token")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> issue(@PathVariable Long benefitId, Authentication authentication) {
        log.info("POST /redemptions/benefits/{}/token by {}", benefitId, authentication.getName());
        return ResponseEntity.status(201).body(redemptionService.issue(authentication.getName(), benefitId));
    }

    @PostMapping("/provider/preview")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<?> preview(
            @Valid @RequestBody RedemptionTokenRequest request,
            Authentication authentication,
            @AuthenticationPrincipal Jwt jwt) {
        log.info("POST /redemptions/provider/preview by {}", authentication.getName());
        return ResponseEntity.ok(redemptionService.preview(
                authentication.getName(), JwtCompanyContext.requireCompanyId(jwt), request.token()));
    }

    @PostMapping("/provider/consume")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<?> consume(
            @Valid @RequestBody RedemptionTokenRequest request,
            Authentication authentication,
            @AuthenticationPrincipal Jwt jwt) {
        log.info("POST /redemptions/provider/consume by {}", authentication.getName());
        return ResponseEntity.ok(redemptionService.consume(
                authentication.getName(), JwtCompanyContext.requireCompanyId(jwt), request.token()));
    }
}

package com.bnfix.ubm.domains.redemption;

import com.bnfix.ubm.domains.redemption.dto.RedemptionTokenRequest;
import com.bnfix.ubm.shared.security.JwtCompanyContext;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/redemptions")
public class RedemptionController {
    private final RedemptionService service;

    public RedemptionController(RedemptionService service) {
        this.service = service;
    }

    @PostMapping("/subscriptions/{subscriptionId}/token")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> issue(@PathVariable Long subscriptionId, Authentication authentication) {
        return ResponseEntity.status(201).body(service.issue(authentication.getName(), subscriptionId));
    }

    @PostMapping("/provider/preview")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<?> preview(
            @Valid @RequestBody RedemptionTokenRequest request,
            Authentication authentication,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(
                service.preview(authentication.getName(), JwtCompanyContext.requireCompanyId(jwt), request.token()));
    }

    @PostMapping("/provider/consume")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<?> consume(
            @Valid @RequestBody RedemptionTokenRequest request,
            Authentication authentication,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(
                service.consume(authentication.getName(), JwtCompanyContext.requireCompanyId(jwt), request.token()));
    }
}

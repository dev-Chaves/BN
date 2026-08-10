package com.bnfix.ubm.domains.subscription;

import com.bnfix.ubm.domains.subscription.dto.*;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/subscriptions")
public class SubscriptionController {
    private final SubscriptionService subscriptionService;

    public SubscriptionController(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    @PostMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<SubscriptionResponse> subscribe(
            @Valid @RequestBody CreateSubscriptionRequest request, @AuthenticationPrincipal Jwt jwt) {
        log.info("POST /subscriptions (benefitId={}) by {}", request.benefitId(), jwt.getSubject());
        return ResponseEntity.status(HttpStatus.CREATED).body(subscriptionService.subscribe(request, jwt.getSubject()));
    }
}

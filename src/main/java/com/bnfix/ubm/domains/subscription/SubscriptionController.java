package com.bnfix.ubm.domains.subscription;

import com.bnfix.ubm.domains.subscription.dto.*;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/subscriptions")
public class SubscriptionController {
    private final SubscriptionService s;

    public SubscriptionController(SubscriptionService s) {
        this.s = s;
    }

    @PostMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<SubscriptionResponse> subscribe(
            @Valid @RequestBody CreateSubscriptionRequest r, @AuthenticationPrincipal Jwt j) {
        return ResponseEntity.status(HttpStatus.CREATED).body(s.subscribe(r, j.getSubject()));
    }
}

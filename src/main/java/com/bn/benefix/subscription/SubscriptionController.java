package com.bn.benefix.subscription;

import com.bn.benefix.infra.security.AccountUserDetails;
import com.bn.benefix.subscription.dto.SubscriptionCreationRequestDTO;
import com.bn.benefix.subscription.dto.SubscriptionResponseDTO;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("subscription")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    public SubscriptionController(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    @PostMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<SubscriptionResponseDTO> subscribe(
            @Valid @RequestBody SubscriptionCreationRequestDTO dto,
            UriComponentsBuilder builder,
            @AuthenticationPrincipal AccountUserDetails userDetails) {

        SubscriptionResponseDTO response = subscriptionService.subscribe(dto, userDetails.getAccount().getId());

        URI uri = builder.path("/subscription/{id}")
                .buildAndExpand(response.id())
                .toUri();
        return ResponseEntity.created(uri).body(response);
    }

    @GetMapping
    public ResponseEntity<List<SubscriptionResponseDTO>> getMySubscriptions(@AuthenticationPrincipal AccountUserDetails userDetails) {
        return ResponseEntity.ok(subscriptionService.findAll(userDetails.getAccount()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancelSubscription(
            @PathVariable Long id,
            @AuthenticationPrincipal AccountUserDetails userDetails) {
        subscriptionService.cancelSubscription(id, userDetails.getAccount());
        return ResponseEntity.noContent().build();
    }
}

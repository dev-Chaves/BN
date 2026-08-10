package com.bnfix.ubm.domains.partnership;

import com.bnfix.ubm.domains.partnership.dto.*;
import jakarta.validation.Valid;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/partnerships")
public class PartnershipController {
    private final PartnershipService partnershipService;

    public PartnershipController(PartnershipService partnershipService) {
        this.partnershipService = partnershipService;
    }

    private Long c(Jwt jwt) {
        Object value = jwt.getClaims().get("companyId");
        return value instanceof Number number ? number.longValue() : Long.valueOf(value.toString());
    }

    @PostMapping
    @PreAuthorize("hasRole('MANAGER')")
    public PartnershipResponse request(
            @Valid @RequestBody CreatePartnershipRequest createPartnershipRequest, @AuthenticationPrincipal Jwt jwt) {
        log.info("POST /partnerships (benefitId={}) by {}", createPartnershipRequest.benefitId(), jwt.getSubject());
        return partnershipService.request(jwt.getSubject(), c(jwt), createPartnershipRequest.benefitId());
    }

    @GetMapping("/provider/pending")
    @PreAuthorize("hasRole('MANAGER')")
    public List<PartnershipResponse> pending(@AuthenticationPrincipal Jwt jwt) {
        return partnershipService.pending(jwt.getSubject(), c(jwt));
    }

    @PutMapping("/accept")
    @PreAuthorize("hasRole('MANAGER')")
    public PartnershipResponse accept(@RequestParam Long partnershipId, @AuthenticationPrincipal Jwt jwt) {
        log.info("PUT /partnerships/accept (partnershipId={}) by {}", partnershipId, jwt.getSubject());
        return partnershipService.transition(jwt.getSubject(), c(jwt), partnershipId, PartnershipStatus.ACTIVE);
    }

    @PutMapping("/reject")
    @PreAuthorize("hasRole('MANAGER')")
    public PartnershipResponse reject(@RequestParam Long partnershipId, @AuthenticationPrincipal Jwt jwt) {
        log.info("PUT /partnerships/reject (partnershipId={}) by {}", partnershipId, jwt.getSubject());
        return partnershipService.transition(jwt.getSubject(), c(jwt), partnershipId, PartnershipStatus.REJECTED);
    }

    @PutMapping("/disable")
    @PreAuthorize("hasRole('MANAGER')")
    public PartnershipResponse disable(@RequestParam Long partnershipId, @AuthenticationPrincipal Jwt jwt) {
        log.info("PUT /partnerships/disable (partnershipId={}) by {}", partnershipId, jwt.getSubject());
        return partnershipService.transition(jwt.getSubject(), c(jwt), partnershipId, PartnershipStatus.DISABLED);
    }
}

package com.bnfix.ubm.domains.benefitrequest;

import com.bnfix.ubm.domains.benefitrequest.dto.*;
import jakarta.validation.Valid;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/benefit-requests")
public class BenefitAccessRequestController {
    private final BenefitAccessRequestService benefitAccessRequestService;

    public BenefitAccessRequestController(BenefitAccessRequestService benefitAccessRequestService) {
        this.benefitAccessRequestService = benefitAccessRequestService;
    }

    private Long c(Jwt jwt) {
        Object value = jwt.getClaims().get("companyId");
        return value instanceof Number number ? number.longValue() : Long.valueOf(value.toString());
    }

    @PostMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<BenefitAccessRequestResponse> request(
            @Valid @RequestBody CreateBenefitAccessRequest createBenefitAccessRequest,
            @AuthenticationPrincipal Jwt jwt) {
        log.info("POST /benefit-requests by {}", jwt.getSubject());
        return ResponseEntity.status(201)
                .body(benefitAccessRequestService.request(jwt.getSubject(), createBenefitAccessRequest.benefitId()));
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('USER')")
    public List<BenefitAccessRequestResponse> mine(@AuthenticationPrincipal Jwt jwt) {
        return benefitAccessRequestService.mine(jwt.getSubject());
    }

    @GetMapping("/provider")
    @PreAuthorize("hasRole('MANAGER')")
    public List<BenefitAccessRequestResponse> pending(@AuthenticationPrincipal Jwt jwt) {
        return benefitAccessRequestService.pending(jwt.getSubject(), c(jwt));
    }

    @PutMapping("/{id}/approve")
    @PreAuthorize("hasRole('MANAGER')")
    public BenefitAccessRequestResponse approve(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        log.info("PUT /benefit-requests/{}/approve by {}", id, jwt.getSubject());
        return benefitAccessRequestService.approve(jwt.getSubject(), c(jwt), id);
    }

    @PutMapping("/{id}/reject")
    @PreAuthorize("hasRole('MANAGER')")
    public BenefitAccessRequestResponse reject(
            @PathVariable Long id,
            @Valid @RequestBody RejectBenefitAccessRequest rejectBenefitAccessRequest,
            @AuthenticationPrincipal Jwt jwt) {
        log.info("PUT /benefit-requests/{}/reject by {}", id, jwt.getSubject());
        return benefitAccessRequestService.reject(jwt.getSubject(), c(jwt), id, rejectBenefitAccessRequest.reason());
    }
}

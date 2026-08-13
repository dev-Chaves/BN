package com.bnfix.ubm.domains.benefit;

import com.bnfix.ubm.domains.benefit.dto.*;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/benefits")
public class BenefitController {
    private final BenefitService benefitService;

    public BenefitController(BenefitService benefitService) {
        this.benefitService = benefitService;
    }

    private String e(Jwt jwt) {
        return jwt.getSubject();
    }

    private Long c(Jwt jwt) {
        Object value = jwt.getClaims().get("companyId");
        return value instanceof Number number ? number.longValue() : Long.valueOf(value.toString());
    }

    @PostMapping
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<BenefitResponse> create(
            @Valid @RequestBody CreateBenefitRequest request, @AuthenticationPrincipal Jwt jwt) {
        log.info("POST /benefits by {}", e(jwt));
        return ResponseEntity.status(201).body(benefitService.create(request, e(jwt), c(jwt)));
    }

    @GetMapping("/tenant")
    @PreAuthorize("hasRole('MANAGER')")
    public Page<BenefitResponse> tenant(
            @RequestParam(required = false) Long categoryId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @AuthenticationPrincipal Jwt jwt) {
        return benefitService.tenant(e(jwt), c(jwt), categoryId, pageable);
    }

    @GetMapping("/marketplace")
    @PreAuthorize("hasRole('MANAGER')")
    public Page<BenefitResponse> market(
            @RequestParam(required = false) Long categoryId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @AuthenticationPrincipal Jwt jwt) {
        return benefitService.marketplace(e(jwt), c(jwt), categoryId, pageable);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('MANAGER')")
    public BenefitResponse update(
            @PathVariable Long id, @Valid @RequestBody UpdateBenefitRequest request, @AuthenticationPrincipal Jwt jwt) {
        log.info("PUT /benefits/{} by {}", id, e(jwt));
        return benefitService.update(id, request, e(jwt), c(jwt));
    }

    @PutMapping("/{id}/activate")
    @PreAuthorize("hasRole('MANAGER')")
    public BenefitResponse activate(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        log.info("PUT /benefits/{}/activate by {}", id, e(jwt));
        return benefitService.status(id, e(jwt), c(jwt), true);
    }

    @PutMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('MANAGER')")
    public BenefitResponse deactivate(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        log.info("PUT /benefits/{}/deactivate by {}", id, e(jwt));
        return benefitService.status(id, e(jwt), c(jwt), false);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<Void> delete(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        log.info("DELETE /benefits/{} by {}", id, e(jwt));
        benefitService.delete(id, e(jwt), c(jwt));
        return ResponseEntity.noContent().build();
    }
}

package com.bnfix.ubm.domains.onboarding;

import com.bnfix.ubm.domains.onboarding.dto.OnboardingRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/onboarding")
public class OnboardingController {
    private final OnboardingService onboardingService;

    public OnboardingController(OnboardingService onboardingService) {
        this.onboardingService = onboardingService;
    }

    @PostMapping
    public ResponseEntity<?> onboard(@Valid @RequestBody OnboardingRequest request) {
        log.info("Onboarding attempt for cnpj {}", request.company().cnpj());
        return ResponseEntity.status(201).body(onboardingService.onboard(request));
    }
}

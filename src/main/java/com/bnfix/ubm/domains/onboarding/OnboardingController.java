package com.bnfix.ubm.domains.onboarding;

import com.bnfix.ubm.domains.onboarding.dto.OnboardingRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/onboarding")
public class OnboardingController {
    private final OnboardingService service;

    public OnboardingController(OnboardingService s) {
        service = s;
    }

    @PostMapping
    public ResponseEntity<?> onboard(@Valid @RequestBody OnboardingRequest r) {
        return ResponseEntity.status(201).body(service.onboard(r));
    }
}

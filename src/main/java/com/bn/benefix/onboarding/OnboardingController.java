package com.bn.benefix.onboarding;

import com.bn.benefix.onboarding.dto.OnboardingRegistrationRequestDTO;
import com.bn.benefix.onboarding.dto.OnboardingRegistrationResponseDTO;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("onboarding")
public class OnboardingController {

    private final OnboardingService onboardingService;

    public OnboardingController(OnboardingService onboardingService) {
        this.onboardingService = onboardingService;
    }

    @PostMapping()
    public ResponseEntity<OnboardingRegistrationResponseDTO> registerCompany(@Valid @RequestBody OnboardingRegistrationRequestDTO dto) {

        onboardingService.registerCompany(dto);
        OnboardingRegistrationResponseDTO responseDTO = new OnboardingRegistrationResponseDTO("Company and manager registered successfully.");
        return ResponseEntity.ok(responseDTO);

    }

}

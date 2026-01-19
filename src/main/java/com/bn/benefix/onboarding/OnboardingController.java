package com.bn.benefix.onboarding;

import com.bn.benefix.company.CompanyService;
import com.bn.benefix.management.ManagerService;
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
    private final CompanyService companyService;
    private final ManagerService managerService;

    public OnboardingController(OnboardingService onboardingService, CompanyService companyService, ManagerService managerService) {
        this.onboardingService = onboardingService;
        this.companyService = companyService;
        this.managerService = managerService;
    }

    @PostMapping()
    public ResponseEntity<OnboardingRegistrationResponseDTO> registerCompany(@Valid @RequestBody OnboardingRegistrationRequestDTO dto) {


        return ResponseEntity.ok(onboardingService.registerCompany(dto));

    }


}

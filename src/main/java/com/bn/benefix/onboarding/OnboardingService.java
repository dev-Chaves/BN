package com.bn.benefix.onboarding;

import com.bn.benefix.company.Company;
import com.bn.benefix.company.CompanyService;
import com.bn.benefix.management.ManagerService;
import com.bn.benefix.onboarding.dto.OnboardingRegistrationRequestDTO;
import com.bn.benefix.onboarding.dto.OnboardingRegistrationResponseDTO;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

@Service
public class OnboardingService {

    private final CompanyService companyService;
    private final ManagerService managerService;

    public OnboardingService(CompanyService companyService, ManagerService managerService) {
        this.companyService = companyService;
        this.managerService = managerService;
    }

    @Transactional
    public OnboardingRegistrationResponseDTO registerCompany(OnboardingRegistrationRequestDTO dto) {
        Company savedCompany = companyService.createCompany(dto.company());

        managerService.createManager(
                dto.manager().name(),
                dto.manager().cpf(),
                savedCompany
        );

        return new OnboardingRegistrationResponseDTO(
                savedCompany.getCnpj().getValue(),
                dto.manager().name(),
                savedCompany.getName()
        );

    }

}

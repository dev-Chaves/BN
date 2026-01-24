package com.bn.benefix.onboarding;

import com.bn.benefix.company.CompanyService;
import com.bn.benefix.manager.ManagerService;
import com.bn.benefix.manager.dto.ManagerCreationRequestDTO;
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
        var companyDto = companyService.createCompany(dto.company());

        managerService.createManager(new ManagerCreationRequestDTO(
                dto.manager().name(),
                dto.manager().cpf(),
                dto.manager().email(),
                dto.manager().password(),
                companyDto.id()
        ));

        return new OnboardingRegistrationResponseDTO(
                companyDto.cnpj(),
                dto.manager().name(),
                companyDto.name()
        );

    }

}

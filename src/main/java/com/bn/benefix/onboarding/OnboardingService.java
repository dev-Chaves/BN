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
        var companyDto = companyService.createCompany(dto.company());
        Company companyEntity = companyService.findByCnpj(companyDto.cnpj());

        managerService.createManager(new com.bn.benefix.management.dto.ManagerCreationRequestDTO(
                dto.manager().name(),
                dto.manager().cpf(),
                companyEntity.getId()
        ));

        return new OnboardingRegistrationResponseDTO(
                companyDto.cnpj(),
                dto.manager().name(),
                companyDto.name()
        );

    }

}

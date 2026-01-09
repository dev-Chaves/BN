package com.bn.benefix.benefit;

import com.bn.benefix.benefit.dto.BenefitCreationRequestDTO;
import com.bn.benefix.company.Company;
import com.bn.benefix.company.CompanyRepository;
import org.springframework.stereotype.Service;

@Service
public class BenefitService {

    private final BeneftiRepository benefitRepository;
    private final CompanyRepository companyRepository;

    public BenefitService(BeneftiRepository benefitRepository, CompanyRepository companyRepository) {
        this.benefitRepository = benefitRepository;
        this.companyRepository = companyRepository;
    }

    public Benefit createBenefit(BenefitCreationRequestDTO dto) {
        Company provider = companyRepository.findByCNPJ(dto.providerCNPJ())
                .orElseThrow(() -> new IllegalArgumentException("Provider company not found"));

        Benefit newBenefit = new Benefit.Builder(
                dto.name(),
                provider)
                .description(dto.description())
                .build();

        return benefitRepository.save(newBenefit);
    }
}

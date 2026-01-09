package com.bn.benefix.partnership;

import com.bn.benefix.benefit.Benefit;
import com.bn.benefix.benefit.BeneftiRepository;
import com.bn.benefix.company.Company;
import com.bn.benefix.company.CompanyRepository;
import com.bn.benefix.partnership.dto.PartnershipCreationRequestDTO;
import org.springframework.stereotype.Service;

@Service
public class PartnershipService {

    private final PartnershipRepository partnershipRepository;
    private final CompanyRepository companyRepository;
    private final BeneftiRepository benefitRepository;

    public PartnershipService(PartnershipRepository partnershipRepository, CompanyRepository companyRepository, BeneftiRepository benefitRepository) {
        this.partnershipRepository = partnershipRepository;
        this.companyRepository = companyRepository;
        this.benefitRepository = benefitRepository;
    }

    private void createPartnership(PartnershipCreationRequestDTO dto) {
        Company clientCompany = companyRepository.findById(dto.clientCompanyId())
                .orElseThrow(() -> new IllegalArgumentException("Client company not found"));

        Benefit benefit = benefitRepository.findById(dto.benefitId())
                .orElseThrow(() -> new IllegalArgumentException("Benefit not found"));

        Partnership newPartnership = new Partnership.Builder(
                clientCompany,
                benefit)
                .build();

        partnershipRepository.save(newPartnership);
    }
}

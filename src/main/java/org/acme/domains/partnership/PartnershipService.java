package org.acme.domains.partnership;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.NotFoundException;
import org.acme.domains.benefit.Benefit;
import org.acme.domains.benefit.BenefitRepository;
import org.acme.domains.company.Company;
import org.acme.domains.company.CompanyRepository;
import org.acme.domains.manager.ManagerRepository;
import org.acme.domains.partnership.dto.CreatePartnershipRequest;
import org.acme.domains.partnership.dto.PartnershipResponse;

@ApplicationScoped
public class PartnershipService {

    private final CompanyRepository companyRepository;
    private final ManagerRepository managerRepository;
    private final BenefitRepository benefitRepository;
    private final PartnershipRepository partnershipRepository;

    public PartnershipService(CompanyRepository companyRepository, ManagerRepository managerRepository, BenefitRepository benefitRepository, PartnershipRepository partnershipRepository) {
        this.companyRepository = companyRepository;
        this.managerRepository = managerRepository;
        this.benefitRepository = benefitRepository;
        this.partnershipRepository = partnershipRepository;
    }

    public Uni<PartnershipResponse> requestPartnership(String managerEmail, Long benefitId){

        return managerRepository.findByEmail(managerEmail).onItem().ifNull().failWith(()-> new NotFoundException("Manager not found"))
        .call(manager -> companyRepository.findByManagerEmail(managerEmail).onItem().ifNull().failWith(()-> new NotFoundException("Requested Company not found"))
                .call(requestCompany -> benefitRepository.findById(benefitId).onItem().ifNull().failWith(()-> new NotFoundException("Requested Benefit not found"))
                        .call(benefit -> validatePartnership(requestCompany.id, benefit.id)
                                .call(()-> verifyClientCompany(requestCompany, benefit.getProvider()))
                                .call(clientCompany -> createPartnership(requestCompany, benefit)))
                        .onItem().transform(partnership -> new PartnershipResponse(
                                partnership.id,
                                partnership.getClientCompany().id,
                                partnership.getBenefit().id,
                                partnership.getStatus(),
                                partnership.getCreatedAt()
                        ))));



    }

    private Uni<Partnership> createPartnership(Company company, Benefit benefit){

        return Uni.createFrom().item(Partnership.builder(company, benefit).build());

    }

    private Uni<Company> verifyClientCompany(Company companyProvider, Company clientCompany){
        
        if(companyProvider.equals(clientCompany)){
            return Uni.createFrom().failure(new IllegalArgumentException("Company cannot be its own provider"));
        }

        return Uni.createFrom().item(clientCompany);

    }

    private Uni<Boolean> validatePartnership(Long company, Long benefit){

        return partnershipRepository.findExistingPartnership(company, benefit).call(exists -> {
            if(exists){
                return Uni.createFrom().failure(new IllegalStateException("Partnership already exists"));
            }
            return Uni.createFrom().voidItem();
        });

    }

    

}

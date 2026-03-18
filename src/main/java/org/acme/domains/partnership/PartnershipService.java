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

    public PartnershipService(CompanyRepository companyRepository, ManagerRepository managerRepository, BenefitRepository benefitRepository) {
        this.companyRepository = companyRepository;
        this.managerRepository = managerRepository;
        this.benefitRepository = benefitRepository;
    }

    public Uni<PartnershipResponse> requestPartnership(String managerEmail, Long benefitId){

        return managerRepository.findByEmail(managerEmail).onItem().ifNull().failWith(() -> new NotFoundException("Manager not found"))
                .call(manager -> companyRepository.findByManagerEmail(managerEmail).onItem().ifNull().failWith(() -> new NotFoundException("Company not found"))

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

    private Uni<Company> checkIfTheCompanyAlreadyHasAPartnershipWithTheSameBenefit(Company companyProvider, Company clientCompany, Benefit benefit){
     
        if(companyProvider.getOfferedBenefits().contains(benefit)){
            return Uni.createFrom().failure(new IllegalArgumentException("Company already has a partnership with the same benefit"));
        }

        return Uni.createFrom().item(clientCompany);

    }   

    

}

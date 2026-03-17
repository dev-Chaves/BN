package org.acme.domains.partnership;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.NotFoundException;
import org.acme.domains.benefit.Benefit;
import org.acme.domains.company.Company;
import org.acme.domains.company.CompanyRepository;
import org.acme.domains.manager.ManagerRepository;
import org.acme.domains.partnership.dto.CreatePartnershipRequest;
import org.acme.domains.partnership.dto.PartnershipResponse;

@ApplicationScoped
public class PartnershipService {

    private final CompanyRepository companyRepository;
    private final ManagerRepository managerRepository;

    public PartnershipService(CompanyRepository companyRepository, ManagerRepository managerRepository) {
        this.companyRepository = companyRepository;
        this.managerRepository = managerRepository;
    }

    public Uni<PartnershipResponse> requestPartnership(String managerEmail){

        return managerRepository.findByEmail(managerEmail).onItem().ifNull().failWith(new NotFoundException("Manager not found"))
                .flatMap(manager ->
                        companyRepository.findByManagerEmail(managerEmail).onItem().ifNull().failWith(new NotFoundException("Company not found"))
                                .flatMap(company ->

                                        ))

    }

    private Uni<Partnership> createPartnership(Company company, Benefit benefit){

        return Uni.createFrom().item(Partnership.builder(company, benefit).build());

    }

    private Uni<Company> verifyClientCompany(Company company){

    }

}

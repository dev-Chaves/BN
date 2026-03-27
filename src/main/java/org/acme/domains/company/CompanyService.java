package org.acme.domains.company;

import io.smallrye.mutiny.Uni;
import io.quarkus.hibernate.reactive.panache.common.WithSession;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.NotFoundException;
import org.acme.domains.company.dto.CompanyResponse;

@ApplicationScoped
public class CompanyService {

    private final CompanyRepository companyRepository;

    public CompanyService(CompanyRepository companyRepository) {
        this.companyRepository = companyRepository;
    }

    @WithSession
    public Uni<CompanyResponse> getByManagerEmail(String managerEmail) {
        return companyRepository.findByManagerEmail(managerEmail)
                .onItem().ifNull().failWith(() -> new NotFoundException("Company not found"))
                .map(this::toResponse);
    }

    private CompanyResponse toResponse(Company company) {
        return new CompanyResponse(
                company.id,
                company.getName(),
                company.getCnpj().getValue(),
                company.getActive(),
                company.getCreatedAt()
        );
    }
}

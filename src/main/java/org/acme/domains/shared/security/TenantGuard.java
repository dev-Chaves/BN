package org.acme.domains.shared.security;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;
import org.acme.domains.company.Company;
import org.acme.domains.company.CompanyRepository;
import org.acme.domains.manager.Manager;

@ApplicationScoped
public class TenantGuard {

    private final CompanyRepository companyRepository;

    @Inject
    public TenantGuard(CompanyRepository companyRepository) {
        this.companyRepository = companyRepository;
    }

    public Uni<Company> verifyTenant(Long companyId, Long userId) {

        if(!companyId.equals(userId)){
            return Uni.createFrom().failure(new SecurityException("Unauthorized access: Tenant mismatch"));
        }

        return companyRepository.findById(companyId).onItem().ifNull().failWith(() -> new NotFoundException("Unauthorized access: Company not found"));

    }

}

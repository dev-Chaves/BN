package org.acme.domains.partnership;

import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.tuples.Tuple2;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.NotFoundException;
import org.acme.domains.benefit.Benefit;
import org.acme.domains.benefit.BenefitRepository;
import org.acme.domains.company.Company;
import org.acme.domains.company.CompanyRepository;
import org.acme.domains.manager.Manager;
import org.acme.domains.manager.ManagerRepository;
import org.acme.domains.partnership.dto.PartnershipResponse;
import org.acme.domains.shared.security.TenantGuard;
import org.jboss.logging.Logger;
import java.time.LocalDateTime;

@ApplicationScoped
public class PartnershipService {
    private static final Logger LOG = Logger.getLogger(PartnershipService.class);

    private final CompanyRepository companyRepository;
    private final ManagerRepository managerRepository;
    private final BenefitRepository benefitRepository;
    private final PartnershipRepository partnershipRepository;
    private final TenantGuard tenantGuard;

    public PartnershipService(CompanyRepository companyRepository, ManagerRepository managerRepository, BenefitRepository benefitRepository, PartnershipRepository partnershipRepository, TenantGuard tenantGuard) {
        this.companyRepository = companyRepository;
        this.managerRepository = managerRepository;
        this.benefitRepository = benefitRepository;
        this.partnershipRepository = partnershipRepository;
        this.tenantGuard = tenantGuard;
    }

    @WithTransaction
    public Uni<PartnershipResponse> requestPartnership(String managerEmail, Long companyId, Long benefitId) {
        return validateManagerExists(managerEmail, companyId)
            .flatMap(manager -> tenantGuard.verifyManagerCompanyAccess(manager, manager.getCompany().id)
                .replaceWith(manager))
            .flatMap(manager -> fetchCompanyAndBenefit(manager, benefitId))
            .call(this::validateBusinessRules)
            .flatMap(this::createAndPersistPartnership)
            .map(this::toPartnershipResponse);
    }

    @WithTransaction
    public Uni<PartnershipResponse> acceptPartnership(String managerEmail, Long companyId, Long partnershipId) {
        return validateManagerExists(managerEmail, companyId)
                .flatMap(manager -> getPartnership(partnershipId)
                        .flatMap(partnership -> tenantGuard.verifyManagerPartnershipProviderAccess(manager, partnership)))
                .flatMap(partnership -> transactionPartnershipStatus(partnership, PartnershipStatus.ACTIVE)
                .map(this::toPartnershipResponse));
    }

    @WithTransaction
    public Uni<PartnershipResponse> rejectPartnership(String managerEmail, Long companyId, Long partnershipId) {
        return validateManagerExists(managerEmail, companyId)
                .flatMap(manager -> getPartnership(partnershipId)
                        .flatMap(partnership -> tenantGuard.verifyManagerPartnershipProviderAccess(manager, partnership)))
                .flatMap(partnership -> transactionPartnershipStatus(partnership, PartnershipStatus.REJECTED))
                .map(this::toPartnershipResponse);
    }

    @WithTransaction
    public Uni<PartnershipResponse> disablePartnership(String managerEmail, Long companyId, Long partnershipId) {
        return validateManagerExists(managerEmail, companyId)
                .flatMap(manager -> getPartnership(partnershipId)
                        .flatMap(partnership -> tenantGuard.verifyManagerPartnershipProviderAccess(manager, partnership)))
                .flatMap(partnership -> transactionPartnershipStatus(partnership, PartnershipStatus.DISABLED))
                .map(this::toPartnershipResponse);
    }


    private Uni<Partnership> getPartnership(Long partnershipId) {
        return partnershipRepository.findByIdWithProvider(partnershipId)
                .onItem().ifNull().failWith(() -> new NotFoundException("Partnership not found with id: " + partnershipId));
    }

    private Uni<Partnership> transactionPartnershipStatus(Partnership partnership, PartnershipStatus status) {

        if(partnership.getStatus().equals(PartnershipStatus.PENDING) && status.equals(PartnershipStatus.ACTIVE)) {
            partnership.updateStatus(status);
            return Uni.createFrom().item(partnership);
        }

        if(partnership.getStatus().equals(PartnershipStatus.PENDING) && status.equals(PartnershipStatus.REJECTED)) {
            partnership.updateStatus(status);
            return Uni.createFrom().item(partnership);
        }

        if (partnership.getStatus().equals(PartnershipStatus.ACTIVE) && status.equals(PartnershipStatus.DISABLED)) {
            partnership.updateStatus(status);
            return Uni.createFrom().item(partnership);
        }

        LOG.warnf(
                "Invalid partnership transition partnershipId=%d current=%s target=%s",
                partnership.id,
                partnership.getStatus(),
                status
        );
        return Uni.createFrom().failure(new IllegalStateException("Invalid partnership status transition from " + partnership.getStatus() + " to " + status));

    }

    private Uni<Manager> validateManagerExists(String email, Long companyId) {
        return managerRepository.findByEmailAndCompanyId(email, companyId)
            .onItem().ifNull().failWith(() -> 
                new NotFoundException("Manager not found with email: " + email)
            );
    }

    private Uni<Benefit> getBenefitById(Long benefitId) {
        return benefitRepository.findById(benefitId)
            .onItem().ifNull().failWith(() -> 
                new NotFoundException("Benefit not found with id: " + benefitId)
            );
    }

    private Uni<Tuple2<Company, Benefit>> fetchCompanyAndBenefit(Manager manager, Long benefitId) {
        Company company = manager.getCompany();

        if (company == null) {
            return Uni.createFrom().failure(
                new IllegalStateException("Manager does not have an associated company")
            );
        }

        return getBenefitById(benefitId)
            .map(benefit -> Tuple2.of(company, benefit));
    }

    private Uni<Void> validatePartnershipDoesNotExist(Long companyId, Long benefitId) {
        return partnershipRepository.findExistingPartnership(companyId, benefitId)
            .flatMap(exists -> {
                if (exists) {
                    return Uni.createFrom().failure(
                        new IllegalStateException(
                            "Partnership already exists between company " + 
                            companyId + " and benefit " + benefitId
                        )
                    );
                }
                return Uni.createFrom().voidItem();
            });
    }

    private Uni<Void> validateCompanyIsNotOwnProvider(Company client, Benefit benefit) {
        if (client.id.equals(benefit.getProvider().id)) {
            return Uni.createFrom().failure(
                new IllegalArgumentException(
                    "Company " + client.getName() + 
                    " cannot request a benefit from itself"
                )
            );
        }
        return Uni.createFrom().voidItem();
    }

    private Uni<Void> validateBusinessRules(Tuple2<Company, Benefit> tuple) {
        Company company = tuple.getItem1();
        Benefit benefit = tuple.getItem2();

        if (!benefit.isDiscoverableAt(LocalDateTime.now())) {
            return Uni.createFrom().failure(new IllegalStateException("Benefit is not available for partnership"));
        }

        return validatePartnershipDoesNotExist(company.id, benefit.id)
            .call(() -> validateCompanyIsNotOwnProvider(company, benefit));
    }

    private Uni<Partnership> createAndPersistPartnership(Tuple2<Company, Benefit> tuple) {
        Company company = tuple.getItem1();
        Benefit benefit = tuple.getItem2();

        Partnership partnership = Partnership.builder(company, benefit).build();

        return partnershipRepository.persist(partnership);
    }

    private PartnershipResponse toPartnershipResponse(Partnership partnership) {
        return new PartnershipResponse(
            partnership.id,
            partnership.getClientCompany().id,
            partnership.getBenefit().id,
            partnership.getStatus(),
            partnership.getCreatedAt()
        );
    }

}

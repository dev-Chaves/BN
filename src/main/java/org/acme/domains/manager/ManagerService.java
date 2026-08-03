package org.acme.domains.manager;

import io.quarkus.elytron.security.common.BcryptUtil;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.quarkus.hibernate.reactive.panache.common.WithSession;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.BadRequestException;
import org.acme.domains.account.Account;
import org.acme.domains.account.AccountRepository;
import org.acme.domains.company.Company;
import org.acme.domains.company.CompanyRepository;
import org.acme.domains.manager.dto.CreateManagerRequest;
import org.acme.domains.manager.dto.ChangeManagerPasswordRequest;
import org.acme.domains.manager.dto.ManagerResponse;
import org.acme.domains.manager.dto.UpdateManagerEmailRequest;
import org.acme.domains.shared.domain.CPF;
import org.acme.domains.shared.enums.Role;
import org.acme.domains.shared.security.AccessStatusGuard;

import java.util.Locale;

@ApplicationScoped
public class ManagerService {

    private final ManagerRepository managerRepository;
    private final CompanyRepository companyRepository;
    private final AccountRepository accountRepository;

    public ManagerService(ManagerRepository managerRepository, CompanyRepository companyRepository, AccountRepository accountRepository) {
        this.managerRepository = managerRepository;
        this.companyRepository = companyRepository;
        this.accountRepository = accountRepository;
    }

    @WithTransaction
    public Uni<ManagerResponse> createManager(CreateManagerRequest request) {
        return validateAccountAvailable(request.email(), request.cpf())
                .flatMap(ignored -> companyRepository.findById(request.companyId()))
                .onItem().ifNull().failWith(() -> new NotFoundException("Company not found"))
                .flatMap(company -> createManager(request, company));
    }

    private Uni<Void> validateAccountAvailable(String email, String cpf) {
        return accountRepository.findByEmail(email)
                .flatMap(byEmail -> {
                    if (byEmail != null) return Uni.createFrom().failure(new IllegalStateException("Email already in use"));
                    return accountRepository.findByCPF(cpf);
                })
                .flatMap(byCpf -> byCpf != null
                        ? Uni.createFrom().failure(new IllegalStateException("CPF already in use"))
                        : Uni.createFrom().voidItem());
    }

    @WithSession
    public Uni<ManagerResponse> getCurrentManager(String email, Long companyId) {
        return managerRepository.findByEmailAndCompanyId(email, companyId)
                .onItem().ifNull().failWith(() -> new NotFoundException("Manager not found"))
                .map(AccessStatusGuard::requireActive)
                .map(this::toResponse);
    }

    @WithTransaction
    public Uni<ManagerResponse> updateCurrentEmail(
            String currentEmail,
            Long companyId,
            UpdateManagerEmailRequest request
    ) {
        String normalizedEmail = request.email().trim().toLowerCase(Locale.ROOT);

        return managerRepository.findByEmailAndCompanyId(currentEmail, companyId)
                .onItem().ifNull().failWith(() -> new NotFoundException("Manager not found"))
                .map(AccessStatusGuard::requireActive)
                .map(manager -> requirePassword(manager, request.currentPassword()))
                .flatMap(manager -> {
                    if (manager.getAccount().getEmail().equalsIgnoreCase(normalizedEmail)) {
                        return Uni.createFrom().item(manager);
                    }

                    return accountRepository.findByEmail(normalizedEmail)
                            .flatMap(existing -> existing != null
                                    ? Uni.createFrom().failure(new IllegalStateException("Email already in use"))
                                    : Uni.createFrom().item(manager));
                })
                .invoke(manager -> manager.getAccount().updateEmail(normalizedEmail))
                .map(this::toResponse);
    }

    @WithTransaction
    public Uni<ManagerResponse> changeCurrentPassword(
            String email,
            Long companyId,
            ChangeManagerPasswordRequest request
    ) {
        return managerRepository.findByEmailAndCompanyId(email, companyId)
                .onItem().ifNull().failWith(() -> new NotFoundException("Manager not found"))
                .map(AccessStatusGuard::requireActive)
                .map(manager -> requirePassword(manager, request.currentPassword()))
                .map(manager -> requireNewPassword(manager, request.newPassword()))
                .invoke(manager -> manager.getAccount().updatePassword(BcryptUtil.bcryptHash(request.newPassword())))
                .map(this::toResponse);
    }

    private Manager requirePassword(Manager manager, String password) {
        if (!BcryptUtil.matches(password, manager.getAccount().getPassword())) {
            throw new BadRequestException("Password is incorrect");
        }
        return manager;
    }

    private Manager requireNewPassword(Manager manager, String newPassword) {
        if (BcryptUtil.matches(newPassword, manager.getAccount().getPassword())) {
            throw new BadRequestException("New password must be different");
        }
        return manager;
    }

    private Uni<ManagerResponse> createManager(CreateManagerRequest request, Company company) {
        Account account = Account.builder(
                request.name(),
                CPF.of(request.cpf()),
                BcryptUtil.bcryptHash(request.password()),
                request.email(),
                Role.MANAGER
        ).build();

        Manager manager = Manager.builder(request.name(), company, account).build();

        return accountRepository.persist(account)
                .flatMap(ignored -> managerRepository.persist(manager))
                .map(this::toResponse);
    }

    private ManagerResponse toResponse(Manager manager) {
        return new ManagerResponse(
                manager.id,
                manager.getName(),
                manager.getAccount().getEmail(),
                manager.getCompany().id,
                manager.getActive(),
                manager.getCreatedAt()
        );
    }
}

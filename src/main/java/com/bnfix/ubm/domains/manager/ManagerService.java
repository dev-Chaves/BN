package com.bnfix.ubm.domains.manager;

import com.bnfix.ubm.domains.account.*;
import com.bnfix.ubm.domains.company.*;
import com.bnfix.ubm.domains.manager.dto.*;
import com.bnfix.ubm.domains.shared.domain.CPF;
import com.bnfix.ubm.domains.shared.enums.Role;
import com.bnfix.ubm.shared.security.AccessStatusGuard;
import jakarta.transaction.Transactional;
import java.util.Locale;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Service
public class ManagerService {
    private final ManagerRepository managerRepository;
    private final CompanyRepository companyRepository;
    private final AccountRepository accountRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public ManagerService(
            ManagerRepository managerRepository,
            CompanyRepository companyRepository,
            AccountRepository accountRepository,
            BCryptPasswordEncoder passwordEncoder) {
        this.managerRepository = managerRepository;
        this.companyRepository = companyRepository;
        this.accountRepository = accountRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public ManagerResponse create(CreateManagerRequest request) {
        if (accountRepository.findByEmail(request.email()).isPresent()) throw conflict("Email already in use");
        if (accountRepository.findByCPF(request.cpf()).isPresent()) throw conflict("CPF already in use");
        Company company =
                companyRepository.findById(request.companyId()).orElseThrow(() -> notFound("Company not found"));
        Account account = accountRepository.save(Account.builder(
                        request.name(),
                        CPF.of(request.cpf()),
                        passwordEncoder.encode(request.password()),
                        request.email(),
                        Role.MANAGER)
                .build());
        Manager manager = managerRepository.save(
                Manager.builder(request.name(), company, account).build());
        log.info("Manager {} created for company {}", manager.id, company.id);
        return response(manager);
    }

    @Transactional
    public ManagerResponse me(String email, Long companyId) {
        return response(active(email, companyId));
    }

    @Transactional
    public ManagerResponse updateEmail(String email, Long companyId, UpdateManagerEmailRequest request) {
        Manager manager = active(email, companyId);
        String normalized = request.email().trim().toLowerCase(Locale.ROOT);
        checkPassword(manager, request.currentPassword());
        if (!manager.getAccount().getEmail().equalsIgnoreCase(normalized)
                && accountRepository.findByEmail(normalized).isPresent()) throw conflict("Email already in use");
        manager.getAccount().updateEmail(normalized);
        log.info("Manager {} email updated (company {})", manager.id, companyId);
        return response(manager);
    }

    @Transactional
    public ManagerResponse changePassword(String email, Long companyId, ChangeManagerPasswordRequest request) {
        Manager manager = active(email, companyId);
        checkPassword(manager, request.currentPassword());
        if (passwordEncoder.matches(request.newPassword(), manager.getAccount().getPassword()))
            throw bad("New password must be different");
        manager.getAccount().updatePassword(passwordEncoder.encode(request.newPassword()));
        log.info("Manager {} password changed (company {})", manager.id, companyId);
        return response(manager);
    }

    private Manager active(String email, Long companyId) {
        return AccessStatusGuard.requireActive(managerRepository
                .findByEmailAndCompanyId(email, companyId)
                .orElseThrow(() -> notFound("Manager not found")));
    }

    private void checkPassword(Manager manager, String password) {
        if (!passwordEncoder.matches(password, manager.getAccount().getPassword())) throw bad("Password is incorrect");
    }

    private ManagerResponse response(Manager manager) {
        return new ManagerResponse(
                manager.id,
                manager.getName(),
                manager.getAccount().getEmail(),
                manager.getCompany().id,
                manager.getActive(),
                manager.getCreatedAt());
    }

    private ResponseStatusException notFound(String message) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, message);
    }

    private ResponseStatusException conflict(String message) {
        return new ResponseStatusException(HttpStatus.CONFLICT, message);
    }

    private ResponseStatusException bad(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}

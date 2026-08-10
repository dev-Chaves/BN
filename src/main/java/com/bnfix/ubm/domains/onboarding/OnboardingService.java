package com.bnfix.ubm.domains.onboarding;

import com.bnfix.ubm.domains.account.*;
import com.bnfix.ubm.domains.company.*;
import com.bnfix.ubm.domains.company.dto.CreateCompanyRequest;
import com.bnfix.ubm.domains.manager.*;
import com.bnfix.ubm.domains.onboarding.dto.*;
import com.bnfix.ubm.domains.shared.domain.*;
import com.bnfix.ubm.domains.shared.enums.Role;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Service
public class OnboardingService {
    private final AccountRepository accountRepository;
    private final ManagerRepository managerRepository;
    private final CompanyRepository companyRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public OnboardingService(
            AccountRepository accountRepository,
            ManagerRepository managerRepository,
            CompanyRepository companyRepository,
            BCryptPasswordEncoder passwordEncoder) {
        this.accountRepository = accountRepository;
        this.managerRepository = managerRepository;
        this.companyRepository = companyRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public OnboardingResponse onboard(OnboardingRequest request) {
        CreateCompanyRequest createCompanyRequest = request.company();
        if (companyRepository.findByCNPJ(createCompanyRequest.cnpj()).isPresent()) throw bad("CNPJ already registered");
        var managerRegistrationData = request.manager();
        Account
                byEmail =
                        accountRepository
                                .findByEmail(managerRegistrationData.email())
                                .orElse(null),
                byCpf =
                        accountRepository
                                .findByCPF(managerRegistrationData.cpf())
                                .orElse(null);
        Account account;
        if (byEmail == null && byCpf == null)
            account = accountRepository.save(Account.builder(
                            managerRegistrationData.name(),
                            CPF.of(managerRegistrationData.cpf()),
                            passwordEncoder.encode(managerRegistrationData.password()),
                            managerRegistrationData.email(),
                            Role.MANAGER)
                    .build());
        else {
            if (byEmail == null
                    || byCpf == null
                    || byEmail.id == null
                    || !byEmail.id.equals(byCpf.id)
                    || byEmail.getRole() != Role.MANAGER
                    || !passwordEncoder.matches(managerRegistrationData.password(), byEmail.getPassword()))
                throw bad("Manager identity or credentials are invalid");
            account = byEmail;
        }
        Company company = companyRepository.save(
                Company.builder(createCompanyRequest.name(), CNPJ.of(createCompanyRequest.cnpj()))
                        .build());
        Manager manager = managerRepository.save(Manager.builder(managerRegistrationData.name(), company, account)
                .companyOwner()
                .build());
        log.info("Company {} on-boarded with manager {}", company.id, manager.id);
        return new OnboardingResponse(
                company.id, manager.id, company.getCnpj().getValue(), company.getName(), manager.getName());
    }

    private ResponseStatusException bad(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}

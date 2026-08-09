package com.bnfix.ubm.domains.onboarding;

import com.bnfix.ubm.domains.account.*;
import com.bnfix.ubm.domains.company.*;
import com.bnfix.ubm.domains.company.dto.CreateCompanyRequest;
import com.bnfix.ubm.domains.manager.*;
import com.bnfix.ubm.domains.onboarding.dto.*;
import com.bnfix.ubm.domains.shared.domain.*;
import com.bnfix.ubm.domains.shared.enums.Role;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class OnboardingService {
    private final AccountRepository accounts;
    private final ManagerRepository managers;
    private final CompanyRepository companies;
    private final BCryptPasswordEncoder passwords;

    public OnboardingService(AccountRepository a, ManagerRepository m, CompanyRepository c, BCryptPasswordEncoder p) {
        accounts = a;
        managers = m;
        companies = c;
        passwords = p;
    }

    @Transactional
    public OnboardingResponse onboard(OnboardingRequest r) {
        CreateCompanyRequest cr = r.company();
        if (companies.findByCNPJ(cr.cnpj()).isPresent()) throw bad("CNPJ already registered");
        var d = r.manager();
        Account byEmail = accounts.findByEmail(d.email()).orElse(null),
                byCpf = accounts.findByCPF(d.cpf()).orElse(null);
        Account account;
        if (byEmail == null && byCpf == null)
            account = accounts.save(
                    Account.builder(d.name(), CPF.of(d.cpf()), passwords.encode(d.password()), d.email(), Role.MANAGER)
                            .build());
        else {
            if (byEmail == null
                    || byCpf == null
                    || byEmail.id == null
                    || !byEmail.id.equals(byCpf.id)
                    || byEmail.getRole() != Role.MANAGER
                    || !passwords.matches(d.password(), byEmail.getPassword()))
                throw bad("Manager identity or credentials are invalid");
            account = byEmail;
        }
        Company company =
                companies.save(Company.builder(cr.name(), CNPJ.of(cr.cnpj())).build());
        Manager manager = managers.save(
                Manager.builder(d.name(), company, account).companyOwner().build());
        return new OnboardingResponse(
                company.id, manager.id, company.getCnpj().getValue(), company.getName(), manager.getName());
    }

    private ResponseStatusException bad(String s) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, s);
    }
}

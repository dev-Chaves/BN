package com.bnfix.ubm.domains.auth;

import com.bnfix.ubm.domains.account.Account;
import com.bnfix.ubm.domains.account.AccountRepository;
import com.bnfix.ubm.domains.auth.dto.AuthMeResponse;
import com.bnfix.ubm.domains.company.Company;
import com.bnfix.ubm.domains.company.CompanyRepository;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/**
 * Constrói o {@link AuthMeResponse} (perfil visível ao frontend) a partir do
 * token autenticado. O token em si nunca é exposto ao cliente — apenas este
 * perfil, sem dados sensíveis.
 */
@Slf4j
@Service
@Profile("!test")
public class AuthMeService {
    private final AccountRepository accountRepository;
    private final CompanyRepository companyRepository;

    public AuthMeService(AccountRepository accountRepository, CompanyRepository companyRepository) {
        this.accountRepository = accountRepository;
        this.companyRepository = companyRepository;
    }

    /** Reconstrói o perfil a partir das claims — usado por GET /auth/me. */
    public AuthMeResponse build(String email, String role, Long companyId) {
        Account account = accountRepository.findByEmail(email).orElse(null);
        UUID accountId = account == null ? null : account.id;
        String name = account == null ? email : account.getName();
        String companyName = companyName(companyId);
        return new AuthMeResponse(accountId, email, role, companyId, companyName, name);
    }

    /** Constrói o perfil a partir das entidades já carregadas — usado por login/switch. */
    public AuthMeResponse build(Account account, String role, Company company) {
        Long companyId = company == null ? null : company.id;
        String companyName = company == null ? null : company.getName();
        return new AuthMeResponse(account.id, account.getEmail(), role, companyId, companyName, account.getName());
    }

    private String companyName(Long companyId) {
        if (companyId == null) return null;
        return companyRepository.findById(companyId).map(Company::getName).orElse(null);
    }
}

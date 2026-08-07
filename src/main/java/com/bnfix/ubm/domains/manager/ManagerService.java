package com.bnfix.ubm.domains.manager;

import com.bnfix.ubm.domains.account.*;
import com.bnfix.ubm.domains.company.*;
import com.bnfix.ubm.domains.manager.dto.*;
import com.bnfix.ubm.domains.shared.domain.CPF;
import com.bnfix.ubm.domains.shared.enums.Role;
import com.bnfix.ubm.shared.security.AccessStatusGuard;
import jakarta.transaction.Transactional;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ManagerService {
    private final ManagerRepository managers; private final CompanyRepository companies; private final AccountRepository accounts; private final BCryptPasswordEncoder passwords;
    public ManagerService(ManagerRepository managers, CompanyRepository companies, AccountRepository accounts, BCryptPasswordEncoder passwords) { this.managers=managers;this.companies=companies;this.accounts=accounts;this.passwords=passwords; }
    @Transactional public ManagerResponse create(CreateManagerRequest r) {
        if (accounts.findByEmail(r.email()).isPresent()) throw conflict("Email already in use");
        if (accounts.findByCPF(r.cpf()).isPresent()) throw conflict("CPF already in use");
        Company c=companies.findById(r.companyId()).orElseThrow(()->notFound("Company not found"));
        Account a=accounts.save(Account.builder(r.name(), CPF.of(r.cpf()), passwords.encode(r.password()), r.email(), Role.MANAGER).build());
        return response(managers.save(Manager.builder(r.name(), c, a).build()));
    }
    @Transactional public ManagerResponse me(String email, Long id) { return response(active(email,id)); }
    @Transactional public ManagerResponse updateEmail(String email, Long id, UpdateManagerEmailRequest r) {
        Manager m=active(email,id); String normalized=r.email().trim().toLowerCase(Locale.ROOT); checkPassword(m,r.currentPassword());
        if (!m.getAccount().getEmail().equalsIgnoreCase(normalized) && accounts.findByEmail(normalized).isPresent()) throw conflict("Email already in use");
        m.getAccount().updateEmail(normalized); return response(m);
    }
    @Transactional public ManagerResponse changePassword(String email, Long id, ChangeManagerPasswordRequest r) {
        Manager m=active(email,id); checkPassword(m,r.currentPassword()); if(passwords.matches(r.newPassword(),m.getAccount().getPassword())) throw bad("New password must be different");
        m.getAccount().updatePassword(passwords.encode(r.newPassword())); return response(m);
    }
    private Manager active(String email,Long id){return AccessStatusGuard.requireActive(managers.findByEmailAndCompanyId(email,id).orElseThrow(()->notFound("Manager not found")));}
    private void checkPassword(Manager m,String p){if(!passwords.matches(p,m.getAccount().getPassword()))throw bad("Password is incorrect");}
    private ManagerResponse response(Manager m){return new ManagerResponse(m.id,m.getName(),m.getAccount().getEmail(),m.getCompany().id,m.getActive(),m.getCreatedAt());}
    private ResponseStatusException notFound(String s){return new ResponseStatusException(HttpStatus.NOT_FOUND,s);} private ResponseStatusException conflict(String s){return new ResponseStatusException(HttpStatus.CONFLICT,s);} private ResponseStatusException bad(String s){return new ResponseStatusException(HttpStatus.BAD_REQUEST,s);}
}

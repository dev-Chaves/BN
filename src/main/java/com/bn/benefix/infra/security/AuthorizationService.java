package com.bn.benefix.infra.security;

import com.bn.benefix.account.AccountRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class AuthorizationService implements UserDetailsService {

    private final AccountRepository repository;

    public AuthorizationService(AccountRepository repository) {
        this.repository = repository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return repository.findByEmail(username)
                .map(AccountUserDetails::new) // Utilizando um adaptador
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }
}

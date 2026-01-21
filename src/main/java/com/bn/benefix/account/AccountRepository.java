package com.bn.benefix.account;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface AccountRepository extends JpaRepository<Account, UUID> {

    @Query(value = "SELECT a from Account a where a.cpf =: cpf")
    Optional<Account> findByCpf(String cpf);
}

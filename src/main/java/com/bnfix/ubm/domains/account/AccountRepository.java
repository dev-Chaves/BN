package com.bnfix.ubm.domains.account;
import java.util.Optional; import java.util.UUID; import org.springframework.data.jpa.repository.*; import org.springframework.data.repository.query.Param;
public interface AccountRepository extends JpaRepository<Account, UUID> {
 @Query("select a from Account a where a.cpf.value = :cpf") Optional<Account> findByCPF(@Param("cpf") String cpf);
 @Query("select a from Account a where lower(a.email) = lower(:email)") Optional<Account> findByEmail(@Param("email") String email);
}

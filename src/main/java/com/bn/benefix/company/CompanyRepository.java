package com.bn.benefix.company;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CompanyRepository extends JpaRepository<Company, Long> {

    @Query(value = "SELECT c FROM Company c WHERE c.cnpj =: cnpj")
    Optional<Company> findByCNPJ(String cnpj);

}

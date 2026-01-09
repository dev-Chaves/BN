package com.bn.benefix.management;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ManagerRepository extends JpaRepository<Manager, Long> {

    @Query(value = "select m from Manager m where m.cpf =: cpf")
    Optional<Manager> findByCPF(String cpf);

}


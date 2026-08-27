package com.bnfix.ubm.domains.company;

import java.util.Optional;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface CompanyRepository extends JpaRepository<Company, Long> {
    @Query("select c from Company c where c.cnpj.value = :cnpj")
    Optional<Company> findByCNPJ(@Param("cnpj") String cnpj);

    Optional<Company> findByName(String name);

    @Query("select distinct c from Company c join c.offeredBenefits b where b.id = :id")
    Optional<Company> findByBenefitId(@Param("id") Long benefitId);

    @Modifying
    @Query("update Benefit b set b.active = false where b.provider.id = :companyId")
    int deactivateBenefits(@Param("companyId") Long companyId);
}

package com.bnfix.ubm.domains.employee;

import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    List<Employee> findByCompanyId(Long companyId);

    @Query("select e from Employee e where e.company.id=:id order by e.createdAt desc")
    Page<Employee> findByCompanyId(@Param("id") Long companyId, Pageable pageable);

    default List<Employee> findByCompanyId(Long id, int page, int size) {
        return findByCompanyId(id, PageRequest.of(page, size)).getContent();
    }

    List<Employee> findByCompanyIdAndActive(Long companyId, EmployeeStatus active);

    @Query("select e from Employee e join fetch e.account join fetch e.company where e.account.id=:id")
    Optional<Employee> findByAccountId(@Param("id") UUID accountId);

    @Modifying
    @Query("update Employee e set e.active=:status where e.company.id=:id and e.active <> :status")
    int disableByCompanyId(@Param("id") Long companyId, @Param("status") EmployeeStatus status);

    default int disableByCompanyId(Long id) {
        return disableByCompanyId(id, EmployeeStatus.DISABLED);
    }
}

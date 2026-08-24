package com.bnfix.ubm.domains.benefit;

import java.util.*;

import com.bnfix.ubm.domains.benefit.dto.BenefitSearchProjection;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface BenefitRepository extends JpaRepository<Benefit, Long> {

    @Query(
            "select distinct b from Benefit b join fetch b.provider left join fetch b.categories where b.provider.id=:id order by b.createdAt desc")
    Page<Benefit> findByCompanyId(@Param("id") Long id, Pageable pageable);

    @Query(
            "select distinct b from Benefit b join fetch b.provider left join fetch b.categories c where b.provider.id=:id and c.id=:categoryId order by b.createdAt desc")
    Page<Benefit> findByCompanyIdAndCategoryId(
            @Param("id") Long id, @Param("categoryId") Long categoryId, Pageable pageable);

    @Query(
            "select distinct b from Benefit b join fetch b.provider p left join fetch b.categories where p.id<>:id and p.active=true and b.active=true and b.publiclyVisible=true and (b.validFrom is null or b.validFrom<=CURRENT_TIMESTAMP) and (b.validUntil is null or b.validUntil>CURRENT_TIMESTAMP) order by b.createdAt desc")
    Page<Benefit> findPublicAvailableByProviderNot(@Param("id") Long id, Pageable pageable);

    @Query(
            "select distinct b from Benefit b join fetch b.provider p left join fetch b.categories c where p.id<>:id and p.active=true and b.active=true and b.publiclyVisible=true and (b.validFrom is null or b.validFrom<=CURRENT_TIMESTAMP) and (b.validUntil is null or b.validUntil>CURRENT_TIMESTAMP) and c.id=:categoryId order by b.createdAt desc")
    Page<Benefit> findActiveByProviderNotAndCategoryId(
            @Param("id") Long id, @Param("categoryId") Long categoryId, Pageable pageable);

    List<Benefit> findByProviderIdAndActiveTrue(Long id);

    @Query("select distinct b from Benefit b join fetch b.provider left join fetch b.categories where b.id=:id")
    Optional<Benefit> findByIdWithProviderAndCategories(@Param("id") Long id);

    @Query("select b from Benefit b join fetch b.provider where b.active = true and b.publiclyVisible = true")
    Page<Benefit> findActiveBenefits(Pageable pageable);

    @Query(value = """
select * from benefits b where description_tsv @@ websearch_to_tsquery('portuguese', :termo) order by ts_rank(
b.description_tsv, websearch_to_tsquery('portuguese', :termo) ) DESC , b.id DESC
""", nativeQuery = true)
    Page<Benefit> searchByDescriptionFullText(@Param("termo") String termo, Pageable pageable);

    @Query(value = """
    SELECT b.*
    FROM benefits b
    INNER JOIN companies c on b.provider_id = c.id
    WHERE :termo <% b.description
    ORDER BY word_similarity(:termo ,b.description) desc, b.id desc
    """, nativeQuery = true)
    Page<BenefitSearchProjection> searchByDescriptionSimilarity(@Param("termo") String termo, Pageable pageable);

}

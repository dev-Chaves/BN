package com.bnfix.ubm.domains.announcement;
import java.util.*; import org.springframework.data.domain.*; import org.springframework.data.jpa.repository.*; import org.springframework.data.repository.query.Param;
public interface AnnouncementRepository extends JpaRepository<Announcement,Long> { @Query("select a from Announcement a join fetch a.author where a.company.id=:id order by a.publishedAt desc,a.id desc") List<Announcement> findByCompanyId(@Param("id") Long companyId,Pageable pageable); }

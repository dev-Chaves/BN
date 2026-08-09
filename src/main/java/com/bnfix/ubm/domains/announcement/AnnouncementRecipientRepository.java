package com.bnfix.ubm.domains.announcement;

import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AnnouncementRecipientRepository extends JpaRepository<AnnouncementRecipient, Long> {
    @Query(
            "select r from AnnouncementRecipient r join fetch r.announcement a join fetch a.author where r.employee.id = :employee and a.company.id = :company order by a.publishedAt desc, a.id desc")
    List<AnnouncementRecipient> findByEmployeeId(
            @Param("employee") Long employeeId, @Param("company") Long companyId, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
            "select r from AnnouncementRecipient r join fetch r.announcement a join fetch a.author where a.id = :announcement and r.employee.id = :employee and a.company.id = :company")
    Optional<AnnouncementRecipient> findForRead(
            @Param("announcement") Long announcementId,
            @Param("employee") Long employeeId,
            @Param("company") Long companyId);

    long countByEmployeeIdAndReadAtIsNull(Long employeeId);

    @Modifying
    @Query("update AnnouncementRecipient r set r.readAt = :at where r.employee.id = :id and r.readAt is null")
    int markAllAsRead(@Param("id") Long employeeId, @Param("at") LocalDateTime readAt);

    @Query(
            "select r.announcement.id, count(r.id) from AnnouncementRecipient r where r.announcement.id in :ids group by r.announcement.id")
    List<Object[]> countByAnnouncementIdsRows(@Param("ids") List<Long> ids);

    default Map<Long, Long> countByAnnouncementIds(List<Long> ids) {
        Map<Long, Long> result = new HashMap<>();
        if (ids != null && !ids.isEmpty())
            for (Object[] row : countByAnnouncementIdsRows(ids))
                result.put(((Number) row[0]).longValue(), ((Number) row[1]).longValue());
        return result;
    }
}

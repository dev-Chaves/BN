package org.acme.domains.announcement;

import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.LockModeType;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class AnnouncementRecipientRepository implements PanacheRepository<AnnouncementRecipient> {

    public Uni<List<AnnouncementRecipient>> findByEmployeeId(
            Long employeeId,
            Long companyId,
            int offset,
            int limit
    ) {
        return find("""
                select recipient from AnnouncementRecipient recipient
                join fetch recipient.announcement announcement
                join fetch announcement.author
                where recipient.employee.id = ?1
                  and announcement.company.id = ?2
                order by announcement.publishedAt desc, announcement.id desc
                """, employeeId, companyId)
                .range(offset, offset + limit - 1)
                .list();
    }

    public Uni<AnnouncementRecipient> findForRead(Long announcementId, Long employeeId, Long companyId) {
        return find("""
                select recipient from AnnouncementRecipient recipient
                join fetch recipient.announcement announcement
                join fetch announcement.author
                where announcement.id = ?1
                  and recipient.employee.id = ?2
                  and announcement.company.id = ?3
                """, announcementId, employeeId, companyId)
                .withLock(LockModeType.PESSIMISTIC_WRITE)
                .firstResult();
    }

    public Uni<Long> countUnreadByEmployeeId(Long employeeId) {
        return count("employee.id = ?1 and readAt is null", employeeId);
    }

    public Uni<Integer> markAllAsRead(Long employeeId, LocalDateTime readAt) {
        return update("readAt = ?1 where employee.id = ?2 and readAt is null", readAt, employeeId);
    }

    public Uni<Map<Long, Long>> countByAnnouncementIds(List<Long> announcementIds) {
        if (announcementIds.isEmpty()) {
            return Uni.createFrom().item(Map.of());
        }

        return getSession()
                .flatMap(session -> session.createSelectionQuery("""
                                select recipient.announcement.id, count(recipient.id)
                                from AnnouncementRecipient recipient
                                where recipient.announcement.id in :announcementIds
                                group by recipient.announcement.id
                                """, Object[].class)
                        .setParameter("announcementIds", announcementIds)
                        .getResultList())
                .map(rows -> {
                    Map<Long, Long> counts = new HashMap<>();
                    for (Object[] row : rows) {
                        counts.put(((Number) row[0]).longValue(), ((Number) row[1]).longValue());
                    }
                    return counts;
                });
    }
}

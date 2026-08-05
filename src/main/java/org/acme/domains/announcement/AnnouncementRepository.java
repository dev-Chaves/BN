package org.acme.domains.announcement;

import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

@ApplicationScoped
public class AnnouncementRepository implements PanacheRepository<Announcement> {

    public Uni<List<Announcement>> findByCompanyId(Long companyId, int offset, int limit) {
        return find("""
                select announcement from Announcement announcement
                join fetch announcement.author
                where announcement.company.id = ?1
                order by announcement.publishedAt desc, announcement.id desc
                """, companyId)
                .range(offset, offset + limit - 1)
                .list();
    }
}

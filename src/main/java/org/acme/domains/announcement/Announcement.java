package org.acme.domains.announcement;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import org.acme.domains.company.Company;
import org.acme.domains.manager.Manager;

import java.time.LocalDateTime;

@Entity
@Table(name = "announcements")
public class Announcement extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "announcementsSeq")
    @SequenceGenerator(name = "announcementsSeq", sequenceName = "announcements_SEQ", allocationSize = 50)
    public Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_manager_id", nullable = false)
    private Manager author;

    @Column(nullable = false, length = 160)
    private String title;

    @Column(nullable = false, length = 4000)
    private String content;

    @Column(name = "published_at", nullable = false)
    private LocalDateTime publishedAt;

    protected Announcement() {
    }

    public Announcement(Company company, Manager author, String title, String content) {
        this.company = company;
        this.author = author;
        this.title = requireText(title, 160, "Title");
        this.content = requireText(content, 4000, "Content");
    }

    @PrePersist
    void onCreate() {
        if (publishedAt == null) {
            publishedAt = LocalDateTime.now();
        }
    }

    public Company getCompany() {
        return company;
    }

    public Manager getAuthor() {
        return author;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public LocalDateTime getPublishedAt() {
        return publishedAt;
    }

    private static String requireText(String value, int maxLength, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " cannot be blank");
        }
        String normalized = value.strip();
        if (normalized.length() > maxLength) {
            throw new IllegalArgumentException(field + " cannot exceed " + maxLength + " characters");
        }
        return normalized;
    }
}

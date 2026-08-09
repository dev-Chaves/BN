package com.bnfix.ubm.domains.announcement;

import com.bnfix.ubm.domains.company.Company;
import com.bnfix.ubm.domains.manager.Manager;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "announcements")
public class Announcement {
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

    protected Announcement() {}

    public Announcement(Company c, Manager a, String t, String x) {
        company = c;
        author = a;
        title = require(t, 160, "Title");
        content = require(x, 4000, "Content");
    }

    @PrePersist
    void onCreate() {
        if (publishedAt == null) publishedAt = LocalDateTime.now();
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

    private static String require(String v, int max, String f) {
        if (v == null || v.isBlank()) throw new IllegalArgumentException(f + " cannot be blank");
        v = v.strip();
        if (v.length() > max) throw new IllegalArgumentException(f + " cannot exceed " + max + " characters");
        return v;
    }
}

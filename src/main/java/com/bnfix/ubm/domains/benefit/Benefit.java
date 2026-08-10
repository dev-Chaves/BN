package com.bnfix.ubm.domains.benefit;

import com.bnfix.ubm.domains.category.Category;
import com.bnfix.ubm.domains.company.Company;
import com.bnfix.ubm.domains.subscription.Subscription;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.*;

@Entity
@Table(name = "benefits")
public class Benefit {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "benefitsSeq")
    @SequenceGenerator(name = "benefitsSeq", sequenceName = "benefits_SEQ", allocationSize = 50)
    public Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, length = 500)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "provider_id", nullable = false)
    private Company provider;

    private Boolean active;

    @Column(name = "publicly_visible", nullable = false)
    private Boolean publiclyVisible = true;

    @Column(name = "valid_from")
    private LocalDateTime validFrom;

    @Column(name = "valid_until")
    private LocalDateTime validUntil;

    @Column(name = "max_uses_per_user", nullable = false)
    private Integer maxUsesPerUser = 1;

    @Column(columnDefinition = "TEXT")
    private String terms;

    @Column(name = "usages")
    private Integer usages;

    @OneToMany(mappedBy = "benefit")
    private List<Subscription> subscriptions;

    @ManyToMany
    @JoinTable(
            name = "benefit_categories",
            joinColumns = @JoinColumn(name = "benefit_id"),
            inverseJoinColumns = @JoinColumn(name = "category_id"))
    private Set<Category> categories = new HashSet<>();

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    protected Benefit() {}

    private Benefit(Builder builder) {
        name = builder.name;
        provider = builder.provider;
        description = builder.description;
        categories = new HashSet<>(builder.categories);
        publiclyVisible = builder.publiclyVisible;
        validFrom = builder.validFrom;
        validUntil = builder.validUntil;
        maxUsesPerUser = builder.maxUsesPerUser;
        terms = builder.terms;
        validateValidityWindow();
    }

    public static Builder builder(String name, Company provider) {
        return new Builder(name, provider);
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Company getProvider() {
        return provider;
    }

    public Boolean getActive() {
        return active;
    }

    public Boolean getPubliclyVisible() {
        return publiclyVisible;
    }

    public LocalDateTime getValidFrom() {
        return validFrom;
    }

    public LocalDateTime getValidUntil() {
        return validUntil;
    }

    public Integer getMaxUsesPerUser() {
        return maxUsesPerUser;
    }

    public String getTerms() {
        return terms;
    }

    public Set<Category> getCategories() {
        return categories;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public Integer getUsages() {
        return usages;
    }

    @PrePersist
    public void onCreate() {
        createdAt = LocalDateTime.now();
        if (active == null) active = false;
        if (publiclyVisible == null) publiclyVisible = true;
        if (maxUsesPerUser == null) maxUsesPerUser = 1;
    }

    public void activeBenefit() {
        active = true;
    }

    public void deactivateBenefit() {
        active = false;
    }

    public void update(String name, String description) {
        if (name != null && !name.isBlank()) this.name = name;
        if (description != null && !description.isBlank()) this.description = description;
    }

    public void updateCategories(Set<Category> categories) {
        if (categories != null) this.categories = new HashSet<>(categories);
    }

    public void updateAvailability(
            Boolean visible, LocalDateTime from, LocalDateTime until, Integer max, String terms) {
        if (visible != null) publiclyVisible = visible;
        if (from != null) validFrom = from;
        if (until != null) validUntil = until;
        if (max != null) maxUsesPerUser = max;
        if (terms != null) this.terms = terms;
        validateValidityWindow();
    }

    public boolean isOperationalAt(LocalDateTime now) {
        return provider != null
                && Boolean.TRUE.equals(provider.getActive())
                && Boolean.TRUE.equals(active)
                && (validFrom == null || !validFrom.isAfter(now))
                && (validUntil == null || validUntil.isAfter(now));
    }

    public boolean isAvailableAt(LocalDateTime now) {
        return isOperationalAt(now);
    }

    public boolean isDiscoverableAt(LocalDateTime now) {
        return Boolean.TRUE.equals(publiclyVisible) && isOperationalAt(now);
    }

    private void validateValidityWindow() {
        if (validFrom != null && validUntil != null && !validUntil.isAfter(validFrom))
            throw new IllegalArgumentException("Valid until must be after valid from");
    }

    public static class Builder {
        private final String name;
        private final Company provider;
        private String description;
        private Set<Category> categories = new HashSet<>();
        private Boolean publiclyVisible = true;
        private LocalDateTime validFrom, validUntil;
        private Integer maxUsesPerUser = 1;
        private String terms;

        public Builder(String name, Company provider) {
            this.name = name;
            this.provider = provider;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder categories(Set<Category> categories) {
            if (categories != null) this.categories.addAll(categories);
            return this;
        }

        public Builder availability(
                Boolean visible,
                LocalDateTime validFrom,
                LocalDateTime validUntil,
                Integer maxUsesPerUser,
                String terms) {
            if (visible != null) publiclyVisible = visible;
            this.validFrom = validFrom;
            this.validUntil = validUntil;
            if (maxUsesPerUser != null) this.maxUsesPerUser = maxUsesPerUser;
            this.terms = terms;
            return this;
        }

        public Benefit build() {
            if (name == null || description == null)
                throw new IllegalArgumentException("Name and description are required");
            if (maxUsesPerUser < 1) throw new IllegalArgumentException("Max uses per user must be at least 1");
            return new Benefit(this);
        }
    }
}

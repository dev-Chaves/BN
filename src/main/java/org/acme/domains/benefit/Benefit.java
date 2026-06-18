package org.acme.domains.benefit;

import org.acme.domains.category.Category;
import org.acme.domains.company.Company;
import org.acme.domains.subscription.Subscription;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "benefits")
public class Benefit extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "benefitsSeq")
    @SequenceGenerator(name = "benefitsSeq", sequenceName = "benefits_SEQ", allocationSize = 50)
    public Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "provider_id")
    private Company provider;

    private Boolean active;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "benefit")
    private List<Subscription> subscriptions;

    @ManyToMany
    @JoinTable(
            name = "benefit_categories",
            joinColumns = @JoinColumn(name = "benefit_id"),
            inverseJoinColumns = @JoinColumn(name = "category_id")
    )
    private Set<Category> categories = new HashSet<>();

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    protected Benefit() {}

    public static Builder builder(String name, Company provider) {
        return new Builder(name, provider);
    }

    private Benefit(Builder builder) {
        this.name = builder.name;
        this.provider = builder.provider;
        this.description = builder.description;
        this.categories = new HashSet<>(builder.categories);
    }

    public String getName() { return name; }
    public String getDescription() { return description; }
    public Company getProvider() { return provider; }
    public Boolean getActive() { return active; }
    public List<Subscription> getSubscriptions() { return subscriptions; }
    public Set<Category> getCategories() { return categories; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    private void setName(String name) { this.name = name; }
    private void setDescription(String description) { this.description = description; }
    private void setProvider(Company provider) { this.provider = provider; }
    private void setActive(Boolean active) { this.active = active; }
    private void setSubscriptions(List<Subscription> subscriptions) { this.subscriptions = subscriptions; }
    private void setCategories(Set<Category> categories) { this.categories = categories; }
    private void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    @PrePersist
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.active = Boolean.FALSE;
    }

    public void update(String name, String description) {
        if (name != null && !name.isBlank()) {
            this.name = name;
        }
        if (description != null && !description.isBlank()) {
            this.description = description;
        }
    }

    public void updateCategories(Set<Category> categories) {
        if (categories != null) {
            this.categories = new HashSet<>(categories);
        }
    }

    public static class Builder {

        private final String name;
        private final Company provider;
        private String description;
        private final Set<Category> categories = new HashSet<>();

        public Builder(String name, Company provider) {
            this.name = name;
            this.provider = provider;
        }

        public Builder description(String val) {
            if (val.isBlank()) throw new IllegalArgumentException("Description cant be null");
            description = val;
            return this;
        }

        public Builder category(Category category) {
            this.categories.add(category);
            return this;
        }

        public Builder categories(Set<Category> categories) {
            this.categories.addAll(categories);
            return this;
        }

        public Benefit build() {
            return new Benefit(this);
        }
    }

    public void activeBenefit() {
        this.active = Boolean.TRUE;
    }

    public void deactivateBenefit() {
        this.active = Boolean.FALSE;
    }
}

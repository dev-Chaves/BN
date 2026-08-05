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
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.acme.domains.employee.Employee;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "announcement_recipients",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_announcement_recipient",
                columnNames = {"announcement_id", "employee_id"}
        )
)
public class AnnouncementRecipient extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "announcementRecipientsSeq")
    @SequenceGenerator(
            name = "announcementRecipientsSeq",
            sequenceName = "announcement_recipients_SEQ",
            allocationSize = 50
    )
    public Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "announcement_id", nullable = false)
    private Announcement announcement;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    protected AnnouncementRecipient() {
    }

    public AnnouncementRecipient(Announcement announcement, Employee employee) {
        this.announcement = announcement;
        this.employee = employee;
    }

    public Announcement getAnnouncement() {
        return announcement;
    }

    public Employee getEmployee() {
        return employee;
    }

    public LocalDateTime getReadAt() {
        return readAt;
    }

    public boolean isRead() {
        return readAt != null;
    }

    public void markRead(LocalDateTime timestamp) {
        if (readAt == null) {
            readAt = timestamp;
        }
    }
}

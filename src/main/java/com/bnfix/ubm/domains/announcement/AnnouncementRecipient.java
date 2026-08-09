package com.bnfix.ubm.domains.announcement;

import com.bnfix.ubm.domains.employee.Employee;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "announcement_recipients",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uq_announcement_recipient",
                        columnNames = {"announcement_id", "employee_id"}))
public class AnnouncementRecipient {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "announcementRecipientsSeq")
    @SequenceGenerator(
            name = "announcementRecipientsSeq",
            sequenceName = "announcement_recipients_SEQ",
            allocationSize = 50)
    public Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "announcement_id", nullable = false)
    private Announcement announcement;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    protected AnnouncementRecipient() {}

    public AnnouncementRecipient(Announcement a, Employee e) {
        announcement = a;
        employee = e;
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

    public void markRead(LocalDateTime t) {
        if (readAt == null) readAt = t;
    }
}

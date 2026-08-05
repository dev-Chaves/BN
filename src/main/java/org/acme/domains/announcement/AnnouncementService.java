package org.acme.domains.announcement;

import io.quarkus.hibernate.reactive.panache.common.WithSession;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.NotFoundException;
import org.acme.domains.account.AccountRepository;
import org.acme.domains.announcement.dto.CreateAnnouncementRequest;
import org.acme.domains.announcement.dto.EmployeeAnnouncement;
import org.acme.domains.announcement.dto.EmployeeAnnouncementPage;
import org.acme.domains.announcement.dto.ManagerAnnouncement;
import org.acme.domains.announcement.dto.ManagerAnnouncementPage;
import org.acme.domains.announcement.dto.UnreadCountResponse;
import org.acme.domains.employee.Employee;
import org.acme.domains.employee.EmployeeRepository;
import org.acme.domains.manager.Manager;
import org.acme.domains.manager.ManagerRepository;
import org.acme.domains.shared.security.AccessStatusGuard;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class AnnouncementService {
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;

    private final AnnouncementRepository announcementRepository;
    private final AnnouncementRecipientRepository recipientRepository;
    private final AccountRepository accountRepository;
    private final EmployeeRepository employeeRepository;
    private final ManagerRepository managerRepository;

    public AnnouncementService(
            AnnouncementRepository announcementRepository,
            AnnouncementRecipientRepository recipientRepository,
            AccountRepository accountRepository,
            EmployeeRepository employeeRepository,
            ManagerRepository managerRepository
    ) {
        this.announcementRepository = announcementRepository;
        this.recipientRepository = recipientRepository;
        this.accountRepository = accountRepository;
        this.employeeRepository = employeeRepository;
        this.managerRepository = managerRepository;
    }

    @WithTransaction
    public Uni<ManagerAnnouncement> publish(
            String managerEmail,
            Long companyId,
            CreateAnnouncementRequest request
    ) {
        return findManager(managerEmail, companyId)
                .flatMap(manager -> announcementRepository.persist(new Announcement(
                                manager.getCompany(),
                                manager,
                                request.title(),
                                request.content()
                        ))
                        .flatMap(announcement -> employeeRepository.findActiveByCompanyId(companyId)
                                .flatMap(employees -> persistRecipients(announcement, employees)
                                        .replaceWith(toManagerAnnouncement(announcement, employees.size())))));
    }

    @WithSession
    public Uni<ManagerAnnouncementPage> listCompany(
            String managerEmail,
            Long companyId,
            int requestedPage,
            int requestedSize
    ) {
        PageSpec page = normalizePage(requestedPage, requestedSize);
        return findManager(managerEmail, companyId)
                .flatMap(manager -> announcementRepository.findByCompanyId(
                        manager.getCompany().id,
                        page.offset(),
                        page.size() + 1
                ))
                .flatMap(announcements -> {
                    boolean hasMore = announcements.size() > page.size();
                    List<Announcement> visible = hasMore
                            ? List.copyOf(announcements.subList(0, page.size()))
                            : List.copyOf(announcements);
                    List<Long> announcementIds = visible.stream().map(item -> item.id).toList();
                    return recipientRepository.countByAnnouncementIds(announcementIds)
                            .map(counts -> new ManagerAnnouncementPage(
                                    visible.stream()
                                            .map(item -> toManagerAnnouncement(
                                                    item,
                                                    counts.getOrDefault(item.id, 0L)
                                            ))
                                            .toList(),
                                    page.page(),
                                    page.size(),
                                    hasMore
                            ));
                });
    }

    @WithSession
    public Uni<EmployeeAnnouncementPage> listMine(
            String employeeEmail,
            Long companyId,
            int requestedPage,
            int requestedSize
    ) {
        PageSpec page = normalizePage(requestedPage, requestedSize);
        return findEmployee(employeeEmail, companyId)
                .flatMap(employee -> recipientRepository.findByEmployeeId(
                        employee.id,
                        companyId,
                        page.offset(),
                        page.size() + 1
                ))
                .map(recipients -> {
                    boolean hasMore = recipients.size() > page.size();
                    List<AnnouncementRecipient> visible = hasMore
                            ? recipients.subList(0, page.size())
                            : recipients;
                    return new EmployeeAnnouncementPage(
                            visible.stream().map(this::toEmployeeAnnouncement).toList(),
                            page.page(),
                            page.size(),
                            hasMore
                    );
                });
    }

    @WithSession
    public Uni<UnreadCountResponse> unreadCount(String employeeEmail, Long companyId) {
        return findEmployee(employeeEmail, companyId)
                .flatMap(employee -> recipientRepository.countUnreadByEmployeeId(employee.id))
                .map(UnreadCountResponse::new);
    }

    @WithTransaction
    public Uni<EmployeeAnnouncement> markRead(String employeeEmail, Long companyId, Long announcementId) {
        return findEmployee(employeeEmail, companyId)
                .flatMap(employee -> recipientRepository.findForRead(announcementId, employee.id, companyId))
                .onItem().ifNull().failWith(() -> new NotFoundException("Announcement not found"))
                .invoke(recipient -> recipient.markRead(LocalDateTime.now()))
                .map(this::toEmployeeAnnouncement);
    }

    @WithTransaction
    public Uni<UnreadCountResponse> markAllAsRead(String employeeEmail, Long companyId) {
        return findEmployee(employeeEmail, companyId)
                .flatMap(employee -> recipientRepository.markAllAsRead(employee.id, LocalDateTime.now()))
                .replaceWith(new UnreadCountResponse(0));
    }

    private Uni<Void> persistRecipients(Announcement announcement, List<Employee> employees) {
        if (employees.isEmpty()) {
            return Uni.createFrom().voidItem();
        }
        List<AnnouncementRecipient> recipients = employees.stream()
                .map(employee -> new AnnouncementRecipient(announcement, employee))
                .toList();
        return recipientRepository.persist(recipients);
    }

    private Uni<Manager> findManager(String email, Long companyId) {
        return managerRepository.findByEmailAndCompanyId(email, companyId)
                .onItem().ifNull().failWith(() -> new NotFoundException("Manager not found"))
                .map(AccessStatusGuard::requireActive)
                .map(manager -> {
                    requireCompany(manager.getCompany() == null ? null : manager.getCompany().id, companyId);
                    return manager;
                });
    }

    private Uni<Employee> findEmployee(String email, Long companyId) {
        return accountRepository.findByEmail(email)
                .onItem().ifNull().failWith(() -> new NotFoundException("Account not found"))
                .flatMap(account -> employeeRepository.findByAccountId(account.id))
                .onItem().ifNull().failWith(() -> new NotFoundException("Employee not found"))
                .map(AccessStatusGuard::requireActive)
                .map(employee -> {
                    requireCompany(employee.getCompany() == null ? null : employee.getCompany().id, companyId);
                    return employee;
                });
    }

    private void requireCompany(Long actualCompanyId, Long claimedCompanyId) {
        if (actualCompanyId == null || !actualCompanyId.equals(claimedCompanyId)) {
            throw new SecurityException("Unauthorized access: Tenant mismatch");
        }
    }

    private ManagerAnnouncement toManagerAnnouncement(Announcement announcement, long recipientCount) {
        return new ManagerAnnouncement(
                announcement.id,
                announcement.getTitle(),
                announcement.getContent(),
                announcement.getAuthor().getName(),
                announcement.getPublishedAt(),
                recipientCount
        );
    }

    private EmployeeAnnouncement toEmployeeAnnouncement(AnnouncementRecipient recipient) {
        Announcement announcement = recipient.getAnnouncement();
        return new EmployeeAnnouncement(
                announcement.id,
                announcement.getTitle(),
                announcement.getContent(),
                announcement.getAuthor().getName(),
                announcement.getPublishedAt(),
                recipient.isRead(),
                recipient.getReadAt()
        );
    }

    private PageSpec normalizePage(int requestedPage, int requestedSize) {
        int page = Math.max(0, requestedPage);
        int size = requestedSize <= 0
                ? DEFAULT_PAGE_SIZE
                : Math.min(requestedSize, MAX_PAGE_SIZE);
        long offset = (long) page * size;
        if (offset > Integer.MAX_VALUE - (size + 1L)) {
            throw new IllegalArgumentException("Page is too large");
        }
        return new PageSpec(page, size, (int) offset);
    }

    private record PageSpec(int page, int size, int offset) {
    }
}

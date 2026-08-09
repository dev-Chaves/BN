package com.bnfix.ubm.domains.announcement;

import com.bnfix.ubm.domains.account.AccountRepository;
import com.bnfix.ubm.domains.announcement.dto.*;
import com.bnfix.ubm.domains.employee.Employee;
import com.bnfix.ubm.domains.employee.EmployeeRepository;
import com.bnfix.ubm.domains.employee.EmployeeStatus;
import com.bnfix.ubm.domains.manager.Manager;
import com.bnfix.ubm.domains.manager.ManagerRepository;
import com.bnfix.ubm.shared.security.AccessStatusGuard;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
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
            ManagerRepository managerRepository) {
        this.announcementRepository = announcementRepository;
        this.recipientRepository = recipientRepository;
        this.accountRepository = accountRepository;
        this.employeeRepository = employeeRepository;
        this.managerRepository = managerRepository;
    }

    @Transactional
    public ManagerAnnouncement publish(String email, Long companyId, CreateAnnouncementRequest request) {
        Manager manager = findManager(email, companyId);
        Announcement announcement = announcementRepository.save(
                new Announcement(manager.getCompany(), manager, request.title(), request.content()));
        List<Employee> employees = employeeRepository.findByCompanyIdAndActive(companyId, EmployeeStatus.ACTIVE);
        recipientRepository.saveAll(employees.stream()
                .map(employee -> new AnnouncementRecipient(announcement, employee))
                .toList());
        return toManagerAnnouncement(announcement, employees.size());
    }

    @Transactional(readOnly = true)
    public ManagerAnnouncementPage listCompany(String email, Long companyId, int requestedPage, int requestedSize) {
        PageSpec page = page(requestedPage, requestedSize);
        findManager(email, companyId);
        List<Announcement> announcements =
                announcementRepository.findByCompanyId(companyId, PageRequest.of(page.page(), page.size() + 1));
        boolean hasMore = announcements.size() > page.size();
        List<Announcement> visible = hasMore ? announcements.subList(0, page.size()) : announcements;
        List<Long> ids = visible.stream().map(item -> item.id).toList();
        Map<Long, Long> counts = recipientRepository.countByAnnouncementIds(ids);
        return new ManagerAnnouncementPage(
                visible.stream()
                        .map(item -> toManagerAnnouncement(item, counts.getOrDefault(item.id, 0L)))
                        .toList(),
                page.page(),
                page.size(),
                hasMore);
    }

    @Transactional(readOnly = true)
    public EmployeeAnnouncementPage listMine(String email, Long companyId, int requestedPage, int requestedSize) {
        PageSpec page = page(requestedPage, requestedSize);
        Employee employee = findEmployee(email, companyId);
        List<AnnouncementRecipient> recipients = recipientRepository.findByEmployeeId(
                employee.id, companyId, PageRequest.of(page.page(), page.size() + 1));
        boolean hasMore = recipients.size() > page.size();
        List<AnnouncementRecipient> visible = hasMore ? recipients.subList(0, page.size()) : recipients;
        return new EmployeeAnnouncementPage(
                visible.stream().map(this::toEmployeeAnnouncement).toList(), page.page(), page.size(), hasMore);
    }

    @Transactional(readOnly = true)
    public UnreadCountResponse unreadCount(String email, Long companyId) {
        return new UnreadCountResponse(
                recipientRepository.countByEmployeeIdAndReadAtIsNull(findEmployee(email, companyId).id));
    }

    @Transactional
    public EmployeeAnnouncement markRead(String email, Long companyId, Long announcementId) {
        Employee employee = findEmployee(email, companyId);
        AnnouncementRecipient recipient = recipientRepository
                .findForRead(announcementId, employee.id, companyId)
                .orElseThrow(() -> notFound("Announcement not found"));
        recipient.markRead(LocalDateTime.now());
        return toEmployeeAnnouncement(recipient);
    }

    @Transactional
    public UnreadCountResponse markAllAsRead(String email, Long companyId) {
        recipientRepository.markAllAsRead(findEmployee(email, companyId).id, LocalDateTime.now());
        return new UnreadCountResponse(0);
    }

    private Manager findManager(String email, Long companyId) {
        Manager manager = AccessStatusGuard.requireActive(managerRepository
                .findByEmailAndCompanyId(email, companyId)
                .orElseThrow(() -> notFound("Manager not found")));
        requireCompany(manager.getCompany() == null ? null : manager.getCompany().id, companyId);
        return manager;
    }

    private Employee findEmployee(String email, Long companyId) {
        var account = accountRepository.findByEmail(email).orElseThrow(() -> notFound("Account not found"));
        Employee employee = AccessStatusGuard.requireActive(
                employeeRepository.findByAccountId(account.id).orElseThrow(() -> notFound("Employee not found")));
        requireCompany(employee.getCompany() == null ? null : employee.getCompany().id, companyId);
        return employee;
    }

    private void requireCompany(Long actual, Long expected) {
        if (actual == null || !actual.equals(expected))
            throw new SecurityException("Unauthorized access: Tenant mismatch");
    }

    private ManagerAnnouncement toManagerAnnouncement(Announcement a, long count) {
        return new ManagerAnnouncement(
                a.id, a.getTitle(), a.getContent(), a.getAuthor().getName(), a.getPublishedAt(), count);
    }

    private EmployeeAnnouncement toEmployeeAnnouncement(AnnouncementRecipient r) {
        Announcement a = r.getAnnouncement();
        return new EmployeeAnnouncement(
                a.id,
                a.getTitle(),
                a.getContent(),
                a.getAuthor().getName(),
                a.getPublishedAt(),
                r.isRead(),
                r.getReadAt());
    }

    private PageSpec page(int requestedPage, int requestedSize) {
        int p = Math.max(0, requestedPage);
        int s = requestedSize <= 0 ? DEFAULT_PAGE_SIZE : Math.min(requestedSize, MAX_PAGE_SIZE);
        long offset = (long) p * s;
        if (offset > Integer.MAX_VALUE - (s + 1L)) throw new IllegalArgumentException("Page is too large");
        return new PageSpec(p, s);
    }

    private ResponseStatusException notFound(String message) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, message);
    }

    private record PageSpec(int page, int size) {}
}

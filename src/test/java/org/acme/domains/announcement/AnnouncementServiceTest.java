package org.acme.domains.announcement;

import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.NotFoundException;
import org.acme.domains.account.Account;
import org.acme.domains.account.AccountRepository;
import org.acme.domains.announcement.dto.CreateAnnouncementRequest;
import org.acme.domains.announcement.dto.EmployeeAnnouncement;
import org.acme.domains.announcement.dto.EmployeeAnnouncementPage;
import org.acme.domains.announcement.dto.ManagerAnnouncement;
import org.acme.domains.announcement.dto.ManagerAnnouncementPage;
import org.acme.domains.company.Company;
import org.acme.domains.employee.Employee;
import org.acme.domains.employee.EmployeeRepository;
import org.acme.domains.manager.Manager;
import org.acme.domains.manager.ManagerRepository;
import org.acme.domains.shared.domain.CNPJ;
import org.acme.domains.shared.domain.CPF;
import org.acme.domains.shared.enums.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AnnouncementServiceTest {

    @Mock
    AnnouncementRepository announcementRepository;

    @Mock
    AnnouncementRecipientRepository recipientRepository;

    @Mock
    AccountRepository accountRepository;

    @Mock
    EmployeeRepository employeeRepository;

    @Mock
    ManagerRepository managerRepository;

    private AnnouncementService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new AnnouncementService(
                announcementRepository,
                recipientRepository,
                accountRepository,
                employeeRepository,
                managerRepository
        );
    }

    @Test
    void shouldPublishOnlyToEmployeesReturnedAsActiveForTheCurrentCompany() {
        Company company = activeCompany(10L);
        Manager manager = activeManager(20L, company);
        Employee first = activeEmployee(30L, company, "first@acme.com", "52998224725");
        Employee second = activeEmployee(31L, company, "second@acme.com", "11144477735");

        when(managerRepository.findByEmailAndCompanyId("manager@acme.com", 10L))
                .thenReturn(Uni.createFrom().item(manager));
        when(announcementRepository.persist(any(Announcement.class))).thenAnswer(invocation -> {
            Announcement announcement = invocation.getArgument(0);
            announcement.id = 40L;
            announcement.onCreate();
            return Uni.createFrom().item(announcement);
        });
        when(employeeRepository.findActiveByCompanyId(10L))
                .thenReturn(Uni.createFrom().item(List.of(first, second)));
        when(recipientRepository.persist(anyList())).thenReturn(Uni.createFrom().voidItem());

        ManagerAnnouncement response = service.publish(
                "manager@acme.com",
                10L,
                new CreateAnnouncementRequest("  New benefit  ", "  Read the details  ")
        ).await().indefinitely();

        assertEquals(40L, response.id());
        assertEquals("New benefit", response.title());
        assertEquals("Read the details", response.content());
        assertEquals("Manager", response.author());
        assertEquals(2, response.recipientCount());
        assertNotNull(response.publishedAt());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<AnnouncementRecipient>> captor = ArgumentCaptor.forClass(List.class);
        verify(recipientRepository).persist(captor.capture());
        assertEquals(List.of(30L, 31L), captor.getValue().stream()
                .map(recipient -> recipient.getEmployee().id)
                .toList());
        verify(employeeRepository).findActiveByCompanyId(10L);
    }

    @Test
    void shouldNotPublishForAnInactiveManager() {
        Company company = activeCompany(10L);
        Manager inactive = Manager.builder("Manager", company, managerAccount()).build();
        inactive.id = 20L;

        when(managerRepository.findByEmailAndCompanyId("manager@acme.com", 10L))
                .thenReturn(Uni.createFrom().item(inactive));

        assertThrows(SecurityException.class, () -> service.publish(
                "manager@acme.com",
                10L,
                new CreateAnnouncementRequest("Title", "Content")
        ).await().indefinitely());

        verify(announcementRepository, never()).persist(any(Announcement.class));
        verify(employeeRepository, never()).findActiveByCompanyId(any());
    }

    @Test
    void shouldReturnPaginatedCompanyHistoryWithRecipientCounts() {
        Company company = activeCompany(10L);
        Manager manager = activeManager(20L, company);
        Announcement first = announcement(101L, manager, "First");
        Announcement second = announcement(100L, manager, "Second");
        Announcement lookAhead = announcement(99L, manager, "Look ahead");

        when(managerRepository.findByEmailAndCompanyId("manager@acme.com", 10L))
                .thenReturn(Uni.createFrom().item(manager));
        when(announcementRepository.findByCompanyId(10L, 2, 3))
                .thenReturn(Uni.createFrom().item(List.of(first, second, lookAhead)));
        when(recipientRepository.countByAnnouncementIds(List.of(101L, 100L)))
                .thenReturn(Uni.createFrom().item(Map.of(101L, 4L, 100L, 3L)));

        ManagerAnnouncementPage response = service.listCompany(
                "manager@acme.com", 10L, 1, 2
        ).await().indefinitely();

        assertEquals(1, response.page());
        assertEquals(2, response.size());
        assertTrue(response.hasMore());
        assertEquals(List.of(101L, 100L), response.items().stream().map(ManagerAnnouncement::id).toList());
        assertEquals(List.of(4L, 3L), response.items().stream().map(ManagerAnnouncement::recipientCount).toList());
    }

    @Test
    void shouldListOnlyTheAuthenticatedEmployeeRecipientsAndKeepReadHistory() {
        Company company = activeCompany(10L);
        Manager manager = activeManager(20L, company);
        Employee employee = activeEmployee(30L, company, "employee@acme.com", "52998224725");
        Announcement first = announcement(101L, manager, "First");
        Announcement second = announcement(100L, manager, "Second");
        Announcement lookAhead = announcement(99L, manager, "Look ahead");
        AnnouncementRecipient firstRecipient = new AnnouncementRecipient(first, employee);
        AnnouncementRecipient secondRecipient = new AnnouncementRecipient(second, employee);
        secondRecipient.markRead(java.time.LocalDateTime.now());
        AnnouncementRecipient lookAheadRecipient = new AnnouncementRecipient(lookAhead, employee);

        mockEmployeeLookup(employee);
        when(recipientRepository.findByEmployeeId(30L, 10L, 0, 3))
                .thenReturn(Uni.createFrom().item(List.of(
                        firstRecipient,
                        secondRecipient,
                        lookAheadRecipient
                )));

        EmployeeAnnouncementPage response = service.listMine(
                "employee@acme.com", 10L, 0, 2
        ).await().indefinitely();

        assertTrue(response.hasMore());
        assertEquals(2, response.items().size());
        assertFalse(response.items().getFirst().read());
        assertTrue(response.items().get(1).read());
        assertNotNull(response.items().get(1).readAt());
    }

    @Test
    void shouldMarkOneAnnouncementAsReadIdempotently() {
        Company company = activeCompany(10L);
        Manager manager = activeManager(20L, company);
        Employee employee = activeEmployee(30L, company, "employee@acme.com", "52998224725");
        AnnouncementRecipient recipient = new AnnouncementRecipient(
                announcement(101L, manager, "First"),
                employee
        );

        mockEmployeeLookup(employee);
        when(recipientRepository.findForRead(101L, 30L, 10L))
                .thenReturn(Uni.createFrom().item(recipient));

        EmployeeAnnouncement firstRead = service.markRead(
                "employee@acme.com", 10L, 101L
        ).await().indefinitely();
        EmployeeAnnouncement secondRead = service.markRead(
                "employee@acme.com", 10L, 101L
        ).await().indefinitely();

        assertTrue(firstRead.read());
        assertEquals(firstRead.readAt(), secondRead.readAt());
        assertNotNull(secondRead.readAt());
    }

    @Test
    void shouldHideAnnouncementsThatDoNotBelongToTheEmployee() {
        Company company = activeCompany(10L);
        Employee employee = activeEmployee(30L, company, "employee@acme.com", "52998224725");
        mockEmployeeLookup(employee);
        when(recipientRepository.findForRead(999L, 30L, 10L))
                .thenReturn(Uni.createFrom().nullItem());

        assertThrows(NotFoundException.class, () -> service.markRead(
                "employee@acme.com", 10L, 999L
        ).await().indefinitely());
    }

    @Test
    void shouldRejectAnEmployeeTokenForAnotherCompany() {
        Company company = activeCompany(10L);
        Employee employee = activeEmployee(30L, company, "employee@acme.com", "52998224725");
        mockEmployeeLookup(employee);

        assertThrows(SecurityException.class, () -> service.unreadCount(
                "employee@acme.com", 11L
        ).await().indefinitely());

        verify(recipientRepository, never()).countUnreadByEmployeeId(any());
    }

    @Test
    void shouldMarkAllUnreadAnnouncementsForOnlyTheAuthenticatedEmployee() {
        Company company = activeCompany(10L);
        Employee employee = activeEmployee(30L, company, "employee@acme.com", "52998224725");
        mockEmployeeLookup(employee);
        when(recipientRepository.markAllAsRead(any(), any()))
                .thenReturn(Uni.createFrom().item(3));

        var response = service.markAllAsRead(
                "employee@acme.com", 10L
        ).await().indefinitely();

        assertEquals(0, response.unreadCount());
        verify(recipientRepository).markAllAsRead(org.mockito.ArgumentMatchers.eq(30L), any());
    }

    @Test
    void shouldRejectBlankAndOversizedAnnouncementTextAtTheDomainBoundary() {
        Company company = activeCompany(10L);
        Manager manager = activeManager(20L, company);

        assertThrows(IllegalArgumentException.class,
                () -> new Announcement(company, manager, " ", "Content"));
        assertThrows(IllegalArgumentException.class,
                () -> new Announcement(company, manager, "Title", "x".repeat(4001)));
    }

    private void mockEmployeeLookup(Employee employee) {
        when(accountRepository.findByEmail(employee.getAccount().getEmail()))
                .thenReturn(Uni.createFrom().item(employee.getAccount()));
        when(employeeRepository.findByAccountId(employee.getAccount().id))
                .thenReturn(Uni.createFrom().item(employee));
    }

    private Company activeCompany(Long id) {
        Company company = Company.builder("ACME", CNPJ.of("11222333000181")).build();
        company.id = id;
        company.onCreate();
        return company;
    }

    private Manager activeManager(Long id, Company company) {
        Manager manager = Manager.builder("Manager", company, managerAccount()).build();
        manager.id = id;
        manager.onCreate();
        return manager;
    }

    private Employee activeEmployee(Long id, Company company, String email, String cpf) {
        Account account = Account.builder("Employee", CPF.of(cpf), "password", email, Role.USER).build();
        account.id = UUID.randomUUID();
        Employee employee = Employee.builder("Employee", company, account).build();
        employee.id = id;
        employee.active();
        return employee;
    }

    private Account managerAccount() {
        Account account = Account.builder(
                "Manager",
                CPF.of("93541134780"),
                "password",
                "manager@acme.com",
                Role.MANAGER
        ).build();
        account.id = UUID.randomUUID();
        return account;
    }

    private Announcement announcement(Long id, Manager manager, String title) {
        Announcement announcement = new Announcement(
                manager.getCompany(),
                manager,
                title,
                title + " content"
        );
        announcement.id = id;
        announcement.onCreate();
        return announcement;
    }
}

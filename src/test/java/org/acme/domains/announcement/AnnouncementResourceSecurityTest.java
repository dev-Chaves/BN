package org.acme.domains.announcement;

import jakarta.annotation.security.RolesAllowed;
import org.acme.domains.announcement.dto.CreateAnnouncementRequest;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AnnouncementResourceSecurityTest {

    @Test
    void shouldRestrictManagerEndpointsToManagers() throws Exception {
        assertRole(AnnouncementResource.class.getMethod("publish", CreateAnnouncementRequest.class), "MANAGER");
        assertRole(AnnouncementResource.class.getMethod("listCompany", int.class, int.class), "MANAGER");
    }

    @Test
    void shouldRestrictEmployeeEndpointsToUsers() throws Exception {
        assertRole(AnnouncementResource.class.getMethod("listMine", int.class, int.class), "USER");
        assertRole(AnnouncementResource.class.getMethod("unreadCount"), "USER");
        assertRole(AnnouncementResource.class.getMethod("markRead", Long.class), "USER");
        assertRole(AnnouncementResource.class.getMethod("markAllAsRead"), "USER");
    }

    private void assertRole(Method method, String expectedRole) {
        RolesAllowed annotation = method.getAnnotation(RolesAllowed.class);
        assertEquals(List.of(expectedRole), Arrays.asList(annotation.value()));
    }
}

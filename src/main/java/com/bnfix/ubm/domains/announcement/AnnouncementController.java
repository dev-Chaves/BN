package com.bnfix.ubm.domains.announcement;

import com.bnfix.ubm.domains.announcement.dto.CreateAnnouncementRequest;
import com.bnfix.ubm.shared.security.JwtCompanyContext;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/announcements")
public class AnnouncementController {
    private final AnnouncementService service;
    public AnnouncementController(AnnouncementService service) { this.service = service; }
    @PostMapping @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<?> publish(@Valid @RequestBody CreateAnnouncementRequest request, Authentication auth, @AuthenticationPrincipal Jwt jwt) { return ResponseEntity.status(201).body(service.publish(auth.getName(), companyId(jwt), request)); }
    @GetMapping("/company") @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<?> listCompany(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size, Authentication auth, @AuthenticationPrincipal Jwt jwt) { return ResponseEntity.ok(service.listCompany(auth.getName(), companyId(jwt), page, size)); }
    @GetMapping("/me") @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> listMine(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size, Authentication auth, @AuthenticationPrincipal Jwt jwt) { return ResponseEntity.ok(service.listMine(auth.getName(), companyId(jwt), page, size)); }
    @GetMapping("/me/unread-count") @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> unreadCount(Authentication auth, @AuthenticationPrincipal Jwt jwt) { return ResponseEntity.ok(service.unreadCount(auth.getName(), companyId(jwt))); }
    @PutMapping("/{announcementId}/read") @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> markRead(@PathVariable Long announcementId, Authentication auth, @AuthenticationPrincipal Jwt jwt) { return ResponseEntity.ok(service.markRead(auth.getName(), companyId(jwt), announcementId)); }
    @PutMapping("/me/read-all") @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> markAllAsRead(Authentication auth, @AuthenticationPrincipal Jwt jwt) { return ResponseEntity.ok(service.markAllAsRead(auth.getName(), companyId(jwt))); }
    private Long companyId(Jwt jwt) { return JwtCompanyContext.requireCompanyId(jwt); }
}

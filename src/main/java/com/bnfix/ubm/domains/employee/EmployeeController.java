package com.bnfix.ubm.domains.employee;

import com.bnfix.ubm.domains.employee.dto.*;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/employees")
public class EmployeeController {
    private final EmployeeService employeeService;

    public EmployeeController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    @PostMapping
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<?> create(
            @Valid @RequestBody CreateEmployeeRequest request, @AuthenticationPrincipal Jwt jwt) {
        log.info("POST /employees by {}", email(jwt));
        return ResponseEntity.status(201).body(employeeService.create(request, email(jwt), company(jwt)));
    }

    @PutMapping("/disable")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<?> disable(@RequestParam Long employeeId, @AuthenticationPrincipal Jwt jwt) {
        log.info("PUT /employees/disable (employeeId={}) by {}", employeeId, email(jwt));
        return ResponseEntity.ok(employeeService.disable(employeeId, email(jwt), company(jwt)));
    }

    @PutMapping("/activate")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<?> activate(@RequestParam Long employeeId, @AuthenticationPrincipal Jwt jwt) {
        log.info("PUT /employees/activate (employeeId={}) by {}", employeeId, email(jwt));
        return ResponseEntity.ok(employeeService.activate(employeeId, email(jwt), company(jwt)));
    }

    @PutMapping("/{employeeId}")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<?> update(
            @PathVariable Long employeeId,
            @Valid @RequestBody UpdateEmployeeRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        log.info("PUT /employees/{} by {}", employeeId, email(jwt));
        return ResponseEntity.ok(employeeService.update(employeeId, request, email(jwt), company(jwt)));
    }

    @GetMapping
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<?> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(employeeService.list(email(jwt), company(jwt), page, size));
    }

    private String email(Jwt jwt) {
        return jwt.getClaimAsString("email") != null ? jwt.getClaimAsString("email") : jwt.getSubject();
    }

    private Long company(Jwt jwt) {
        return com.bnfix.ubm.shared.security.JwtCompanyContext.requireCompanyId(jwt);
    }
}

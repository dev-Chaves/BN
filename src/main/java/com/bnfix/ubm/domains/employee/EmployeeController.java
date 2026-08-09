package com.bnfix.ubm.domains.employee;

import com.bnfix.ubm.domains.employee.dto.*;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/employees")
public class EmployeeController {
    private final EmployeeService service;

    public EmployeeController(EmployeeService s) {
        service = s;
    }

    @PostMapping
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<?> create(@Valid @RequestBody CreateEmployeeRequest r, @AuthenticationPrincipal Jwt j) {
        return ResponseEntity.status(201).body(service.create(r, email(j), company(j)));
    }

    @PutMapping("/disable")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<?> disable(@RequestParam Long employeeId, @AuthenticationPrincipal Jwt j) {
        return ResponseEntity.ok(service.disable(employeeId, email(j), company(j)));
    }

    @PutMapping("/activate")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<?> activate(@RequestParam Long employeeId, @AuthenticationPrincipal Jwt j) {
        return ResponseEntity.ok(service.activate(employeeId, email(j), company(j)));
    }

    @PutMapping("/{employeeId}")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<?> update(
            @PathVariable Long employeeId,
            @Valid @RequestBody UpdateEmployeeRequest r,
            @AuthenticationPrincipal Jwt j) {
        return ResponseEntity.ok(service.update(employeeId, r, email(j), company(j)));
    }

    @GetMapping
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<?> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @AuthenticationPrincipal Jwt j) {
        return ResponseEntity.ok(service.list(email(j), company(j), page, size));
    }

    private String email(Jwt j) {
        return j.getClaimAsString("email") != null ? j.getClaimAsString("email") : j.getSubject();
    }

    private Long company(Jwt j) {
        return com.bnfix.ubm.shared.security.JwtCompanyContext.requireCompanyId(j);
    }
}

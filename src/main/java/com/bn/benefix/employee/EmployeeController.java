package com.bn.benefix.employee;

import com.bn.benefix.employee.dto.EmployeeCreationRequestDTO;
import com.bn.benefix.employee.dto.EmployeeCreationResponseDTO;
import com.bn.benefix.employee.dto.EmployeeUpdateRequestDTO;
import com.bn.benefix.infra.security.AccountUserDetails;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("employee")
public class EmployeeController {

    private final EmployeeService employeeService;

    public EmployeeController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    @PostMapping()
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<EmployeeCreationResponseDTO> createEmployee(
            @Valid @RequestBody EmployeeCreationRequestDTO dto,
            UriComponentsBuilder builder,
            @AuthenticationPrincipal AccountUserDetails userDetails){

        EmployeeCreationResponseDTO response = employeeService.createEmployee(dto, userDetails.getAccount().getId());

        URI uri = builder.path("/employee/{id}")
                .buildAndExpand(response.id())
                .toUri();
        return ResponseEntity.created(uri).body(response);
    }

    @GetMapping
    public ResponseEntity<List<EmployeeCreationResponseDTO>> getAllEmployees(@AuthenticationPrincipal AccountUserDetails userDetails) {
        return ResponseEntity.ok(employeeService.findAll(userDetails.getAccount()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<EmployeeCreationResponseDTO> getEmployeeById(@PathVariable Long id, @AuthenticationPrincipal AccountUserDetails userDetails) {
        return ResponseEntity.ok(employeeService.findById(id, userDetails.getAccount()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<EmployeeCreationResponseDTO> updateEmployee(
            @PathVariable Long id,
            @RequestBody EmployeeUpdateRequestDTO dto,
            @AuthenticationPrincipal AccountUserDetails userDetails) {
        return ResponseEntity.ok(employeeService.update(id, dto, userDetails.getAccount().getId()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<Void> deleteEmployee(
            @PathVariable Long id,
            @AuthenticationPrincipal AccountUserDetails userDetails) {
        employeeService.delete(id, userDetails.getAccount().getId());
        return ResponseEntity.noContent().build();
    }
}

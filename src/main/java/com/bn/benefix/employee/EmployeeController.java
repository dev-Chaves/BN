package com.bn.benefix.employee;

import com.bn.benefix.employee.dto.EmployeeCreationRequestDTO;
import com.bn.benefix.employee.dto.EmployeeCreationResponseDTO;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.LocalDateTime;

@RestController
@RequestMapping("employee")
public class EmployeeController {

    private final EmployeeService employeeService;

    public EmployeeController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    @PostMapping()
    public ResponseEntity<EmployeeCreationResponseDTO> createEmployee(@Valid @RequestBody EmployeeCreationRequestDTO dto, UriComponentsBuilder builder){

        EmployeeCreationResponseDTO response = employeeService.createEmployee(dto);

        URI uri = builder.path("/employee/{id}")
                .buildAndExpand(response.id())
                .toUri();
        return ResponseEntity.created(uri).body(response);
    }

    @org.springframework.web.bind.annotation.GetMapping
    public ResponseEntity<java.util.List<EmployeeCreationResponseDTO>> getAllEmployees() {
        return ResponseEntity.ok(employeeService.findAll());
    }

    @org.springframework.web.bind.annotation.GetMapping("/{id}")
    public ResponseEntity<EmployeeCreationResponseDTO> getEmployeeById(@org.springframework.web.bind.annotation.PathVariable Long id) {
        return ResponseEntity.ok(employeeService.findById(id));
    }

    @org.springframework.web.bind.annotation.PutMapping("/{id}")
    public ResponseEntity<EmployeeCreationResponseDTO> updateEmployee(
            @org.springframework.web.bind.annotation.PathVariable Long id,
            @RequestBody com.bn.benefix.employee.dto.EmployeeUpdateRequestDTO dto) {
        return ResponseEntity.ok(employeeService.update(id, dto));
    }

    @org.springframework.web.bind.annotation.DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEmployee(@org.springframework.web.bind.annotation.PathVariable Long id) {
        employeeService.delete(id);
        return ResponseEntity.noContent().build();
    }

}

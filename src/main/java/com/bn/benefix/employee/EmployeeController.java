package com.bn.benefix.employee;

import com.bn.benefix.employee.dto.EmployeeCreationRequestDTO;
import com.bn.benefix.employee.dto.EmployeeCreationResponseDTO;
import com.bn.benefix.employee.dto.EmployeeUpdateRequestDTO;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<EmployeeCreationResponseDTO> createEmployee(@Valid @RequestBody EmployeeCreationRequestDTO dto, UriComponentsBuilder builder){

        EmployeeCreationResponseDTO response = employeeService.createEmployee(dto);

        URI uri = builder.path("/employee/{id}")
                .buildAndExpand(response.id())
                .toUri();
        return ResponseEntity.created(uri).body(response);
    }

    @GetMapping
    public ResponseEntity<List<EmployeeCreationResponseDTO>> getAllEmployees() {
        return ResponseEntity.ok(employeeService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<EmployeeCreationResponseDTO> getEmployeeById(@PathVariable Long id) {
        return ResponseEntity.ok(employeeService.findById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<EmployeeCreationResponseDTO> updateEmployee(
            @PathVariable Long id,
            @RequestBody EmployeeUpdateRequestDTO dto) {
        return ResponseEntity.ok(employeeService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEmployee(@PathVariable Long id) {
        employeeService.delete(id);
        return ResponseEntity.noContent().build();
    }
}

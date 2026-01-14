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

        Employee employee = employeeService.createEmployee(dto);

        EmployeeCreationResponseDTO response = new EmployeeCreationResponseDTO(
                employee.getId(),
                employee.getName(),
                employee.getCpf().getValue(),
                employee.getCompany().getId(),
                employee.getActive(),
                employee.getCreatedAt()
        );

        URI uri = builder.path("/employee/{id}")
                .buildAndExpand(employee.getId())
                .toUri();
        return ResponseEntity.created(uri).body(response);
    }

}

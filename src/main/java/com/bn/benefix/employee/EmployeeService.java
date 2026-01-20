package com.bn.benefix.employee;

import com.bn.benefix.company.Company;
import com.bn.benefix.company.CompanyRepository;
import com.bn.benefix.employee.dto.EmployeeCreationRequestDTO;
import com.bn.benefix.employee.dto.EmployeeCreationResponseDTO;
import com.bn.benefix.employee.dto.EmployeeUpdateRequestDTO;
import com.bn.benefix.shared.domain.CPF;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final CompanyRepository companyRepository;

    public EmployeeService(EmployeeRepository employeeRepository, CompanyRepository companyRepository) {
        this.employeeRepository = employeeRepository;
        this.companyRepository = companyRepository;
    }

    public EmployeeCreationResponseDTO createEmployee(EmployeeCreationRequestDTO dto) {
        Company company = companyRepository.findById(dto.companyId())
                .orElseThrow(() -> new IllegalArgumentException("Company not found"));

        Employee newEmployee = new Employee.Builder(
                dto.name(),
                CPF.of(dto.cpf()))
                .build();
        
        newEmployee.defineCompany(company);

        Employee savedEmployee = employeeRepository.save(newEmployee);

        return new EmployeeCreationResponseDTO(
                savedEmployee.getId(),
                savedEmployee.getName(),
                savedEmployee.getCpf().getValue(),
                savedEmployee.getCompany().getId(),
                savedEmployee.getActive(),
                savedEmployee.getCreatedAt()
        );
    }

    public List<EmployeeCreationResponseDTO> findAll() {
        return employeeRepository.findAll().stream()
                .map(e -> new EmployeeCreationResponseDTO(
                        e.getId(),
                        e.getName(),
                        e.getCpf().getValue(),
                        e.getCompany().getId(),
                        e.getActive(),
                        e.getCreatedAt()
                ))
                .toList();
    }

    public EmployeeCreationResponseDTO findById(Long id) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Employee not found"));
        return new EmployeeCreationResponseDTO(
                employee.getId(),
                employee.getName(),
                employee.getCpf().getValue(),
                employee.getCompany().getId(),
                employee.getActive(),
                employee.getCreatedAt()
        );
    }

    @Transactional
    public EmployeeCreationResponseDTO update(Long id, EmployeeUpdateRequestDTO dto) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Employee not found"));

        employee.update(dto.name());

        return new EmployeeCreationResponseDTO(
                employee.getId(),
                employee.getName(),
                employee.getCpf().getValue(),
                employee.getCompany().getId(),
                employee.getActive(),
                employee.getCreatedAt()
        );
    }

    @Transactional
    public void delete(Long id) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Employee not found"));
        
        // Assuming we want to set it to DISABLE when "deleting"
        if (employee.getActive() != EmployeeStatus.DISABLE) {
            employee.disableEmployee(EmployeeStatus.DISABLE);
        }
    }
}

package com.bn.benefix.employee;

import com.bn.benefix.company.Company;
import com.bn.benefix.company.CompanyRepository;
import com.bn.benefix.employee.dto.EmployeeCreationRequestDTO;
import com.bn.benefix.shared.domain.CPF;
import org.springframework.stereotype.Service;

@Service
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final CompanyRepository companyRepository;

    public EmployeeService(EmployeeRepository employeeRepository, CompanyRepository companyRepository) {
        this.employeeRepository = employeeRepository;
        this.companyRepository = companyRepository;
    }

    private void createEmployee(EmployeeCreationRequestDTO dto) {
        Company company = companyRepository.findById(dto.companyId())
                .orElseThrow(() -> new IllegalArgumentException("Company not found"));

        Employee newEmployee = new Employee.Builder(
                dto.name(),
                CPF.of(dto.cpf()))
                .build();
        
        newEmployee.defineCompany(company);

        employeeRepository.save(newEmployee);
    }
}

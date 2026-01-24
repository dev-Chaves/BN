package com.bn.benefix.employee;

import com.bn.benefix.manager.Manager;
import com.bn.benefix.manager.ManagerRepository;
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
import java.util.UUID;

@Service
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final CompanyRepository companyRepository;
    private final com.bn.benefix.account.AccountRepository accountRepository;
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;
    private final ManagerRepository managerRepository;

    public EmployeeService(EmployeeRepository employeeRepository, CompanyRepository companyRepository, com.bn.benefix.account.AccountRepository accountRepository, org.springframework.security.crypto.password.PasswordEncoder passwordEncoder, ManagerRepository managerRepository) {
        this.employeeRepository = employeeRepository;
        this.companyRepository = companyRepository;
        this.accountRepository = accountRepository;
        this.passwordEncoder = passwordEncoder;
        this.managerRepository = managerRepository;
    }

    public EmployeeCreationResponseDTO createEmployee(EmployeeCreationRequestDTO dto, UUID accountId) {
        Company company = companyRepository.findById(dto.companyId())
                .orElseThrow(() -> new IllegalArgumentException("Company not found"));
        
        Manager manager = managerRepository.findByAccountId(accountId)
                .orElseThrow(() -> new EntityNotFoundException("Manager not found"));
        
        if (!manager.getCompany().getId().equals(company.getId())) {
            throw new IllegalArgumentException("Manager does not belong to this company");
        }

        if (accountRepository.findByCpf(dto.cpf()).isPresent()) {
            throw new IllegalArgumentException("Account with this CPF already exists");
        }

        if(isEmailTaken(dto.email())) {
            throw new IllegalArgumentException("Email is already in use");
        }

        com.bn.benefix.account.Account newAccount = new com.bn.benefix.account.Account.Builder(
                dto.name(),
                CPF.of(dto.cpf()),
                passwordEncoder.encode(dto.password()),
                dto.email(),
                com.bn.benefix.shared.enums.Role.USER
        ).build();

        com.bn.benefix.account.Account savedAccount = accountRepository.save(newAccount);

        Employee newEmployee = new Employee.Builder(
                dto.name(),
                company,
                savedAccount)
                .build();
        
        Employee savedEmployee = employeeRepository.save(newEmployee);

        return new EmployeeCreationResponseDTO(
                savedEmployee.getId(),
                savedEmployee.getName(),
                savedEmployee.getCompany().getId(),
                savedEmployee.getActive(),
                savedEmployee.getCreatedAt()
        );
    }

    private boolean isEmailTaken(String email) {
        return accountRepository.findByEmail(email).isPresent();
    }

    public List<EmployeeCreationResponseDTO> findAll(com.bn.benefix.account.Account requester) {
        if (requester.getRole() == com.bn.benefix.shared.enums.Role.ADMIN) {
            return employeeRepository.findAll().stream()
                    .map(this::convertToDTO)
                    .toList();
        }

        Manager manager = managerRepository.findByAccountId(requester.getId())
                .orElseThrow(() -> new EntityNotFoundException("Manager not found"));

        return employeeRepository.findAll().stream()
                .filter(e -> e.getCompany().getId().equals(manager.getCompany().getId()))
                .map(this::convertToDTO)
                .toList();
    }

    public EmployeeCreationResponseDTO findById(Long id, com.bn.benefix.account.Account requester) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Employee not found"));

        if (requester.getRole() != com.bn.benefix.shared.enums.Role.ADMIN) {
            Manager manager = managerRepository.findByAccountId(requester.getId())
                    .orElseThrow(() -> new EntityNotFoundException("Manager not found"));

            if (!employee.getCompany().getId().equals(manager.getCompany().getId())) {
                throw new SecurityException("You do not have permission to view this employee");
            }
        }

        return convertToDTO(employee);
    }

    private EmployeeCreationResponseDTO convertToDTO(Employee e) {
        return new EmployeeCreationResponseDTO(
                e.getId(),
                e.getName(),
                e.getCompany().getId(),
                e.getActive(),
                e.getCreatedAt()
        );
    }

    @Transactional
    public EmployeeCreationResponseDTO update(Long id, EmployeeUpdateRequestDTO dto, UUID accountId) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Employee not found"));

        validateManagerAuthorization(accountId, employee.getCompany().getId());

        employee.update(dto.name());

        return convertToDTO(employee);
    }

    @Transactional
    public void delete(Long id, UUID accountId) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Employee not found"));

        validateManagerAuthorization(accountId, employee.getCompany().getId());

        // Assuming we want to set it to DISABLE when "deleting"
        if (employee.getActive() != EmployeeStatus.DISABLE) {
            employee.disableEmployee(EmployeeStatus.DISABLE);
        }
    }

    private void validateManagerAuthorization(UUID accountId, Long companyId) {
        com.bn.benefix.account.Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new EntityNotFoundException("Account not found"));
        
        if (account.getRole() == com.bn.benefix.shared.enums.Role.ADMIN) return;

        com.bn.benefix.manager.Manager manager = managerRepository.findByAccountId(accountId)
                .orElseThrow(() -> new EntityNotFoundException("Manager not found"));

        if (!manager.getCompany().getId().equals(companyId)) {
            throw new SecurityException("User is not authorized to access this company's resources");
        }
    }
}

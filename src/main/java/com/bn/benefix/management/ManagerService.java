package com.bn.benefix.management;

import com.bn.benefix.account.Account;
import com.bn.benefix.account.AccountRepository;
import com.bn.benefix.company.Company;
import com.bn.benefix.company.CompanyRepository;
import com.bn.benefix.management.dto.ManagerCreationRequestDTO;
import com.bn.benefix.management.dto.ManagerCreationResponseDTO;
import com.bn.benefix.management.dto.ManagerUpdateRequestDTO;
import com.bn.benefix.shared.domain.CPF;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class ManagerService {

    private final ManagerRepository managerRepository;
    private final CompanyRepository companyRepository;
    private final AccountRepository accountRepository;

    public ManagerService(ManagerRepository managerRepository, CompanyRepository companyRepository, AccountRepository accountRepository) {
        this.managerRepository = managerRepository;
        this.companyRepository = companyRepository;
        this.accountRepository = accountRepository;
    }

    public ManagerCreationResponseDTO createManager(ManagerCreationRequestDTO dto) {
        Company company = companyRepository.findById(dto.companyId())
                .orElseThrow(() -> new IllegalArgumentException("Company not found"));

        if (accountRepository.findByCpf(dto.cpf()).isPresent()) {
            throw new IllegalArgumentException("Account with this CPF already exists");
        }

        // Ideally check by email too, assuming finding by email is supported or added later.
        
        Account newAccount = new Account.Builder(
                dto.name(),
                CPF.of(dto.cpf()),
                dto.password(), // In a real app, hash this!
                dto.email(),
                com.bn.benefix.shared.enums.Role.MANAGER
        ).build();

        Account savedAccount = accountRepository.save(newAccount);

        Manager newManager = new Manager.Builder(
                dto.name(),
                CPF.of(dto.cpf()),
                company,
                savedAccount
                )
                .build();

        Manager savedManager = managerRepository.save(newManager);

        return new ManagerCreationResponseDTO(
                savedManager.getId(),
                savedManager.getName(),
                savedManager.getCompany().getId(),
                savedManager.getActive(),
                savedManager.getCreatedAt()
        );
    }

    public List<ManagerCreationResponseDTO> findAll() {
        return managerRepository.findAll().stream()
                .map(m -> new ManagerCreationResponseDTO(
                        m.getId(),
                        m.getName(),
                        m.getCompany().getId(),
                        m.getActive(),
                        m.getCreatedAt()
                ))
                .toList();
    }

    public ManagerCreationResponseDTO findById(Long id) {
        Manager manager = managerRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Manager not found"));
        return new ManagerCreationResponseDTO(
                manager.getId(),
                manager.getName(),
                manager.getCompany().getId(),
                manager.getActive(),
                manager.getCreatedAt()
        );
    }

    @Transactional
    public ManagerCreationResponseDTO update(Long id, ManagerUpdateRequestDTO dto) {
        Manager manager = managerRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Manager not found"));

        manager.update(dto.name());

        return new ManagerCreationResponseDTO(
                manager.getId(),
                manager.getName(),
                manager.getCompany().getId(),
                manager.getActive(),
                manager.getCreatedAt()
        );
    }

    @Transactional
    public void delete(Long id) {
        Manager manager = managerRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Manager not found"));
        
        manager.deactivateManager();
    }

    public Manager findByCpf(UUID id) {
        return managerRepository.findByAccountId(id)
                .orElseThrow(() -> new RuntimeException("Manager not found with ID: " + id));
    }

}

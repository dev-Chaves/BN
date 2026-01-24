package com.bn.benefix.manager;

import com.bn.benefix.account.Account;
import com.bn.benefix.account.AccountRepository;
import com.bn.benefix.company.Company;
import com.bn.benefix.company.CompanyRepository;
import com.bn.benefix.manager.dto.ManagerCreationRequestDTO;
import com.bn.benefix.manager.dto.ManagerCreationResponseDTO;
import com.bn.benefix.manager.dto.ManagerUpdateRequestDTO;
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
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    public ManagerService(ManagerRepository managerRepository, CompanyRepository companyRepository, AccountRepository accountRepository, org.springframework.security.crypto.password.PasswordEncoder passwordEncoder) {
        this.managerRepository = managerRepository;
        this.companyRepository = companyRepository;
        this.accountRepository = accountRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public ManagerCreationResponseDTO createManager(ManagerCreationRequestDTO dto) {
        Company company = companyRepository.findById(dto.companyId())
                .orElseThrow(() -> new IllegalArgumentException("Company not found"));

        if (accountRepository.findByCpf(dto.cpf()).isPresent()) {
            throw new IllegalArgumentException("Account with this CPF already exists");
        }

        if(isEmailTaken(dto.email())) {
            throw new IllegalArgumentException("Email is already in use");
        }
        
        Account newAccount = new Account.Builder(
                dto.name(),
                CPF.of(dto.cpf()),
                passwordEncoder.encode(dto.password()),
                dto.email(),
                com.bn.benefix.shared.enums.Role.MANAGER
        ).build();

        Account savedAccount = accountRepository.save(newAccount);

        Manager newManager = new Manager.Builder(
                dto.name(),
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

    public Manager findByAccountId(UUID id) {
        return managerRepository.findByAccountId(id)
                .orElseThrow(() -> new RuntimeException("Manager not found with ID: " + id));
    }

    private boolean isEmailTaken(String email) {
        return accountRepository.findByEmail(email).isPresent();
    }

}

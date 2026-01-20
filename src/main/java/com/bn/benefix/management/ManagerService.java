package com.bn.benefix.management;

import com.bn.benefix.company.Company;
import com.bn.benefix.management.dto.ManagerCreationRequestDTO;
import com.bn.benefix.management.dto.ManagerCreationResponseDTO;
import com.bn.benefix.management.dto.ManagerUpdateRequestDTO;
import com.bn.benefix.shared.domain.CPF;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

@Service
public class ManagerService {

    private final ManagerRepository managerRepository;
    private final com.bn.benefix.company.CompanyRepository companyRepository;

    public ManagerService(ManagerRepository managerRepository, com.bn.benefix.company.CompanyRepository companyRepository) {
        this.managerRepository = managerRepository;
        this.companyRepository = companyRepository;
    }

    public ManagerCreationResponseDTO createManager(ManagerCreationRequestDTO dto) {
        Company company = companyRepository.findById(dto.companyId())
                .orElseThrow(() -> new IllegalArgumentException("Company not found"));

        Manager newManager = new Manager.Builder(
                dto.name(),
                CPF.of(dto.cpf()),
                company)
                .build();

        Manager savedManager = managerRepository.save(newManager);

        return new ManagerCreationResponseDTO(
                savedManager.getId(),
                savedManager.getName(),
                savedManager.getCpf().getValue(),
                savedManager.getCompany().getId(),
                savedManager.getCreatedAt()
        );
    }

    public java.util.List<ManagerCreationResponseDTO> findAll() {
        return managerRepository.findAll().stream()
                .map(m -> new ManagerCreationResponseDTO(
                        m.getId(),
                        m.getName(),
                        m.getCpf().getValue(),
                        m.getCompany().getId(),
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
                manager.getCpf().getValue(),
                manager.getCompany().getId(),
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
                manager.getCpf().getValue(),
                manager.getCompany().getId(),
                manager.getCreatedAt()
        );
    }

    public void delete(Long id) {
        if (!managerRepository.existsById(id)) {
            throw new EntityNotFoundException("Manager not found");
        }
        managerRepository.deleteById(id);
    }

    public Manager findByCpf(String cpf) {
        return managerRepository.findByCPF(cpf)
                .orElseThrow(() -> new RuntimeException("Manager not found with CPF: " + cpf));
    }

}

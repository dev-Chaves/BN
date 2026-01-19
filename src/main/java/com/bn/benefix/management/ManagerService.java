package com.bn.benefix.management;

import com.bn.benefix.company.Company;
import com.bn.benefix.shared.domain.CPF;
import org.springframework.stereotype.Service;

@Service
public class ManagerService {

    private final ManagerRepository managerRepository;
    public ManagerService(ManagerRepository managerRepository) {
        this.managerRepository = managerRepository;
    }

    public Manager createManager(String name, String cpf, Company company) {
        Manager newManager = new Manager.Builder(
                name,
                CPF.of(cpf),
                company)
                .build();

        return managerRepository.save(newManager);
    }

    public Manager findByCpf(String cpf) {
        return managerRepository.findByCPF(cpf)
                .orElseThrow(() -> new RuntimeException("Manager not found with CPF: " + cpf));
    }

}

package org.acme.domains.partnership;

import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.tuples.Tuple2;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.NotFoundException;
import org.acme.domains.benefit.Benefit;
import org.acme.domains.benefit.BenefitRepository;
import org.acme.domains.company.Company;
import org.acme.domains.company.CompanyRepository;
import org.acme.domains.manager.Manager;
import org.acme.domains.manager.ManagerRepository;
import org.acme.domains.partnership.dto.PartnershipResponse;

@ApplicationScoped
public class PartnershipService {

    private final CompanyRepository companyRepository;
    private final ManagerRepository managerRepository;
    private final BenefitRepository benefitRepository;
    private final PartnershipRepository partnershipRepository;

    public PartnershipService(CompanyRepository companyRepository, ManagerRepository managerRepository, BenefitRepository benefitRepository, PartnershipRepository partnershipRepository) {
        this.companyRepository = companyRepository;
        this.managerRepository = managerRepository;
        this.benefitRepository = benefitRepository;
        this.partnershipRepository = partnershipRepository;
    }

    // ============================================================================
    // MÉTODO PRINCIPAL - Linear, legível, sem aninhamento!
    // ============================================================================
    
    /**
     * Solicita uma parceria entre uma empresa (via manager) e um benefício.
     * 
     * Fluxo:
     * 1. Valida que o manager existe e obtém o manager
     * 2. Extrai company do manager e busca benefit
     * 3. Valida regras de negócio (partnership não existe + não é self-provider)
     * 4. Cria e persiste a partnership
     * 5. Converte para DTO de resposta
     * 
     * @param managerEmail Email do manager que está solicitando a partnership
     * @param benefitId ID do benefício desejado
     * @return Uni<PartnershipResponse> DTO com dados da partnership criada
     */
    @WithTransaction
    public Uni<PartnershipResponse> requestPartnership(String managerEmail, Long benefitId) {
        return validateManagerExists(managerEmail)
            .flatMap(manager -> fetchCompanyAndBenefit(manager, benefitId))
            .call(this::validateBusinessRules)
            .flatMap(this::createAndPersistPartnership)
            .map(this::toPartnershipResponse);
    }
    
    // ============================================================================
    // MÉTODOS PRIVADOS - Cada um faz UMA COISA
    // ============================================================================
    
    // --- GRUPO: Validações e Buscas ---
    
    /**
     * Valida que o manager existe e o retorna.
     * 
     * Pattern: Fast-fail - se manager não existe, falha ANTES de buscar outras coisas.
     * Retorna o Manager (não Uni<Void>) porque precisamos usar manager.getCompany() depois.
     * 
     * @param email Email do manager
     * @return Uni<Manager> Manager validado
     */
    private Uni<Manager> validateManagerExists(String email) {
        return managerRepository.findByEmail(email)
            .onItem().ifNull().failWith(() -> 
                new NotFoundException("Manager not found with email: " + email)
            );
    }
    
    /**
     * Busca Benefit por ID com validação de null.
     * 
     * Pattern: Busca + validação encapsulada com mensagem clara.
     * 
     * @param benefitId ID do benefício
     * @return Uni<Benefit> Benefit validado
     */
    private Uni<Benefit> getBenefitById(Long benefitId) {
        return benefitRepository.findById(benefitId)
            .onItem().ifNull().failWith(() -> 
                new NotFoundException("Benefit not found with id: " + benefitId)
            );
    }
    
    /**
     * Obtém Company do Manager e busca Benefit.
     * 
     * Nota: Company vem do relacionamento Manager.getCompany(), não é busca no banco.
     * Benefit é buscado do banco. Como são operações diferentes (uma é getField, 
     * outra é I/O), não usamos Uni.combine() aqui.
     * 
     * @param manager Manager já validado
     * @param benefitId ID do benefício
     * @return Uni<Tuple2<Company, Benefit>> Tupla com Company e Benefit
     */
    private Uni<Tuple2<Company, Benefit>> fetchCompanyAndBenefit(Manager manager, Long benefitId) {
        Company company = manager.getCompany();
        
        // Valida que manager tem company associada
        if (company == null) {
            return Uni.createFrom().failure(
                new IllegalStateException("Manager does not have an associated company")
            );
        }
        
        // Busca benefit e combina com a company
        return getBenefitById(benefitId)
            .map(benefit -> Tuple2.of(company, benefit));
    }
    
    // --- GRUPO: Validações de Negócio (retornam Uni<Void>) ---
    
    /**
     * Valida que a partnership NÃO existe ainda entre company e benefit.
     * 
     * Pattern: Uni<Void> para validações - passa ou falha, sem valor de retorno.
     * 
     * @param companyId ID da company cliente
     * @param benefitId ID do benefício
     * @return Uni<Void> Vazio se validação passou, emite erro se falhou
     */
    private Uni<Void> validatePartnershipDoesNotExist(Long companyId, Long benefitId) {
        return partnershipRepository.findExistingPartnership(companyId, benefitId)
            .flatMap(exists -> {
                if (exists) {
                    return Uni.createFrom().failure(
                        new IllegalStateException(
                            "Partnership already exists between company " + 
                            companyId + " and benefit " + benefitId
                        )
                    );
                }
                return Uni.createFrom().voidItem();
            });
    }
    
    /**
     * Valida que a Company NÃO está tentando ser seu próprio provider.
     * 
     * Pattern: Validação síncrona (sem I/O) - apenas comparação em memória.
     * 
     * @param client Company cliente que está solicitando
     * @param benefit Benefit que contém o provider
     * @return Uni<Void> Vazio se validação passou, emite erro se falhou
     */
    private Uni<Void> validateCompanyIsNotOwnProvider(Company client, Benefit benefit) {
        // Validação síncrona - apenas comparação em memória
        if (client.id.equals(benefit.getProvider().id)) {
            return Uni.createFrom().failure(
                new IllegalArgumentException(
                    "Company " + client.getName() + 
                    " cannot request a benefit from itself"
                )
            );
        }
        return Uni.createFrom().voidItem();
    }
    
    /**
     * Valida todas as regras de negócio.
     * 
     * Pattern: Orquestrador de validações - encadeia múltiplas validações.
     * Usa .call() para manter o Tuple2 após validações.
     * 
     * @param tuple Tupla com Company e Benefit
     * @return Uni<Void> Vazio se todas validações passaram
     */
    private Uni<Void> validateBusinessRules(Tuple2<Company, Benefit> tuple) {
        Company company = tuple.getItem1();
        Benefit benefit = tuple.getItem2();
        
        return validatePartnershipDoesNotExist(company.id, benefit.id)
            .call(() -> validateCompanyIsNotOwnProvider(company, benefit));
    }
    
    // --- GRUPO: Operações de Persistência ---
    
    /**
     * Cria a Partnership entity e persiste no banco.
     * 
     * Pattern: Builder pattern + persist.
     * Valida primeiro, persiste depois (validate early, persist late).
     * 
     * @param tuple Tupla com Company e Benefit validados
     * @return Uni<Partnership> Partnership persistida com ID gerado
     */
    private Uni<Partnership> createAndPersistPartnership(Tuple2<Company, Benefit> tuple) {
        Company company = tuple.getItem1();
        Benefit benefit = tuple.getItem2();
        
        // Cria a entidade usando builder pattern
        Partnership partnership = Partnership.builder(company, benefit).build();
        
        // Persiste e retorna a entidade com ID gerado
        return partnershipRepository.persist(partnership);
    }
    
    // --- GRUPO: Conversões (transformações síncronas) ---
    
    /**
     * Converte Partnership entity para DTO de resposta.
     * 
     * Pattern: Transformação síncrona - usa map() no método principal.
     * Apenas copia campos, sem I/O.
     * 
     * @param partnership Partnership persistida
     * @return PartnershipResponse DTO com dados da partnership
     */
    private PartnershipResponse toPartnershipResponse(Partnership partnership) {
        return new PartnershipResponse(
            partnership.id,
            partnership.getClientCompany().id,
            partnership.getBenefit().id,
            partnership.getStatus(),
            partnership.getCreatedAt()
        );
    }

}

package com.bn.benefix.partnership;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("partnership")
public class PartnershipController {

    private final PartnershipService partnershipService;

    public PartnershipController(PartnershipService partnershipService) {
        this.partnershipService = partnershipService;
    }

    @org.springframework.web.bind.annotation.PostMapping()
    public org.springframework.http.ResponseEntity<com.bn.benefix.partnership.dto.PartnershipCreationResponseDTO> createPartnership(
            @jakarta.validation.Valid @org.springframework.web.bind.annotation.RequestBody com.bn.benefix.partnership.dto.PartnershipCreationRequestDTO dto,
            org.springframework.web.util.UriComponentsBuilder builder){

        com.bn.benefix.partnership.dto.PartnershipCreationResponseDTO response = partnershipService.createPartnership(dto);

        java.net.URI uri = builder.path("/partnership/{id}")
                .buildAndExpand(response.id())
                .toUri();
        return org.springframework.http.ResponseEntity.created(uri).body(response);
    }

    @org.springframework.web.bind.annotation.GetMapping
    public org.springframework.http.ResponseEntity<java.util.List<com.bn.benefix.partnership.dto.PartnershipCreationResponseDTO>> getAllPartnerships() {
        return org.springframework.http.ResponseEntity.ok(partnershipService.findAll());
    }

    @org.springframework.web.bind.annotation.GetMapping("/{id}")
    public org.springframework.http.ResponseEntity<com.bn.benefix.partnership.dto.PartnershipCreationResponseDTO> getPartnershipById(@org.springframework.web.bind.annotation.PathVariable Long id) {
        return org.springframework.http.ResponseEntity.ok(partnershipService.findById(id));
    }

    @org.springframework.web.bind.annotation.PutMapping("/{id}")
    public org.springframework.http.ResponseEntity<com.bn.benefix.partnership.dto.PartnershipCreationResponseDTO> updatePartnership(
            @org.springframework.web.bind.annotation.PathVariable Long id,
            @org.springframework.web.bind.annotation.RequestBody com.bn.benefix.partnership.dto.PartnershipUpdateRequestDTO dto) {
        return org.springframework.http.ResponseEntity.ok(partnershipService.update(id, dto));
    }

    @org.springframework.web.bind.annotation.DeleteMapping("/{id}")
    public org.springframework.http.ResponseEntity<Void> deletePartnership(@org.springframework.web.bind.annotation.PathVariable Long id) {
        partnershipService.delete(id);
        return org.springframework.http.ResponseEntity.noContent().build();
    }
}
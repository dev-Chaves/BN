package com.bn.benefix.benefit;

import com.bn.benefix.benefit.dto.BenefitCreationRequestDTO;
import com.bn.benefix.benefit.dto.BenefitCreationResponseDTO;
import com.bn.benefix.benefit.dto.BenefitUpdateRequestDTO;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("benefit")
public class BenefitController {

    private final BenefitService benefitService;

    public BenefitController(BenefitService benefitService) {
        this.benefitService = benefitService;
    }

    @PostMapping()
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<BenefitCreationResponseDTO> createBenefit(
            @Valid @RequestBody BenefitCreationRequestDTO dto,
            UriComponentsBuilder builder,
            @org.springframework.security.core.annotation.AuthenticationPrincipal com.bn.benefix.infra.security.AccountUserDetails userDetails){

        BenefitCreationResponseDTO response = benefitService.createBenefit(dto, userDetails.getAccount().getId());

        URI uri = builder.path("/benefit/{id}").buildAndExpand(response.id()).toUri();

        return ResponseEntity.created(uri).body(response);

    }

    @GetMapping
    public ResponseEntity<List<BenefitCreationResponseDTO>> getAllBenefits() {
        return ResponseEntity.ok(benefitService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<BenefitCreationResponseDTO> getBenefitById(@PathVariable Long id) {
        return ResponseEntity.ok(benefitService.findById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<BenefitCreationResponseDTO> updateBenefit(
            @PathVariable Long id,
            @RequestBody BenefitUpdateRequestDTO dto,
            @org.springframework.security.core.annotation.AuthenticationPrincipal com.bn.benefix.infra.security.AccountUserDetails userDetails) {
        return ResponseEntity.ok(benefitService.update(id, dto, userDetails.getAccount().getId()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<Void> deleteBenefit(
            @PathVariable Long id,
            @org.springframework.security.core.annotation.AuthenticationPrincipal com.bn.benefix.infra.security.AccountUserDetails userDetails) {
        benefitService.delete(id, userDetails.getAccount().getId());
        return ResponseEntity.noContent().build();
    }

}

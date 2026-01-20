package com.bn.benefix.benefit;

import com.bn.benefix.benefit.dto.BenefitCreationRequestDTO;
import com.bn.benefix.benefit.dto.BenefitCreationResponseDTO;
import com.bn.benefix.benefit.dto.BenefitUpdateRequestDTO;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<BenefitCreationResponseDTO> createBenefit(@Valid @RequestBody BenefitCreationRequestDTO dto, UriComponentsBuilder builder){

        BenefitCreationResponseDTO response = benefitService.createBenefit(dto);

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
    public ResponseEntity<BenefitCreationResponseDTO> updateBenefit(
            @PathVariable Long id,
            @RequestBody BenefitUpdateRequestDTO dto) {
        return ResponseEntity.ok(benefitService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBenefit(@PathVariable Long id) {
        benefitService.delete(id);
        return ResponseEntity.noContent().build();
    }

}

package com.bn.benefix.partnership;

import com.bn.benefix.partnership.dto.PartnershipCreationRequestDTO;
import com.bn.benefix.partnership.dto.PartnershipCreationResponseDTO;
import com.bn.benefix.partnership.dto.PartnershipUpdateRequestDTO;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("partnership")
public class PartnershipController {

    private final PartnershipService partnershipService;

    public PartnershipController(PartnershipService partnershipService) {
        this.partnershipService = partnershipService;
    }

    @PostMapping()
    public ResponseEntity<PartnershipCreationResponseDTO> createPartnership(
            @Valid @RequestBody PartnershipCreationRequestDTO dto,
            UriComponentsBuilder builder){

        PartnershipCreationResponseDTO response = partnershipService.createPartnership(dto);

        URI uri = builder.path("/partnership/{id}")
                .buildAndExpand(response.id())
                .toUri();
        return ResponseEntity.created(uri).body(response);
    }

    @GetMapping
    public ResponseEntity<List<PartnershipCreationResponseDTO>> getAllPartnerships() {
        return ResponseEntity.ok(partnershipService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PartnershipCreationResponseDTO> getPartnershipById(@PathVariable Long id) {
        return ResponseEntity.ok(partnershipService.findById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PartnershipCreationResponseDTO> updatePartnership(
            @PathVariable Long id,
            @RequestBody PartnershipUpdateRequestDTO dto) {
        return ResponseEntity.ok(partnershipService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePartnership(@PathVariable Long id) {
        partnershipService.delete(id);
        return ResponseEntity.noContent().build();
    }

}
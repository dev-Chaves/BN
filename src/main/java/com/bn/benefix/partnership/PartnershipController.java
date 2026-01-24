package com.bn.benefix.partnership;

import com.bn.benefix.partnership.dto.PartnershipCreationRequestDTO;
import com.bn.benefix.partnership.dto.PartnershipCreationResponseDTO;
import com.bn.benefix.partnership.dto.PartnershipUpdateRequestDTO;
import com.bn.benefix.infra.security.AccountUserDetails;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<PartnershipCreationResponseDTO> createPartnership(
            @Valid @RequestBody PartnershipCreationRequestDTO dto,
            UriComponentsBuilder builder,
            @AuthenticationPrincipal AccountUserDetails userDetails){

        PartnershipCreationResponseDTO response = partnershipService.createPartnership(dto, userDetails.getAccount().getId());

        URI uri = builder.path("/partnership/{id}")
                .buildAndExpand(response.id())
                .toUri();
        return ResponseEntity.created(uri).body(response);
    }

    @PatchMapping("/{id}/accept")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<Void> acceptPartnership(
            @PathVariable Long id,
            @AuthenticationPrincipal AccountUserDetails userDetails) {
        partnershipService.acceptPartnership(id, userDetails.getAccount().getId());
        return ResponseEntity.ok().build();
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
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<PartnershipCreationResponseDTO> updatePartnership(
            @PathVariable Long id,
            @RequestBody PartnershipUpdateRequestDTO dto,
            @AuthenticationPrincipal AccountUserDetails userDetails) {
        return ResponseEntity.ok(partnershipService.update(id, dto, userDetails.getAccount().getId()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<Void> deletePartnership(
            @PathVariable Long id,
            @AuthenticationPrincipal AccountUserDetails userDetails) {
        partnershipService.delete(id, userDetails.getAccount().getId());
        return ResponseEntity.noContent().build();
    }

}
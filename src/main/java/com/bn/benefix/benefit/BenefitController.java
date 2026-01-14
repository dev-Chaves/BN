package com.bn.benefix.benefit;

import com.bn.benefix.benefit.dto.BenefitCreationRequestDTO;
import com.bn.benefix.benefit.dto.BenefitCreationResponseDTO;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("benefit")
public class BenefitController {

    private final BenefitService benefitService;

    public BenefitController(BenefitService benefitService) {
        this.benefitService = benefitService;
    }

    @PostMapping()
    public ResponseEntity<BenefitCreationResponseDTO> createBenefit(@Valid @RequestBody BenefitCreationRequestDTO dto, UriComponentsBuilder builder){

        Benefit benefit = benefitService.createBenefit(dto);

        BenefitCreationResponseDTO response = new BenefitCreationResponseDTO(
                benefit.getId(), benefit.getName(), benefit.getProvider().getName(), benefit.getActive(), benefit.getCreatedAt()
        );

        URI uri = builder.path("/benefit/{id}").buildAndExpand(benefit.getId()).toUri();

        return ResponseEntity.created(uri).body(response);

    }

}

package com.bnfix.ubm.domains.benefit;

import com.bnfix.ubm.domains.benefit.dto.BenefitPublicResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("benefits/public")
public class PublicBenefitController {

    private final BenefitService benefitService;

    public PublicBenefitController(BenefitService benefitService) {
        this.benefitService = benefitService;
    }

    @GetMapping("")
    public Page<BenefitPublicResponse> getActiveBenefits(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        log.info("GET public-benefits");

        return benefitService.publicMarketplace(pageable);
    }

    @GetMapping("/search")
    public Page<BenefitPublicResponse> searchBenefitByTermo(
            @RequestParam String termo, @PageableDefault(size = 20) Pageable pageable) {
        log.info("Been search the term: {}", termo);

        return benefitService.publicSearch(pageable, termo);
    }
}

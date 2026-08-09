package com.bnfix.ubm.domains.sharedbenefit;

import com.bnfix.ubm.domains.sharedbenefit.dto.*;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/shared-benefits")
public class SharedBenefitController {
    private final SharedBenefitService s;

    public SharedBenefitController(SharedBenefitService s) {
        this.s = s;
    }

    @GetMapping("/available")
    @PreAuthorize("hasRole('USER')")
    public List<SharedBenefitResponse> available(@AuthenticationPrincipal Jwt j) {
        return s.available(j.getSubject());
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('USER')")
    public List<SharedBenefitResponse> mine(@AuthenticationPrincipal Jwt j) {
        return s.mine(j.getSubject());
    }
}

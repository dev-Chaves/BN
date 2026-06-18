package org.acme.domains.benefit.dto;

import java.util.List;

public record UpdateBenefitRequest(
        String name,
        String description,
        List<Long> categoryIds
) {
}

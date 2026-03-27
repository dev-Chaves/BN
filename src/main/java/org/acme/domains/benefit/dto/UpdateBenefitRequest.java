package org.acme.domains.benefit.dto;

public record UpdateBenefitRequest(
        String name,
        String description
) {
}

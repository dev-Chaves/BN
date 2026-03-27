package org.acme.domains.partnership.dto;

import org.acme.domains.partnership.PartnershipStatus;

public record UpdatePartnershipRequest(
        PartnershipStatus status
) {
}

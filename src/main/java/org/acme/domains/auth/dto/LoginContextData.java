package org.acme.domains.auth.dto;

import org.acme.domains.shared.enums.Role;

public record LoginContextData(
        String token,
        Role role,
        Long profileId
) {
}

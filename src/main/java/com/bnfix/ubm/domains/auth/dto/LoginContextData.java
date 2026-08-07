package com.bnfix.ubm.domains.auth.dto;

import com.bnfix.ubm.domains.shared.enums.Role;

public record LoginContextData(String token, Role role, Long profileId) {}

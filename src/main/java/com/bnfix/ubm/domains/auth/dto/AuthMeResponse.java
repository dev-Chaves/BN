package com.bnfix.ubm.domains.auth.dto;

import java.util.UUID;

/**
 * Perfil do usuário autenticado — retornado no body de login/switch-company e
 * por {@code GET /auth/me}. Não contém o token (este vive apenas no cookie
 * httpOnly).
 */
public record AuthMeResponse(
        UUID accountId, String email, String role, Long companyId, String companyName, String name) {}

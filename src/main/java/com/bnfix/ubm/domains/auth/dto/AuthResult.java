package com.bnfix.ubm.domains.auth.dto;

/**
 * Resultado interno do login/troca de empresa: o token segue apenas para o
 * cookie httpOnly (via {@link com.bnfix.ubm.domains.auth.AuthController}); o
 * perfil vai no body.
 */
public record AuthResult(String token, AuthMeResponse user) {}

package com.bnfix.ubm.shared.security;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * In-memory blocklist para revogação antecipada de JWTs via logout.
 *
 * <p>Armazena {@code jti -> exp} (segundos de epoch). Entradas expiradas são
 * removidas preguiçosamente na leitura. Sem dependências externas — adequado ao
 * deploy atual de container único (ver {@code .github/workflows/deploy.yaml}).
 *
 * <p><b>Atenção:</b> em um cluster com múltiplas réplicas este estado não é
 * compartilhado entre pods; migrar para Redis ou tabela {@code revoked_tokens}
 * quando escalar horizontalmente.
 */
@Slf4j
@Component
public class RevokedTokenStore {
    private final ConcurrentHashMap<String, Long> revoked = new ConcurrentHashMap<>();

    public void revoke(UUID jti, Instant expiresAt) {
        if (jti == null || expiresAt == null) return;
        revoked.put(jti.toString(), expiresAt.getEpochSecond());
        log.info("Token revoked jti={} exp={}", jti, expiresAt);
    }

    public boolean isRevoked(UUID jti, Instant expiresAt) {
        if (jti == null) return false;
        Long exp = revoked.get(jti.toString());
        if (exp == null) return false;
        long now = Instant.now().getEpochSecond();
        if (exp <= now) {
            // limpeza preguiçosa: token naturalmente expirou, removemos do mapa
            revoked.remove(jti.toString());
            return false;
        }
        return true;
    }

    /** Remove todas as entradas já expiradas — disponível para limpeza periódica. */
    public void purgeExpired() {
        long now = Instant.now().getEpochSecond();
        revoked.entrySet().removeIf(entry -> entry.getValue() <= now);
    }
}

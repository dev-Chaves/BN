-- Verificação pós-ensaio do experimento de concorrência de resgate.
--
-- Uso:
--   psql "$DATABASE_URL" -v token_hash=<sha256> -v recent=50 -f verify.sql
--
-- O backend persiste apenas o hash SHA-256 (hex) do token bruto. Para obter o
-- hash de um token emitido:
--   printf '%s' '<token>' | shasum -a 256 | cut -d' ' -f1

\echo '--- 1. Estado do token informado'
SELECT t.id AS token_id,
       t.status,
       t.issued_at,
       t.consumed_at,
       t.expires_at
FROM redemption_tokens t
WHERE t.token_hash = :'token_hash';

\echo '--- 2. Resgates persistidos para o token informado (esperado: 1)'
SELECT count(*) AS redemptions
FROM benefit_redemptions r
JOIN redemption_tokens t ON t.id = r.token_id
WHERE t.token_hash = :'token_hash';

\echo '--- 3. Duplicações globais: tokens com mais de um resgate (esperado: 0 linhas)'
SELECT r.token_id, count(*) AS redemptions
FROM benefit_redemptions r
GROUP BY r.token_id
HAVING count(*) <> 1;

\echo '--- 4. Tokens emitidos na última hora e quantos resgates cada um tem'
SELECT t.id AS token_id,
       t.status,
       count(r.id) AS redemptions
FROM redemption_tokens t
LEFT JOIN benefit_redemptions r ON r.token_id = t.id
WHERE t.issued_at > now() - interval '1 hour'
GROUP BY t.id, t.status
ORDER BY t.issued_at DESC
LIMIT :recent;

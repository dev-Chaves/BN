#!/usr/bin/env bash
# Seed do ambiente de teste via API pública.
# Uso: BASE_URL=http://127.0.0.1:8081 ./seed.sh
# Requisitos: curl, jq. Gera seed-result.json para consumo pelo k6.
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"
SUFFIX="$(date +%s)"
PASSWORD="LoadTest#${SUFFIX}"

HTTP_BODY="$(mktemp)"
HTTP_HEADERS="$(mktemp)"
trap 'rm -f "$HTTP_BODY" "$HTTP_HEADERS"' EXIT

rand_digits() { LC_ALL=C tr -dc '0-9' < /dev/urandom | head -c "$1"; }

# CPF válido (dígitos verificadores calculados; rejeita sequências repetidas por probabilidade desprezível)
gen_cpf() {
  local base sum r i d
  base="$(rand_digits 9)"
  sum=0
  for i in 0 1 2 3 4 5 6 7 8; do
    d=${base:$i:1}; sum=$((sum + d * (10 - i)))
  done
  r=$((11 - sum % 11)); [ "$r" -ge 10 ] && r=0
  local d10=$r
  sum=0
  for i in 0 1 2 3 4 5 6 7 8 9; do
    if [ "$i" -lt 9 ]; then d=${base:$i:1}; else d=$d10; fi
    sum=$((sum + d * (11 - i)))
  done
  r=$((11 - sum % 11)); [ "$r" -ge 10 ] && r=0
  echo "${base}${d10}${r}"
}

# CNPJ válido (raiz aleatória + filial 0001)
gen_cnpj() {
  local base sum r i d
  base="$(rand_digits 8)0001"
  sum=0
  local wa=(5 4 3 2 9 8 7 6 5 4 3 2)
  for i in 0 1 2 3 4 5 6 7 8 9 10 11; do
    d=${base:$i:1}; sum=$((sum + d * wa[i]))
  done
  r=$((sum % 11)); [ "$r" -lt 2 ] && r=11
  local d13=$((11 - r))
  sum=0
  local wb=(6 5 4 3 2 9 8 7 6 5 4 3 2)
  for i in 0 1 2 3 4 5 6 7 8 9 10 11 12; do
    if [ "$i" -lt 12 ]; then d=${base:$i:1}; else d=$d13; fi
    sum=$((sum + d * wb[i]))
  done
  r=$((sum % 11)); [ "$r" -lt 2 ] && r=11
  echo "${base}${d13}$((11 - r))"
}

http() { # method url [json-body] [jwt] -> imprime status HTTP; corpo fica em $HTTP_BODY
  local method=$1 url=$2 payload=${3:-} jwt=${4:-}
  local args=(-sS -X "$method" "$url" -H 'Content-Type: application/json' -o "$HTTP_BODY" -D "$HTTP_HEADERS" -w '%{http_code}')
  [ -n "$payload" ] && args+=(-d "$payload")
  [ -n "$jwt" ] && args+=(-H "Cookie: jwt=$jwt")
  curl "${args[@]}"
}

assert_status() {
  local expected=$1 actual=$2 what=$3
  if [ "$actual" != "$expected" ]; then
    echo "ERRO: $what (esperado $expected, obtido $actual)" >&2
    cat "$HTTP_BODY" >&2; echo >&2
    exit 1
  fi
}

jwt_from_last_response() {
  grep -i '^Set-Cookie: *jwt=' "$HTTP_HEADERS" | head -1 | sed -E 's/.*jwt=([^;]+).*/\1/'
}

PROVIDER_MANAGER_EMAIL="load-manager-provider-${SUFFIX}@test.local"
CLIENT_MANAGER_EMAIL="load-manager-client-${SUFFIX}@test.local"
EMPLOYEE_EMAIL="load-employee-${SUFFIX}@test.local"

echo "==> Onboarding fornecedor"
PROVIDER_STATUS=$(http POST "${BASE_URL}/onboarding" "{
  \"company\": {\"name\": \"Load Provider ${SUFFIX}\", \"cnpj\": \"$(gen_cnpj)\"},
  \"manager\": {\"name\": \"Load Provider Manager\", \"cpf\": \"$(gen_cpf)\", \"email\": \"${PROVIDER_MANAGER_EMAIL}\", \"password\": \"${PASSWORD}\"}
}")
assert_status 201 "$PROVIDER_STATUS" "onboarding fornecedor"
PROVIDER_COMPANY_ID=$(jq -r .companyId "$HTTP_BODY")

sleep 1 # onboarding é limitado a 2 req/s

echo "==> Onboarding cliente"
CLIENT_STATUS=$(http POST "${BASE_URL}/onboarding" "{
  \"company\": {\"name\": \"Load Client ${SUFFIX}\", \"cnpj\": \"$(gen_cnpj)\"},
  \"manager\": {\"name\": \"Load Client Manager\", \"cpf\": \"$(gen_cpf)\", \"email\": \"${CLIENT_MANAGER_EMAIL}\", \"password\": \"${PASSWORD}\"}
}")
assert_status 201 "$CLIENT_STATUS" "onboarding cliente"
CLIENT_COMPANY_ID=$(jq -r .companyId "$HTTP_BODY")

echo "==> Login fornecedor"
# Nota: construir o payload em variável antes de chamar http() — bash 3.2 (macOS)
# quebra o quoting de JSON inline dentro de assert X "$(http ... payload ...)".
LOGIN_PAYLOAD="{\"email\": \"${PROVIDER_MANAGER_EMAIL}\", \"password\": \"${PASSWORD}\"}"
LOGIN_STATUS=$(http POST "${BASE_URL}/auth/login" "$LOGIN_PAYLOAD")
assert_status 200 "$LOGIN_STATUS" "login fornecedor"
PROVIDER_JWT=$(jwt_from_last_response)

echo "==> Criação do benefício (maxUsesPerUser=1000)"
BENEFIT_PAYLOAD="{
  \"name\": \"Load Test Benefit ${SUFFIX}\",
  \"description\": \"Benefit created for redemption concurrency experiment\",
  \"companyId\": ${PROVIDER_COMPANY_ID},
  \"categoryIds\": null,
  \"publiclyVisible\": true,
  \"validFrom\": null,
  \"validUntil\": null,
  \"maxUsesPerUser\": 1000,
  \"terms\": \"Load test terms\",
  \"availableToProviderEmployees\": false
}"
BENEFIT_STATUS=$(http POST "${BASE_URL}/benefits" "$BENEFIT_PAYLOAD" "$PROVIDER_JWT")
assert_status 201 "$BENEFIT_STATUS" "criação do benefício"
BENEFIT_ID=$(jq -r .id "$HTTP_BODY")

echo "==> Ativação do benefício"
BENEFIT_ACTIVATE_STATUS=$(http PUT "${BASE_URL}/benefits/${BENEFIT_ID}/activate" "" "$PROVIDER_JWT")
assert_status 200 "$BENEFIT_ACTIVATE_STATUS" "ativação do benefício"

echo "==> Login cliente"
CLIENT_LOGIN_PAYLOAD="{\"email\": \"${CLIENT_MANAGER_EMAIL}\", \"password\": \"${PASSWORD}\"}"
CLIENT_LOGIN_STATUS=$(http POST "${BASE_URL}/auth/login" "$CLIENT_LOGIN_PAYLOAD")
assert_status 200 "$CLIENT_LOGIN_STATUS" "login cliente"
CLIENT_JWT=$(jwt_from_last_response)

echo "==> Criação do funcionário"
EMPLOYEE_PAYLOAD="{
  \"name\": \"Load Employee ${SUFFIX}\",
  \"cpf\": \"$(gen_cpf)\",
  \"email\": \"${EMPLOYEE_EMAIL}\",
  \"password\": \"${PASSWORD}\",
  \"companyId\": ${CLIENT_COMPANY_ID}
}"
EMPLOYEE_STATUS=$(http POST "${BASE_URL}/employees" "$EMPLOYEE_PAYLOAD" "$CLIENT_JWT")
assert_status 201 "$EMPLOYEE_STATUS" "criação do funcionário"
EMPLOYEE_ID=$(jq -r .id "$HTTP_BODY")

echo "==> Ativação do funcionário"
ACTIVATE_STATUS=$(http PUT "${BASE_URL}/employees/activate?employeeId=${EMPLOYEE_ID}" "" "$CLIENT_JWT")
assert_status 200 "$ACTIVATE_STATUS" "ativação do funcionário"

echo "==> Login funcionário"
EMPLOYEE_LOGIN_PAYLOAD="{\"email\": \"${EMPLOYEE_EMAIL}\", \"password\": \"${PASSWORD}\"}"
EMPLOYEE_LOGIN_STATUS=$(http POST "${BASE_URL}/auth/login" "$EMPLOYEE_LOGIN_PAYLOAD")
assert_status 200 "$EMPLOYEE_LOGIN_STATUS" "login funcionário"
EMPLOYEE_JWT=$(jwt_from_last_response)

echo "==> Solicitação de parceria (cliente)"
PARTNERSHIP_PAYLOAD="{\"benefitId\": ${BENEFIT_ID}}"
PARTNERSHIP_STATUS=$(http POST "${BASE_URL}/partnerships" "$PARTNERSHIP_PAYLOAD" "$CLIENT_JWT")
assert_status 200 "$PARTNERSHIP_STATUS" "solicitação de parceria"
PARTNERSHIP_ID=$(jq -r .id "$HTTP_BODY")

echo "==> Aceite da parceria (fornecedor)"
ACCEPT_STATUS=$(http PUT "${BASE_URL}/partnerships/accept?partnershipId=${PARTNERSHIP_ID}" "" "$PROVIDER_JWT")
assert_status 200 "$ACCEPT_STATUS" "aceite da parceria"

echo "==> Smoke test: emissão, preview e consumo de um token"
ISSUE_STATUS=$(http POST "${BASE_URL}/redemptions/benefits/${BENEFIT_ID}/token" "" "$EMPLOYEE_JWT")
assert_status 201 "$ISSUE_STATUS" "emissão do token (smoke)"
SMOKE_TOKEN=$(jq -r .token "$HTTP_BODY")
SMOKE_PAYLOAD="{\"token\": \"${SMOKE_TOKEN}\"}"
PREVIEW_STATUS=$(http POST "${BASE_URL}/redemptions/provider/preview" "$SMOKE_PAYLOAD" "$PROVIDER_JWT")
if [ "$PREVIEW_STATUS" != "200" ]; then
  # Preview é leitura; falha aqui não impede o experimento (o k6 usa apenas consume).
  # Suspeita conhecida: readOnly + FOR UPDATE rejeitado pelo PostgreSQL em produção.
  echo "AVISO: preview (smoke) retornou ${PREVIEW_STATUS} — seguindo para o consumo" >&2
  cat "$HTTP_BODY" >&2; echo >&2
fi
CONSUME_STATUS=$(http POST "${BASE_URL}/redemptions/provider/consume" "$SMOKE_PAYLOAD" "$PROVIDER_JWT")
assert_status 200 "$CONSUME_STATUS" "consumo (smoke)"

cat > seed-result.json <<EOF
{
  "baseUrl": "${BASE_URL}",
  "provider": {"companyId": ${PROVIDER_COMPANY_ID}, "managerEmail": "${PROVIDER_MANAGER_EMAIL}", "password": "${PASSWORD}"},
  "client": {"companyId": ${CLIENT_COMPANY_ID}, "managerEmail": "${CLIENT_MANAGER_EMAIL}", "password": "${PASSWORD}"},
  "employee": {"email": "${EMPLOYEE_EMAIL}", "password": "${PASSWORD}"},
  "benefitId": ${BENEFIT_ID},
  "partnershipId": ${PARTNERSHIP_ID},
  "seededAt": "$(date -u +%Y-%m-%dT%H:%M:%SZ)"
}
EOF

echo ""
echo "Seed concluído. Artefato: $(pwd)/seed-result.json"
echo "  benefitId:        ${BENEFIT_ID}"
echo "  provider manager: ${PROVIDER_MANAGER_EMAIL}"
echo "  employee:         ${EMPLOYEE_EMAIL}"

#!/usr/bin/env python3
"""
Script de validação da Collection Postman da BN API.

Níveis de validação:
1. Estrutural (offline): verifica se todos os endpoints esperados estão presentes
   na collection com os métodos HTTP corretos.
2. Funcional (online): faz requests reais contra api.bnfix.com.br para confirmar
   que os endpoints existem, respondem corretamente e estão protegidos quando necessário.

Uso:
    python3 postman/scripts/validate_urls.py
"""

import json
import sys
import urllib.request
import urllib.error
import ssl
from pathlib import Path

# Configurações
BASE_URL = "https://api.bnfix.com.br"
COLLECTION_PATH = Path(__file__).parent.parent / "BN_API.postman_collection.json"

# Lista canônica de endpoints esperados (extraídos do código-fonte)
EXPECTED_ENDPOINTS = [
    ("POST", "/onboarding"),
    ("POST", "/auth/login"),
    ("GET", "/companies/me"),
    ("GET", "/managers/me"),
    ("POST", "/managers"),
    ("POST", "/employees"),
    ("GET", "/employees"),
    ("PUT", "/employees/activate"),
    ("PUT", "/employees/disable"),
    ("PUT", "/employees/{employeeId}"),
    ("POST", "/benefits"),
    ("GET", "/benefits/tenant"),
    ("GET", "/benefits/marketplace"),
    ("PUT", "/benefits/{benefitId}"),
    ("PUT", "/benefits/{benefitId}/activate"),
    ("PUT", "/benefits/{benefitId}/deactivate"),
    ("DELETE", "/benefits/{benefitId}"),
    ("POST", "/partnerships"),
    ("PUT", "/partnerships/accept"),
    ("PUT", "/partnerships/reject"),
    ("PUT", "/partnerships/disable"),
    ("POST", "/subscriptions"),
    ("GET", "/q/health"),
    ("GET", "/q/openapi"),
]


def load_collection():
    """Carrega e retorna o JSON da collection."""
    if not COLLECTION_PATH.exists():
        print(f"❌ Collection não encontrada: {COLLECTION_PATH}")
        sys.exit(1)
    with open(COLLECTION_PATH, "r", encoding="utf-8") as f:
        return json.load(f)


def extract_endpoints(collection):
    """Extrai todos os (method, path) da collection, normalizando paths com variáveis."""
    found = []

    def traverse(items):
        for item in items:
            if "item" in item:
                traverse(item["item"])
            elif "request" in item:
                req = item["request"]
                method = req.get("method", "GET").upper()
                url = req.get("url", {})
                if isinstance(url, str):
                    path = url
                else:
                    path_parts = url.get("path", [])
                    path = "/" + "/".join(path_parts)
                # Normalizar variáveis Postman {{var}} para {var}
                path = path.replace("{{", "{").replace("}}", "}")
                found.append((method, path))

    traverse(collection.get("item", []))
    return found


def validate_structure(collection):
    """Valida se todos os endpoints esperados estão presentes na collection."""
    found = extract_endpoints(collection)
    found_set = set(found)
    missing = []
    matched = []

    for method, path in EXPECTED_ENDPOINTS:
        # Tentar match exato
        if (method, path) in found_set:
            matched.append((method, path))
        else:
            # Tentar match parcial (ex: query params podem variar)
            partial = any(
                f[0] == method and f[1].startswith(path) for f in found
            )
            if partial:
                matched.append((method, path))
            else:
                missing.append((method, path))

    print("\n📋 === VALIDAÇÃO ESTRUTURAL ===")
    print(f"   Endpoints esperados: {len(EXPECTED_ENDPOINTS)}")
    print(f"   Endpoints encontrados: {len(found)}")
    print(f"   ✅ Match: {len(matched)}")
    print(f"   ❌ Faltando: {len(missing)}")

    if missing:
        print("\n   Endpoints ausentes na collection:")
        for method, path in missing:
            print(f"      - {method} {path}")
        return False
    else:
        print("\n   ✅ Todos os endpoints esperados estão presentes!")
        return True


def http_request(method, path, body=None, headers=None, cookie=None):
    """Faz um request HTTP e retorna (status_code, response_body)."""
    url = f"{BASE_URL}{path}"
    req_headers = headers or {}
    if cookie:
        req_headers["Cookie"] = f"jwt={cookie}"
    if body and "Content-Type" not in req_headers:
        req_headers["Content-Type"] = "application/json"

    data = body.encode("utf-8") if body else None
    req = urllib.request.Request(url, data=data, method=method, headers=req_headers)

    # Ignorar verificação SSL para ambientes com cert auto-assinado (remover em prod se necessário)
    ctx = ssl.create_default_context()
    ctx.check_hostname = False
    ctx.verify_mode = ssl.CERT_NONE

    try:
        with urllib.request.urlopen(req, timeout=15, context=ctx) as resp:
            return resp.status, resp.read().decode("utf-8", errors="ignore")
    except urllib.error.HTTPError as e:
        return e.code, e.read().decode("utf-8", errors="ignore")
    except urllib.error.URLError as e:
        return None, str(e.reason)
    except Exception as e:
        return None, str(e)


def validate_functional():
    """Valida endpoints contra a API real."""
    print("\n🌐 === VALIDAÇÃO FUNCIONAL ===")
    print(f"   Base URL: {BASE_URL}\n")

    results = []

    # 1. Health check (em produção pode estar desabilitado ou em path diferente)
    status, body = http_request("GET", "/q/health")
    ok = status in (200, 404)
    results.append(("GET /q/health", status, ok, "Health check (prod: pode ser 404)"))
    print(f"   {'✅' if ok else '❌'} GET /q/health → {status} (aceito 200 ou 404)")

    # 2. OpenAPI (em produção exige Basic Auth, conforme README — 401 é esperado)
    status, body = http_request("GET", "/q/openapi")
    ok = status in (200, 401)
    results.append(("GET /q/openapi", status, ok, "OpenAPI (prod: protegido com Basic Auth)"))
    print(f"   {'✅' if ok else '❌'} GET /q/openapi → {status} (aceito 200 ou 401)")

    # 3. Endpoint público sem auth: onboarding (deve retornar 400 por body vazio, não 404)
    status, body = http_request("POST", "/onboarding", body="{}")
    ok = status in (200, 201, 400, 422)
    results.append(("POST /onboarding", status, ok, "Onboarding (sem auth)"))
    print(f"   {'✅' if ok else '❌'} POST /onboarding → {status} (esperado 400/422, não 404)")

    # 4. Endpoint público sem auth: login (deve retornar 400 por body vazio, não 404)
    status, body = http_request("POST", "/auth/login", body="{}")
    ok = status in (200, 201, 400, 422)
    results.append(("POST /auth/login", status, ok, "Login (sem auth)"))
    print(f"   {'✅' if ok else '❌'} POST /auth/login → {status} (esperado 400/422, não 404)")

    # 5. Endpoint autenticado sem cookie: companies/me (deve retornar 401)
    status, body = http_request("GET", "/companies/me")
    ok = status == 401
    results.append(("GET /companies/me (sem auth)", status, ok, "Proteção JWT"))
    print(f"   {'✅' if ok else '❌'} GET /companies/me (sem auth) → {status} (esperado 401)")

    # 6. Endpoint autenticado sem cookie: managers/me (deve retornar 401)
    status, body = http_request("GET", "/managers/me")
    ok = status == 401
    results.append(("GET /managers/me (sem auth)", status, ok, "Proteção JWT"))
    print(f"   {'✅' if ok else '❌'} GET /managers/me (sem auth) → {status} (esperado 401)")

    # 7. Endpoint autenticado sem cookie: employees (deve retornar 401)
    status, body = http_request("GET", "/employees")
    ok = status == 401
    results.append(("GET /employees (sem auth)", status, ok, "Proteção JWT"))
    print(f"   {'✅' if ok else '❌'} GET /employees (sem auth) → {status} (esperado 401)")

    # 8. Endpoint autenticado sem cookie: benefits (deve retornar 401)
    status, body = http_request("GET", "/benefits/tenant")
    ok = status == 401
    results.append(("GET /benefits/tenant (sem auth)", status, ok, "Proteção JWT"))
    print(f"   {'✅' if ok else '❌'} GET /benefits/tenant (sem auth) → {status} (esperado 401)")

    # 9. Endpoint autenticado sem cookie: subscriptions (deve retornar 401)
    status, body = http_request("POST", "/subscriptions", body="{}")
    ok = status == 401
    results.append(("POST /subscriptions (sem auth)", status, ok, "Proteção JWT"))
    print(f"   {'✅' if ok else '❌'} POST /subscriptions (sem auth) → {status} (esperado 401)")

    # 10. Endpoint autenticado sem cookie: partnerships (deve retornar 401)
    status, body = http_request("POST", "/partnerships", body="{}")
    ok = status == 401
    results.append(("POST /partnerships (sem auth)", status, ok, "Proteção JWT"))
    print(f"   {'✅' if ok else '❌'} POST /partnerships (sem auth) → {status} (esperado 401)")

    # 11. Endpoint com path param: update employee (deve retornar 401 sem auth, não 404)
    status, body = http_request("PUT", "/employees/1", body="{}")
    ok = status == 401
    results.append(("PUT /employees/1 (sem auth)", status, ok, "Proteção JWT + path param"))
    print(f"   {'✅' if ok else '❌'} PUT /employees/1 (sem auth) → {status} (esperado 401)")

    # 12. Endpoint com path param: benefits/{id} (deve retornar 401 sem auth)
    status, body = http_request("PUT", "/benefits/1", body="{}")
    ok = status == 401
    results.append(("PUT /benefits/1 (sem auth)", status, ok, "Proteção JWT + path param"))
    print(f"   {'✅' if ok else '❌'} PUT /benefits/1 (sem auth) → {status} (esperado 401)")

    # 13. Endpoint DELETE com path param (deve retornar 401 sem auth)
    status, body = http_request("DELETE", "/benefits/1")
    ok = status == 401
    results.append(("DELETE /benefits/1 (sem auth)", status, ok, "Proteção JWT + path param"))
    print(f"   {'✅' if ok else '❌'} DELETE /benefits/1 (sem auth) → {status} (esperado 401)")

    # 14. Endpoint com query param: employees/activate (deve retornar 401 sem auth)
    status, body = http_request("PUT", "/employees/activate?employeeId=1")
    ok = status == 401
    results.append(("PUT /employees/activate?employeeId=1 (sem auth)", status, ok, "Proteção JWT + query param"))
    print(f"   {'✅' if ok else '❌'} PUT /employees/activate?employeeId=1 (sem auth) → {status} (esperado 401)")

    # 15. Endpoint com query param: partnerships/accept (deve retornar 401 sem auth)
    status, body = http_request("PUT", "/partnerships/accept?partnershipId=1")
    ok = status == 401
    results.append(("PUT /partnerships/accept?partnershipId=1 (sem auth)", status, ok, "Proteção JWT + query param"))
    print(f"   {'✅' if ok else '❌'} PUT /partnerships/accept?partnershipId=1 (sem auth) → {status} (esperado 401)")

    passed = sum(1 for _, _, ok, _ in results if ok)
    total = len(results)
    print(f"\n   📊 Resultado funcional: {passed}/{total} passaram")
    return passed == total


def main():
    print("=" * 60)
    print("🔍 VALIDADOR DE COLLECTION POSTMAN - BN API")
    print("=" * 60)

    collection = load_collection()
    structure_ok = validate_structure(collection)
    functional_ok = validate_functional()

    print("\n" + "=" * 60)
    if structure_ok and functional_ok:
        print("✅ VALIDAÇÃO COMPLETA: Tudo certo!")
        sys.exit(0)
    else:
        print("❌ VALIDAÇÃO INCOMPLETA: Verifique os erros acima.")
        sys.exit(1)


if __name__ == "__main__":
    main()

# Deploy AWS Free Tier + Supabase (BN API)

Este guia sobe a API na AWS com custo mínimo (free-tier) e banco no Supabase Postgres (512MB).

## 1) Arquitetura alvo (low-cost)

- API: `EC2 t3.micro` (ou `t2.micro`) com Docker
- Banco: Supabase Postgres (externo), com SSL obrigatório
- HTTPS: Nginx + Let's Encrypt
- Logs: Docker + `journalctl`

Fluxo:

`Client -> Nginx (443) -> container bn-api (localhost:8080) -> Supabase Postgres`

## 2) Pré-requisitos

- Conta AWS com free-tier ativo
- Conta Supabase com projeto criado
- Domínio (recomendado para HTTPS)
- AWS CLI configurado (`aws configure`)
- Chave SSH local (`.pem`)

## 3) Criar EC2 (free-tier)

### Security Group

Liberar:

- `22` (SSH) apenas para seu IP
- `80` (HTTP) público
- `443` (HTTPS) público

### Instância

- AMI: Ubuntu 22.04 LTS
- Tipo: `t3.micro` (ou `t2.micro`)
- Storage: 20GB gp3
- Vincular o Security Group acima

## 4) Preparar servidor

```bash
ssh -i /caminho/sua-chave.pem ubuntu@SEU_IP

sudo apt update && sudo apt install -y docker.io nginx certbot python3-certbot-nginx git
sudo usermod -aG docker $USER
newgrp docker
```

## 5) Subir código e buildar imagem (manual)

```bash
git clone <URL_DO_REPO> bn
cd bn
./mvnw -DskipTests package
docker build -f src/main/docker/Dockerfile.jvm -t bn-api:1.0.0 .
```

## 6) CI/CD + GraalVM (recomendado)

Para não sobrecarregar a EC2 micro, faça o build no CI (GitHub Actions) e só faça deploy da imagem pronta.

### Estratégia

- CI faz build/test
- Build nativo com GraalVM (`-Dnative`)
- Publica imagem no GHCR (ou ECR)
- Deploy via SSH na EC2

### Exemplo de comandos no pipeline

```bash
# Build nativo (GraalVM)
./mvnw -Dnative -DskipTests package

# Build de imagem nativa
docker build -f src/main/docker/Dockerfile.native -t ghcr.io/SEU_USER/bn-api:${GITHUB_SHA} .
docker push ghcr.io/SEU_USER/bn-api:${GITHUB_SHA}
```

### Deploy no servidor (passo do CI)

```bash
ssh ubuntu@SEU_IP "docker pull ghcr.io/SEU_USER/bn-api:${GITHUB_SHA} && \
docker stop bn-api || true && docker rm bn-api || true && \
docker run -d --name bn-api --restart unless-stopped \
  --env-file /opt/bn/.env -p 127.0.0.1:8080:8080 \
  ghcr.io/SEU_USER/bn-api:${GITHUB_SHA}"
```

> Se preferir simplificar no início, mantenha imagem JVM no deploy e evolua para native no CI depois.

## 7) Configurar segredos e conexão Supabase

Crie diretórios:

```bash
sudo mkdir -p /opt/bn/secrets
sudo chown -R $USER:$USER /opt/bn
```

### JWT keys (produção)

Crie arquivos:

- `/opt/bn/secrets/privateKey.pem`
- `/opt/bn/secrets/publicKey.pem`

> Não use as chaves de dev do repositório em produção.

### Arquivo `/opt/bn/.env`

```env
QUARKUS_PROFILE=prod
QUARKUS_HTTP_HOST=0.0.0.0
QUARKUS_HTTP_PORT=8080

QUARKUS_DATASOURCE_DB_KIND=postgresql
QUARKUS_DATASOURCE_USERNAME=SEU_USER_SUPABASE
QUARKUS_DATASOURCE_PASSWORD=SUA_SENHA_SUPABASE
QUARKUS_DATASOURCE_JDBC_URL=jdbc:postgresql://SEU_HOST_SUPABASE:5432/postgres?sslmode=require
QUARKUS_DATASOURCE_REACTIVE_URL=postgresql://SEU_HOST_SUPABASE:5432/postgres?sslmode=require

# Limites conservadores para Supabase 512MB
QUARKUS_DATASOURCE_REACTIVE_MAX_SIZE=5
QUARKUS_DATASOURCE_JDBC_MAX_SIZE=3

QUARKUS_FLYWAY_MIGRATE_AT_START=true
QUARKUS_HIBERNATE_ORM_DATABASE_GENERATION=validate
QUARKUS_HIBERNATE_ORM_LOG_SQL=false
QUARKUS_SWAGGER_UI_ALWAYS_INCLUDE=false

MP_JWT_VERIFY_PUBLICKEY_LOCATION=file:/opt/bn/secrets/publicKey.pem
MP_JWT_VERIFY_ISSUER=bn-api
SMALLRYE_JWT_SIGN_KEY_LOCATION=file:/opt/bn/secrets/privateKey.pem
```

## 8) Rodar container

```bash
docker run -d \
  --name bn-api \
  --restart unless-stopped \
  --env-file /opt/bn/.env \
  -p 127.0.0.1:8080:8080 \
  bn-api:1.0.0
```

Verificar:

```bash
docker ps
docker logs --tail 200 bn-api
curl -i http://127.0.0.1:8080/q/health
```

## 9) Configurar Nginx (reverse proxy)

Arquivo `/etc/nginx/sites-available/bn-api`:

```nginx
server {
    listen 80;
    server_name api.seudominio.com;

    location / {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

Aplicar:

```bash
sudo ln -s /etc/nginx/sites-available/bn-api /etc/nginx/sites-enabled/bn-api
sudo nginx -t
sudo systemctl reload nginx
```

## 10) HTTPS com Let's Encrypt

```bash
sudo certbot --nginx -d api.seudominio.com
sudo certbot renew --dry-run
```

## 11) Smoke test pós-deploy

```bash
curl -i https://api.seudominio.com/q/health

curl -i -X POST https://api.seudominio.com/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"manager.dev@bn.local","password":"manager-pass-123"}'
```

## 12) Operação diária

```bash
docker logs -f bn-api
docker restart bn-api
docker image ls
```

## 13) Rollback rápido

```bash
docker stop bn-api && docker rm bn-api
docker run -d \
  --name bn-api \
  --restart unless-stopped \
  --env-file /opt/bn/.env \
  -p 127.0.0.1:8080:8080 \
  bn-api:IMAGEM_ANTERIOR
```

## 14) Troubleshooting comum

- `FATAL: too many connections`: reduza `QUARKUS_DATASOURCE_REACTIVE_MAX_SIZE` para `3`.
- Erro SSL no banco: confirme `sslmode=require` nas URLs JDBC e reactive.
- `Flyway` falhando no boot: veja logs do container e ajuste migration pendente.
- 502 no Nginx: validar se `bn-api` está ativo e ouvindo `127.0.0.1:8080`.

## 15) Upgrade futuro (quando sair do free-tier)

- Migrar de EC2 único para ECS/Fargate + ALB
- Mover segredos para AWS Secrets Manager
- Adicionar CloudWatch alarms (5xx, CPU, memória, restart)

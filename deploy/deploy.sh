#!/usr/bin/env bash
# Atualiza os repositórios do backend e do frontend, reconstrói as imagens e
# sobe o sistema com o override de produção. Pensado para SSH manual hoje e
# para um GitHub Action amanhã: falha (código != 0) em qualquer pré-checagem,
# serviço não saudável ou verificação final pelo nginx.
#
#   deploy/deploy.sh
#   BRANCH=develop deploy/deploy.sh
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BACKEND_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"

BRANCH="${BRANCH:-main}"
FRONTEND_DIR="${FRONTEND_DIR:-$BACKEND_DIR/../api-externa-frontend-v2}"
HEALTH_TIMEOUT="${HEALTH_TIMEOUT:-180}"
HEALTH_INTERVAL="${HEALTH_INTERVAL:-5}"
FRONTEND_PORT="${FRONTEND_PORT:-8081}"

MIN_COMPOSE_MAJOR=2
MIN_COMPOSE_MINOR=24

erro() {
  echo "ERRO: $1" >&2
  exit 1
}

cd "$BACKEND_DIR"

# --- pré-checagem: .env ---
if [ ! -f "$BACKEND_DIR/.env" ]; then
  erro ".env não encontrado em $BACKEND_DIR — copie .env.example e preencha antes do deploy"
fi

# --- pré-checagem: versão do Docker Compose (aceita 2.24+ e qualquer major maior) ---
compose_version_raw="$(docker compose version)"
compose_version="$(echo "$compose_version_raw" | grep -Eo '[0-9]+\.[0-9]+(\.[0-9]+)?' | head -n1)"
compose_major="${compose_version%%.*}"
compose_rest="${compose_version#*.}"
compose_minor="${compose_rest%%.*}"

if [ -z "$compose_major" ] || [ -z "$compose_minor" ]; then
  erro "não foi possível determinar a versão do Docker Compose (saída: $compose_version_raw)"
fi

if [ "$compose_major" -lt "$MIN_COMPOSE_MAJOR" ] || { [ "$compose_major" -eq "$MIN_COMPOSE_MAJOR" ] && [ "$compose_minor" -lt "$MIN_COMPOSE_MINOR" ]; }; then
  erro "Docker Compose $MIN_COMPOSE_MAJOR.$MIN_COMPOSE_MINOR ou mais novo é exigido (encontrado: $compose_version_raw)"
fi

# --- pré-checagem: árvores de trabalho limpas ---
checar_limpo() {
  dir="$1"
  nome="$2"
  if [ -n "$(git -C "$dir" status --porcelain)" ]; then
    erro "repositório $nome ($dir) tem alterações não commitadas — commit ou descarte antes do deploy"
  fi
}
checar_limpo "$BACKEND_DIR" "backend"
checar_limpo "$FRONTEND_DIR" "frontend"

# --- atualiza os dois repositórios para origin/$BRANCH ---
atualizar_repo() {
  dir="$1"
  git -C "$dir" fetch origin "$BRANCH"
  git -C "$dir" checkout "$BRANCH"
  git -C "$dir" reset --hard "origin/$BRANCH"
}
atualizar_repo "$BACKEND_DIR"
atualizar_repo "$FRONTEND_DIR"

# --- sobe os serviços com o override de produção ---
# Variável do shell vence o FRONTEND_PATH do .env: o build do frontend sai do
# mesmo repositório que acabou de ser atualizado.
export FRONTEND_PATH="$FRONTEND_DIR"
docker compose \
  -f "$BACKEND_DIR/docker-compose.yml" \
  -f "$BACKEND_DIR/docker-compose.prod.yml" \
  up -d --build --remove-orphans

# --- espera cada serviço ficar healthy ---
esperar_saudavel() {
  container="$1"
  elapsed=0
  status="desconhecido"
  while [ "$elapsed" -lt "$HEALTH_TIMEOUT" ]; do
    status="$(docker inspect --format='{{.State.Health.Status}}' "$container" 2>/dev/null || echo "desconhecido")"
    if [ "$status" = "healthy" ]; then
      return 0
    fi
    sleep "$HEALTH_INTERVAL"
    elapsed=$((elapsed + HEALTH_INTERVAL))
  done
  echo "ERRO: serviço $container não ficou saudável em ${HEALTH_TIMEOUT}s (status: $status)" >&2
  docker logs --tail 50 "$container" >&2 || true
  exit 1
}
esperar_saudavel investimentos-api
esperar_saudavel investimentos-web

# --- verificação final pelo nginx ---
saude_code="$(curl -s -o /dev/null -w '%{http_code}' "http://localhost:${FRONTEND_PORT}/saude")"
if [ "$saude_code" != "200" ]; then
  erro "/saude respondeu $saude_code pelo nginx (esperado 200)"
fi

# /corretoras é encaminhada pelo nginx ao backend e exige token: só o Spring
# responde 401 ali. Rota que o nginx não encaminha cai no SPA e daria 200 mesmo
# com a API fora do ar.
api_code="$(curl -s -o /dev/null -w '%{http_code}' "http://localhost:${FRONTEND_PORT}/corretoras")"
if [ "$api_code" != "401" ]; then
  erro "rota da API /corretoras respondeu $api_code pelo nginx (esperado 401 do backend sem token)"
fi

# --- imprime o commit implantado de cada repositório ---
commit_backend="$(git -C "$BACKEND_DIR" rev-parse --short HEAD)"
commit_frontend="$(git -C "$FRONTEND_DIR" rev-parse --short HEAD)"
echo "backend: commit $commit_backend implantado"
echo "frontend: commit $commit_frontend implantado"

docker image prune -f >/dev/null 2>&1 || true

exit 0

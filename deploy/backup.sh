#!/usr/bin/env bash
# Dump diário do banco via pg_dump, compactado e datado, com retenção curta.
# Se o pg_dump falhar, nenhum arquivo novo fica no diretório de backups.
#
#   deploy/backup.sh
#   BACKUP_DIR=/opt/investimentos/backups RETENCAO_DIAS=7 deploy/backup.sh
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BACKEND_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"

BACKUP_DIR="${BACKUP_DIR:-/opt/investimentos/backups}"
RETENCAO_DIAS="${RETENCAO_DIAS:-7}"

erro() {
  echo "ERRO: $1" >&2
  exit 1
}

cd "$BACKEND_DIR"

if [ ! -f "$BACKEND_DIR/.env" ]; then
  erro ".env não encontrado em $BACKEND_DIR"
fi

POSTGRES_DB="$(grep -E '^POSTGRES_DB=' "$BACKEND_DIR/.env" | tail -n1 | cut -d'=' -f2-)"
POSTGRES_USER="$(grep -E '^POSTGRES_USER=' "$BACKEND_DIR/.env" | tail -n1 | cut -d'=' -f2-)"

if [ -z "$POSTGRES_DB" ] || [ -z "$POSTGRES_USER" ]; then
  erro "POSTGRES_DB e POSTGRES_USER precisam estar definidos no .env"
fi

mkdir -p "$BACKUP_DIR"

timestamp="$(date +%Y-%m-%d_%H%M)"
arquivo_final="$BACKUP_DIR/investimentos-${timestamp}.sql.gz"
arquivo_tmp="$BACKUP_DIR/.investimentos-${timestamp}.sql.gz.tmp"

rm -f "$arquivo_tmp"

# --- dump: grava em arquivo temporário; só vira o arquivo final se tudo der certo ---
if ! docker compose \
    -f "$BACKEND_DIR/docker-compose.yml" \
    -f "$BACKEND_DIR/docker-compose.prod.yml" \
    exec -T postgres pg_dump -U "$POSTGRES_USER" "$POSTGRES_DB" | gzip > "$arquivo_tmp"; then
  rm -f "$arquivo_tmp"
  erro "pg_dump falhou — nenhum backup foi gerado"
fi

mv "$arquivo_tmp" "$arquivo_final"

# --- retenção: remove dumps com mais de RETENCAO_DIAS dias ---
find "$BACKUP_DIR" -maxdepth 1 -name 'investimentos-*.sql.gz' -mtime "+${RETENCAO_DIAS}" -delete

echo "backup criado: $arquivo_final"
exit 0

# Tasks: Deploy no homelab

> feature: deploy-homelab

## T-482 — Override de produção do compose e perfil prod [concluida]
- Refs: US-438, AC-521, AC-522, AC-523, AC-524, AC-525
- Arquivos: docker-compose.prod.yml, src/main/resources/application-prod.properties, src/test/java/com/apiexternabackend/deploy/ConfiguracaoProducaoTest.java
- Esforço: baixo
- Notas: `ports: !reset []` em `postgres` e `backend`; `SPRING_PROFILES_ACTIVE=prod`,
  `JAVA_TOOL_OPTIONS=-XX:MaxRAMPercentage=75` e `mem_limit: 768m` no backend;
  `mem_limit: 1g` e `shared_buffers=256MB` no postgres; `logging` json-file
  (`max-size: 10m`, `max-file: "3"`) nos três serviços. O teste lê os YAML com
  SnakeYAML (já vem pelo Spring Boot) tratando a tag `!reset`, e o properties
  direto do classpath — sem subir contexto nem precisar de Docker no CI.
  Validação manual complementar: `docker compose -f docker-compose.yml -f docker-compose.prod.yml config`.

## T-483 — Script de deploy [concluida]
- Refs: US-439, AC-526, AC-527, AC-528, AC-529, AC-530, AC-531, AC-532, AC-533
- Arquivos: deploy/deploy.sh, src/test/java/com/apiexternabackend/deploy/DeployScriptTest.java, src/test/java/com/apiexternabackend/deploy/ScriptSandbox.java
- Esforço: medio
- Notas: `set -euo pipefail`; resolve o próprio diretório; `BRANCH` (padrão
  `main`), `FRONTEND_DIR` (padrão `../api-externa-frontend-v2` relativo ao
  backend), `HEALTH_TIMEOUT` (padrão 180 s) e `HEALTH_INTERVAL` por variável —
  os testes encurtam o prazo. Ordem: pré-checagens (.env, versão do compose,
  árvores limpas) → fetch + reset nos dois repos → up -d --build
  --remove-orphans → espera `healthy` de `investimentos-api` e
  `investimentos-web` → smoke em `http://localhost:${FRONTEND_PORT:-8081}/saude`
  e numa rota da API pelo nginx (qualquer código diferente de 502/503/504) →
  imprime commits → `docker image prune -f`. O `ScriptSandbox` copia o script
  para um diretório temporário com repos falsos e põe `docker`, `git` e `curl`
  falsos no `PATH`, gravando as chamadas num arquivo para o teste inspecionar.
  Ambiente: o script roda no Ubuntu, mas os testes rodam também no macOS com
  bash 3.2 — nada de `mapfile`, arrays associativos, `${var,,}` ou `readarray`;
  `date` e `find` só com opções comuns a GNU e BSD. A checagem de versão do
  Compose compara major e minor numericamente: aceita 2.24+ e qualquer major
  maior (o Compose local é v5.3.1); a saída de `docker compose version` pode
  ter prefixo `v` e sufixo como `-desktop.1`. O `ScriptSandbox` é genérico
  (recebe o nome do script) porque a T-484 reusa.

## T-484 — Script de backup [concluida]
- Refs: US-440, AC-534, AC-535, AC-536
- Arquivos: deploy/backup.sh, src/test/java/com/apiexternabackend/deploy/BackupScriptTest.java
- Esforço: baixo
- Notas: `docker compose exec -T postgres pg_dump` com usuário e banco lidos do
  `.env`, gravando em arquivo temporário e renomeando para
  `investimentos-AAAA-MM-DD_HHMM.sql.gz` só no sucesso; `BACKUP_DIR` (padrão
  `/opt/investimentos/backups`) e `RETENCAO_DIAS` (padrão 7) por variável;
  retenção por `find -mtime`. Reusa o `ScriptSandbox` da T-483 (já mesclado
  quando esta tarefa roda) sem reescrevê-lo. Mesmas restrições de ambiente da
  T-483: bash 3.2 e opções de `date`/`find` comuns a GNU e BSD — no teste de
  retenção, envelheça os arquivos com `Files.setLastModifiedTime`.

## T-485 — Guia de deploy [concluida]
- Refs: US-438, US-439, US-440
- Arquivos: docs/deploy.md, README.md
- Esforço: baixo
- Notas: preparo inicial (versão do compose, docker habilitado no boot, grupo
  docker, `/opt/investimentos`, clone por HTTPS, `.env` com segredos gerados e
  `chmod 600`), primeiro deploy, `tailscale serve --bg 8081`, cron do backup,
  como acessar banco/Swagger sem porta publicada, rollback manual (com o limite
  do Flyway), restore de backup e checklist manual do servidor (ASM-451).
  README ganha só um link para o guia.

## T-486 — Frontend só em 127.0.0.1 no override de produção [concluida]
- Refs: US-438, AC-537
- Arquivos: docker-compose.prod.yml, src/test/java/com/apiexternabackend/deploy/ConfiguracaoProducaoTest.java
- Esforço: baixo
- Notas: `ports: !override ["127.0.0.1:${FRONTEND_PORT:-8081}:80"]` no
  `frontend`. Precisa ser `!override`, não lista simples: o Compose soma a lista
  de portas do override à do base e o `0.0.0.0:8081` continuaria publicado
  (conferido com `docker compose config`, v5.3.1). O `tailscale serve` encaminha
  para localhost, então a tailnet segue funcionando. O construtor SnakeYAML do
  teste passa a tratar `!override` como sequência, como já faz com `!reset`, e o
  merge do teste troca a lista marcada com a tag em vez de somar.

## T-487 — deploy.sh: FRONTEND_PATH do FRONTEND_DIR e verificação da API pelo proxy [concluida]
- Refs: US-439, AC-538, AC-539
- Arquivos: deploy/deploy.sh, src/test/java/com/apiexternabackend/deploy/DeployScriptTest.java, src/test/java/com/apiexternabackend/deploy/ScriptSandbox.java, docs/deploy.md
- Esforço: baixo
- Notas: `export FRONTEND_PATH="$FRONTEND_DIR"` antes do `docker compose` — a
  variável do shell vence o `.env` na interpolação do Compose, e o dev local não
  muda porque não usa o script. Verificação da API: `GET /corretoras` pelo nginx
  exige `401` (Spring sem token, `AUT-005`); o `/api/ativos` atual cai no SPA.
  `ScriptSandbox`: o `docker` falso grava o `FRONTEND_PATH` recebido no `up` e o
  `curl` falso responde `401` por padrão na rota da API. Guia: no `.env` do
  servidor, `FRONTEND_PATH=../api-externa-frontend-v2` — rollback e restore
  chamam o compose direto, sem o script.

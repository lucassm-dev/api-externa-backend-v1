#!/usr/bin/env bash
# executar-tarefas.sh — gerado por `onp-spec plano spec-14-jwt-resource-server` em 2026-11-20 00:04
# NÃO edite à mão: mudou tasks.md ou a config, regenere o plano.
#
# uso:
#   bash executar-tarefas.sh                  tudo (ondas → sequenciais → gate)
#   bash executar-tarefas.sh --faixa <id>     reexecuta UMA faixa (+ merge + gate)
#   bash executar-tarefas.sh --seq <T-xxx>    reexecuta UMA tarefa sequencial
#   bash executar-tarefas.sh --gate           só o gate (verify + audit)
#   bash executar-tarefas.sh --listar         mostra faixas, tarefas e estados
#   (acrescente --sem-gate para não rodar o gate ao final)
#
# resumo do que está rolando, a qualquer momento: onp-spec resumo spec-14-jwt-resource-server
set -u
set -o pipefail

RUN_ID='api-externa-backend-v1-spec-14-jwt-resource-server-mwo7waae'
FEATURE='spec-14-jwt-resource-server'
BASE_BRANCH='spec/spec-14-jwt-resource-server'
ENGINE='.claude/skills/onp-spec-driven/scripts/onp-spec.mjs'
CLAUDE_FLAGS=(--permission-mode acceptEdits --allowedTools 'Bash(git add:*),Bash(git commit:*),Bash(git status:*),Bash(git diff:*),Bash(git log:*),Bash(./mvnw:*)')
STREAM_FLAGS=(--output-format stream-json --verbose)
FALHAS=""
COM_GATE=1
RESUMO_MODEL='claude-haiku-4-5'
RESUMO_PID=""

verde()    { printf '\033[32m%s\033[0m\n' "$*"; }
vermelho() { printf '\033[31m%s\033[0m\n' "$*"; }
amarelo()  { printf '\033[33m%s\033[0m\n' "$*"; }
info()     { printf '· %s\n' "$*"; }
falhar()   { vermelho "✘ $*"; exit 1; }

# eventos vão para o ledger GLOBAL (~/.onp-spec/painel/ledger.jsonl):
# um arquivo para todos os projetos, é o que o onp-spec resumo lê
evento() { node "$ENGINE" evento --run "$RUN_ID" "$@" >/dev/null 2>&1 || true; }

# ── ambiente (todos os modos passam por aqui) ────────────────────────
preparar_ambiente() {
  command -v git >/dev/null 2>&1 || falhar "git não encontrado"
  command -v node >/dev/null 2>&1 || falhar "node não encontrado"
  command -v claude >/dev/null 2>&1 || falhar "Claude Code CLI (claude) não encontrado — instale-o ou siga o modo manual em plano-execucao.md"
  TOPLEVEL=$(git rev-parse --show-toplevel 2>/dev/null) || falhar "fora de um repositório git"
  cd "$TOPLEVEL" || exit 1
  # artefatos recém-gerados pelo `onp-spec plano` são sujeira esperada:
  # se forem a ÚNICA sujeira, o script mesmo commita; qualquer outra, aborta
  if [ -n "$(git status --porcelain)" ]; then
    if [ -z "$(git status --porcelain | grep -v -e 'plano-execucao\.' -e 'plano\.json' -e 'executar-tarefas\.sh')" ]; then
      git add -A
      git commit -q -m "plano de execução: $FEATURE (artefatos gerados)"
      info "artefatos do plano commitados"
    else
      falhar "árvore suja além dos artefatos do plano — commite ou faça git stash antes (os worktrees partem do último commit)"
    fi
  fi
  git ls-files --error-unmatch -- '.spec/features/spec-14-jwt-resource-server/spec.md' >/dev/null 2>&1 || falhar "spec.md não está commitada — os worktrees das faixas precisam dela no git"
  ATUAL=$(git rev-parse --abbrev-ref HEAD)
  [ "$ATUAL" != "HEAD" ] || falhar "HEAD destacado — troque para uma branch"
  if [ "$ATUAL" != "$BASE_BRANCH" ]; then
    if git show-ref --verify --quiet "refs/heads/$BASE_BRANCH"; then
      git checkout -q "$BASE_BRANCH" || falhar "não consegui trocar para $BASE_BRANCH"
    else
      git checkout -q -b "$BASE_BRANCH" || falhar "não consegui criar $BASE_BRANCH"
    fi
    info "branch de trabalho: $BASE_BRANCH (a partir de $ATUAL)"
  fi
  git worktree prune
  LOG_DIR="$(dirname "$TOPLEVEL")/onp-worktrees/api-externa-backend-v1-spec-14-jwt-resource-server-logs"
  WT_BASE="$(dirname "$TOPLEVEL")/onp-worktrees/api-externa-backend-v1-spec-14-jwt-resource-server"
  STREAMS_DIR="${ONP_SPEC_HOME:-$HOME/.onp-spec}/painel/streams/$RUN_ID"
  mkdir -p "$LOG_DIR" "$STREAMS_DIR"
}

# worktree limpo mesmo depois de uma tentativa que falhou
preparar_worktree() { # $1=faixa $2=branch $3=worktree
  git worktree prune
  if [ -e "$3" ]; then git worktree remove --force "$3" >/dev/null 2>&1; rm -rf "$3"; fi
  if git show-ref --verify --quiet "refs/heads/$2"; then git branch -D "$2" >/dev/null 2>&1; fi
  git worktree add "$3" -b "$2" >/dev/null 2>&1 || { vermelho "✘ não consegui criar o worktree de $1 em $3"; return 1; }
}

tentativa() { # $1=faixa — conta reexecuções (vai para o ledger)
  local arq="$LOG_DIR/.tentativa-$1"
  local n=1
  [ -f "$arq" ] && n=$(( $(cat "$arq") + 1 ))
  printf "%s" "$n" > "$arq"
  printf "%s" "$n"
}

# uma tarefa = uma sessão claude headless com contexto limpo.
# o JSONL da sessão vira o stream da tarefa no ledger
rodar_tarefa() { # $1=escopo(faixa|seq) $2=T-xxx $3=prompt $4=modelo $5=esforço
  local chave="$1--$2"
  local stream="$STREAMS_DIR/$chave.jsonl"
  evento --tipo tarefa --tarefa "$2" --faixa "$1" --estado executando --stream "$chave"
  info "$2 — claude -p ($4 · $5) · stream: $chave"
  if claude -p "$3" --model "$4" --effort "$5" "${STREAM_FLAGS[@]}" "${CLAUDE_FLAGS[@]}" > "$stream" 2>>"$LOG_DIR/$1.log"; then
    evento --tipo tarefa --tarefa "$2" --faixa "$1" --estado concluida --stream "$chave"
    node "$ENGINE" stream-resumo "$RUN_ID" "$chave" 2>/dev/null || true
    return 0
  fi
  evento --tipo tarefa --tarefa "$2" --faixa "$1" --estado falhou --stream "$chave"
  node "$ENGINE" stream-resumo "$RUN_ID" "$chave" 2>/dev/null || true
  return 1
}

mesclar_faixa() { # $1=faixa $2=branch $3=worktree $4=exit-da-faixa
  if [ "$4" -ne 0 ]; then
    evento --tipo faixa --faixa "$1" --estado falhou
    vermelho "✘ $1 falhou (log: $LOG_DIR/$1.log) — worktree mantido para inspeção: $3"
    amarelo "  reexecute só ela: bash .spec/features/spec-14-jwt-resource-server/executar-tarefas.sh --faixa $1"
    FALHAS="$FALHAS $1"; return 1
  fi
  evento --tipo faixa --faixa "$1" --estado mesclando
  if git merge --no-ff "$2" -m "merge $1 ($FEATURE)"; then
    git worktree remove --force "$3" >/dev/null 2>&1
    git branch -d "$2" >/dev/null 2>&1
    evento --tipo faixa --faixa "$1" --estado mesclada
    verde "✔ $1 mesclada em $BASE_BRANCH"
  else
    git merge --abort >/dev/null 2>&1
    evento --tipo faixa --faixa "$1" --estado conflito
    vermelho "✘ conflito ao mesclar $1 — resolva na mão: git merge $2 (worktree mantido: $3)"
    FALHAS="$FALHAS $1"; return 1
  fi
}

marcar_concluidas() { # $@=T-xxx
  for t in "$@"; do node "$ENGINE" tarefa "$FEATURE" "$t" concluida >/dev/null || true; done
}

# ── resumo geral de andamento: 1/min enquanto a execução roda ─────────
# escrito por IA (claude -p, sem ferramentas) com fallback do motor; vai
# para o terminal e para o ledger — o agente repassa o texto no chat.
gerar_resumo() {
  local ctx ia
  ctx=$(node "$ENGINE" resumo "$FEATURE" --contexto 2>/dev/null) || ctx=""
  [ -n "$ctx" ] || return 0
  ia=$(claude -p "Você narra, para o dono do produto, uma execução de tarefas de código em andamento. Estado mecânico:

$ctx

Escreva o RESUMO GERAL DE ANDAMENTO: um parágrafo único de 2 a 4 frases, em português simples, dizendo o que está acontecendo agora, o que já terminou, o que falhou e se o usuário precisa agir. Sem markdown, sem listas." --model "$RESUMO_MODEL" 2>/dev/null)
  if [ -n "$ia" ]; then
    node "$ENGINE" resumo "$FEATURE" --gravar --origem ia --texto "$ia" >/dev/null 2>&1 || true
    printf '\n📣 resumo (IA): %s\n' "$ia"
  else
    node "$ENGINE" resumo "$FEATURE" --gravar >/dev/null 2>&1 || true
    printf '\n📣 resumo: %s\n' "$(node "$ENGINE" resumo "$FEATURE" 2>/dev/null)"
  fi
}

# mata o loop E o sleep filho — senão o sleep herda o stdout e quem chamou
# o script via pipe fica esperando EOF por até 60s depois do exit
parar_resumos() {
  [ -n "$RESUMO_PID" ] || return 0
  command -v pkill >/dev/null 2>&1 && pkill -P "$RESUMO_PID" 2>/dev/null
  kill "$RESUMO_PID" 2>/dev/null
  RESUMO_PID=""
}

iniciar_resumos() {
  ( while :; do sleep 60; gerar_resumo; done ) &
  RESUMO_PID=$!
  # ao sair: para o loop e grava um último resumo (o estado final, do motor)
  trap 'parar_resumos; node "$ENGINE" resumo "$FEATURE" --gravar >/dev/null 2>&1 || true' EXIT
}

# ── sequencial T-494 (ordem do tasks.md) ──
executar_seq_T_494() {
  info 'sequencial T-494 — Migra emissão e leitura do token para o resource server'
  if rodar_tarefa seq 'T-494' 'Você executa UMA tarefa da feature "spec-14-jwt-resource-server" (fluxo onp-spec, spec-anchored).
Leia primeiro: .spec/features/spec-14-jwt-resource-server/spec.md, .spec/features/spec-14-jwt-resource-server/tasks.md e .spec/constituicao.md.

Sua tarefa (somente ela):
T-494 — "Migra emissão e leitura do token para o resource server"
  critérios/refs: AC-545 (O token não leva e-mail nem CPF), AC-546 (O token vale 60 minutos por padrão), AC-547 (Token adulterado é recusado como inválido), AC-548 (Token de outro emissor é recusado como inválido), AC-550 (A biblioteca jjwt não faz mais parte da aplicação), AC-439 (Endpoint protegido sem token retorna 401), AC-440 (Endpoint protegido com token válido funciona normalmente), AC-441 (Token expirado retorna 401 com código específico)
  arquivos permitidos (e seus testes): pom.xml, src/main/java/com/apiexternabackend/config/SecurityConfig.java, src/main/java/com/apiexternabackend/config/JwtService.java, src/main/java/com/apiexternabackend/config/InvestidorPrincipal.java, src/main/java/com/apiexternabackend/config/JwtAuthenticationEntryPoint.java, src/main/java/com/apiexternabackend/services/AutenticacaoService.java, src/main/resources/application.properties, src/main/resources/application-dev.properties, src/main/resources/application-prod.properties, src/main/resources/application-test.properties, src/test/java/com/apiexternabackend/services/AutenticacaoServiceTest.java, src/test/java/com/apiexternabackend/resources/AcaoResourceTest.java, src/test/java/com/apiexternabackend/resources/CarteiraResourceTest.java, src/test/java/com/apiexternabackend/resources/InvestidorResourceTest.java, src/test/java/com/apiexternabackend/resources/MercadoResourceTest.java, src/test/java/com/apiexternabackend/resources/OperacaoResourceTest.java, src/test/java/com/apiexternabackend/resources/exceptions/GlobalExceptionHandlerTest.java
  mensagem de commit: "T-494 spec-14-jwt-resource-server: Migra emissão e leitura do token para o resource server"

Regras inegociáveis:
- Todo critério de aceite referenciado vira teste com @spec:AC-xxx no título.
- NUNCA enfraqueça, pule (skip/todo) ou apague um teste para passar — teste pulado não é prova e o audit acusa.
- Rode os testes localmente com `./mvnw -q test` até passarem.
- NÃO edite tasks.md, NÃO rode onp-spec verify/audit e NÃO toque em outras tarefas — o orquestrador cuida disso.
- Ao final de CADA tarefa: `git add` só no que você tocou e um commit próprio.' 'claude-sonnet-5' medium >> "$LOG_DIR/seq.log" 2>&1; then
    # commit de segurança se o agente esqueceu (rastreabilidade > perfeição)
    if [ -n "$(git status --porcelain)" ]; then
      git add -A && git commit -q -m 'T-494 spec-14-jwt-resource-server: Migra emissão e leitura do token para o resource server (auto-commit do plano)'
    fi
    marcar_concluidas T-494
    verde "✔ T-494 concluída"
    return 0
  fi
  vermelho "✘ T-494 falhou (log: $LOG_DIR/seq.log)"
  amarelo "  reexecute só ela: bash .spec/features/spec-14-jwt-resource-server/executar-tarefas.sh --seq T-494"
  FALHAS="$FALHAS T-494"
  return 1
}

# ── sequencial T-495 (ordem do tasks.md) ──
executar_seq_T_495() {
  info 'sequencial T-495 — Swagger só no perfil dev'
  if rodar_tarefa seq 'T-495' 'Você executa UMA tarefa da feature "spec-14-jwt-resource-server" (fluxo onp-spec, spec-anchored).
Leia primeiro: .spec/features/spec-14-jwt-resource-server/spec.md, .spec/features/spec-14-jwt-resource-server/tasks.md e .spec/constituicao.md.

Sua tarefa (somente ela):
T-495 — "Swagger só no perfil dev"
  critérios/refs: AC-549 (Swagger fora do perfil dev não responde)
  arquivos permitidos (e seus testes): src/main/resources/application.properties, src/main/resources/application-dev.properties
  mensagem de commit: "T-495 spec-14-jwt-resource-server: Swagger só no perfil dev"

Regras inegociáveis:
- Todo critério de aceite referenciado vira teste com @spec:AC-xxx no título.
- NUNCA enfraqueça, pule (skip/todo) ou apague um teste para passar — teste pulado não é prova e o audit acusa.
- Rode os testes localmente com `./mvnw -q test` até passarem.
- NÃO edite tasks.md, NÃO rode onp-spec verify/audit e NÃO toque em outras tarefas — o orquestrador cuida disso.
- Ao final de CADA tarefa: `git add` só no que você tocou e um commit próprio.' 'claude-sonnet-5' medium >> "$LOG_DIR/seq.log" 2>&1; then
    # commit de segurança se o agente esqueceu (rastreabilidade > perfeição)
    if [ -n "$(git status --porcelain)" ]; then
      git add -A && git commit -q -m 'T-495 spec-14-jwt-resource-server: Swagger só no perfil dev (auto-commit do plano)'
    fi
    marcar_concluidas T-495
    verde "✔ T-495 concluída"
    return 0
  fi
  vermelho "✘ T-495 falhou (log: $LOG_DIR/seq.log)"
  amarelo "  reexecute só ela: bash .spec/features/spec-14-jwt-resource-server/executar-tarefas.sh --seq T-495"
  FALHAS="$FALHAS T-495"
  return 1
}

# ── sequencial T-496 (ordem do tasks.md) ──
executar_seq_T_496() {
  info 'sequencial T-496 — Testes de integração do token novo'
  if rodar_tarefa seq 'T-496' 'Você executa UMA tarefa da feature "spec-14-jwt-resource-server" (fluxo onp-spec, spec-anchored).
Leia primeiro: .spec/features/spec-14-jwt-resource-server/spec.md, .spec/features/spec-14-jwt-resource-server/tasks.md e .spec/constituicao.md.

Sua tarefa (somente ela):
T-496 — "Testes de integração do token novo"
  critérios/refs: AC-545 (O token não leva e-mail nem CPF), AC-546 (O token vale 60 minutos por padrão), AC-547 (Token adulterado é recusado como inválido), AC-548 (Token de outro emissor é recusado como inválido), AC-549 (Swagger fora do perfil dev não responde), AC-550 (A biblioteca jjwt não faz mais parte da aplicação), AC-441 (Token expirado retorna 401 com código específico)
  arquivos permitidos (e seus testes): src/test/java/com/apiexternabackend/config/SecurityIntegrationTest.java
  mensagem de commit: "T-496 spec-14-jwt-resource-server: Testes de integração do token novo"

Regras inegociáveis:
- Todo critério de aceite referenciado vira teste com @spec:AC-xxx no título.
- NUNCA enfraqueça, pule (skip/todo) ou apague um teste para passar — teste pulado não é prova e o audit acusa.
- Rode os testes localmente com `./mvnw -q test` até passarem.
- NÃO edite tasks.md, NÃO rode onp-spec verify/audit e NÃO toque em outras tarefas — o orquestrador cuida disso.
- Ao final de CADA tarefa: `git add` só no que você tocou e um commit próprio.' 'claude-sonnet-5' medium >> "$LOG_DIR/seq.log" 2>&1; then
    # commit de segurança se o agente esqueceu (rastreabilidade > perfeição)
    if [ -n "$(git status --porcelain)" ]; then
      git add -A && git commit -q -m 'T-496 spec-14-jwt-resource-server: Testes de integração do token novo (auto-commit do plano)'
    fi
    marcar_concluidas T-496
    verde "✔ T-496 concluída"
    return 0
  fi
  vermelho "✘ T-496 falhou (log: $LOG_DIR/seq.log)"
  amarelo "  reexecute só ela: bash .spec/features/spec-14-jwt-resource-server/executar-tarefas.sh --seq T-496"
  FALHAS="$FALHAS T-496"
  return 1
}

# ── sequencial T-497 (ordem do tasks.md) ──
executar_seq_T_497() {
  info 'sequencial T-497 — Specs e exemplo de ambiente acompanham a migração'
  if rodar_tarefa seq 'T-497' 'Você executa UMA tarefa da feature "spec-14-jwt-resource-server" (fluxo onp-spec, spec-anchored).
Leia primeiro: .spec/features/spec-14-jwt-resource-server/spec.md, .spec/features/spec-14-jwt-resource-server/tasks.md e .spec/constituicao.md.

Sua tarefa (somente ela):
T-497 — "Specs e exemplo de ambiente acompanham a migração"
  critérios/refs: AC-546 (O token vale 60 minutos por padrão)
  arquivos permitidos (e seus testes): .spec/features/autenticacao-investidor/spec.md, .env.example
  mensagem de commit: "T-497 spec-14-jwt-resource-server: Specs e exemplo de ambiente acompanham a migração"

Regras inegociáveis:
- Todo critério de aceite referenciado vira teste com @spec:AC-xxx no título.
- NUNCA enfraqueça, pule (skip/todo) ou apague um teste para passar — teste pulado não é prova e o audit acusa.
- Rode os testes localmente com `./mvnw -q test` até passarem.
- NÃO edite tasks.md, NÃO rode onp-spec verify/audit e NÃO toque em outras tarefas — o orquestrador cuida disso.
- Ao final de CADA tarefa: `git add` só no que você tocou e um commit próprio.' 'claude-sonnet-5' medium >> "$LOG_DIR/seq.log" 2>&1; then
    # commit de segurança se o agente esqueceu (rastreabilidade > perfeição)
    if [ -n "$(git status --porcelain)" ]; then
      git add -A && git commit -q -m 'T-497 spec-14-jwt-resource-server: Specs e exemplo de ambiente acompanham a migração (auto-commit do plano)'
    fi
    marcar_concluidas T-497
    verde "✔ T-497 concluída"
    return 0
  fi
  vermelho "✘ T-497 falhou (log: $LOG_DIR/seq.log)"
  amarelo "  reexecute só ela: bash .spec/features/spec-14-jwt-resource-server/executar-tarefas.sh --seq T-497"
  FALHAS="$FALHAS T-497"
  return 1
}

# ── gate: quem decide é a máquina ────────────────────────────────────
rodar_gate() {
  echo
  info "gate: verify + audit --ci"
  evento --tipo gate --etapa inicio
  node "$ENGINE" verify "$FEATURE"
  local v=$?
  evento --tipo gate --etapa verify --exit "$v"
  node "$ENGINE" audit --ci
  AUDIT=$?
  evento --tipo gate --etapa audit --exit "$AUDIT"
  # fecha a contabilidade: status das tarefas + prova do verify no git
  if [ -n "$(git status --porcelain -- '.spec')" ]; then
    git add -A -- '.spec'
    git commit -q -m "$FEATURE: status das tarefas + prova do verify (plano)"
    info "status das tarefas e prova do verify commitados"
  fi
  return "$AUDIT"
}

encerrar() { # $1=escopo
  echo
  if [ -n "$FALHAS" ]; then vermelho "faixas/tarefas com falha:$FALHAS"; fi
  # sem gate não existe veredito: NUNCA anunciar alinhamento sem o audit
  if [ "$COM_GATE" -eq 0 ]; then
    evento --tipo fim --exit 1 --escopo "$1"
    if [ -z "$FALHAS" ]; then
      amarelo "○ trabalho de '$1' terminou SEM o gate (--sem-gate) — isto NÃO é prova de nada"
      amarelo "  para o veredito: bash .spec/features/spec-14-jwt-resource-server/executar-tarefas.sh --gate"
      exit 0
    fi
    vermelho "e ainda há falhas — conserte e rode o gate"
    exit 1
  fi
  rodar_gate
  local audit=$?
  if [ "$audit" -eq 0 ] && [ -z "$FALHAS" ]; then
    evento --tipo fim --exit 0 --escopo "$1"
    verde "✔ plano concluído — especificação e código alinhados (audit exit 0) na branch $BASE_BRANCH"
    info "próximo passo: revise e leve para a main quando quiser (git merge $BASE_BRANCH)"
    exit 0
  fi
  evento --tipo fim --exit 1 --escopo "$1"
  vermelho "plano terminou com pendências — leia a saída do audit acima e os logs em $LOG_DIR"
  amarelo "dica: reexecute só o que falhou (--faixa <id> / --seq <T-xxx>)"
  exit 1
}

executar_tudo() {
  evento --tipo inicio --escopo tudo
  iniciar_resumos
  info "logs em: $LOG_DIR"
  info "resumo geral de andamento: a cada 1 min aqui no terminal (e via: onp-spec resumo)"
  executar_seq_T_494 || true
  executar_seq_T_495 || true
  executar_seq_T_496 || true
  executar_seq_T_497 || true
  encerrar tudo
}

listar() {
  echo "execução: $RUN_ID (feature $FEATURE, branch $BASE_BRANCH)"
  echo "  seq       T-494 (sequencial)"
  echo "  seq       T-495 (sequencial)"
  echo "  seq       T-496 (sequencial)"
  echo "  seq       T-497 (sequencial)"
  echo
  echo "reexecutar uma faixa:    --faixa <id>"
  echo "reexecutar sequencial:   --seq <T-xxx>"
  echo "só o gate:               --gate"
}

MODO="tudo"
ALVO=""
while [ $# -gt 0 ]; do
  case "$1" in
    --listar) MODO="listar" ;;
    --gate) MODO="gate" ;;
    --sem-gate) COM_GATE=0 ;;
    --faixa) MODO="faixa"; ALVO="${2:-}"; shift ;;
    --seq) MODO="seq"; ALVO="${2:-}"; shift ;;
    -h|--help) sed -n "2,14p" "$0"; exit 0 ;;
    *) vermelho "argumento desconhecido: $1"; sed -n "2,14p" "$0"; exit 2 ;;
  esac
  shift
done

if [ "$MODO" = "listar" ]; then listar; exit 0; fi

preparar_ambiente

case "$MODO" in
  tudo) executar_tudo ;;
  gate) COM_GATE=1; iniciar_resumos; encerrar gate ;;
  faixa)
    case "$ALVO" in
      *) falhar "faixa desconhecida: '$ALVO' — veja as disponíveis com --listar" ;;
    esac ;;
  seq)
    case "$ALVO" in
      T-494) evento --tipo inicio --escopo "seq:T-494"; iniciar_resumos; executar_seq_T_494 || true; encerrar "seq:T-494" ;;
      T-495) evento --tipo inicio --escopo "seq:T-495"; iniciar_resumos; executar_seq_T_495 || true; encerrar "seq:T-495" ;;
      T-496) evento --tipo inicio --escopo "seq:T-496"; iniciar_resumos; executar_seq_T_496 || true; encerrar "seq:T-496" ;;
      T-497) evento --tipo inicio --escopo "seq:T-497"; iniciar_resumos; executar_seq_T_497 || true; encerrar "seq:T-497" ;;
      *) falhar "tarefa sequencial desconhecida: '$ALVO' — veja as disponíveis com --listar" ;;
    esac ;;
esac

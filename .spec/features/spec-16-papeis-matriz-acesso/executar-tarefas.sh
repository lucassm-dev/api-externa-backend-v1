#!/usr/bin/env bash
# executar-tarefas.sh — gerado por `onp-spec plano spec-16-papeis-matriz-acesso` em 2026-11-21 04:21
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
# resumo do que está rolando, a qualquer momento: onp-spec resumo spec-16-papeis-matriz-acesso
set -u
set -o pipefail

RUN_ID='api-externa-backend-v1-spec-16-papeis-matriz-acesso-mwpwie69'
FEATURE='spec-16-papeis-matriz-acesso'
BASE_BRANCH='spec/spec-16-papeis-matriz-acesso'
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
  git ls-files --error-unmatch -- '.spec/features/spec-16-papeis-matriz-acesso/spec.md' >/dev/null 2>&1 || falhar "spec.md não está commitada — os worktrees das faixas precisam dela no git"
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
  LOG_DIR="$(dirname "$TOPLEVEL")/onp-worktrees/api-externa-backend-v1-spec-16-papeis-matriz-acesso-logs"
  WT_BASE="$(dirname "$TOPLEVEL")/onp-worktrees/api-externa-backend-v1-spec-16-papeis-matriz-acesso"
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
    amarelo "  reexecute só ela: bash .spec/features/spec-16-papeis-matriz-acesso/executar-tarefas.sh --faixa $1"
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

# ── sequencial T-501 (ordem do tasks.md) ──
executar_seq_T_501() {
  info 'sequencial T-501 — Papel real no token e login do ADMIN'
  if rodar_tarefa seq 'T-501' 'Você executa UMA tarefa da feature "spec-16-papeis-matriz-acesso" (fluxo onp-spec, spec-anchored).
Leia primeiro: .spec/features/spec-16-papeis-matriz-acesso/spec.md, .spec/features/spec-16-papeis-matriz-acesso/tasks.md e .spec/constituicao.md.

Sua tarefa (somente ela):
T-501 — "Papel real no token e login do ADMIN"
  critérios/refs: AC-558 (Token leva o papel do usuário, e o ADMIN consegue entrar), AC-568 (Autocadastro ignora papel enviado e nenhuma entrada da API aceita papel)
  arquivos permitidos (e seus testes): src/main/java/com/apiexternabackend/config/JwtService.java, src/main/java/com/apiexternabackend/services/AutenticacaoService.java, src/test/java/com/apiexternabackend/services/AutenticacaoServiceTest.java, src/test/java/com/apiexternabackend/config/IdentidadeUsuarioIntegrationTest.java
  mensagem de commit: "T-501 spec-16-papeis-matriz-acesso: Papel real no token e login do ADMIN"

Regras inegociáveis:
- Todo critério de aceite referenciado vira teste com @spec:AC-xxx no título.
- NUNCA enfraqueça, pule (skip/todo) ou apague um teste para passar — teste pulado não é prova e o audit acusa.
- Rode os testes localmente com `./mvnw -q test` até passarem.
- NÃO edite tasks.md, NÃO rode onp-spec verify/audit e NÃO toque em outras tarefas — o orquestrador cuida disso.
- Ao final de CADA tarefa: `git add` só no que você tocou e um commit próprio.' 'claude-sonnet-5' medium >> "$LOG_DIR/seq.log" 2>&1; then
    # commit de segurança se o agente esqueceu (rastreabilidade > perfeição)
    if [ -n "$(git status --porcelain)" ]; then
      git add -A && git commit -q -m 'T-501 spec-16-papeis-matriz-acesso: Papel real no token e login do ADMIN (auto-commit do plano)'
    fi
    marcar_concluidas T-501
    verde "✔ T-501 concluída"
    return 0
  fi
  vermelho "✘ T-501 falhou (log: $LOG_DIR/seq.log)"
  amarelo "  reexecute só ela: bash .spec/features/spec-16-papeis-matriz-acesso/executar-tarefas.sh --seq T-501"
  FALHAS="$FALHAS T-501"
  return 1
}

# ── sequencial T-502 (ordem do tasks.md) ──
executar_seq_T_502() {
  info 'sequencial T-502 — Matriz de acesso nas rotas'
  if rodar_tarefa seq 'T-502' 'Você executa UMA tarefa da feature "spec-16-papeis-matriz-acesso" (fluxo onp-spec, spec-anchored).
Leia primeiro: .spec/features/spec-16-papeis-matriz-acesso/spec.md, .spec/features/spec-16-papeis-matriz-acesso/tasks.md e .spec/constituicao.md.

Sua tarefa (somente ela):
T-502 — "Matriz de acesso nas rotas"
  critérios/refs: AC-559 (Os dois papéis consultam corretoras), AC-560 (Só o ADMIN cadastra e exclui corretora), AC-561 (Os dois papéis pesquisam ações), AC-562 (Só o ADMIN altera o catálogo de ações), AC-563 (ADMIN não acessa carteiras, operações nem perfil de investidor)
  arquivos permitidos (e seus testes): src/main/java/com/apiexternabackend/config/SecurityConfig.java, src/main/java/com/apiexternabackend/resources/CorretoraResource.java, src/main/java/com/apiexternabackend/resources/AcaoResource.java, src/main/java/com/apiexternabackend/resources/MercadoResource.java, src/main/java/com/apiexternabackend/resources/CarteiraResource.java, src/main/java/com/apiexternabackend/resources/OperacaoResource.java, src/main/java/com/apiexternabackend/resources/InvestidorResource.java, src/test/java/com/apiexternabackend/config/MatrizAcessoIntegrationTest.java, src/test/java/com/apiexternabackend/resources/CorretoraResourceTest.java, src/test/java/com/apiexternabackend/resources/AcaoResourceTest.java, src/test/java/com/apiexternabackend/resources/MercadoResourceTest.java, src/test/java/com/apiexternabackend/resources/CarteiraResourceTest.java, src/test/java/com/apiexternabackend/resources/OperacaoResourceTest.java, src/test/java/com/apiexternabackend/resources/InvestidorResourceTest.java
  mensagem de commit: "T-502 spec-16-papeis-matriz-acesso: Matriz de acesso nas rotas"

Regras inegociáveis:
- Todo critério de aceite referenciado vira teste com @spec:AC-xxx no título.
- NUNCA enfraqueça, pule (skip/todo) ou apague um teste para passar — teste pulado não é prova e o audit acusa.
- Rode os testes localmente com `./mvnw -q test` até passarem.
- NÃO edite tasks.md, NÃO rode onp-spec verify/audit e NÃO toque em outras tarefas — o orquestrador cuida disso.
- Ao final de CADA tarefa: `git add` só no que você tocou e um commit próprio.' 'claude-sonnet-5' high >> "$LOG_DIR/seq.log" 2>&1; then
    # commit de segurança se o agente esqueceu (rastreabilidade > perfeição)
    if [ -n "$(git status --porcelain)" ]; then
      git add -A && git commit -q -m 'T-502 spec-16-papeis-matriz-acesso: Matriz de acesso nas rotas (auto-commit do plano)'
    fi
    marcar_concluidas T-502
    verde "✔ T-502 concluída"
    return 0
  fi
  vermelho "✘ T-502 falhou (log: $LOG_DIR/seq.log)"
  amarelo "  reexecute só ela: bash .spec/features/spec-16-papeis-matriz-acesso/executar-tarefas.sh --seq T-502"
  FALHAS="$FALHAS T-502"
  return 1
}

# ── sequencial T-503 (ordem do tasks.md) ──
executar_seq_T_503() {
  info 'sequencial T-503 — Primeiro ADMIN pelas variáveis de ambiente'
  if rodar_tarefa seq 'T-503' 'Você executa UMA tarefa da feature "spec-16-papeis-matriz-acesso" (fluxo onp-spec, spec-anchored).
Leia primeiro: .spec/features/spec-16-papeis-matriz-acesso/spec.md, .spec/features/spec-16-papeis-matriz-acesso/tasks.md e .spec/constituicao.md.

Sua tarefa (somente ela):
T-503 — "Primeiro ADMIN pelas variáveis de ambiente"
  critérios/refs: AC-564 (Inicialização cria o ADMIN uma única vez), AC-565 (A senha do ADMIN não aparece em log nem no código), AC-566 (Sem as variáveis, sobe sem ADMIN e avisa), AC-567 (E-mail de investidor não vira ADMIN)
  arquivos permitidos (e seus testes): src/main/java/com/apiexternabackend/config/AdminInicial.java, src/main/java/com/apiexternabackend/repositories/UsuarioRepository.java, src/main/resources/application.properties, .env.example, src/test/java/com/apiexternabackend/config/AdminInicialTest.java
  mensagem de commit: "T-503 spec-16-papeis-matriz-acesso: Primeiro ADMIN pelas variáveis de ambiente"

Regras inegociáveis:
- Todo critério de aceite referenciado vira teste com @spec:AC-xxx no título.
- NUNCA enfraqueça, pule (skip/todo) ou apague um teste para passar — teste pulado não é prova e o audit acusa.
- Rode os testes localmente com `./mvnw -q test` até passarem.
- NÃO edite tasks.md, NÃO rode onp-spec verify/audit e NÃO toque em outras tarefas — o orquestrador cuida disso.
- Ao final de CADA tarefa: `git add` só no que você tocou e um commit próprio.' 'claude-sonnet-5' medium >> "$LOG_DIR/seq.log" 2>&1; then
    # commit de segurança se o agente esqueceu (rastreabilidade > perfeição)
    if [ -n "$(git status --porcelain)" ]; then
      git add -A && git commit -q -m 'T-503 spec-16-papeis-matriz-acesso: Primeiro ADMIN pelas variáveis de ambiente (auto-commit do plano)'
    fi
    marcar_concluidas T-503
    verde "✔ T-503 concluída"
    return 0
  fi
  vermelho "✘ T-503 falhou (log: $LOG_DIR/seq.log)"
  amarelo "  reexecute só ela: bash .spec/features/spec-16-papeis-matriz-acesso/executar-tarefas.sh --seq T-503"
  FALHAS="$FALHAS T-503"
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
      amarelo "  para o veredito: bash .spec/features/spec-16-papeis-matriz-acesso/executar-tarefas.sh --gate"
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
  executar_seq_T_501 || true
  executar_seq_T_502 || true
  executar_seq_T_503 || true
  encerrar tudo
}

listar() {
  echo "execução: $RUN_ID (feature $FEATURE, branch $BASE_BRANCH)"
  echo "  seq       T-501 (sequencial)"
  echo "  seq       T-502 (sequencial)"
  echo "  seq       T-503 (sequencial)"
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
      T-501) evento --tipo inicio --escopo "seq:T-501"; iniciar_resumos; executar_seq_T_501 || true; encerrar "seq:T-501" ;;
      T-502) evento --tipo inicio --escopo "seq:T-502"; iniciar_resumos; executar_seq_T_502 || true; encerrar "seq:T-502" ;;
      T-503) evento --tipo inicio --escopo "seq:T-503"; iniciar_resumos; executar_seq_T_503 || true; encerrar "seq:T-503" ;;
      *) falhar "tarefa sequencial desconhecida: '$ALVO' — veja as disponíveis com --listar" ;;
    esac ;;
esac

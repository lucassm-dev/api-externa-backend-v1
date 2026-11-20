# Plano de execução — spec-14-jwt-resource-server

> gerado por `onp-spec plano` em 2026-11-20 00:04 — NÃO edite à mão;
> mudou tasks.md ou a config? Regenere: `onp-spec plano spec-14-jwt-resource-server --sequencial`

## Resumo — o que vai acontecer

- **modo SEQUENCIAL (escolha do usuário)**: 4 tarefa(s) pendente(s), UMA APÓS A OUTRA, na árvore principal
- sem worktrees e sem paralelismo — cada tarefa roda numa janela de contexto limpa, na ordem do tasks.md
- tudo acontece na branch de trabalho `spec/spec-14-jwt-resource-server`; levar para a main é decisão sua

## Ordem de execução (uma tarefa após a outra)

| tarefa | título | modelo | esforço |
|---|---|---|---|
| T-494 | Migra emissão e leitura do token para o resource server | `claude-sonnet-5` | medium |
| T-495 | Swagger só no perfil dev | `claude-sonnet-5` | medium |
| T-496 | Testes de integração do token novo | `claude-sonnet-5` | medium |
| T-497 | Specs e exemplo de ambiente acompanham a migração | `claude-sonnet-5` | medium |

## Gestão de branches e commits

1. branch de trabalho `spec/spec-14-jwt-resource-server` criada do ponto atual (se ainda não existir)
2. as tarefas rodam nela mesma, na ordem — **1 tarefa = 1 commit** (`T-xxx feature: título`), marcada `[concluida]` só com trabalho feito
3. gate final na branch de trabalho: `onp-spec verify spec-14-jwt-resource-server` + `onp-spec audit --ci` — **exit 0 ou não está pronto**

## Como executar

### ▶ Execução — Claude Code headless

```bash
bash .spec/features/spec-14-jwt-resource-server/executar-tarefas.sh
```

Cada tarefa roda `claude -p` com **janela de contexto limpa**, na árvore principal,
uma após a outra, com `--model` e `--effort` já definidos por tarefa e permissões `acceptEdits`.
Os prompts exatos estão embutidos no script.
Logs: `../onp-worktrees/api-externa-backend-v1-spec-14-jwt-resource-server-logs/`.

### 📣 Acompanhamento — tabela + resumo no chat (a cada 1 min)

O script roda em **background**: o agente AVISA o usuário antes de iniciar e,
enquanto roda, posta no chat a cada ~1 minuto a **tabela de andamento** (qual
tarefa está rodando, qual não está, o que concluiu/falhou) junto com o
**resumo geral de andamento** (escrito por IA; sem IA, o motor resume). Ao
final, o usuário recebe o resumo completo da execução. A qualquer momento:

```bash
onp-spec resumo spec-14-jwt-resource-server --tabela   # a tabela de andamento
onp-spec resumo spec-14-jwt-resource-server            # o resumo em texto
```


# Plano de execução — spec-13-autorizacao-investidor

> gerado por `onp-spec plano` em 2026-11-19 02:41 — NÃO edite à mão;
> mudou tasks.md ou a config? Regenere: `onp-spec plano spec-13-autorizacao-investidor --sequencial`

## Resumo — o que vai acontecer

- **modo SEQUENCIAL (escolha do usuário)**: 4 tarefa(s) pendente(s), UMA APÓS A OUTRA, na árvore principal
- sem worktrees e sem paralelismo — cada tarefa roda numa janela de contexto limpa, na ordem do tasks.md
- tudo acontece na branch de trabalho `spec/spec-13-autorizacao-investidor`; levar para a main é decisão sua

## Ordem de execução (uma tarefa após a outra)

| tarefa | título | modelo | esforço |
|---|---|---|---|
| T-490 | Service e resource de investidor recebem o investidor logado | `claude-sonnet-5` | medium |
| T-491 | Testes de unidade de investidor na nova assinatura | `claude-sonnet-5` | medium |
| T-492 | Teste de integração: investidor A contra investidor B | `claude-sonnet-5` | medium |
| T-493 | Specs antigas apontam para esta | `claude-sonnet-5` | medium |

## Gestão de branches e commits

1. branch de trabalho `spec/spec-13-autorizacao-investidor` criada do ponto atual (se ainda não existir)
2. as tarefas rodam nela mesma, na ordem — **1 tarefa = 1 commit** (`T-xxx feature: título`), marcada `[concluida]` só com trabalho feito
3. gate final na branch de trabalho: `onp-spec verify spec-13-autorizacao-investidor` + `onp-spec audit --ci` — **exit 0 ou não está pronto**

## Como executar

### ▶ Execução — Claude Code headless

```bash
bash .spec/features/spec-13-autorizacao-investidor/executar-tarefas.sh
```

Cada tarefa roda `claude -p` com **janela de contexto limpa**, na árvore principal,
uma após a outra, com `--model` e `--effort` já definidos por tarefa e permissões `acceptEdits`.
Os prompts exatos estão embutidos no script.
Logs: `../onp-worktrees/api-externa-backend-v1-spec-13-autorizacao-investidor-logs/`.

### 📣 Acompanhamento — tabela + resumo no chat (a cada 1 min)

O script roda em **background**: o agente AVISA o usuário antes de iniciar e,
enquanto roda, posta no chat a cada ~1 minuto a **tabela de andamento** (qual
tarefa está rodando, qual não está, o que concluiu/falhou) junto com o
**resumo geral de andamento** (escrito por IA; sem IA, o motor resume). Ao
final, o usuário recebe o resumo completo da execução. A qualquer momento:

```bash
onp-spec resumo spec-13-autorizacao-investidor --tabela   # a tabela de andamento
onp-spec resumo spec-13-autorizacao-investidor            # o resumo em texto
```


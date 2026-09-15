# Plano de execução — deploy-homelab

> gerado por `onp-spec plano` em 2026-09-15 02:46 — NÃO edite à mão;
> mudou tasks.md ou a config? Regenere: `onp-spec plano deploy-homelab --paralelizar T-482,T-483`

## Resumo — o que vai acontecer

- **4 tarefa(s) pendente(s)**: 2 em 2 faixa(s) paralela(s) + 2 sequencial(is)
- **seleção do usuário**: paralelizar só T-482, T-483 — as demais rodam uma após a outra, ao final
- **1 faixa = 1 worktree + 1 branch + 1 janela de contexto limpa** — faixas não compartilham nenhum arquivo entre si
- prefere outra seleção ou uma após a outra? Regenere com `onp-spec plano deploy-homelab --paralelizar T-xxx,T-yyy` ou `--sequencial`
- tudo acontece na branch de trabalho `spec/deploy-homelab`; levar para a main é decisão sua

## Faixas e ondas

### Onda 1 — faixa-1 ∥ faixa-2

#### faixa-1 — branch `spec/deploy-homelab-faixa-1` — worktree `../onp-worktrees/api-externa-backend-v1-deploy-homelab-faixa-1`

| tarefa | título | modelo | esforço | arquivos |
|---|---|---|---|---|
| T-482 | Override de produção do compose e perfil prod | `claude-sonnet-5` | low | `docker-compose.prod.yml`, `src/main/resources/application-prod.properties`, `src/test/java/com/apiexternabackend/deploy/ConfiguracaoProducaoTest.java` |

#### faixa-2 — branch `spec/deploy-homelab-faixa-2` — worktree `../onp-worktrees/api-externa-backend-v1-deploy-homelab-faixa-2`

| tarefa | título | modelo | esforço | arquivos |
|---|---|---|---|---|
| T-483 | Script de deploy | `claude-sonnet-5` | medium | `deploy/deploy.sh`, `src/test/java/com/apiexternabackend/deploy/DeployScriptTest.java`, `src/test/java/com/apiexternabackend/deploy/ScriptSandbox.java` |

## Tarefas sequenciais (após as ondas, na árvore principal)

| tarefa | título | modelo | esforço | por que sequencial |
|---|---|---|---|---|
| T-484 | Script de backup | `claude-sonnet-5` | low | fora da seleção do usuário |
| T-485 | Guia de deploy | `claude-sonnet-5` | low | fora da seleção do usuário |

## Gestão de branches e commits

1. branch de trabalho `spec/deploy-homelab` criada do ponto atual (se ainda não existir)
2. cada faixa nasce dela como branch própria e roda no seu worktree — **1 tarefa = 1 commit** (`T-xxx feature: título`)
3. terminou a onda → merge `--no-ff` de cada faixa de volta, na ordem; conflito interrompe a faixa e pede resolução humana
4. faixa mesclada → worktree removido, branch apagada, tarefa marcada `[concluida]` no tasks.md
5. gate final na branch de trabalho: `onp-spec verify deploy-homelab` + `onp-spec audit --ci` — **exit 0 ou não está pronto**

## Como executar

### ▶ Execução — Claude Code headless

```bash
bash .spec/features/deploy-homelab/executar-tarefas.sh
```

Cada faixa roda `claude -p` com **janela de contexto limpa**, no seu worktree, com
`--model` e `--effort` já definidos por tarefa e permissões `acceptEdits`. Os prompts exatos estão
embutidos no script — quer rodar uma faixa na mão, é só copiá-los de lá.
Logs: `../onp-worktrees/api-externa-backend-v1-deploy-homelab-logs/`.

### 📣 Acompanhamento — tabela + resumo no chat (a cada 1 min)

O script roda em **background**: o agente AVISA o usuário antes de iniciar e,
enquanto roda, posta no chat a cada ~1 minuto a **tabela de andamento** (qual
tarefa está rodando, qual não está, o que concluiu/falhou) junto com o
**resumo geral de andamento** (escrito por IA; sem IA, o motor resume). Ao
final, o usuário recebe o resumo completo da execução. A qualquer momento:

```bash
onp-spec resumo deploy-homelab --tabela   # a tabela de andamento
onp-spec resumo deploy-homelab            # o resumo em texto
```


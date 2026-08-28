# Plano de execução — refactor-cnpj-delete

> gerado por `onp-spec plano` em 2026-08-27 00:40 — NÃO edite à mão;
> mudou tasks.md ou a config? Regenere: `onp-spec plano refactor-cnpj-delete`

## Resumo — o que vai acontecer

- **3 tarefa(s) pendente(s)**: 3 em 3 faixa(s) paralela(s) + 0 sequencial(is)
- **1 faixa = 1 worktree + 1 branch + 1 janela de contexto limpa** — faixas não compartilham nenhum arquivo entre si
- prefere outra seleção ou uma após a outra? Regenere com `onp-spec plano refactor-cnpj-delete --paralelizar T-xxx,T-yyy` ou `--sequencial`
- tudo acontece na branch de trabalho `spec/refactor-cnpj-delete`; levar para a main é decisão sua

## Faixas e ondas

### Onda 1 — faixa-1 ∥ faixa-2 ∥ faixa-3

#### faixa-1 — branch `spec/refactor-cnpj-delete-faixa-1` — worktree `../onp-worktrees/api-externa-backend-v1-refactor-cnpj-delete-faixa-1`

| tarefa | título | modelo | esforço | arquivos |
|---|---|---|---|---|
| T-001 | Validação CNPJ no CnpjFacade + DELETE Corretora | `claude-sonnet-5` | medium | `src/main/java/com/apiexternabackend/infra/facade/CnpjFacade.java`, `src/main/java/com/apiexternabackend/services/CorretoraService.java`, `src/main/java/com/apiexternabackend/domains/Corretora.java`, `src/main/java/com/apiexternabackend/resources/CorretoraResource.java`, `src/main/resources/db/migration/V8__add_ativo_corretora.sql`, `src/test/java/com/apiexternabackend/services/CorretoraServiceTest.java`, `src/test/java/com/apiexternabackend/resources/CorretoraResourceTest.java` |

#### faixa-2 — branch `spec/refactor-cnpj-delete-faixa-2` — worktree `../onp-worktrees/api-externa-backend-v1-refactor-cnpj-delete-faixa-2`

| tarefa | título | modelo | esforço | arquivos |
|---|---|---|---|---|
| T-002 | DELETE Investidor | `claude-sonnet-5` | medium | `src/main/java/com/apiexternabackend/domains/Investidor.java`, `src/main/java/com/apiexternabackend/services/InvestidorService.java`, `src/main/java/com/apiexternabackend/resources/InvestidorResource.java`, `src/main/resources/db/migration/V9__add_ativo_investidor.sql`, `src/test/java/com/apiexternabackend/services/InvestidorServiceTest.java`, `src/test/java/com/apiexternabackend/resources/InvestidorResourceTest.java` |

#### faixa-3 — branch `spec/refactor-cnpj-delete-faixa-3` — worktree `../onp-worktrees/api-externa-backend-v1-refactor-cnpj-delete-faixa-3`

| tarefa | título | modelo | esforço | arquivos |
|---|---|---|---|---|
| T-003 | DELETE Ação | `claude-sonnet-5` | medium | `src/main/java/com/apiexternabackend/domains/Acao.java`, `src/main/java/com/apiexternabackend/services/AcaoService.java`, `src/main/java/com/apiexternabackend/resources/AcaoResource.java`, `src/main/resources/db/migration/V10__add_ativo_acao.sql`, `src/test/java/com/apiexternabackend/services/AcaoServiceTest.java`, `src/test/java/com/apiexternabackend/resources/AcaoResourceTest.java` |

## Gestão de branches e commits

1. branch de trabalho `spec/refactor-cnpj-delete` criada do ponto atual (se ainda não existir)
2. cada faixa nasce dela como branch própria e roda no seu worktree — **1 tarefa = 1 commit** (`T-xxx feature: título`)
3. terminou a onda → merge `--no-ff` de cada faixa de volta, na ordem; conflito interrompe a faixa e pede resolução humana
4. faixa mesclada → worktree removido, branch apagada, tarefa marcada `[concluida]` no tasks.md
5. gate final na branch de trabalho: `onp-spec verify refactor-cnpj-delete` + `onp-spec audit --ci` — **exit 0 ou não está pronto**

## Como executar

### ▶ Execução — Claude Code headless

```bash
bash .spec/features/refactor-cnpj-delete/executar-tarefas.sh
```

Cada faixa roda `claude -p` com **janela de contexto limpa**, no seu worktree, com
`--model` e `--effort` já definidos por tarefa e permissões `acceptEdits`. Os prompts exatos estão
embutidos no script — quer rodar uma faixa na mão, é só copiá-los de lá.
Logs: `../onp-worktrees/api-externa-backend-v1-refactor-cnpj-delete-logs/`.

### 📣 Acompanhamento — tabela + resumo no chat (a cada 1 min)

O script roda em **background**: o agente AVISA o usuário antes de iniciar e,
enquanto roda, posta no chat a cada ~1 minuto a **tabela de andamento** (qual
tarefa está rodando, qual não está, o que concluiu/falhou) junto com o
**resumo geral de andamento** (escrito por IA; sem IA, o motor resume). Ao
final, o usuário recebe o resumo completo da execução. A qualquer momento:

```bash
onp-spec resumo refactor-cnpj-delete --tabela   # a tabela de andamento
onp-spec resumo refactor-cnpj-delete            # o resumo em texto
```


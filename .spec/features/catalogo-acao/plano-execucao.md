# Plano de execução — catalogo-acao

> gerado por `onp-spec plano` em 2026-08-22 23:19 — NÃO edite à mão;
> mudou tasks.md ou a config? Regenere: `onp-spec plano catalogo-acao`

## Resumo — o que vai acontecer

- **8 tarefa(s) pendente(s)**: 8 em 8 faixa(s) paralela(s) + 0 sequencial(is)
- **1 faixa = 1 worktree + 1 branch + 1 janela de contexto limpa** — faixas não compartilham nenhum arquivo entre si
- prefere outra seleção ou uma após a outra? Regenere com `onp-spec plano catalogo-acao --paralelizar T-xxx,T-yyy` ou `--sequencial`
- tudo acontece na branch de trabalho `spec/catalogo-acao`; levar para a main é decisão sua

## Faixas e ondas

### Onda 1 — faixa-1 ∥ faixa-2 ∥ faixa-3

#### faixa-1 — branch `spec/catalogo-acao-faixa-1` — worktree `../onp-worktrees/api-externa-backend-v1-catalogo-acao-faixa-1`

| tarefa | título | modelo | esforço | arquivos |
|---|---|---|---|---|
| T-200 | Entidade Acao, enum de mercado, repositório e migration | `claude-sonnet-5` | medium | `src/main/java/com/apiexternabackend/domains/Acao.java`, `src/main/java/com/apiexternabackend/domains/enums/Mercado.java`, `src/main/java/com/apiexternabackend/repositories/AcaoRepository.java`, `src/main/resources/db/migration/V4__create_acao.sql` |

#### faixa-2 — branch `spec/catalogo-acao-faixa-2` — worktree `../onp-worktrees/api-externa-backend-v1-catalogo-acao-faixa-2`

| tarefa | título | modelo | esforço | arquivos |
|---|---|---|---|---|
| T-201 | DTOs e mapper da ação | `claude-sonnet-5` | medium | `src/main/java/com/apiexternabackend/domains/dtos/AcaoRequestDTO.java`, `src/main/java/com/apiexternabackend/domains/dtos/AcaoResponseDTO.java`, `src/main/java/com/apiexternabackend/mappers/AcaoMapper.java` |

#### faixa-3 — branch `spec/catalogo-acao-faixa-3` — worktree `../onp-worktrees/api-externa-backend-v1-catalogo-acao-faixa-3`

| tarefa | título | modelo | esforço | arquivos |
|---|---|---|---|---|
| T-202 | Adapter de cotação (isolamento, Item 15) | `claude-sonnet-5` | medium | `src/main/java/com/apiexternabackend/infra/adapter/CotacaoAdapter.java`, `src/main/java/com/apiexternabackend/infra/adapter/CotacaoResultado.java` |

### Onda 2 — faixa-4 ∥ faixa-5 ∥ faixa-6

#### faixa-4 — branch `spec/catalogo-acao-faixa-4` — worktree `../onp-worktrees/api-externa-backend-v1-catalogo-acao-faixa-4`

| tarefa | título | modelo | esforço | arquivos |
|---|---|---|---|---|
| T-203 | Integração brapi (mercado BR) | `claude-sonnet-5` | medium | `src/main/java/com/apiexternabackend/infra/client/brapi/BrapiClient.java`, `src/main/java/com/apiexternabackend/infra/client/brapi/dtos/BrapiResponseDTO.java`, `src/main/java/com/apiexternabackend/infra/client/brapi/dtos/BrapiResultDTO.java`, `src/main/java/com/apiexternabackend/infra/adapter/BrapiAdapter.java` |

#### faixa-5 — branch `spec/catalogo-acao-faixa-5` — worktree `../onp-worktrees/api-externa-backend-v1-catalogo-acao-faixa-5`

| tarefa | título | modelo | esforço | arquivos |
|---|---|---|---|---|
| T-204 | Integração Twelve Data (mercado US) | `claude-sonnet-5` | medium | `src/main/java/com/apiexternabackend/infra/client/twelvedata/TwelveDataClient.java`, `src/main/java/com/apiexternabackend/infra/client/twelvedata/dtos/TwelveDataResponseDTO.java`, `src/main/java/com/apiexternabackend/infra/adapter/TwelveDataAdapter.java` |

#### faixa-6 — branch `spec/catalogo-acao-faixa-6` — worktree `../onp-worktrees/api-externa-backend-v1-catalogo-acao-faixa-6`

| tarefa | título | modelo | esforço | arquivos |
|---|---|---|---|---|
| T-205 | Serviço da ação | `claude-sonnet-5` | medium | `src/main/java/com/apiexternabackend/services/AcaoService.java` |

### Onda 3 — faixa-7 ∥ faixa-8

#### faixa-7 — branch `spec/catalogo-acao-faixa-7` — worktree `../onp-worktrees/api-externa-backend-v1-catalogo-acao-faixa-7`

| tarefa | título | modelo | esforço | arquivos |
|---|---|---|---|---|
| T-206 | Controller da ação | `claude-sonnet-5` | medium | `src/main/java/com/apiexternabackend/resources/AcaoResource.java` |

#### faixa-8 — branch `spec/catalogo-acao-faixa-8` — worktree `../onp-worktrees/api-externa-backend-v1-catalogo-acao-faixa-8`

| tarefa | título | modelo | esforço | arquivos |
|---|---|---|---|---|
| T-207 | Testes da ação | `claude-sonnet-5` | medium | `src/test/java/com/apiexternabackend/services/AcaoServiceTest.java`, `src/test/java/com/apiexternabackend/resources/AcaoResourceTest.java` |

## Gestão de branches e commits

1. branch de trabalho `spec/catalogo-acao` criada do ponto atual (se ainda não existir)
2. cada faixa nasce dela como branch própria e roda no seu worktree — **1 tarefa = 1 commit** (`T-xxx feature: título`)
3. terminou a onda → merge `--no-ff` de cada faixa de volta, na ordem; conflito interrompe a faixa e pede resolução humana
4. faixa mesclada → worktree removido, branch apagada, tarefa marcada `[concluida]` no tasks.md
5. gate final na branch de trabalho: `onp-spec verify catalogo-acao` + `onp-spec audit --ci` — **exit 0 ou não está pronto**

## Como executar

### ▶ Execução — Claude Code headless

```bash
bash .spec/features/catalogo-acao/executar-tarefas.sh
```

Cada faixa roda `claude -p` com **janela de contexto limpa**, no seu worktree, com
`--model` e `--effort` já definidos por tarefa e permissões `acceptEdits`. Os prompts exatos estão
embutidos no script — quer rodar uma faixa na mão, é só copiá-los de lá.
Logs: `../onp-worktrees/api-externa-backend-v1-catalogo-acao-logs/`.

### 📣 Acompanhamento — tabela + resumo no chat (a cada 1 min)

O script roda em **background**: o agente AVISA o usuário antes de iniciar e,
enquanto roda, posta no chat a cada ~1 minuto a **tabela de andamento** (qual
tarefa está rodando, qual não está, o que concluiu/falhou) junto com o
**resumo geral de andamento** (escrito por IA; sem IA, o motor resume). Ao
final, o usuário recebe o resumo completo da execução. A qualquer momento:

```bash
onp-spec resumo catalogo-acao --tabela   # a tabela de andamento
onp-spec resumo catalogo-acao            # o resumo em texto
```


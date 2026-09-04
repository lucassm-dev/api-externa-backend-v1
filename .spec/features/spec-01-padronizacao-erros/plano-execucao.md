# Plano de execução — spec-01-padronizacao-erros

> gerado por `onp-spec plano` em 2026-09-04 17:59 — NÃO edite à mão;
> mudou tasks.md ou a config? Regenere: `onp-spec plano spec-01-padronizacao-erros --paralelizar T-408,T-409,T-410,T-411,T-412`

## Resumo — o que vai acontecer

- **6 tarefa(s) pendente(s)**: 5 em 5 faixa(s) paralela(s) + 1 sequencial(is) (1 já concluída(s): T-407)
- **seleção do usuário**: paralelizar só T-408, T-409, T-410, T-411, T-412 — as demais rodam uma após a outra, ao final
- **1 faixa = 1 worktree + 1 branch + 1 janela de contexto limpa** — faixas não compartilham nenhum arquivo entre si
- prefere outra seleção ou uma após a outra? Regenere com `onp-spec plano spec-01-padronizacao-erros --paralelizar T-xxx,T-yyy` ou `--sequencial`
- tudo acontece na branch de trabalho `spec/spec-01-padronizacao-erros`; levar para a main é decisão sua

## Faixas e ondas

### Onda 1 — faixa-1 ∥ faixa-2 ∥ faixa-3

#### faixa-1 — branch `spec/spec-01-padronizacao-erros-faixa-1` — worktree `../onp-worktrees/api-externa-backend-v1-spec-01-padronizacao-erros-faixa-1`

| tarefa | título | modelo | esforço | arquivos |
|---|---|---|---|---|
| T-408 | Migrar catálogo de Ação (ACA-001/002) e comportamento de cota estourada em atualizar-cotacao | `claude-sonnet-5` | medium | `src/main/java/com/apiexternabackend/services/AcaoService.java`, `src/test/java/com/apiexternabackend/services/AcaoServiceTest.java`, `src/test/java/com/apiexternabackend/resources/AcaoResourceTest.java` |

#### faixa-2 — branch `spec/spec-01-padronizacao-erros-faixa-2` — worktree `../onp-worktrees/api-externa-backend-v1-spec-01-padronizacao-erros-faixa-2`

| tarefa | título | modelo | esforço | arquivos |
|---|---|---|---|---|
| T-409 | Migrar adapters de cotação (Brapi/TwelveData) para IntegracaoExternaException tipada (EXT-008/009/010) | `claude-sonnet-5` | medium | `src/main/java/com/apiexternabackend/infra/adapter/BrapiAdapter.java`, `src/main/java/com/apiexternabackend/infra/adapter/TwelveDataAdapter.java`, `src/main/java/com/apiexternabackend/config/FeignConfig.java` |

#### faixa-3 — branch `spec/spec-01-padronizacao-erros-faixa-3` — worktree `../onp-worktrees/api-externa-backend-v1-spec-01-padronizacao-erros-faixa-3`

| tarefa | título | modelo | esforço | arquivos |
|---|---|---|---|---|
| T-410 | Migrar catálogo de Corretora (COR-001/002/003) e separar CVM indisponível de CVM não autorizada (EXT-007) | `claude-sonnet-5` | medium | `src/main/java/com/apiexternabackend/services/CorretoraService.java`, `src/main/java/com/apiexternabackend/infra/facade/CnpjFacade.java`, `src/main/java/com/apiexternabackend/infra/facade/CepFacade.java`, `src/main/java/com/apiexternabackend/infra/facade/CvmFacade.java`, `src/main/java/com/apiexternabackend/config/CvmFeignConfig.java`, `src/test/java/com/apiexternabackend/services/CorretoraServiceTest.java`, `src/test/java/com/apiexternabackend/resources/CorretoraResourceTest.java`, `src/test/java/com/apiexternabackend/infra/facade/CnpjFacadeTest.java` |

### Onda 2 — faixa-4 ∥ faixa-5

#### faixa-4 — branch `spec/spec-01-padronizacao-erros-faixa-4` — worktree `../onp-worktrees/api-externa-backend-v1-spec-01-padronizacao-erros-faixa-4`

| tarefa | título | modelo | esforço | arquivos |
|---|---|---|---|---|
| T-411 | Migrar catálogo de Carteira (CAR-001) e Investidor/AUT (AUT-001/002/003) | `claude-sonnet-5` | medium | `src/main/java/com/apiexternabackend/services/CarteiraService.java`, `src/main/java/com/apiexternabackend/services/InvestidorService.java`, `src/test/java/com/apiexternabackend/services/CarteiraServiceTest.java`, `src/test/java/com/apiexternabackend/resources/CarteiraResourceTest.java`, `src/test/java/com/apiexternabackend/resources/InvestidorResourceTest.java` |

#### faixa-5 — branch `spec/spec-01-padronizacao-erros-faixa-5` — worktree `../onp-worktrees/api-externa-backend-v1-spec-01-padronizacao-erros-faixa-5`

| tarefa | título | modelo | esforço | arquivos |
|---|---|---|---|---|
| T-412 | Migrar catálogo de Operação (OPE-001/002/003/004) | `claude-sonnet-5` | medium | `src/main/java/com/apiexternabackend/services/OperacaoService.java`, `src/test/java/com/apiexternabackend/services/OperacaoServiceTest.java`, `src/test/java/com/apiexternabackend/resources/OperacaoResourceTest.java` |

## Tarefas sequenciais (após as ondas, na árvore principal)

| tarefa | título | modelo | esforço | por que sequencial |
|---|---|---|---|---|
| T-413 | Catálogo docs/erros.md e testes diretos do GlobalExceptionHandler (AC-426/427/428) | `claude-sonnet-5` | medium | fora da seleção do usuário |

## Gestão de branches e commits

1. branch de trabalho `spec/spec-01-padronizacao-erros` criada do ponto atual (se ainda não existir)
2. cada faixa nasce dela como branch própria e roda no seu worktree — **1 tarefa = 1 commit** (`T-xxx feature: título`)
3. terminou a onda → merge `--no-ff` de cada faixa de volta, na ordem; conflito interrompe a faixa e pede resolução humana
4. faixa mesclada → worktree removido, branch apagada, tarefa marcada `[concluida]` no tasks.md
5. gate final na branch de trabalho: `onp-spec verify spec-01-padronizacao-erros` + `onp-spec audit --ci` — **exit 0 ou não está pronto**

## Como executar

### ▶ Execução — Claude Code headless

```bash
bash .spec/features/spec-01-padronizacao-erros/executar-tarefas.sh
```

Cada faixa roda `claude -p` com **janela de contexto limpa**, no seu worktree, com
`--model` e `--effort` já definidos por tarefa e permissões `acceptEdits`. Os prompts exatos estão
embutidos no script — quer rodar uma faixa na mão, é só copiá-los de lá.
Logs: `../onp-worktrees/api-externa-backend-v1-spec-01-padronizacao-erros-logs/`.

### 📣 Acompanhamento — tabela + resumo no chat (a cada 1 min)

O script roda em **background**: o agente AVISA o usuário antes de iniciar e,
enquanto roda, posta no chat a cada ~1 minuto a **tabela de andamento** (qual
tarefa está rodando, qual não está, o que concluiu/falhou) junto com o
**resumo geral de andamento** (escrito por IA; sem IA, o motor resume). Ao
final, o usuário recebe o resumo completo da execução. A qualquer momento:

```bash
onp-spec resumo spec-01-padronizacao-erros --tabela   # a tabela de andamento
onp-spec resumo spec-01-padronizacao-erros            # o resumo em texto
```


# Plano de execução — investidor

> gerado por `onp-spec plano` em 2026-08-22 23:25 — NÃO edite à mão;
> mudou tasks.md ou a config? Regenere: `onp-spec plano investidor`

## Resumo — o que vai acontecer

- **7 tarefa(s) pendente(s)**: 7 em 7 faixa(s) paralela(s) + 0 sequencial(is)
- **1 faixa = 1 worktree + 1 branch + 1 janela de contexto limpa** — faixas não compartilham nenhum arquivo entre si
- prefere outra seleção ou uma após a outra? Regenere com `onp-spec plano investidor --paralelizar T-xxx,T-yyy` ou `--sequencial`
- tudo acontece na branch de trabalho `spec/investidor`; levar para a main é decisão sua

## Faixas e ondas

### Onda 1 — faixa-1 ∥ faixa-2 ∥ faixa-3

#### faixa-1 — branch `spec/investidor-faixa-1` — worktree `../onp-worktrees/api-externa-backend-v1-investidor-faixa-1`

| tarefa | título | modelo | esforço | arquivos |
|---|---|---|---|---|
| T-050 | Entidade Investidor, repositório e migration | `claude-sonnet-5` | medium | `src/main/java/com/apiexternabackend/domains/Investidor.java`, `src/main/java/com/apiexternabackend/repositories/InvestidorRepository.java`, `src/main/resources/db/migration/V1__create_investidor.sql` |

#### faixa-2 — branch `spec/investidor-faixa-2` — worktree `../onp-worktrees/api-externa-backend-v1-investidor-faixa-2`

| tarefa | título | modelo | esforço | arquivos |
|---|---|---|---|---|
| T-051 | DTOs e mapper do investidor | `claude-sonnet-5` | medium | `src/main/java/com/apiexternabackend/domains/dtos/InvestidorRequestDTO.java`, `src/main/java/com/apiexternabackend/domains/dtos/InvestidorResponseDTO.java`, `src/main/java/com/apiexternabackend/mappers/InvestidorMapper.java` |

#### faixa-3 — branch `spec/investidor-faixa-3` — worktree `../onp-worktrees/api-externa-backend-v1-investidor-faixa-3`

| tarefa | título | modelo | esforço | arquivos |
|---|---|---|---|---|
| T-052 | Tratamento de erro centralizado (RNF06) | `claude-sonnet-5` | medium | `src/main/java/com/apiexternabackend/resources/exceptions/GlobalExceptionHandler.java`, `src/main/java/com/apiexternabackend/resources/exceptions/StandardError.java`, `src/main/java/com/apiexternabackend/resources/exceptions/ResourceNotFoundException.java`, `src/main/java/com/apiexternabackend/resources/exceptions/DuplicateResourceException.java`, `src/main/java/com/apiexternabackend/resources/exceptions/BusinessException.java`, `src/main/java/com/apiexternabackend/resources/exceptions/ExternalServiceException.java` |

### Onda 2 — faixa-4 ∥ faixa-5 ∥ faixa-6

#### faixa-4 — branch `spec/investidor-faixa-4` — worktree `../onp-worktrees/api-externa-backend-v1-investidor-faixa-4`

| tarefa | título | modelo | esforço | arquivos |
|---|---|---|---|---|
| T-053 | Serviço do investidor | `claude-sonnet-5` | medium | `src/main/java/com/apiexternabackend/services/InvestidorService.java` |

#### faixa-5 — branch `spec/investidor-faixa-5` — worktree `../onp-worktrees/api-externa-backend-v1-investidor-faixa-5`

| tarefa | título | modelo | esforço | arquivos |
|---|---|---|---|---|
| T-054 | Controller do investidor | `claude-sonnet-5` | medium | `src/main/java/com/apiexternabackend/resources/InvestidorResource.java` |

#### faixa-6 — branch `spec/investidor-faixa-6` — worktree `../onp-worktrees/api-externa-backend-v1-investidor-faixa-6`

| tarefa | título | modelo | esforço | arquivos |
|---|---|---|---|---|
| T-055 | Testes do investidor | `claude-sonnet-5` | medium | `src/test/java/com/apiexternabackend/services/InvestidorServiceTest.java`, `src/test/java/com/apiexternabackend/resources/InvestidorResourceTest.java` |

### Onda 3 — faixa-7

#### faixa-7 — branch `spec/investidor-faixa-7` — worktree `../onp-worktrees/api-externa-backend-v1-investidor-faixa-7`

| tarefa | título | modelo | esforço | arquivos |
|---|---|---|---|---|
| T-056 | Configuração base do Feign | `claude-sonnet-5` | medium | `src/main/java/com/apiexternabackend/config/FeignConfig.java` |

## Gestão de branches e commits

1. branch de trabalho `spec/investidor` criada do ponto atual (se ainda não existir)
2. cada faixa nasce dela como branch própria e roda no seu worktree — **1 tarefa = 1 commit** (`T-xxx feature: título`)
3. terminou a onda → merge `--no-ff` de cada faixa de volta, na ordem; conflito interrompe a faixa e pede resolução humana
4. faixa mesclada → worktree removido, branch apagada, tarefa marcada `[concluida]` no tasks.md
5. gate final na branch de trabalho: `onp-spec verify investidor` + `onp-spec audit --ci` — **exit 0 ou não está pronto**

## Como executar

### ▶ Execução — Claude Code headless

```bash
bash .spec/features/investidor/executar-tarefas.sh
```

Cada faixa roda `claude -p` com **janela de contexto limpa**, no seu worktree, com
`--model` e `--effort` já definidos por tarefa e permissões `acceptEdits`. Os prompts exatos estão
embutidos no script — quer rodar uma faixa na mão, é só copiá-los de lá.
Logs: `../onp-worktrees/api-externa-backend-v1-investidor-logs/`.

### 📣 Acompanhamento — tabela + resumo no chat (a cada 1 min)

O script roda em **background**: o agente AVISA o usuário antes de iniciar e,
enquanto roda, posta no chat a cada ~1 minuto a **tabela de andamento** (qual
tarefa está rodando, qual não está, o que concluiu/falhou) junto com o
**resumo geral de andamento** (escrito por IA; sem IA, o motor resume). Ao
final, o usuário recebe o resumo completo da execução. A qualquer momento:

```bash
onp-spec resumo investidor --tabela   # a tabela de andamento
onp-spec resumo investidor            # o resumo em texto
```


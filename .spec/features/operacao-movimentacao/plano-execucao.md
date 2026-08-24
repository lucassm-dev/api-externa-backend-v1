# Plano de execução — operacao-movimentacao

> gerado por `onp-spec plano` em 2026-08-22 23:19 — NÃO edite à mão;
> mudou tasks.md ou a config? Regenere: `onp-spec plano operacao-movimentacao`

## Resumo — o que vai acontecer

- **7 tarefa(s) pendente(s)**: 7 em 7 faixa(s) paralela(s) + 0 sequencial(is)
- **1 faixa = 1 worktree + 1 branch + 1 janela de contexto limpa** — faixas não compartilham nenhum arquivo entre si
- prefere outra seleção ou uma após a outra? Regenere com `onp-spec plano operacao-movimentacao --paralelizar T-xxx,T-yyy` ou `--sequencial`
- tudo acontece na branch de trabalho `spec/operacao-movimentacao`; levar para a main é decisão sua

## Faixas e ondas

### Onda 1 — faixa-1 ∥ faixa-2 ∥ faixa-3

#### faixa-1 — branch `spec/operacao-movimentacao-faixa-1` — worktree `../onp-worktrees/api-externa-backend-v1-operacao-movimentacao-faixa-1`

| tarefa | título | modelo | esforço | arquivos |
|---|---|---|---|---|
| T-400 | Domínio de operação e posição, repositórios e migration | `claude-sonnet-5` | medium | `src/main/java/com/apiexternabackend/domains/Operacao.java`, `src/main/java/com/apiexternabackend/domains/enums/TipoOperacao.java`, `src/main/java/com/apiexternabackend/domains/CarteiraAcao.java`, `src/main/java/com/apiexternabackend/infra/converters/TipoOperacaoConverter.java`, `src/main/java/com/apiexternabackend/repositories/OperacaoRepository.java`, `src/main/java/com/apiexternabackend/repositories/CarteiraAcaoRepository.java`, `src/main/resources/db/migration/V6__create_operacao_posicao.sql` |

#### faixa-2 — branch `spec/operacao-movimentacao-faixa-2` — worktree `../onp-worktrees/api-externa-backend-v1-operacao-movimentacao-faixa-2`

| tarefa | título | modelo | esforço | arquivos |
|---|---|---|---|---|
| T-401 | DTOs e mappers de operação | `claude-sonnet-5` | medium | `src/main/java/com/apiexternabackend/domains/dtos/OperacaoRequestDTO.java`, `src/main/java/com/apiexternabackend/domains/dtos/OperacaoResponseDTO.java`, `src/main/java/com/apiexternabackend/domains/dtos/CarteiraAcaoResponseDTO.java`, `src/main/java/com/apiexternabackend/mappers/OperacaoMapper.java`, `src/main/java/com/apiexternabackend/mappers/CarteiraAcaoMapper.java` |

#### faixa-3 — branch `spec/operacao-movimentacao-faixa-3` — worktree `../onp-worktrees/api-externa-backend-v1-operacao-movimentacao-faixa-3`

| tarefa | título | modelo | esforço | arquivos |
|---|---|---|---|---|
| T-402 | Cálculo de posição e preço médio (recálculo do histórico) | `claude-sonnet-5` | medium | `src/main/java/com/apiexternabackend/services/PosicaoService.java` |

### Onda 2 — faixa-4 ∥ faixa-5 ∥ faixa-6

#### faixa-4 — branch `spec/operacao-movimentacao-faixa-4` — worktree `../onp-worktrees/api-externa-backend-v1-operacao-movimentacao-faixa-4`

| tarefa | título | modelo | esforço | arquivos |
|---|---|---|---|---|
| T-403 | Serviço de operação (compra e venda) | `claude-sonnet-5` | medium | `src/main/java/com/apiexternabackend/services/OperacaoService.java` |

#### faixa-5 — branch `spec/operacao-movimentacao-faixa-5` — worktree `../onp-worktrees/api-externa-backend-v1-operacao-movimentacao-faixa-5`

| tarefa | título | modelo | esforço | arquivos |
|---|---|---|---|---|
| T-404 | Consultas: histórico e rentabilidade não realizada | `claude-sonnet-5` | medium | `src/main/java/com/apiexternabackend/services/ConsultaOperacaoService.java` |

#### faixa-6 — branch `spec/operacao-movimentacao-faixa-6` — worktree `../onp-worktrees/api-externa-backend-v1-operacao-movimentacao-faixa-6`

| tarefa | título | modelo | esforço | arquivos |
|---|---|---|---|---|
| T-405 | Controller de operação | `claude-sonnet-5` | medium | `src/main/java/com/apiexternabackend/resources/OperacaoResource.java` |

### Onda 3 — faixa-7

#### faixa-7 — branch `spec/operacao-movimentacao-faixa-7` — worktree `../onp-worktrees/api-externa-backend-v1-operacao-movimentacao-faixa-7`

| tarefa | título | modelo | esforço | arquivos |
|---|---|---|---|---|
| T-406 | Testes de operação | `claude-sonnet-5` | medium | `src/test/java/com/apiexternabackend/services/OperacaoServiceTest.java`, `src/test/java/com/apiexternabackend/services/PosicaoServiceTest.java`, `src/test/java/com/apiexternabackend/resources/OperacaoResourceTest.java` |

## Gestão de branches e commits

1. branch de trabalho `spec/operacao-movimentacao` criada do ponto atual (se ainda não existir)
2. cada faixa nasce dela como branch própria e roda no seu worktree — **1 tarefa = 1 commit** (`T-xxx feature: título`)
3. terminou a onda → merge `--no-ff` de cada faixa de volta, na ordem; conflito interrompe a faixa e pede resolução humana
4. faixa mesclada → worktree removido, branch apagada, tarefa marcada `[concluida]` no tasks.md
5. gate final na branch de trabalho: `onp-spec verify operacao-movimentacao` + `onp-spec audit --ci` — **exit 0 ou não está pronto**

## Como executar

### ▶ Execução — Claude Code headless

```bash
bash .spec/features/operacao-movimentacao/executar-tarefas.sh
```

Cada faixa roda `claude -p` com **janela de contexto limpa**, no seu worktree, com
`--model` e `--effort` já definidos por tarefa e permissões `acceptEdits`. Os prompts exatos estão
embutidos no script — quer rodar uma faixa na mão, é só copiá-los de lá.
Logs: `../onp-worktrees/api-externa-backend-v1-operacao-movimentacao-logs/`.

### 📣 Acompanhamento — tabela + resumo no chat (a cada 1 min)

O script roda em **background**: o agente AVISA o usuário antes de iniciar e,
enquanto roda, posta no chat a cada ~1 minuto a **tabela de andamento** (qual
tarefa está rodando, qual não está, o que concluiu/falhou) junto com o
**resumo geral de andamento** (escrito por IA; sem IA, o motor resume). Ao
final, o usuário recebe o resumo completo da execução. A qualquer momento:

```bash
onp-spec resumo operacao-movimentacao --tabela   # a tabela de andamento
onp-spec resumo operacao-movimentacao            # o resumo em texto
```


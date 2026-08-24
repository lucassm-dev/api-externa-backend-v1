# Plano de execução — carteira

> gerado por `onp-spec plano` em 2026-08-22 23:19 — NÃO edite à mão;
> mudou tasks.md ou a config? Regenere: `onp-spec plano carteira`

## Resumo — o que vai acontecer

- **5 tarefa(s) pendente(s)**: 5 em 5 faixa(s) paralela(s) + 0 sequencial(is)
- **1 faixa = 1 worktree + 1 branch + 1 janela de contexto limpa** — faixas não compartilham nenhum arquivo entre si
- prefere outra seleção ou uma após a outra? Regenere com `onp-spec plano carteira --paralelizar T-xxx,T-yyy` ou `--sequencial`
- tudo acontece na branch de trabalho `spec/carteira`; levar para a main é decisão sua

## Faixas e ondas

### Onda 1 — faixa-1 ∥ faixa-2 ∥ faixa-3

#### faixa-1 — branch `spec/carteira-faixa-1` — worktree `../onp-worktrees/api-externa-backend-v1-carteira-faixa-1`

| tarefa | título | modelo | esforço | arquivos |
|---|---|---|---|---|
| T-300 | Entidade Carteira, repositório e migration | `claude-sonnet-5` | medium | `src/main/java/com/apiexternabackend/domains/Carteira.java`, `src/main/java/com/apiexternabackend/repositories/CarteiraRepository.java`, `src/main/resources/db/migration/V5__create_carteira.sql` |

#### faixa-2 — branch `spec/carteira-faixa-2` — worktree `../onp-worktrees/api-externa-backend-v1-carteira-faixa-2`

| tarefa | título | modelo | esforço | arquivos |
|---|---|---|---|---|
| T-301 | DTOs e mapper da carteira | `claude-sonnet-5` | medium | `src/main/java/com/apiexternabackend/domains/dtos/CarteiraRequestDTO.java`, `src/main/java/com/apiexternabackend/domains/dtos/CarteiraResponseDTO.java`, `src/main/java/com/apiexternabackend/mappers/CarteiraMapper.java` |

#### faixa-3 — branch `spec/carteira-faixa-3` — worktree `../onp-worktrees/api-externa-backend-v1-carteira-faixa-3`

| tarefa | título | modelo | esforço | arquivos |
|---|---|---|---|---|
| T-302 | Serviço da carteira | `claude-sonnet-5` | medium | `src/main/java/com/apiexternabackend/services/CarteiraService.java` |

### Onda 2 — faixa-4 ∥ faixa-5

#### faixa-4 — branch `spec/carteira-faixa-4` — worktree `../onp-worktrees/api-externa-backend-v1-carteira-faixa-4`

| tarefa | título | modelo | esforço | arquivos |
|---|---|---|---|---|
| T-303 | Controller da carteira | `claude-sonnet-5` | medium | `src/main/java/com/apiexternabackend/resources/CarteiraResource.java` |

#### faixa-5 — branch `spec/carteira-faixa-5` — worktree `../onp-worktrees/api-externa-backend-v1-carteira-faixa-5`

| tarefa | título | modelo | esforço | arquivos |
|---|---|---|---|---|
| T-304 | Testes da carteira | `claude-sonnet-5` | medium | `src/test/java/com/apiexternabackend/services/CarteiraServiceTest.java`, `src/test/java/com/apiexternabackend/resources/CarteiraResourceTest.java` |

## Gestão de branches e commits

1. branch de trabalho `spec/carteira` criada do ponto atual (se ainda não existir)
2. cada faixa nasce dela como branch própria e roda no seu worktree — **1 tarefa = 1 commit** (`T-xxx feature: título`)
3. terminou a onda → merge `--no-ff` de cada faixa de volta, na ordem; conflito interrompe a faixa e pede resolução humana
4. faixa mesclada → worktree removido, branch apagada, tarefa marcada `[concluida]` no tasks.md
5. gate final na branch de trabalho: `onp-spec verify carteira` + `onp-spec audit --ci` — **exit 0 ou não está pronto**

## Como executar

### ▶ Execução — Claude Code headless

```bash
bash .spec/features/carteira/executar-tarefas.sh
```

Cada faixa roda `claude -p` com **janela de contexto limpa**, no seu worktree, com
`--model` e `--effort` já definidos por tarefa e permissões `acceptEdits`. Os prompts exatos estão
embutidos no script — quer rodar uma faixa na mão, é só copiá-los de lá.
Logs: `../onp-worktrees/api-externa-backend-v1-carteira-logs/`.

### 📣 Acompanhamento — tabela + resumo no chat (a cada 1 min)

O script roda em **background**: o agente AVISA o usuário antes de iniciar e,
enquanto roda, posta no chat a cada ~1 minuto a **tabela de andamento** (qual
tarefa está rodando, qual não está, o que concluiu/falhou) junto com o
**resumo geral de andamento** (escrito por IA; sem IA, o motor resume). Ao
final, o usuário recebe o resumo completo da execução. A qualquer momento:

```bash
onp-spec resumo carteira --tabela   # a tabela de andamento
onp-spec resumo carteira            # o resumo em texto
```


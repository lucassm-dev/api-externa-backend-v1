# Plano de execução — cadastro-corretora

> gerado por `onp-spec plano` em 2026-08-22 23:25 — NÃO edite à mão;
> mudou tasks.md ou a config? Regenere: `onp-spec plano cadastro-corretora`

## Resumo — o que vai acontecer

- **9 tarefa(s) pendente(s)**: 9 em 9 faixa(s) paralela(s) + 0 sequencial(is)
- **1 faixa = 1 worktree + 1 branch + 1 janela de contexto limpa** — faixas não compartilham nenhum arquivo entre si
- prefere outra seleção ou uma após a outra? Regenere com `onp-spec plano cadastro-corretora --paralelizar T-xxx,T-yyy` ou `--sequencial`
- tudo acontece na branch de trabalho `spec/cadastro-corretora`; levar para a main é decisão sua

## Faixas e ondas

### Onda 1 — faixa-1 ∥ faixa-2 ∥ faixa-3

#### faixa-1 — branch `spec/cadastro-corretora-faixa-1` — worktree `../onp-worktrees/api-externa-backend-v1-cadastro-corretora-faixa-1`

| tarefa | título | modelo | esforço | arquivos |
|---|---|---|---|---|
| T-100 | Entidade Corretora, repositório e migration | `claude-sonnet-5` | medium | `src/main/java/com/apiexternabackend/domains/Corretora.java`, `src/main/java/com/apiexternabackend/repositories/CorretoraRepository.java`, `src/main/resources/db/migration/V2__create_corretora.sql` |

#### faixa-2 — branch `spec/cadastro-corretora-faixa-2` — worktree `../onp-worktrees/api-externa-backend-v1-cadastro-corretora-faixa-2`

| tarefa | título | modelo | esforço | arquivos |
|---|---|---|---|---|
| T-101 | DTOs e mapper da corretora | `claude-sonnet-5` | medium | `src/main/java/com/apiexternabackend/domains/dtos/CorretoraRequestDTO.java`, `src/main/java/com/apiexternabackend/domains/dtos/CorretoraResponseDTO.java`, `src/main/java/com/apiexternabackend/mappers/CorretoraMapper.java` |

#### faixa-3 — branch `spec/cadastro-corretora-faixa-3` — worktree `../onp-worktrees/api-externa-backend-v1-cadastro-corretora-faixa-3`

| tarefa | título | modelo | esforço | arquivos |
|---|---|---|---|---|
| T-103 | Integração BrasilAPI (CNPJ) | `claude-sonnet-5` | medium | `src/main/java/com/apiexternabackend/infra/client/cnpj/CnpjClient.java`, `src/main/java/com/apiexternabackend/infra/client/cnpj/dto/CnpjResponseDTO.java`, `src/main/java/com/apiexternabackend/infra/facade/CnpjFacade.java` |

### Onda 2 — faixa-4 ∥ faixa-5 ∥ faixa-6

#### faixa-4 — branch `spec/cadastro-corretora-faixa-4` — worktree `../onp-worktrees/api-externa-backend-v1-cadastro-corretora-faixa-4`

| tarefa | título | modelo | esforço | arquivos |
|---|---|---|---|---|
| T-104 | Integração ViaCEP (CEP) | `claude-sonnet-5` | medium | `src/main/java/com/apiexternabackend/infra/client/cep/CepClient.java`, `src/main/java/com/apiexternabackend/infra/client/cep/dto/CepResponseDTO.java`, `src/main/java/com/apiexternabackend/infra/facade/CepFacade.java` |

#### faixa-5 — branch `spec/cadastro-corretora-faixa-5` — worktree `../onp-worktrees/api-externa-backend-v1-cadastro-corretora-faixa-5`

| tarefa | título | modelo | esforço | arquivos |
|---|---|---|---|---|
| T-105 | Ingestão da base da CVM | `claude-sonnet-5` | medium | `src/main/java/com/apiexternabackend/domains/CvmParticipante.java`, `src/main/java/com/apiexternabackend/repositories/CvmParticipanteRepository.java`, `src/main/resources/db/migration/V3__create_cvm_participante.sql`, `src/main/java/com/apiexternabackend/infra/client/cvm/CvmCorretoraClient.java`, `src/main/java/com/apiexternabackend/infra/client/cvm/dto/CvmCorretoraResponseDTO.java`, `src/main/java/com/apiexternabackend/services/CvmIngestaoService.java`, `src/main/java/com/apiexternabackend/config/CvmIngestaoScheduler.java` |

#### faixa-6 — branch `spec/cadastro-corretora-faixa-6` — worktree `../onp-worktrees/api-externa-backend-v1-cadastro-corretora-faixa-6`

| tarefa | título | modelo | esforço | arquivos |
|---|---|---|---|---|
| T-106 | Verificação de autorização na CVM (facade) | `claude-sonnet-5` | medium | `src/main/java/com/apiexternabackend/infra/facade/CvmFacade.java` |

### Onda 3 — faixa-7 ∥ faixa-8 ∥ faixa-9

#### faixa-7 — branch `spec/cadastro-corretora-faixa-7` — worktree `../onp-worktrees/api-externa-backend-v1-cadastro-corretora-faixa-7`

| tarefa | título | modelo | esforço | arquivos |
|---|---|---|---|---|
| T-107 | Serviço da corretora | `claude-sonnet-5` | medium | `src/main/java/com/apiexternabackend/services/CorretoraService.java` |

#### faixa-8 — branch `spec/cadastro-corretora-faixa-8` — worktree `../onp-worktrees/api-externa-backend-v1-cadastro-corretora-faixa-8`

| tarefa | título | modelo | esforço | arquivos |
|---|---|---|---|---|
| T-108 | Controller da corretora | `claude-sonnet-5` | medium | `src/main/java/com/apiexternabackend/resources/CorretoraResource.java` |

#### faixa-9 — branch `spec/cadastro-corretora-faixa-9` — worktree `../onp-worktrees/api-externa-backend-v1-cadastro-corretora-faixa-9`

| tarefa | título | modelo | esforço | arquivos |
|---|---|---|---|---|
| T-109 | Testes da corretora | `claude-sonnet-5` | medium | `src/test/java/com/apiexternabackend/services/CorretoraServiceTest.java`, `src/test/java/com/apiexternabackend/infra/facade/CvmFacadeTest.java`, `src/test/java/com/apiexternabackend/resources/CorretoraResourceTest.java` |

## Gestão de branches e commits

1. branch de trabalho `spec/cadastro-corretora` criada do ponto atual (se ainda não existir)
2. cada faixa nasce dela como branch própria e roda no seu worktree — **1 tarefa = 1 commit** (`T-xxx feature: título`)
3. terminou a onda → merge `--no-ff` de cada faixa de volta, na ordem; conflito interrompe a faixa e pede resolução humana
4. faixa mesclada → worktree removido, branch apagada, tarefa marcada `[concluida]` no tasks.md
5. gate final na branch de trabalho: `onp-spec verify cadastro-corretora` + `onp-spec audit --ci` — **exit 0 ou não está pronto**

## Como executar

### ▶ Execução — Claude Code headless

```bash
bash .spec/features/cadastro-corretora/executar-tarefas.sh
```

Cada faixa roda `claude -p` com **janela de contexto limpa**, no seu worktree, com
`--model` e `--effort` já definidos por tarefa e permissões `acceptEdits`. Os prompts exatos estão
embutidos no script — quer rodar uma faixa na mão, é só copiá-los de lá.
Logs: `../onp-worktrees/api-externa-backend-v1-cadastro-corretora-logs/`.

### 📣 Acompanhamento — tabela + resumo no chat (a cada 1 min)

O script roda em **background**: o agente AVISA o usuário antes de iniciar e,
enquanto roda, posta no chat a cada ~1 minuto a **tabela de andamento** (qual
tarefa está rodando, qual não está, o que concluiu/falhou) junto com o
**resumo geral de andamento** (escrito por IA; sem IA, o motor resume). Ao
final, o usuário recebe o resumo completo da execução. A qualquer momento:

```bash
onp-spec resumo cadastro-corretora --tabela   # a tabela de andamento
onp-spec resumo cadastro-corretora            # o resumo em texto
```


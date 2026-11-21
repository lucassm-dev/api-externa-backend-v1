# Plano de execução — spec-18-gerenciar-usuarios

> gerado por `onp-spec plano` em 2026-11-21 19:13 — NÃO edite à mão;
> mudou tasks.md ou a config? Regenere: `onp-spec plano spec-18-gerenciar-usuarios`

## Resumo — o que vai acontecer

- **4 tarefa(s) pendente(s)**: 4 em 1 faixa(s) paralela(s) + 0 sequencial(is)
- **1 faixa = 1 worktree + 1 branch + 1 janela de contexto limpa** — faixas não compartilham nenhum arquivo entre si
- prefere outra seleção ou uma após a outra? Regenere com `onp-spec plano spec-18-gerenciar-usuarios --paralelizar T-xxx,T-yyy` ou `--sequencial`
- tudo acontece na branch de trabalho `spec/spec-18-gerenciar-usuarios`; levar para a main é decisão sua

## Faixas e ondas

### Onda 1 — faixa-1

#### faixa-1 — branch `spec/spec-18-gerenciar-usuarios-faixa-1` — worktree `../onp-worktrees/api-externa-backend-v1-spec-18-gerenciar-usuarios-faixa-1`

| tarefa | título | modelo | esforço | arquivos |
|---|---|---|---|---|
| T-505 | Bloquear e desbloquear usuário | `claude-sonnet-5` | medium | `src/main/resources/db/migration/V18__bloqueio_usuario.sql`, `src/main/java/com/apiexternabackend/domains/Usuario.java`, `src/main/java/com/apiexternabackend/domains/dtos/BloqueioRequestDTO.java`, `src/main/java/com/apiexternabackend/domains/dtos/UsuarioAdminResponseDTO.java`, `src/main/java/com/apiexternabackend/services/UsuarioAdminService.java`, `src/main/java/com/apiexternabackend/resources/UsuarioAdminResource.java`, `docs/erros.md`, `src/test/java/com/apiexternabackend/services/UsuarioAdminServiceTest.java`, `src/test/java/com/apiexternabackend/config/GestaoUsuariosIntegrationTest.java` |
| T-506 | Bloqueio vale na requisição seguinte | `claude-sonnet-5` | medium | `src/main/java/com/apiexternabackend/config/SecurityConfig.java`, `src/main/java/com/apiexternabackend/config/JwtAuthenticationEntryPoint.java`, `src/main/java/com/apiexternabackend/repositories/UsuarioRepository.java`, `docs/erros.md`, `src/test/java/com/apiexternabackend/config/BloqueioImediatoIntegrationTest.java` |
| T-507 | Listagem com indicadores | `claude-sonnet-5` | medium | `src/main/java/com/apiexternabackend/services/UsuarioAdminService.java`, `src/main/java/com/apiexternabackend/resources/UsuarioAdminResource.java`, `src/main/java/com/apiexternabackend/domains/dtos/UsuarioAdminResponseDTO.java`, `src/main/java/com/apiexternabackend/repositories/UsuarioRepository.java`, `src/main/java/com/apiexternabackend/repositories/CarteiraRepository.java`, `src/main/java/com/apiexternabackend/repositories/CarteiraAcaoRepository.java`, `src/test/java/com/apiexternabackend/config/GestaoUsuariosIntegrationTest.java` |
| T-508 | Criar ADMIN | `claude-sonnet-5` | medium | `src/main/java/com/apiexternabackend/domains/dtos/CriarAdminRequestDTO.java`, `src/main/java/com/apiexternabackend/services/UsuarioAdminService.java`, `src/main/java/com/apiexternabackend/resources/UsuarioAdminResource.java`, `src/test/java/com/apiexternabackend/services/UsuarioAdminServiceTest.java`, `src/test/java/com/apiexternabackend/config/GestaoUsuariosIntegrationTest.java` |

## Gestão de branches e commits

1. branch de trabalho `spec/spec-18-gerenciar-usuarios` criada do ponto atual (se ainda não existir)
2. cada faixa nasce dela como branch própria e roda no seu worktree — **1 tarefa = 1 commit** (`T-xxx feature: título`)
3. terminou a onda → merge `--no-ff` de cada faixa de volta, na ordem; conflito interrompe a faixa e pede resolução humana
4. faixa mesclada → worktree removido, branch apagada, tarefa marcada `[concluida]` no tasks.md
5. gate final na branch de trabalho: `onp-spec verify spec-18-gerenciar-usuarios` + `onp-spec audit --ci` — **exit 0 ou não está pronto**

## Como executar

### ▶ Execução — Claude Code headless

```bash
bash .spec/features/spec-18-gerenciar-usuarios/executar-tarefas.sh
```

Cada faixa roda `claude -p` com **janela de contexto limpa**, no seu worktree, com
`--model` e `--effort` já definidos por tarefa e permissões `acceptEdits`. Os prompts exatos estão
embutidos no script — quer rodar uma faixa na mão, é só copiá-los de lá.
Logs: `../onp-worktrees/api-externa-backend-v1-spec-18-gerenciar-usuarios-logs/`.

### 📣 Acompanhamento — tabela + resumo no chat (a cada 1 min)

O script roda em **background**: o agente AVISA o usuário antes de iniciar e,
enquanto roda, posta no chat a cada ~1 minuto a **tabela de andamento** (qual
tarefa está rodando, qual não está, o que concluiu/falhou) junto com o
**resumo geral de andamento** (escrito por IA; sem IA, o motor resume). Ao
final, o usuário recebe o resumo completo da execução. A qualquer momento:

```bash
onp-spec resumo spec-18-gerenciar-usuarios --tabela   # a tabela de andamento
onp-spec resumo spec-18-gerenciar-usuarios            # o resumo em texto
```


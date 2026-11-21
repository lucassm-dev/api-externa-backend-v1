# Plano de execução: spec-17-auth-me

Harness: **claude** · modo: **sequencial** · execução: api-externa-backend-v1-spec-17-auth-me-mwpxwzcd-ab901e8c

Use o plano/tarefas, perguntas, agentes, timers e notificações NATIVOS disponíveis nesta sessão.
Confirme a seleção de tarefas e o custo quando ainda não autorizados. Modelos são resolvidos no catálogo atual do harness; nenhum nome é presumido.

| faixa | tarefa | modelo solicitado | esforço | arquivos |
|---|---|---|---|---|
| sequencial | T-504 — Rota GET /auth/me | padrão disponível do harness | medium | src/main/java/com/apiexternabackend/config/SecurityConfig.java, src/main/java/com/apiexternabackend/resources/AuthResource.java, src/main/java/com/apiexternabackend/services/AutenticacaoService.java, src/main/java/com/apiexternabackend/domains/dtos/UsuarioLogadoResponseDTO.java, src/test/java/com/apiexternabackend/config/AuthMeIntegrationTest.java |

## Execução

`node .spec/features/spec-17-auth-me/executar-tarefas.mjs`

Opções: --listar, --faixa <id>, --seq <T-xxx>, --gate. Retomadas preservam commits, alterações parciais e sessões registradas.
Cada faixa usa worktree e branch próprios; merge é serial e preserva conflitos para revisão. Tarefas só concluem com commit, árvore limpa e gate verificável.
Antes de implementar, revise o impacto em PRDs, ADRs e domínios anteriores. Conflitos pendentes bloqueiam a execução até resposta explícita do cliente.
A 5% ou menos restantes, o executor aguarda o reset e consulta a cota novamente. Sem telemetria inicial, exige uma ponte nativa antes de iniciar; após bloqueio, preserva a espera até leitura fresca. Nunca inventa um saldo.
Progresso sai como eventos no terminal/ledger para o harness nativo. Notificações dependem da preferência do usuário e da capacidade disponível.

## Encerramento

Rode `onp-spec verify spec-17-auth-me` e `onp-spec audit --ci`.
Pergunte com a ferramenta nativa se o usuário quer limpar os worktrees usados. Liste com `onp-spec worktrees spec-17-auth-me`; remova somente após resposta afirmativa, com `--limpar --confirmar`.
Worktrees sujos, ativos ou não integrados são preservados. Não apague branches automaticamente.

# Plano de execução: spec-16-papeis-matriz-acesso

Harness: **claude** · modo: **sequencial** · execução: api-externa-backend-v1-spec-16-papeis-matriz-acesso-mwpzdxnl-ddaf796b

Use o plano/tarefas, perguntas, agentes, timers e notificações NATIVOS disponíveis nesta sessão.
Confirme a seleção de tarefas e o custo quando ainda não autorizados. Modelos são resolvidos no catálogo atual do harness; nenhum nome é presumido.

| faixa | tarefa | modelo solicitado | esforço | arquivos |
|---|---|---|---|---|
| sequencial | T-501 — Papel real no token e login do ADMIN | padrão disponível do harness | medium | src/main/java/com/apiexternabackend/config/JwtService.java, src/main/java/com/apiexternabackend/services/AutenticacaoService.java, src/test/java/com/apiexternabackend/services/AutenticacaoServiceTest.java, src/test/java/com/apiexternabackend/config/IdentidadeUsuarioIntegrationTest.java, src/test/java/com/apiexternabackend/config/AuthMeIntegrationTest.java |
| sequencial | T-502 — Matriz de acesso nas rotas | padrão disponível do harness | high | src/main/java/com/apiexternabackend/config/SecurityConfig.java, src/main/java/com/apiexternabackend/resources/CorretoraResource.java, src/main/java/com/apiexternabackend/resources/AcaoResource.java, src/main/java/com/apiexternabackend/resources/MercadoResource.java, src/main/java/com/apiexternabackend/resources/CarteiraResource.java, src/main/java/com/apiexternabackend/resources/OperacaoResource.java, src/main/java/com/apiexternabackend/resources/InvestidorResource.java, src/test/java/com/apiexternabackend/config/MatrizAcessoIntegrationTest.java, src/test/java/com/apiexternabackend/resources/CorretoraResourceTest.java, src/test/java/com/apiexternabackend/resources/AcaoResourceTest.java, src/test/java/com/apiexternabackend/resources/MercadoResourceTest.java, src/test/java/com/apiexternabackend/resources/CarteiraResourceTest.java, src/test/java/com/apiexternabackend/resources/OperacaoResourceTest.java, src/test/java/com/apiexternabackend/resources/InvestidorResourceTest.java |
| sequencial | T-503 — Primeiro ADMIN pelas variáveis de ambiente | padrão disponível do harness | medium | src/main/java/com/apiexternabackend/config/AdminInicial.java, src/main/java/com/apiexternabackend/repositories/UsuarioRepository.java, src/main/resources/application.properties, .env.example, src/test/java/com/apiexternabackend/config/AdminInicialTest.java |

## Execução

`node .spec/features/spec-16-papeis-matriz-acesso/execucoes/api-externa-backend-v1-spec-16-papeis-matriz-acesso-mwpzdxnl-ddaf796b/executar-tarefas.mjs`

Opções: --listar, --faixa <id>, --seq <T-xxx>, --gate. Retomadas preservam commits, alterações parciais e sessões registradas.
Cada faixa usa worktree e branch próprios; merge é serial e preserva conflitos para revisão. Tarefas só concluem com commit, árvore limpa e gate verificável.
Antes de implementar, revise o impacto em PRDs, ADRs e domínios anteriores. Conflitos pendentes bloqueiam a execução até resposta explícita do cliente.
A 5% ou menos restantes, o executor aguarda o reset e consulta a cota novamente. Sem telemetria inicial, exige uma ponte nativa antes de iniciar; após bloqueio, preserva a espera até leitura fresca. Nunca inventa um saldo.
Progresso sai como eventos no terminal/ledger para o harness nativo. Notificações dependem da preferência do usuário e da capacidade disponível.

## Encerramento

Rode `onp-spec verify spec-16-papeis-matriz-acesso` e `onp-spec audit --ci`.
Pergunte com a ferramenta nativa se o usuário quer limpar os worktrees usados. Liste com `onp-spec worktrees spec-16-papeis-matriz-acesso`; remova somente após resposta afirmativa, com `--limpar --confirmar`.
Worktrees sujos, ativos ou não integrados são preservados. Não apague branches automaticamente.

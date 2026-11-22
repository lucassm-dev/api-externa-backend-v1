<!-- onp-spec:documentation-contract=v1 -->

# TDD: Registro de uso da API por usuário

> feature: spec-28-registro-uso-api
> status: revisado

[PRD](prd.md) · [ADR](adr.md) · [Spec](spec.md) · [Tarefas](tasks.md) · [Pesquisa](research.md)

## 1. Identificação e responsáveis

Autor: Claude Code (sessão de 2026-11-22). Responsável e dono do produto: Lucas
Mendes. Versão 1. Issue #34.

## 2. Contexto e sistema atual

A autenticação é feita pelo resource server do Spring Security
([ADR-011](../../../docs/adr/011-jwt-pelo-resource-server-do-spring.md)):
`SecurityConfig.autenticacaoDe` monta um `InvestidorPrincipal(id)` por
requisição. As ações administrativas já têm registro próprio
(`registro_administrativo`, spec-19), síncrono e dentro da transação da ação.
Não existe `@EnableAsync` nem `@EnableScheduling` no projeto.

## 3. Problema e motivação

O relatório de picos (#36, PRD 15.5) precisa de uma linha por requisição
autenticada. Sem esta feature, o #36 não tem de onde ler.

## 4. Escopo e não objetivos

Entrega: tabela `registro_uso`, filtro que grava, serviço assíncrono e job de
descarte (AC-651 a AC-656). Não objetivos: endpoint de consulta, agregação,
rate limit, registro de requisições sem token.

## 5. Solução e arquitetura

```text
requisição ─► BearerTokenAuthenticationFilter ─► AuthorizationFilter ─► RegistroUsoFilter ─► DispatcherServlet
                                                                              │ (depois da resposta)
                                                                              └─► RegistroUsoService.registrar  (@Async)
                                                                                     └─► INSERT registro_uso
job @Scheduled 03:00 America/Sao_Paulo ─► RegistroUsoService.descartarAntigos ─► DELETE ... WHERE registrado_em < agora - 90 dias
```

- `RegistroUsoFilter` (`OncePerRequestFilter`, criado em `SecurityConfig`, sem
  `@Component` para não ser registrado de novo como filtro do servlet). Antes de
  seguir, lê o `InvestidorPrincipal` do `SecurityContextHolder`; sem ele, só
  segue (AC-653). Depois de `chain.doFilter`, pega o padrão da rota
  (`BEST_MATCHING_PATTERN_ATTRIBUTE`, só quando o handler é um `HandlerMethod`;
  senão `desconhecida`, ASM-507), o método e `response.getStatus()` e chama o
  serviço. O 403 do `@PreAuthorize` sobe como `AccessDeniedException` (embrulhada
  pelo `DispatcherServlet`) até o `ExceptionTranslationFilter`, que fica antes
  deste filtro; o filtro grava com 403 e relança (ASM-506).
- `RegistroUsoService.registrar(RegistroUso)`: `@Async`. Uma exceção na
  gravação fica na thread do executor e é logada pelo
  `SimpleAsyncUncaughtExceptionHandler` do Spring, sem os dados da requisição (AC-655).
- `RegistroUso`: entidade `@Immutable`, construtor com todos os campos.
- `RegistroUsoRepository extends Repository`: `save`, `count`, `findByUsuarioId` e
  `apagarAnterioresA` (`@Modifying @Query("delete ... where registradoEm < :limite")`).

Modelo de dados (Flyway `V24__registro_uso.sql`):

| Coluna | Tipo | Observação |
|---|---|---|
| id | BIGSERIAL PK | |
| usuario_id | BIGINT NOT NULL | FK para `usuario(id)` |
| metodo | VARCHAR(10) NOT NULL | GET, POST… |
| rota | VARCHAR(200) NOT NULL | padrão, ex. `/carteiras/{id}/posicoes` |
| status | INT NOT NULL | status HTTP da resposta |
| registrado_em | TIMESTAMP NOT NULL | índice, para o descarte e para o #36 |

## 6. Riscos e mitigação

| Risco | Evidência / hipótese | Probabilidade | Impacto | Mitigação | Responsável |
|---|---|---|---|---|---|
| Fila do executor cresce sob pico | Executor padrão do Boot tem fila sem limite | Baixa (uso de homelab) | Memória | Aceito; comentário `shortcut:` no código para trocar por fila limitada se o volume crescer | Dev |
| Perda de registros na queda do processo | Gravação em memória antes do banco | Baixa | Relatório com buracos pequenos | Aceito no ADR | Dono do produto |
| Vazar dado pela URL | Query string e ids na URL | Média se gravar a URL crua | LGPD | Grava só o padrão da rota (AC-652, ASM-507) | Dev |
| Job apagar demais | Erro de data | Baixa | Perda de dado | Teste de fronteira 89/91 dias (AC-656) | Dev |

## 7. Plano de implementação

Sequencial, porque as três tarefas mexem no mesmo serviço e no mesmo teste:
T-522 (tabela, entidade, filtro) → T-523 (assíncrono e tolerante a falha) →
T-524 (descarte). Um ciclo vermelho/verde com commit por critério.

## 8. Segurança e privacidade

Grava só id do usuário, método, padrão da rota, status e data e hora. Não grava
corpo, query string, cabeçalhos, token, senha nem IP (AC-652). Retenção de 90
dias (AC-656) atende à necessidade da LGPD. Ninguém lê a tabela pela API nesta
feature; o acesso do administrador vem no #36, com `hasRole('ADMIN')`.

## 9. Estratégia de testes

`RegistroUsoIntegrationTest` (`@SpringBootTest`, H2, cadeia de segurança e JWT
reais), um teste por critério com `@spec:AC-xxx` no `@DisplayName`:

- AC-651/652/653: chamadas reais via MockMvc e leitura da tabela com espera curta (gravação assíncrona).
- AC-654: um trigger H2 só de teste (`RegistroUsoTrigger`) trava o `INSERT` num `CountDownLatch`; a resposta tem de chegar em menos de 2 s.
- AC-655: o mesmo trigger derruba o `INSERT`; a resposta continua 200 e nada é gravado.
- ASM-506 (403) e ASM-507 (rota inexistente) têm testes próprios, anotados com AC-651 e AC-652.

Lição da implementação: o cache de resultado por sessão do H2 devolvia a
consulta antiga (vazia) depois do `INSERT` feito pela thread da gravação (3 de
12 execuções falhavam). O perfil de teste usa `QUERY_CACHE_SIZE=0` (0 de 16).
- AC-656: grava registros com 91 e 89 dias e chama o descarte direto.

Regressão: a suíte inteira (`./mvnw test`) continua verde.

## 10. Operação e observabilidade

Falha de gravação é logada em ERROR pelo `SimpleAsyncUncaughtExceptionHandler`
com a exceção e o método, sem dados da requisição.
O job loga quantos registros apagou. Diagnóstico: `SELECT count(*) FROM
registro_uso GROUP BY date(registrado_em)`.

## 11. Reversão e recuperação

Gatilho: aumento de latência ou erro ligado ao filtro. Reverter o commit remove
o filtro; a tabela pode ficar (não é lida por ninguém) ou ser apagada por uma
nova migration. Nenhum dado existente é alterado.

## 12. Métricas de sucesso

Linhas por dia próximas do número de requisições autenticadas; zero linhas com
mais de 90 dias depois das 3h. Sem dado de base antes do deploy.

## 13. Glossário e domínios

- Registro de uso: uma linha por requisição autenticada.
- Padrão da rota: o caminho declarado no controller, com `{id}` no lugar do valor.
- Domínios: autenticação (spec-14, spec-16) e administração (spec-19, #36).

## 14. Alternativas e decisões

Ver [ADR](adr.md#alternativas-consideradas): filtro assíncrono escolhido em vez
de gravação síncrona, interceptor MVC ou log em arquivo. Nada pendente.

## 15. Dependências e compatibilidade

Spring Boot 3.3.5 (`@EnableAsync`, `@EnableScheduling`, executor
`applicationTaskExecutor` auto-configurado), PostgreSQL em produção, H2 nos
testes. Sem dependência nova.

## 16. Performance e capacidade

Orçamento: a requisição paga só a montagem de um objeto e a entrega para o
executor (microssegundos); o `INSERT` (estimado em ~1 ms no PostgreSQL local)
roda fora dela. Volume estimado em ~900 mil linhas em 90 dias para 50 usuários
(ver [pesquisa](research.md#viabilidade-matemática-e-estatística)). Não houve
medição antes/depois; o AC-654 prova o desacoplamento, não um número.

## 17. Migração e implantação

`V24__registro_uso.sql` cria a tabela nova e o índice; não toca em dado
existente. Rollout em um passo, junto do deploy normal.

## 18. Perguntas e suposições

Q-405 respondida (90 dias). ASM-504 e ASM-505 confirmadas pelo dono do produto;
ASM-506 e ASM-507 confirmadas na mesma data ([spec](spec.md#suposições)).

## 19. Marcos e acompanhamento

Um marco por tarefa (T-522, T-523, T-524), acompanhado pela lista de tarefas do
harness e pelos commits vermelho/verde.

## 20. Revisão e rastreabilidade

PRD 15.5 / issue #34 → [ADR](adr.md) → este TDD → [spec](spec.md) (US-476 a
US-478, AC-651 a AC-656) → [tarefas](tasks.md) (T-522 a T-524) →
`RegistroUsoIntegrationTest`. Revisor: Lucas Mendes.

Estrutura baseada no [Technical Design Doc Creator, Tech Leads Club](https://github.com/tech-leads-club/agent-skills/blob/main/packages/skills-catalog/skills/%28creation%29/create-technical-design-doc/SKILL.md); texto e requisitos de rastreabilidade adaptados para este projeto.

<!-- onp-spec:documentation-contract=v1 -->

# ADR: Registro de uso da API por filtro assíncrono com prazo de 90 dias

> feature: spec-28-registro-uso-api
> status: revisado

[PRD](prd.md) · [TDD](design.md) · [Spec](spec.md) · [Tarefas](tasks.md) · [Pesquisa](research.md)

## Identificação e contexto

- Data: 2026-11-22 · Decisor: Lucas Mendes (dono do produto) · Domínio: autenticação e administração
- Atende: US-476, US-477, US-478 da [spec](spec.md); requisitos do [PRD](prd.md#requisitos-e-prioridades)

O relatório de picos (#36) precisa saber quem chamou a API, quando e o quê. Hoje
nada disso é guardado.

## Restrições e regras anteriores

- [ADR-011](../../../docs/adr/011-jwt-pelo-resource-server-do-spring.md): JWT pelo resource server. Continua válido; o registro lê o principal já montado.
- [ADR-013](../../../docs/adr/013-controle-de-acesso-por-papeis.md) e spec-19: registro administrativo guarda só ids, sem senha nem token. A mesma regra vale aqui.
- PRD 15.1: relatório do administrador nunca mostra conteúdo de carteira.
- Nenhuma decisão anterior é substituída.

## Alternativas consideradas

| Opção | Benefício | Custo / limite |
|---|---|---|
| A. Filtro na cadeia do Spring Security, gravação `@Async` em tabela | Pega todas as rotas num lugar só; resposta não espera o banco; dado consultável por SQL para o #36 | Registro pode se perder se a aplicação cair com a fila cheia |
| B. Filtro com gravação síncrona | Sem perda | Cada requisição paga um `INSERT`; fere "não deixar mais lenta" |
| C. Interceptor do Spring MVC | Acesso direto ao handler | Não vê requisições que param antes do MVC; mesma questão de sincronia |
| D. Log em arquivo / Micrometer | Zero tabela | Consulta por usuário e período fica difícil; métrica agregada perde o usuário |
| E. Não fazer | — | #36 fica sem dados |

## Decisão e justificativa

Opção A. Um filtro depois do `AuthorizationFilter` lê o usuário autenticado
antes de seguir, e depois da resposta pega método, padrão da rota e status.
A gravação vai para o executor assíncrono do Spring Boot; falha é logada e
engolida. Um job `@Scheduled` diário, às 3h de Brasília, apaga em lote o que
tiver mais de 90 dias.

Decisões do dono do produto (2026-11-22): prazo de 90 dias; só requisições
autenticadas; não guardar corpo, query, token nem IP.

## Consequências e riscos

- Ganho: base de dados para o #36 sem tocar nos controllers.
- Perda aceita: registros na fila são perdidos se o processo cair. Para relatório de picos, perder alguns é tolerável.
- Volume: uma linha por requisição autenticada, limitada a 90 dias. Detalhes no [TDD](design.md#16-performance-e-capacidade).

## Validade, revisão e substituição

Rever se o produto virar serviço comercial (Marco Civil, art. 15, pede 6 meses
de registro de acesso com IP; ver [pesquisa](research.md)), se o volume exigir
fila externa ou se o #36 precisar de dado que não está aqui.

## Respostas e aprovação

| Pergunta | Resposta literal | Origem | Data | Afeta |
|---|---|---|---|---|
| Prazo e escopo do registro | "90 dias, só autenticadas, pode fazer os commits" | Chat Claude Code | 2026-11-22 | PRD, spec (ASM-504, ASM-505), este ADR |

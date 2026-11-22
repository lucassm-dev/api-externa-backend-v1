# Spec: Extrato por carteira, tipo e resultado

> feature: spec-27-extrato-carteira-tipo-resultado
> status: rascunho

## Contexto

Issue #33 (PRD 15.4), filtros decididos no ADR-018. Completa a spec-26: o
`GET /operacoes` passa a aceitar também `carteiraId`, `tipo` e `resultado`,
opcionais, aplicados no servidor antes da paginação e combinados com E entre si
e com `dataInicio`, `dataFim` e `ticker`.

Decisões de projeto:

1. `resultado=LUCRO` devolve vendas com lucro realizado maior que zero;
   `resultado=PREJUIZO`, menor que zero. Compras e vendas com resultado zero não
   entram em nenhum dos dois (ADR-018). O valor é o `lucro_realizado` que a
   SPEC-06 grava em cada venda; o `lucro_realizado_brl` (SPEC-08) é esse valor
   vezes uma taxa positiva, então o sinal é o mesmo.
2. `carteiraId` de carteira de outro investidor, ou inexistente, responde 200
   com lista vazia: não revela se a carteira existe (dono do produto,
   2026-11-22).
3. Parâmetro de URL com tipo inválido (`tipo=TROCA`, `dataInicio=abc`,
   `carteiraId=x`) responde `400 VAL-002` com o nome do parâmetro em
   `fieldErrors`, em todas as rotas. Antes caía no handler genérico e respondia
   `500 SYS-001` (dono do produto, 2026-11-22).
4. Sem nenhum filtro, a resposta continua igual à de antes.

## Histórias

### US-474 — Filtrar o extrato por carteira, tipo e resultado

Como investidor, quero filtrar o meu extrato por carteira, por compra ou venda
e pelas vendas com lucro ou com prejuízo, para ver onde ganhei e onde perdi.

#### AC-644 — Filtro por carteira traz só as operações dela

- **Dado** um investidor com operações em duas carteiras
- **Quando** filtra o extrato por `carteiraId` de uma delas
- **Então** recebe só as operações dessa carteira

#### AC-645 — Carteira de outro investidor devolve lista vazia

- **Dado** dois investidores com operações
- **Quando** um deles filtra o extrato pelo `carteiraId` da carteira do outro
- **Então** recebe 200 com a lista vazia

#### AC-646 — Filtro por tipo

- **Dado** um investidor com compras e vendas
- **Quando** filtra o extrato por `tipo=VENDA`
- **Então** recebe só as vendas

#### AC-647 — Resultado lucro traz só vendas com lucro realizado positivo

- **Dado** um investidor com compras, uma venda com lucro, uma com prejuízo e
  uma com resultado zero
- **Quando** filtra o extrato por `resultado=LUCRO`
- **Então** recebe só a venda com lucro, com o mesmo `lucroRealizado` gravado
  pela SPEC-06

#### AC-648 — Resultado prejuízo traz só vendas com lucro realizado negativo

- **Dado** um investidor com compras, uma venda com lucro, uma com prejuízo e
  uma com resultado zero
- **Quando** filtra o extrato por `resultado=PREJUIZO`
- **Então** recebe só a venda com prejuízo

#### AC-649 — Resultado respeita a posse

- **Dado** dois investidores com vendas com lucro
- **Quando** um deles filtra o extrato por `resultado=LUCRO`
- **Então** recebe só as próprias vendas

### US-475 — Parâmetro inválido é erro do cliente

Como cliente da API, quero que um parâmetro com valor inválido responda 400
com o nome do parâmetro, para corrigir a chamada em vez de ver um erro interno.

#### AC-650 — Valor inválido em parâmetro de URL responde 400 VAL-002

- **Dado** um investidor logado
- **Quando** pede o extrato com `tipo=TROCA`
- **Então** recebe 400 `VAL-002` com `tipo` em `fieldErrors`

## Suposições

Nenhuma.

## Perguntas em aberto

Nenhuma.

## Fora de escopo

- Lucro ou prejuízo de posições ainda abertas (ADR-018)
- Exportar arquivo (ADR-018)
- Somar o resultado filtrado; o total continua em `GET /carteiras/{id}/lucro-realizado`

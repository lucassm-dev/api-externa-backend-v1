# Spec: Extrato por período e por ticker

> feature: spec-26-extrato-periodo-ticker
> status: rascunho

## Contexto

Issue #32 (PRD 15.4), filtros decididos no ADR-018: `GET /operacoes` aceita
filtros opcionais aplicados no servidor antes da paginação, combinados com E.
Esta spec cobre `dataInicio`, `dataFim` e `ticker`; `carteiraId`, `tipo` e
`resultado` ficam para a #33.

Decisões de projeto (respondidas pelo dono do produto em 2026-11-21):

1. `dataInicio` e `dataFim` são datas (`yyyy-MM-dd`), inclusivas nas duas
   pontas sobre a data da operação; cada uma pode vir sozinha.
2. `dataFim` antes de `dataInicio` responde `422 OPE-006`.
3. `ticker` compara exato, sem diferenciar maiúsculas (`petr4` acha `PETR4`).
4. Sem nenhum filtro, a resposta continua igual à de antes.

## Histórias

### US-473 — Filtrar as próprias movimentações

Como investidor, quero filtrar o meu extrato por período e por ticker, para
ver só as movimentações que me interessam.

#### AC-640 — Só movimentações do próprio investidor

- **Dado** dois investidores com operações no mesmo ticker e período
- **Quando** um deles filtra o extrato
- **Então** recebe só as próprias operações

#### AC-641 — Período com data final antes da inicial é recusado

- **Dado** um investidor logado
- **Quando** pede o extrato com `dataFim` antes de `dataInicio`
- **Então** recebe 422 `OPE-006`

#### AC-642 — Período inclui as duas pontas

- **Dado** operações antes, dentro e depois de um período, incluindo uma no
  último dia
- **Quando** filtra por `dataInicio` e `dataFim`
- **Então** recebe só as do período, incluindo as dos dias das pontas

#### AC-643 — Filtros combinados retornam a interseção

- **Dado** operações de dois tickers dentro e fora de um período
- **Quando** filtra por período e por ticker
- **Então** recebe só as do ticker dentro do período, paginadas

## Suposições

Nenhuma.

## Perguntas em aberto

Nenhuma.

## Fora de escopo

- Filtros por carteira, tipo e resultado (#33)
- Exportar arquivo (ADR-018)

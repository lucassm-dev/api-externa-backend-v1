# Spec: Relatório de picos de requisições

> feature: spec-30-relatorio-picos-requisicoes
> status: pronta

## Contexto

Issue #36 (PRD 15.5). Lê o registro de uso da spec-28 (`registro_uso`, 90 dias
de guarda) e mostra ao ADMIN o total de requisições de um período, as horas
de pico e os usuários que mais chamaram a API.

Decisões do dono do produto (2026-11-22):

1. O relatório **identifica o usuário** (id e e-mail), para o controle de uso
   por usuário do PRD 15.5. A decisão fica registrada no PRD 15.5.
2. Picos por hora: as 10 horas com mais requisições e os 10 usuários que mais
   chamaram, mais o total do período.
3. `dataInicio` e `dataFim` opcionais, no formato do extrato (spec-26); sem
   datas, os últimos 7 dias (hoje e os 6 anteriores). `dataFim` antes de
   `dataInicio` responde `422 AUT-018`.
4. A hora é a gravada no registro, no fuso do servidor; fixar o fuso de
   Brasília no deploy é a issue #61.

### Rota (só ADMIN)

`GET /admin/uso?dataInicio=&dataFim=` →
`{"total": n, "picos": [{"hora": "2026-11-20T14:00:00", "requisicoes": n}], "usuarios": [{"id": n, "email": "...", "requisicoes": n}]}`

## Histórias

### US-480 — ADMIN vê os picos de uso da API

Como ADMIN, quero ver quando a API é mais usada e por quem, para controlar o
uso e agir sobre abusos.

#### AC-660 — Horas de pico do período

- **Dado** 3 requisições às 14h, 1 às 15h e 2 às 9h de um mesmo dia
- **Quando** o ADMIN pede o relatório desse dia
- **Então** o total é 6 e os picos vêm em ordem: 14h com 3, 9h com 2, 15h com 1

#### AC-661 — Usuários que mais chamaram a API, identificados

- **Dado** um investidor com 3 requisições e outro com 1 no período
- **Quando** o ADMIN pede o relatório
- **Então** os usuários vêm em ordem, cada um com id, e-mail e número de requisições
- **E** a resposta só tem `total`, `picos` e `usuarios`: nada de rota, carteira ou valor

#### AC-662 — Só o período pedido entra no relatório

- **Dado** requisições dentro e fora do período
- **Quando** o ADMIN pede o relatório com `dataInicio` e `dataFim`
- **Então** só as de dentro contam, com `dataFim` incluído inteiro
- **E** sem datas, valem os últimos 7 dias: uma requisição de 8 dias atrás não conta e uma de ontem conta

#### AC-663 — No máximo 10 horas e 10 usuários

- **Dado** requisições em 12 horas diferentes, de 12 usuários
- **Quando** o ADMIN pede o relatório
- **Então** vêm só as 10 horas e os 10 usuários com mais requisições

#### AC-664 — Período invertido é recusado

- **Dado** `dataFim` anterior a `dataInicio`
- **Quando** o ADMIN pede o relatório
- **Então** recebe 422 `AUT-018`

#### AC-665 — Só o ADMIN vê o relatório

- **Dado** um token de investidor e nenhum token
- **Quando** cada um pede o relatório
- **Então** o investidor recebe 403 `AUT-007` e sem token recebe 401 `AUT-005`

## Fora de escopo

- Gráfico e exportação de arquivo
- Detalhar as rotas mais chamadas
- Alerta automático de pico ou rate limit
- Fixar o fuso de Brasília (issue #61)

## Suposições

| ID | Suposição | Status | Resolução |
|---|---|---|---|
| ASM-511 | Relatório identificado (id e e-mail), top 10 horas e top 10 usuários | confirmada | Dono do produto, 2026-11-22: "Identificado", "Top 10" |
| ASM-512 | Período opcional, padrão últimos 7 dias; invertido responde 422 `AUT-018` | confirmada | Dono do produto, 2026-11-22: "padrão últimos 7 dias", "422 AUT-018" |
| ASM-513 | A hora do pico é a do servidor; o fuso de Brasília fica para a #61 | confirmada | Dono do produto, 2026-11-22: "Usar o fuso do servidor e abrir issue" |
| ASM-514 | Empate de requisições: entre horas vem a mais recente; entre usuários, o de menor id | confirmada | Dono do produto, 2026-11-22: "Hora mais recente / menor id" |

## Perguntas em aberto

| ID | Pergunta | Status | Resposta |
|---|---|---|---|
| Q-406 | O relatório mostra o usuário identificado ou só o agregado? | respondida | Identificado (dono do produto, 2026-11-22) |

## Documentos relacionados

[Tarefas](tasks.md) · [spec-28](../spec-28-registro-uso-api/spec.md) · [PRD 15.5](../../../docs/prd/PRD-sistema-investimentos-acoes.md)

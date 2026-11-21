# Spec: Descontinuar corretora

> feature: spec-20-descontinuar-corretora
> status: rascunho

## Contexto

Issue #23, direção no ADR-014. Corretoras saem do mercado por liquidação ou
incorporação, mas a única saída hoje é excluir, que fica bloqueada enquanto
houver carteira ativa (COR-004, ADR-007). Isso deixa o ADMIN travado. Esta
spec cria uma segunda ação, descontinuar: a corretora para de aceitar
carteira nova e as carteiras que já existem continuam funcionando.

Decisões de projeto (respondidas pelo dono do produto em 2026-11-21):

1. **A corretora descontinuada some de `GET /corretoras`**, que é a lista usada
   para criar carteira. `GET /corretoras/{id}` e `GET /corretoras/cnpj/{cnpj}`
   continuam respondendo, com `descontinuada: true`.
2. **O motivo é obrigatório**: `PATCH /corretoras/{id}/descontinuar
   {motivo}`. O motivo vai para o registro administrativo (spec-19), como no
   bloqueio.
3. **Descontinuar não tem volta nesta issue.** Descontinuar uma corretora que
   já está descontinuada é recusado.

### Rotas novas (só ADMIN)

| Operação | Rota |
|---|---|
| Ver carteiras afetadas | `GET /corretoras/{id}/carteiras-afetadas` → `{carteirasAtivas}` |
| Descontinuar | `PATCH /corretoras/{id}/descontinuar` `{motivo}` |

## Histórias

### US-463 — ADMIN descontinua corretora que saiu do mercado

Como ADMIN, quero descontinuar uma corretora que saiu do mercado, sabendo
antes quantas carteiras serão afetadas, para tirá-la de circulação sem
apagar o histórico de ninguém.

#### AC-593 — ADMIN vê quantas carteiras ativas serão afetadas

- **Dado** uma corretora com 2 carteiras ativas e 1 carteira excluída
- **Quando** o ADMIN consulta as carteiras afetadas
- **Então** recebe 200 com `carteirasAtivas` igual a 2

#### AC-594 — Descontinuar marca a corretora e tira da lista

- **Dado** uma corretora com carteira ativa
- **Quando** o ADMIN a descontinua com o motivo "liquidação extrajudicial"
- **Então** recebe 200 com `descontinuada: true`; a corretora não aparece mais
  em `GET /corretoras`, mas `GET /corretoras/{id}` ainda a mostra com
  `descontinuada: true`

#### AC-595 — Descontinuação sem motivo ou repetida é recusada

- **Dado** uma corretora ativa e outra já descontinuada
- **Quando** o ADMIN descontinua a primeira sem motivo e a segunda com motivo
- **Então** a primeira recebe 400 de validação e continua não descontinuada;
  a segunda recebe 422 `COR-006`

#### AC-596 — Descontinuação fica no registro administrativo

- **Dado** um ADMIN e uma corretora
- **Quando** o ADMIN a descontinua com o motivo "incorporação"
- **Então** existe exatamente um registro novo `DESCONTINUACAO_CORRETORA` com
  o id do ADMIN, alvo `CORRETORA` com o id da corretora e o motivo
  "incorporação"

#### AC-597 — Só o ADMIN descontinua e consulta carteiras afetadas

- **Dado** um token de investidor, um de ADMIN e nenhum token
- **Quando** cada um chama as duas rotas novas
- **Então** o ADMIN é atendido; o investidor recebe 403 `AUT-007`; sem token
  recebe 401 `AUT-005`

### US-464 — Investidor com carteira na corretora descontinuada

Como investidor, quero continuar operando minha carteira de uma corretora
descontinuada e saber que ela foi descontinuada, mas não abrir carteira nova
nela.

#### AC-598 — Criar carteira em corretora descontinuada é recusado

- **Dado** uma corretora descontinuada
- **Quando** o investidor tenta criar uma carteira nela
- **Então** recebe 422 `COR-005` e nenhuma carteira é criada

#### AC-599 — Comprar e vender continuam funcionando

- **Dado** uma carteira ativa de uma corretora que foi descontinuada depois
  que a carteira foi criada
- **Quando** o investidor registra uma compra e depois uma venda
- **Então** as duas operações são aceitas como antes da descontinuação

#### AC-600 — Carteira mostra que a corretora foi descontinuada

- **Dado** uma carteira de uma corretora descontinuada e outra de uma
  corretora normal
- **Quando** o investidor consulta as carteiras
- **Então** a primeira traz `corretoraDescontinuada: true` e a segunda
  `corretoraDescontinuada: false`

### US-465 — Excluir continua protegido

Como dono do produto, quero que descontinuar não abra caminho para excluir
corretora com carteira, para não apagar o histórico.

#### AC-601 — Corretora descontinuada com carteira ativa não é excluída

- **Dado** uma corretora descontinuada com carteira ativa
- **Quando** o ADMIN tenta excluí-la
- **Então** recebe 422 `COR-004` e a corretora continua existindo

## Fora de escopo

- Reverter a descontinuação (decisão 3).
- Encerrar ou migrar carteiras da corretora descontinuada.
- Mudar como corretora excluída é tratada na criação de carteira.

## Suposições

| ID | Suposição | Status | Resolução |
|---|---|---|---|
| ASM-494 | A descontinuação é guardada em `corretora.descontinuada` (booleano, padrão `false`) e `corretora.descontinuada_em`, separada de `ativo`, que continua significando "não excluída". | aberta | — |
| ASM-495 | Corretora descontinuada sem carteira ativa pode ser excluída normalmente (a exclusão só olha carteira ativa). | aberta | — |
| ASM-496 | O motivo tem no máximo 255 caracteres, como no bloqueio (ASM-488). | aberta | — |
| ASM-497 | Recadastrar o CNPJ de uma corretora descontinuada é recusado como duplicado (COR-002), porque ela continua não excluída. | aberta | — |

## Perguntas em aberto

Nenhuma. As decisões que a issue não fechava foram respondidas (decisões 1 a
3 do Contexto).

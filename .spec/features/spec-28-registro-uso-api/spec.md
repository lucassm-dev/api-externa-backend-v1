<!-- onp-spec:documentation-contract=v1 -->

# Spec: Registro de uso da API por usuário

> feature: spec-28-registro-uso-api
> status: implementada

## Contexto

Issue #34 (PRD 15.5), base do relatório de picos de requisições (#36). Cada
requisição autenticada passa a deixar um registro com quem, quando, o método,
o padrão da rota e o status da resposta. A gravação é assíncrona, e um job
diário apaga o que tiver mais de 90 dias. Decisões no [ADR](adr.md).

## Histórias

### US-476 — Registrar o uso da API por usuário

Como administrador, quero que cada requisição autenticada fique registrada com
o usuário, o momento e a operação, para depois ver os picos de uso de cada um.

#### AC-651 — Requisição autenticada deixa registro de quem, quando e o quê

- **Dado** um investidor autenticado
- **Quando** ele consulta as posições de uma carteira dele (`GET /carteiras/{id}/posicoes`)
- **Então** fica um registro com o id dele, a data e hora, o método `GET`, a rota `/carteiras/{id}/posicoes` e o status da resposta

#### AC-652 — Registro não guarda dado sensível

- **Dado** um investidor autenticado
- **Quando** ele consulta o extrato com filtros na URL (`GET /operacoes?ticker=PETR4`)
- **Então** o registro guarda a rota `/operacoes`, sem os filtros
- **E** o registro só tem campos de usuário, data e hora, método, rota e status: nada de corpo, URL com parâmetros, token, senha nem IP

#### AC-653 — Requisição sem token não é registrada

- **Dado** nenhum registro de uso
- **Quando** alguém faz login, se cadastra ou chama uma rota protegida sem token
- **Então** nenhum registro de uso é criado

### US-477 — Registrar sem atrasar quem usa a API

Como investidor, quero que o registro de uso não deixe as minhas consultas mais
lentas nem falhe por causa dele.

#### AC-654 — A resposta não espera a gravação do registro

- **Dado** a gravação do registro de uso travada
- **Quando** um investidor autenticado faz uma consulta
- **Então** recebe a resposta sem esperar a gravação terminar

#### AC-655 — Falha ao gravar o registro não afeta a resposta

- **Dado** a gravação do registro de uso falhando
- **Quando** um investidor autenticado faz uma consulta
- **Então** recebe a resposta normal (200)

### US-478 — Descartar registros antigos

Como dono do produto, quero que os registros de uso fiquem guardados só por 90
dias, para não acumular dado pessoal além do necessário (LGPD).

#### AC-656 — Registros com mais de 90 dias são apagados

- **Dado** registros de uso de 91 e de 89 dias atrás
- **Quando** o descarte diário roda
- **Então** o registro de 91 dias some e o de 89 dias continua

## Fora de escopo

- Relatório de picos e consulta dos registros pelo administrador (#36)
- Limitar a quantidade de requisições por usuário (rate limit)
- Registrar requisições sem token ou recusadas na autenticação (401)

## Suposições

| ID | Suposição | Status | Resolução |
|---|---|---|---|
| ASM-504 | Prazo de guarda de 90 dias, com descarte diário | confirmada | Dono do produto, 2026-11-22 |
| ASM-505 | Só requisições autenticadas são registradas; login, cadastro e rotas sem token ficam de fora | confirmada | Dono do produto, 2026-11-22 |
| ASM-506 | Requisição autenticada recusada por falta de papel (403 do `@PreAuthorize`) também é registrada, com o status 403: houve uso da API pelo usuário | confirmada | Dono do produto, 2026-11-22: "Sim, registra com 403" |
| ASM-507 | Rota sem controller correspondente é registrada como `desconhecida`, nunca com a URL crua, que pode trazer dado digitado pelo usuário | confirmada | Dono do produto, 2026-11-22: "Sim, 'desconhecida'" |

## Perguntas em aberto

| ID | Pergunta | Status | Resposta |
|---|---|---|---|
| Q-405 | Por quanto tempo os registros ficam guardados? | respondida | 90 dias (dono do produto, 2026-11-22) |

## Documentos relacionados

[PRD](prd.md) · [ADR](adr.md) · [TDD](design.md) · [Tarefas](tasks.md) · [Pesquisa](research.md)

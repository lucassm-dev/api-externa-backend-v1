# Spec: Ação entra no catálogo pela pesquisa

> feature: spec-21-acao-pela-pesquisa
> status: rascunho

## Contexto

Issue #24, direção no ADR-015, que substitui o item 2 do ADR-003. Hoje o
investidor cadastra ação manualmente (`POST /acoes`) e só pode fazer isso se
tiver carteira (ACA-004). Como todo dado da ação vem da fonte externa, o
cadastro manual não acrescenta nada. A partir desta spec, pesquisar um ticker
que ainda não está no catálogo consulta a fonte do mercado, grava a ação e a
devolve.

Decisões de projeto (respondidas pelo dono do produto em 2026-11-21):

1. **A pesquisa usa a rota que já existe**:
   `GET /acoes/ticker/{ticker}?mercado=BR|US`. Ticker que já está no catálogo
   volta sem consultar a fonte, com ou sem `mercado`. Ticker fora do catálogo
   precisa de `mercado` para saber qual fonte consultar; sem ele, a resposta
   continua 404 `ACA-001`.
2. **Saem `POST /acoes` e `DELETE /acoes/{ticker}`**, junto com as regras
   ACA-002 (duplicado), ACA-003 (exclusão bloqueada) e ACA-004 (carteira
   obrigatória).
3. **Critérios antigos são migrados ou aposentados.** Os que continuam valendo
   passam a ser provados pela pesquisa: AC-201, AC-202, AC-203, AC-205,
   AC-209, AC-211 e AC-429 (catalogo-acao e spec-01); AC-433 (spec-01) perde
   a parte do ACA-002; AC-446 (spec-03) fica só com o CNPJ; AC-562 (spec-16)
   passa a cobrir a atualização de cotação. Os que perderam sentido saem da
   spec de origem junto com o teste, e os códigos não são reaproveitados:
   AC-204 (catalogo-acao); AC-424 e AC-425 (refactor-cnpj-delete); AC-444,
   AC-447, AC-450 e AC-451 (spec-03); e a spec-05 inteira (AC-466 a AC-468).

## Histórias

### US-466 — Pesquisar inclui a ação no catálogo

Como investidor, quero pesquisar uma ação por ticker e mercado e encontrá-la
mesmo que ninguém a tenha cadastrado, para operar sem passo extra.

#### AC-602 — Ticker novo e válido entra no catálogo na primeira pesquisa

- **Dado** um ticker válido na fonte BR que não está no catálogo
- **Quando** o investidor pesquisa esse ticker com `mercado=BR`
- **Então** recebe 200 com o ticker, mercado BR, moeda BRL, cotação e
  data/hora da cotação; a ação passa a existir no catálogo

#### AC-603 — Segunda pesquisa reaproveita o catálogo

- **Dado** um ticker que já entrou no catálogo por uma pesquisa
- **Quando** alguém pesquisa o mesmo ticker de novo, com ou sem `mercado`
- **Então** recebe a mesma ação, a fonte externa não é chamada outra vez e o
  catálogo continua com uma única ação ativa para esse ticker

#### AC-604 — Ticker inexistente na fonte não entra

- **Dado** um ticker que a fonte do mercado não conhece
- **Quando** o investidor o pesquisa com `mercado=BR`
- **Então** recebe 422 `EXT-008` e nada é gravado no catálogo

#### AC-605 — Investidor sem carteira consegue pesquisar

- **Dado** um investidor sem nenhuma carteira
- **Quando** ele pesquisa um ticker novo e válido com mercado
- **Então** recebe 200 e a ação entra no catálogo

#### AC-606 — Ticker fora do catálogo sem mercado não consulta a fonte

- **Dado** um ticker que não está no catálogo
- **Quando** alguém o pesquisa sem `mercado`
- **Então** recebe 404 `ACA-001`, a fonte externa não é chamada e nada é
  gravado

### US-467 — O catálogo não tem mais cadastro nem exclusão manual

Como dono do produto, quero que a ação só entre no catálogo pela pesquisa,
para que o catálogo tenha um único caminho de entrada.

#### AC-607 — Não existe mais cadastrar nem excluir ação

- **Dado** uma ação no catálogo e um token válido
- **Quando** alguém chama `POST /acoes` e `DELETE /acoes/{ticker}`
- **Então** as duas chamadas são recusadas com erro do cliente (4xx) e a ação
  continua no catálogo

#### AC-608 — Catálogo de erros sem as regras removidas

- **Dado** o arquivo `docs/erros.md`
- **Quando** se procuram os códigos ACA-002, ACA-003 e ACA-004
- **Então** nenhum deles aparece na tabela de códigos

## Fora de escopo

- Mudar a atualização de cotação (`PUT /acoes/{id}/atualizar-cotacao`) ou a
  listagem `GET /acoes`.
- Remover ação do catálogo por qualquer outro caminho.
- Reativar ação que foi excluída antes desta spec.

## Suposições

| ID | Suposição | Status | Resolução |
|---|---|---|---|
| ASM-498 | Ticker que já está no catálogo volta mesmo que o `mercado` informado seja outro: o catálogo manda e o ticker continua único entre as ações ativas (índice `uk_acao_ticker_ativo`). | aberta | — |
| ASM-499 | Ação excluída antes desta spec (inativa) não volta: pesquisar o ticker dela cria uma ação nova, como o recadastro fazia (AC-444). | aberta | — |
| ASM-500 | Duas primeiras pesquisas simultâneas do mesmo ticker podem colidir no índice único; a segunda falha e quem pesquisou de novo recebe a ação. Sem tratamento extra nesta spec. | aberta | — |
| ASM-501 | Fonte indisponível ou cota estourada na primeira pesquisa respondem como hoje (EXT-009/EXT-010, 429) e nada é gravado. | aberta | — |
| ASM-502 | `PreRequisitoNaoAtendidoException` continua existindo mesmo sem uso, porque faz parte da hierarquia de erros. | invalidada | Sem uso depois do ACA-004 e sem tarefa que a mapeie (ARQUIVO_ORFAO); foi apagada junto com a regra |

## Perguntas em aberto

Nenhuma. As decisões que a issue não fechava foram respondidas (decisões 1 a
3 do Contexto).

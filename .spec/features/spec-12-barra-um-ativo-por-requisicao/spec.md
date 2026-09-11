# Spec: SPEC-12 — Barra de cotações dentro do plano gratuito da brapi

> feature: spec-12-barra-um-ativo-por-requisicao
> status: pronta

## Contexto

A barra de cotações pedia cinco ativos numa única requisição à brapi
(`PETR4,ITUB4,IVVB11,^BVSP,IFIX.SA`). O plano gratuito recusa isso com HTTP 400
(`QUOTES_PER_REQUEST_EXCEEDED`, "seu plano permite no máximo 1 ativo por
requisição"), então a parte de ações e índices da barra **nunca funcionou em
produção** — só câmbio e cripto apareciam, com um aviso genérico dizendo que a
brapi estava indisponível.

A brapi estava no ar o tempo todo: cada um dos cinco tickers responde 200 quando
pedido isoladamente. O limite de um ativo por requisição já estava documentado no
Apêndice A do PRD do produto; a barra foi construída violando-o.

Cada ativo passa a custar uma requisição própria da cota mensal (15.000), o que
obriga a encurtar a lista e a alargar o cache.

## Histórias

### US-437 — Ver índices de mercado na barra sem estourar a cota gratuita

Como investidor, quero ver os índices de referência do mercado na barra
superior, para me situar antes de olhar minha carteira — sem que isso consuma a
cota de consultas que as minhas compras e vendas precisam.

#### AC-517 — A barra mostra o índice e as ações de referência

- **Dado** que a brapi está respondendo
- **Quando** consulto a barra de cotações
- **Então** vejo o índice da bolsa brasileira (IBOV) e as ações PETR4 e VALE3,
  além de dólar, euro e bitcoin

#### AC-518 — Cada ativo é pedido isoladamente à fonte

- **Dado** que o plano gratuito aceita um ativo por requisição
- **Quando** a barra busca ações e índices
- **Então** cada símbolo é pedido numa requisição própria, nunca agrupado

#### AC-519 — Ativo que falha não leva os outros junto

- **Dado** que um dos ativos da brapi falha e o outro responde
- **Quando** a barra é montada
- **Então** o que respondeu aparece normalmente, e o aviso nomeia apenas o que
  faltou — em vez de sumir com todos os ativos da fonte

#### AC-520 — A barra guarda o resultado por trinta minutos

- **Dado** que a barra foi consultada há menos de trinta minutos
- **Quando** é consultada de novo
- **Então** reaproveita o resultado guardado sem chamar fonte externa nenhuma,
  mantendo o consumo em torno de 2.900 requisições por mês

## Fora de escopo

- Voltar ITUB4, IFIX e IVVB11 à barra — sairiam da mesma cota que compra e venda
  usam
- Contratar plano pago da brapi
- Alterar o `AcaoService` ou o `BrapiAdapter`, que já pedem um ticker por vez e
  nunca tiveram este problema

## Suposições

| ID | Suposição | Status | Resolução |
|---|---|---|---|
| ASM-447 | IBOV mais as duas ações mais negociadas da B3 (PETR4, VALE3) dão o contexto de mercado que a barra existe para dar | confirmada | Escolhido pelo dono do produto em 09/09/2026: a composição só com índice e ETF não mostrava nenhuma ação |
| ASM-448 | Trinta minutos de defasagem é aceitável para dado de contexto — a barra não é usada para decidir operação | confirmada | Decorre da escolha acima. A cotação usada em compra e venda tem TTL próprio, de 15 minutos, e não muda |
| ASM-449 | O limite de um ativo por requisição vale para toda a API de cotação do plano gratuito, não só para índices | confirmada | Verificado em 09/09/2026: chamada agrupada com 5 tickers devolve 400; cada ticker isolado devolve 200 |

## Perguntas em aberto

| ID | Pergunta | Status | Resposta |
|---|---|---|---|
| Q-MAP-13 | Como impedir que um teste com mock volte a esconder uma recusa real da fonte? | respondida | O mock passa a exigir chamada por ticker (AC-518). Um retorno agrupado deixa de ser representável no teste, que é o que permitiu o defeito passar despercebido |

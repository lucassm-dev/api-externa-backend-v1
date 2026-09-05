# Spec: Barra de cotações do mercado

> feature: spec-09-barra-cotacoes
> status: rascunho

## Contexto

Bloco B.2 do prompt original: uma faixa horizontal (como a do topo do
investidor10.com.br/statusinvest.com.br) com símbolo + preço + variação % do
dia para uma mistura de moedas, índices, cripto, ETF e ações. Composição e
fontes já pesquisadas e aprovadas nesta conversa (Bloco B): brapi para
ações/índices (`PETR4,ITUB4,IVVB11,^BVSP,IFIX.SA` numa única chamada
agregada, economizando cota), AwesomeAPI para USD/EUR (os endpoints
`/api/v2/currency` e `/api/v2/crypto` da brapi exigem plano pago — a
AwesomeAPI já resolve câmbio sem token, mesma fonte da SPEC-08), CoinGecko
para BTC (gratuito, sem token). Cache compartilhado de 15 min — mesmo TTL
já usado nas outras caches deste projeto — evita estourar a cota gratuita
da brapi (15.000 req/mês).

## Histórias

### US-431 — Barra de cotações do mercado

Como investidor, quero ver uma barra com preço e variação do dia de moedas,
índices, cripto e ações populares, para acompanhar o mercado rapidamente
sem sair da tela.

#### AC-498 — Barra retorna os itens combinados das três fontes

- **Dado** que chamo `GET /mercado/barra-cotacoes`
- **Quando** a resposta volta
- **Então** recebo símbolo, preço e variação percentual de USD, EUR, IBOV,
  IFIX, BTC, IVVB11, ITUB4 e PETR4

#### AC-499 — Falha de uma fonte não derruba as outras

- **Dado** que uma das três fontes (brapi, AwesomeAPI ou CoinGecko) falha
- **Quando** a barra é consultada
- **Então** os itens das fontes que funcionaram continuam aparecendo, com
  um aviso indicando quais faltaram — não falha tudo por causa de uma fonte

#### AC-500 — Barra dentro do TTL reaproveita o cache

- **Dado** que a barra foi consultada há menos que o TTL configurado
- **Quando** é consultada de novo
- **Então** reaproveita o cache sem chamar nenhuma fonte externa

#### AC-501 — Barra fora do TTL busca de novo

- **Dado** que a barra está fora do TTL (ou nunca foi consultada)
- **Quando** é consultada
- **Então** busca de novo nas três fontes e atualiza o cache

#### AC-502 — Resposta inclui o horário dos dados

- **Dado** que a barra é consultada
- **Quando** a resposta volta
- **Então** inclui o horário em que os dados foram obtidos (`atualizadoEm`)

## Fora de escopo

- Indicadores fundamentalistas (P/L, P/VP, DY, ROE) — item "extra, decidir
  depois" do prompt original, não aprovado para esta spec.
- Frontend/renderização da faixa — esta spec entrega só o endpoint agregado;
  quem consome decide como exibir (cores, rolagem, etc.).
- Outros ativos além dos 8 itens do exemplo do prompt (USD, EUR, IBOV, IFIX,
  BTC, IVVB11, ITUB4, PETR4) — lista fixa nesta primeira versão.

## Suposições

| ID | Suposição | Status | Resolução |
|---|---|---|---|
| ASM-435 | O endpoint fica atrás de autenticação (JWT), igual a todo o resto da API — não é exposto publicamente sem login, mesmo sendo dado de mercado geral. Consistente com `SecurityConfig` atual, sem caso especial. | aberta | — |
| ASM-436 | Quando uma fonte falha (AC-499), os itens dela simplesmente não aparecem na lista (em vez de aparecer com preço zerado/nulo) — o aviso explica o que faltou. | aberta | — |
| ASM-437 | O cache é global (um único resultado compartilhado por todos os investidores), não por usuário — mesmo padrão do `CambioCacheService` (SPEC-08), já que o dado de mercado é o mesmo para todo mundo. | aberta | — |
| ASM-438 | `TTL` próprio (`mercado.barra-cache-ttl-minutos`), independente do TTL de cotação de ação e de câmbio, mesmo valor padrão (15min). | aberta | — |

## Perguntas em aberto

Nenhuma — a composição da barra, as fontes e a estratégia de cache já foram
pesquisadas e aprovadas nesta conversa (Bloco B.2).
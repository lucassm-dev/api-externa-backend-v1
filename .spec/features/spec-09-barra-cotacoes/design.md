# Design: Barra de cotações do mercado

> feature: spec-09-barra-cotacoes

## Componentes novos

```
infra/client/coingecko/CoinGeckoClient.java              (Feign — /simple/price)
domains/dtos/ItemBarraCotacoesDTO.java
domains/dtos/BarraCotacoesResponseDTO.java
services/BarraCotacoesService.java                        (agregação + cache TTL global)
resources/MercadoResource.java                             (GET /mercado/barra-cotacoes)
```

Reaproveitados sem mudança de contrato: `BrapiClient.buscarCotacao(ticker,
token)` já aceita vários tickers separados por vírgula no mesmo path
(`PETR4,ITUB4,IVVB11,^BVSP,IFIX.SA` numa única chamada — é assim que a
brapi economiza cota, sem precisar de um client novo). `AwesomeApiCambioClient`
ganha um segundo método (`buscarUltimas(pares)`) para buscar USD-BRL e
EUR-BRL na mesma chamada, ao lado do `buscarUsdBrl()` já usado pela SPEC-08.

`BrapiResultDTO` e `AwesomeApiCotacaoDTO` ganham os campos que a barra
precisa e o `CambioFacade`/`AcaoService` não usavam
(`regularMarketChangePercent`/`logourl` na brapi; `name`/`pctChange` na
AwesomeAPI) — aditivo, não quebra nada existente.

## BarraCotacoesService — por que não é um Facade com fallback sequencial

Ao contrário do `CambioFacade` (SPEC-08), aqui não há "fonte primária vs.
fallback" — são **três fontes complementares**, cada uma dona de um
subconjunto dos itens (brapi → ações/índices; AwesomeAPI → câmbio; CoinGecko
→ cripto). Se uma falhar, as outras duas devem continuar aparecendo (AC-499)
— por isso cada chamada é isolada no seu próprio try/catch, sem propagar a
falha de uma para as outras. Isso é diferente do padrão client→adapter→facade
já usado (uma fonte "cai" na outra); aqui a composição é uma soma, não uma
cadeia.

```java
public BarraCotacoesResponseDTO obter() {
    if (dentroDoTtl()) return cacheado;

    List<ItemBarraCotacoesDTO> itens = new ArrayList<>();
    List<String> avisos = new ArrayList<>();

    try { itens.addAll(buscarAcoesEIndicesNaBrapi()); }
    catch (Exception e) { avisos.add("Ações/índices indisponíveis (brapi)."); }

    try { itens.addAll(buscarCambioNaAwesomeApi()); }
    catch (Exception e) { avisos.add("Câmbio indisponível (AwesomeAPI)."); }

    try { itens.add(buscarBtcNoCoinGecko()); }
    catch (Exception e) { avisos.add("Cripto indisponível (CoinGecko)."); }

    cacheado = new BarraCotacoesResponseDTO(itens, LocalDateTime.now(), avisos);
    return cacheado;
}
```

Cache: mesmo padrão global-em-memória do `CambioCacheService` — TTL próprio
(`mercado.barra-cache-ttl-minutos`, padrão 15min). Diferente do câmbio, aqui
**não há fallback para "última barra conhecida"** quando as três fontes
falham ao mesmo tempo — a resposta simplesmente vem com a lista vazia (ou
parcial) e os avisos; não é uma operação financeira que precisa de garantia
de continuidade, é um dado de exibição.

## Endpoint

`GET /mercado/barra-cotacoes` — autenticado (JWT), como todo o resto da API
(ASM-435). Sem parâmetros.

```json
{
  "itens": [
    {"simbolo": "PETR4", "nome": "Petrobras", "preco": 47.50, "variacaoPercentual": -1.31, "logoUrl": "..."},
    {"simbolo": "USD", "nome": "Dólar Americano", "preco": 5.10, "variacaoPercentual": -0.61, "logoUrl": null},
    {"simbolo": "BTC", "nome": "Bitcoin", "preco": 415490.12, "variacaoPercentual": 5.23, "logoUrl": null}
  ],
  "atualizadoEm": "2026-09-05T14:30:00",
  "avisos": []
}
```
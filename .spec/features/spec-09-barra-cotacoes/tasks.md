# Tasks: Barra de cotações do mercado

> feature: spec-09-barra-cotacoes

## T-462 — DTOs de resposta da barra [pendente]

- Refs: AC-498, AC-502
- Arquivos: src/main/java/com/apiexternabackend/domains/dtos/ItemBarraCotacoesDTO.java, src/main/java/com/apiexternabackend/domains/dtos/BarraCotacoesResponseDTO.java

## T-463 — Cliente CoinGecko (BTC) [pendente]

- Refs: AC-498
- Arquivos: src/main/java/com/apiexternabackend/infra/client/coingecko/CoinGeckoClient.java

## T-464 — Campos extras em BrapiResultDTO e AwesomeApiCotacaoDTO [pendente]

- Refs: AC-498
- Arquivos: src/main/java/com/apiexternabackend/infra/client/brapi/dtos/BrapiResultDTO.java, src/main/java/com/apiexternabackend/infra/client/awesomeapi/dtos/AwesomeApiCotacaoDTO.java, src/main/java/com/apiexternabackend/infra/client/awesomeapi/AwesomeApiCambioClient.java

## T-465 — BarraCotacoesService: agrega as três fontes com TTL e isolamento de falha [pendente]

- Refs: AC-498, AC-499, AC-500, AC-501, AC-502
- Arquivos: src/main/java/com/apiexternabackend/services/BarraCotacoesService.java, src/main/resources/application.properties

## T-466 — Endpoint GET /mercado/barra-cotacoes [pendente]

- Refs: AC-498, AC-502
- Arquivos: src/main/java/com/apiexternabackend/resources/MercadoResource.java

## T-467 — Testes de serviço: agregação, TTL e isolamento de falha [pendente]

- Refs: AC-498, AC-499, AC-500, AC-501, AC-502
- Arquivos: src/test/java/com/apiexternabackend/services/BarraCotacoesServiceTest.java

## T-468 — Testes de resource [pendente]

- Refs: AC-498, AC-499, AC-502
- Arquivos: src/test/java/com/apiexternabackend/resources/MercadoResourceTest.java

## T-469 — Atualizar catálogo de erros e mapeamento vivo [pendente]

- Refs: AC-498, AC-499, AC-500, AC-501, AC-502
- Arquivos: docs/erros.md, docs/mapeamento-atual.html
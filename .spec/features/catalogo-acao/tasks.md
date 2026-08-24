# Tasks: Catalogo acao

> feature: catalogo-acao

## T-200 — Entidade Acao, enum de mercado, repositório e migration [pendente]

- Refs: US-201, AC-204
- Arquivos: src/main/java/com/apiexternabackend/domains/Acao.java, src/main/java/com/apiexternabackend/domains/enums/Mercado.java, src/main/java/com/apiexternabackend/repositories/AcaoRepository.java, src/main/resources/db/migration/V4__create_acao.sql
- Notas: ticker (único), nomeEmpresa, mercado (BR/US), moeda, cotacaoAtual, dataHoraCotacao. existsByTicker/findByTicker.

## T-201 — DTOs e mapper da ação [pendente]

- Refs: AC-201, AC-202, AC-206, AC-207
- Arquivos: src/main/java/com/apiexternabackend/domains/dtos/AcaoRequestDTO.java, src/main/java/com/apiexternabackend/domains/dtos/AcaoResponseDTO.java, src/main/java/com/apiexternabackend/mappers/AcaoMapper.java
- Notas: Request com ticker + mercado. Response com cotação e dataHoraCotacao (RN-Q01).

## T-202 — Adapter de cotação (isolamento, Item 15) [pendente]

- Refs: AC-205, AC-209
- Arquivos: src/main/java/com/apiexternabackend/infra/adapter/CotacaoAdapter.java, src/main/java/com/apiexternabackend/infra/adapter/CotacaoResultado.java
- Notas: interface comum que abstrai a fonte; cada mercado resolve para um adapter. Resultado carrega preço + horário.

## T-203 — Integração brapi (mercado BR) [pendente]

- Refs: AC-201, AC-203, AC-210, AC-211
- Arquivos: src/main/java/com/apiexternabackend/infra/client/brapi/BrapiClient.java, src/main/java/com/apiexternabackend/infra/client/brapi/dtos/BrapiResponseDTO.java, src/main/java/com/apiexternabackend/infra/client/brapi/dtos/BrapiResultDTO.java, src/main/java/com/apiexternabackend/infra/adapter/BrapiAdapter.java
- Notas: token via env (BRAPI_TOKEN); ticker inexistente → erro tratado; cota excedida → mensagem própria (RN-Q05).

## T-204 — Integração Twelve Data (mercado US) [pendente]

- Refs: AC-202, AC-203, AC-210, AC-211
- Arquivos: src/main/java/com/apiexternabackend/infra/client/twelvedata/TwelveDataClient.java, src/main/java/com/apiexternabackend/infra/client/twelvedata/dtos/TwelveDataResponseDTO.java, src/main/java/com/apiexternabackend/infra/adapter/TwelveDataAdapter.java
- Notas: chave via env (TWELVEDATA_API_KEY); mesma semântica de erro/cota da brapi.

## T-205 — Serviço da ação [pendente]

- Refs: AC-201, AC-202, AC-203, AC-205, AC-208, AC-209, AC-210, AC-211
- Arquivos: src/main/java/com/apiexternabackend/services/AcaoService.java
- Notas: cadastro roteando pela fonte do mercado; unicidade de ticker; atualizar cotação; resiliência (última cotação conhecida) e limite de cota.

## T-206 — Controller da ação [pendente]

- Refs: AC-206, AC-207, AC-208
- Arquivos: src/main/java/com/apiexternabackend/resources/AcaoResource.java
- Notas: POST /acoes, GET /acoes (paginado), GET /acoes/ticker/{ticker}, PUT /acoes/{id}/atualizar-cotacao.

## T-207 — Testes da ação [pendente]

- Refs: AC-201, AC-202, AC-203, AC-204, AC-205, AC-206, AC-207, AC-208, AC-209, AC-210, AC-211
- Arquivos: src/test/java/com/apiexternabackend/services/AcaoServiceTest.java, src/test/java/com/apiexternabackend/resources/AcaoResourceTest.java
- Notas: fontes de cotação mockadas; um teste por critério.

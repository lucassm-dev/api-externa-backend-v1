# Tasks: Conversão de câmbio USD → BRL e consolidação da carteira

> feature: spec-08-conversao-cambio

## T-448 — Migração V15: câmbio em Operacao e CarteiraAcao [pendente]

- Refs: AC-486, AC-487
- Arquivos: src/main/resources/db/migration/V15__cambio_operacao_carteira_acao.sql

## T-449 — Cliente AwesomeAPI (câmbio USD-BRL) [pendente]

- Refs: AC-486, AC-488
- Arquivos: src/main/java/com/apiexternabackend/infra/client/awesomeapi/AwesomeApiCambioClient.java, src/main/java/com/apiexternabackend/infra/client/awesomeapi/dtos/AwesomeApiCotacaoDTO.java

## T-450 — Cliente PTAX BCB (fallback) [pendente]

- Refs: AC-488
- Arquivos: src/main/java/com/apiexternabackend/infra/client/bcb/BcbPtaxClient.java, src/main/java/com/apiexternabackend/infra/client/bcb/dtos/BcbPtaxResponseDTO.java

## T-451 — CambioFacade: AwesomeAPI primário, PTAX fallback [pendente]

- Refs: AC-488
- Arquivos: src/main/java/com/apiexternabackend/infra/facade/CambioFacade.java

## T-452 — CambioCacheService: TTL global + fallback última taxa conhecida [pendente]

- Refs: AC-489, AC-490, AC-491, AC-492
- Arquivos: src/main/java/com/apiexternabackend/services/CambioCacheService.java, src/main/resources/application.properties

## T-453 — Entidades: Operacao e CarteiraAcao com novos campos [pendente]

- Refs: AC-486, AC-487
- Arquivos: src/main/java/com/apiexternabackend/domains/Operacao.java, src/main/java/com/apiexternabackend/domains/CarteiraAcao.java

## T-454 — OperacaoService.comprar/vender gravam taxa de câmbio (uniforme BRL=1) [pendente]

- Refs: AC-486, AC-487, AC-489, AC-490
- Arquivos: src/main/java/com/apiexternabackend/services/OperacaoService.java, src/main/java/com/apiexternabackend/domains/dtos/OperacaoResponseDTO.java, src/main/java/com/apiexternabackend/mappers/OperacaoMapper.java

## T-455 — PosicaoService.recalcular: custoTotalBrl e lucroRealizadoBrl [pendente]

- Refs: AC-493, AC-495, AC-496
- Arquivos: src/main/java/com/apiexternabackend/services/PosicaoService.java

## T-456 — Corrige lucro-realizado para somar valores em BRL [pendente]

- Refs: AC-496
- Arquivos: src/main/java/com/apiexternabackend/services/ConsultaOperacaoService.java

## T-457 — Endpoint GET /carteiras/{id}/consolidado [pendente]

- Refs: AC-493, AC-494, AC-495
- Arquivos: src/main/java/com/apiexternabackend/services/ConsultaOperacaoService.java, src/main/java/com/apiexternabackend/domains/dtos/CarteiraConsolidadaResponseDTO.java, src/main/java/com/apiexternabackend/resources/OperacaoResource.java

## T-458 — Testes de serviço: CambioFacade, CambioCacheService, recálculo e consolidado [pendente]

- Refs: AC-486, AC-487, AC-488, AC-489, AC-490, AC-491, AC-492, AC-493, AC-495, AC-496
- Arquivos: src/test/java/com/apiexternabackend/infra/facade/CambioFacadeTest.java, src/test/java/com/apiexternabackend/services/CambioCacheServiceTest.java, src/test/java/com/apiexternabackend/services/PosicaoServiceTest.java, src/test/java/com/apiexternabackend/services/OperacaoServiceTest.java, src/test/java/com/apiexternabackend/services/ConsultaOperacaoServiceTest.java

## T-459 — Testes de resource: consolidado e taxa exposta na resposta [pendente]

- Refs: AC-486, AC-493, AC-494
- Arquivos: src/test/java/com/apiexternabackend/resources/OperacaoResourceTest.java

## T-460 — Atualizar catálogo de erros e mapeamento vivo [pendente]

- Refs: AC-486, AC-487, AC-488, AC-489, AC-490, AC-491, AC-492, AC-493, AC-494, AC-495, AC-496
- Arquivos: docs/erros.md, docs/mapeamento-atual.html

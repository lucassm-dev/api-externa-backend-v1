# Tasks: Extrato por período e por ticker

> feature: spec-26-extrato-periodo-ticker

## T-519 — Filtrar o extrato por período e ticker [concluida]
- Refs: US-473, AC-640, AC-641, AC-642, AC-643
- Arquivos: src/main/java/com/apiexternabackend/resources/OperacaoResource.java, src/main/java/com/apiexternabackend/services/ConsultaOperacaoService.java, src/main/java/com/apiexternabackend/repositories/OperacaoRepository.java, docs/erros.md, src/test/java/com/apiexternabackend/config/ExtratoFiltrosIntegrationTest.java
- Notas: TDD (Aula 15). Filtro na query, antes da paginação (ADR-018).

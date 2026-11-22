# Tasks: Extrato por carteira, tipo e resultado

> feature: spec-27-extrato-carteira-tipo-resultado

## T-520 — Filtrar o extrato por carteira, tipo e resultado [concluida]
- Refs: US-474, AC-644, AC-645, AC-646, AC-647, AC-648, AC-649
- Arquivos: src/main/java/com/apiexternabackend/resources/OperacaoResource.java, src/main/java/com/apiexternabackend/services/ConsultaOperacaoService.java, src/main/java/com/apiexternabackend/repositories/OperacaoRepository.java, src/test/java/com/apiexternabackend/config/ExtratoFiltrosIntegrationTest.java, src/main/java/com/apiexternabackend/domains/enums/ResultadoOperacao.java
- Notas: TDD (Aula 15). Filtro na query, antes da paginação (ADR-018). `resultado` vira enum novo com LUCRO e PREJUIZO.

## T-521 — Parâmetro de URL inválido responde 400 VAL-002 [concluida]
- Refs: US-475, AC-650
- Arquivos: src/main/java/com/apiexternabackend/resources/exceptions/GlobalExceptionHandler.java, docs/erros.md, src/test/java/com/apiexternabackend/config/ExtratoFiltrosIntegrationTest.java
- Notas: TDD (Aula 15). Handler para MethodArgumentTypeMismatchException; afeta todas as rotas (domínio da spec-01).

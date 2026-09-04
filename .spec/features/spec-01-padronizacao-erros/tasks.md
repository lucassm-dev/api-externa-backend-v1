# Tasks: SPEC-01 — Padronização e tratamento de erros

> feature: spec-01-padronizacao-erros

## T-407 — Fundação: hierarquia de exceções, StandardError com código, GlobalExceptionHandler [em-andamento]

- Refs: US-412
- Arquivos: src/main/java/com/apiexternabackend/resources/exceptions/NegocioException.java, src/main/java/com/apiexternabackend/resources/exceptions/RecursoNaoEncontradoException.java, src/main/java/com/apiexternabackend/resources/exceptions/RecursoDuplicadoException.java, src/main/java/com/apiexternabackend/resources/exceptions/RegraVioladaException.java, src/main/java/com/apiexternabackend/resources/exceptions/IntegracaoExternaException.java, src/main/java/com/apiexternabackend/resources/exceptions/StandardError.java, src/main/java/com/apiexternabackend/resources/exceptions/GlobalExceptionHandler.java, src/main/java/com/apiexternabackend/resources/exceptions/ResourceNotFoundException.java, src/main/java/com/apiexternabackend/resources/exceptions/DuplicateResourceException.java, src/main/java/com/apiexternabackend/resources/exceptions/BusinessException.java, src/main/java/com/apiexternabackend/resources/exceptions/ExternalServiceException.java
- Notas: pré-requisito de compilação para todas as outras tarefas — feita sequencialmente, fora do plano paralelo, antes de liberar T-408..T-412.

## T-408 — Migrar catálogo de Ação (ACA-001/002) e comportamento de cota estourada em atualizar-cotacao [pendente]

- Refs: AC-429, AC-431, AC-432, AC-433
- Arquivos: src/main/java/com/apiexternabackend/services/AcaoService.java, src/test/java/com/apiexternabackend/services/AcaoServiceTest.java, src/test/java/com/apiexternabackend/resources/AcaoResourceTest.java
- Notas: `atualizarCotacao` passa a distinguir limite excedido (propaga IntegracaoExternaException → 429) de indisponibilidade (mantém fallback pra última cotação, AC-210/AC-432). Ajustar teste que hoje espera 502 em cota excedida para esperar 429.

## T-409 — Migrar adapters de cotação (Brapi/TwelveData) para IntegracaoExternaException tipada (EXT-008/009/010) [pendente]

- Refs: AC-429, AC-430
- Arquivos: src/main/java/com/apiexternabackend/infra/adapter/BrapiAdapter.java, src/main/java/com/apiexternabackend/infra/adapter/TwelveDataAdapter.java, src/main/java/com/apiexternabackend/config/FeignConfig.java
- Notas: rate limit (HTTP 429 do provedor) → IntegracaoExternaException com limiteExcedido=true (429); indisponibilidade/5xx → limiteExcedido=false (503); ticker não encontrado continua RegraVioladaException (422, EXT-008).

## T-410 — Migrar catálogo de Corretora (COR-001/002/003) e separar CVM indisponível de CVM não autorizada (EXT-007) [pendente]

- Refs: AC-433, AC-434
- Arquivos: src/main/java/com/apiexternabackend/services/CorretoraService.java, src/main/java/com/apiexternabackend/infra/facade/CnpjFacade.java, src/main/java/com/apiexternabackend/infra/facade/CepFacade.java, src/main/java/com/apiexternabackend/infra/facade/CvmFacade.java, src/main/java/com/apiexternabackend/config/CvmFeignConfig.java, src/test/java/com/apiexternabackend/services/CorretoraServiceTest.java, src/test/java/com/apiexternabackend/resources/CorretoraResourceTest.java, src/test/java/com/apiexternabackend/infra/facade/CnpjFacadeTest.java
- Notas: `CorretoraService.cadastrar` passa a checar `resultadoCvm.falhaVerificacao()` (→ IntegracaoExternaException EXT-007, 503) separado de `!resultadoCvm.autorizada()` (→ RegraVioladaException COR-003, 422) — comportamento novo confirmado com o Lucas (AC-434).

## T-411 — Migrar catálogo de Carteira (CAR-001) e Investidor/AUT (AUT-001/002/003) [pendente]

- Refs: AC-433
- Arquivos: src/main/java/com/apiexternabackend/services/CarteiraService.java, src/main/java/com/apiexternabackend/services/InvestidorService.java, src/test/java/com/apiexternabackend/services/CarteiraServiceTest.java, src/test/java/com/apiexternabackend/resources/CarteiraResourceTest.java, src/test/java/com/apiexternabackend/resources/InvestidorResourceTest.java
- Notas: prefixo AUT- reservado para Investidor porque a SPEC-02 unifica Investidor com autenticação/JWT.

## T-412 — Migrar catálogo de Operação (OPE-001/002/003/004) [pendente]

- Refs: AC-433
- Arquivos: src/main/java/com/apiexternabackend/services/OperacaoService.java, src/test/java/com/apiexternabackend/services/OperacaoServiceTest.java, src/test/java/com/apiexternabackend/resources/OperacaoResourceTest.java
- Notas: nenhuma mudança de status HTTP aqui — só troca de tipo de exceção e adição de código.

## T-413 — Catálogo docs/erros.md e testes diretos do GlobalExceptionHandler (AC-426/427/428) [pendente]

- Refs: AC-426, AC-427, AC-428, AC-433
- Arquivos: docs/erros.md, src/test/java/com/apiexternabackend/resources/exceptions/GlobalExceptionHandlerTest.java
- Notas: depende de T-408..T-412 concluídas (o catálogo documenta os códigos que elas introduzem). Teste novo cobre: payload padronizado genérico, validação com fieldErrors, exceção não mapeada → 500 com código SYS-001 sem stacktrace.

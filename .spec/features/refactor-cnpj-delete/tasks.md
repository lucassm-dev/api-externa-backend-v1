# Tasks: Refactor cnpj delete

> feature: refactor-cnpj-delete

## T-001 — Validação CNPJ no CnpjFacade + DELETE Corretora [concluida]
- Refs: US-408, US-409, AC-415, AC-416, AC-417, AC-418, AC-419, AC-420, AC-421
- Arquivos: src/main/java/com/apiexternabackend/infra/facade/CnpjFacade.java, src/main/java/com/apiexternabackend/services/CorretoraService.java, src/main/java/com/apiexternabackend/domains/Corretora.java, src/main/java/com/apiexternabackend/resources/CorretoraResource.java, src/main/resources/db/migration/V8__add_ativo_corretora.sql, src/main/java/com/apiexternabackend/config/CvmFeignConfig.java, src/test/java/com/apiexternabackend/services/CorretoraServiceTest.java, src/test/java/com/apiexternabackend/resources/CorretoraResourceTest.java
- Esforço: medio

## T-002 — DELETE Investidor [pendente]

- Refs: US-410, AC-422, AC-423
- Arquivos: src/main/java/com/apiexternabackend/domains/Investidor.java, src/main/java/com/apiexternabackend/services/InvestidorService.java, src/main/java/com/apiexternabackend/resources/InvestidorResource.java, src/main/resources/db/migration/V9__add_ativo_investidor.sql, src/test/java/com/apiexternabackend/services/InvestidorServiceTest.java, src/test/java/com/apiexternabackend/resources/InvestidorResourceTest.java
- Esforço: medio

## T-003 — DELETE Ação [pendente]

- Refs: US-411, AC-424, AC-425
- Arquivos: src/main/java/com/apiexternabackend/domains/Acao.java, src/main/java/com/apiexternabackend/services/AcaoService.java, src/main/java/com/apiexternabackend/resources/AcaoResource.java, src/main/resources/db/migration/V10__add_ativo_acao.sql, src/test/java/com/apiexternabackend/services/AcaoServiceTest.java, src/test/java/com/apiexternabackend/resources/AcaoResourceTest.java
- Esforço: medio

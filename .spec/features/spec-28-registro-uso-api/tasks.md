<!-- onp-spec:documentation-contract=v1 -->

# Tasks: Registro de uso da API por usuário

> feature: spec-28-registro-uso-api

## T-522 — Registrar cada requisição autenticada sem dado sensível [em-andamento]

- Refs: US-476, AC-651, AC-652, AC-653
- Arquivos: src/main/resources/db/migration/V24__registro_uso.sql, src/main/java/com/apiexternabackend/domains/RegistroUso.java, src/main/java/com/apiexternabackend/repositories/RegistroUsoRepository.java, src/main/java/com/apiexternabackend/config/RegistroUsoFilter.java, src/main/java/com/apiexternabackend/config/SecurityConfig.java, src/main/java/com/apiexternabackend/services/RegistroUsoService.java, src/test/java/com/apiexternabackend/config/RegistroUsoIntegrationTest.java
- Notas: TDD (Aula 15), um ciclo vermelho/verde por critério. O AC-653 já passou ao ser escrito (caracterização). ASM-506 (403) teve ciclo próprio. Filtro dentro da cadeia do Spring Security, depois do `AuthorizationFilter`; rota pelo padrão do handler (`BEST_MATCHING_PATTERN_ATTRIBUTE`).

## T-523 — Gravar o registro sem atrasar nem derrubar a requisição [em-andamento]

- Refs: US-477, AC-654, AC-655
- Arquivos: src/main/java/com/apiexternabackend/services/RegistroUsoService.java, src/main/java/com/apiexternabackend/config/RegistroUsoFilter.java, src/main/java/com/apiexternabackend/ApiExternaBackendApplication.java, src/test/java/com/apiexternabackend/config/RegistroUsoIntegrationTest.java, src/test/java/com/apiexternabackend/config/RegistroUsoTrigger.java, src/main/resources/application-test.properties
- Notas: depende da T-522. `@Async` no executor padrão do Spring Boot; erro na gravação vai para o log e não sobe para a requisição. AC-655 já passou ao ser escrito (caracterização). Perfil de teste com `QUERY_CACHE_SIZE=0` no H2.

## T-524 — Descarte diário dos registros com mais de 90 dias [em-andamento]

- Refs: US-478, AC-656
- Arquivos: src/main/java/com/apiexternabackend/services/RegistroUsoService.java, src/main/java/com/apiexternabackend/repositories/RegistroUsoRepository.java, src/main/java/com/apiexternabackend/ApiExternaBackendApplication.java, src/test/java/com/apiexternabackend/config/RegistroUsoIntegrationTest.java
- Notas: depende da T-523. `@Scheduled` às 3h de Brasília; `DELETE` em lote por `registrado_em`.

## Documentos relacionados

[PRD](prd.md) · [ADR](adr.md) · [TDD](design.md) · [Spec](spec.md) · [Pesquisa](research.md)

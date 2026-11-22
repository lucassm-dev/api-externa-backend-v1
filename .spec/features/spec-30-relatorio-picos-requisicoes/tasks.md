# Tasks: Relatório de picos de requisições

> feature: spec-30-relatorio-picos-requisicoes

## T-527 — Relatório de picos e usuários por período [pendente]

- Refs: US-480, AC-660, AC-661, AC-662, AC-663, AC-665
- Arquivos: src/main/java/com/apiexternabackend/resources/RelatorioUsoResource.java, src/main/java/com/apiexternabackend/services/RelatorioUsoService.java, src/main/java/com/apiexternabackend/repositories/RegistroUsoRepository.java, src/main/java/com/apiexternabackend/domains/dtos/RelatorioUsoResponseDTO.java, src/test/java/com/apiexternabackend/config/RelatorioUsoIntegrationTest.java
- Notas: TDD (Aula 15), um ciclo vermelho/verde por critério. Consultas nativas com `DATE_TRUNC('hour', ...)` e `LIMIT 10`, que funcionam no H2 e no PostgreSQL.

## T-528 — Período invertido responde 422 AUT-018 [pendente]

- Refs: US-480, AC-664
- Arquivos: src/main/java/com/apiexternabackend/services/RelatorioUsoService.java, docs/erros.md, src/test/java/com/apiexternabackend/config/RelatorioUsoIntegrationTest.java
- Notas: depende da T-527. Mesmo padrão do OPE-006 (`RegraVioladaException`).

## T-529 — Registrar no PRD 15.5 a decisão de identificar o usuário [pendente]

- Refs: US-480
- Arquivos: docs/prd/PRD-sistema-investimentos-acoes.md
- Notas: pedido da issue #36; troca o `[A]` da identificação pela decisão de 2026-11-22.

## Documentos relacionados

[Spec](spec.md)

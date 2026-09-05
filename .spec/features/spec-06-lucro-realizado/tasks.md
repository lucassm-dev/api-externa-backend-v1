# Tasks: Lucro realizado e exclusão segura de operação

> feature: spec-06-lucro-realizado

## T-429 — Migração V14: campos de lucro realizado e soft delete em Operacao [pendente]

- Refs: AC-469, AC-472
- Arquivos: src/main/resources/db/migration/V14__lucro_realizado_soft_delete_operacao.sql
- Notas: `ativo boolean not null default true`, `preco_medio_compra_no_momento numeric(18,4)` (nullable), `lucro_realizado numeric(18,4)` (nullable).

## T-430 — Entidade Operacao + repositório com filtro por ativo [pendente]

- Refs: AC-469, AC-472, AC-473
- Arquivos: src/main/java/com/apiexternabackend/domains/Operacao.java, src/main/java/com/apiexternabackend/repositories/OperacaoRepository.java

## T-431 — OperacaoService: soft delete e busca filtrando inativas [pendente]

- Refs: AC-472, AC-473
- Arquivos: src/main/java/com/apiexternabackend/services/OperacaoService.java

## T-432 — PosicaoService.recalcular: recompõe lucro realizado a partir do histórico ativo [pendente]

- Refs: AC-469, AC-474, AC-475, AC-476
- Arquivos: src/main/java/com/apiexternabackend/services/PosicaoService.java
- Notas: reprocessa a série ordenada por data (só ativas) e regrava `precoMedioCompraNoMomento`/`lucroRealizado` em toda venda — é isso que faz o recálculo em cascata (AC-476) funcionar sem lógica especial em `editar()` (ASM-423).

## T-433 — Consulta de lucro realizado por carteira e por ticker [pendente]

- Refs: AC-470, AC-471
- Arquivos: src/main/java/com/apiexternabackend/services/ConsultaOperacaoService.java, src/main/java/com/apiexternabackend/repositories/OperacaoRepository.java, src/main/java/com/apiexternabackend/domains/dtos/LucroRealizadoResponseDTO.java

## T-434 — Endpoint GET /carteiras/{id}/lucro-realizado [pendente]

- Refs: AC-470, AC-471
- Arquivos: src/main/java/com/apiexternabackend/resources/OperacaoResource.java

## T-435 — Expor precoMedioCompraNoMomento e lucroRealizado na resposta da operação [pendente]

- Refs: AC-469
- Arquivos: src/main/java/com/apiexternabackend/domains/dtos/OperacaoResponseDTO.java, src/main/java/com/apiexternabackend/mappers/OperacaoMapper.java

## T-436 — Testes de serviço: lucro realizado, soft delete e recálculo em cascata [pendente]

- Refs: AC-469, AC-470, AC-471, AC-472, AC-473, AC-474, AC-475, AC-476
- Arquivos: src/test/java/com/apiexternabackend/services/OperacaoServiceTest.java, src/test/java/com/apiexternabackend/services/PosicaoServiceTest.java, src/test/java/com/apiexternabackend/services/ConsultaOperacaoServiceTest.java

## T-437 — Testes de resource: endpoint de lucro realizado e 404 em operação excluída [pendente]

- Refs: AC-470, AC-471, AC-473
- Arquivos: src/test/java/com/apiexternabackend/resources/OperacaoResourceTest.java

## T-438 — Atualizar catálogo de erros e mapeamento vivo [pendente]

- Refs: AC-469, AC-470, AC-471, AC-472, AC-473, AC-474, AC-475, AC-476
- Arquivos: docs/erros.md, docs/mapeamento-atual.html

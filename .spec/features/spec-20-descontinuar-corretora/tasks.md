# Tasks: Descontinuar corretora

> feature: spec-20-descontinuar-corretora

## T-511 — ADMIN descontinua corretora [concluida]
- Refs: US-463, US-465, AC-593, AC-594, AC-595, AC-596, AC-597, AC-601
- Arquivos: src/main/resources/db/migration/V20__descontinuar_corretora.sql, src/main/java/com/apiexternabackend/domains/Corretora.java, src/main/java/com/apiexternabackend/domains/dtos/CorretoraResponseDTO.java, src/main/java/com/apiexternabackend/domains/dtos/DescontinuacaoRequestDTO.java, src/main/java/com/apiexternabackend/domains/dtos/CarteirasAfetadasResponseDTO.java, src/main/java/com/apiexternabackend/repositories/CorretoraRepository.java, src/main/java/com/apiexternabackend/services/CorretoraService.java, src/main/java/com/apiexternabackend/resources/CorretoraResource.java, docs/erros.md, src/test/java/com/apiexternabackend/services/CorretoraServiceTest.java, src/test/java/com/apiexternabackend/config/DescontinuarCorretoraIntegrationTest.java
- Notas: decisões 1 a 3. Novo código COR-006 (já descontinuada). Registro `DESCONTINUACAO_CORRETORA` na mesma transação (spec-19). AC-601 não muda código: a exclusão já olha carteira ativa.

## T-512 — Carteira de corretora descontinuada [concluida]
- Refs: US-464, AC-598, AC-599, AC-600
- Arquivos: src/test/java/com/apiexternabackend/resources/CarteiraResourceTest.java, src/test/java/com/apiexternabackend/services/CarteiraServiceTest.java, src/main/java/com/apiexternabackend/services/CarteiraService.java, src/main/java/com/apiexternabackend/domains/dtos/CarteiraResponseDTO.java, src/main/java/com/apiexternabackend/mappers/CarteiraMapper.java, docs/erros.md, src/test/java/com/apiexternabackend/config/DescontinuarCorretoraIntegrationTest.java
- Notas: novo código COR-005. OperacaoService não consulta a corretora, então AC-599 é teste de regressão.

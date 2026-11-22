# Tasks: Trocar senha no perfil

> feature: spec-23-trocar-senha

## T-516 — Trocar senha com a senha atual [concluida]
- Refs: US-470, AC-620, AC-621, AC-622, AC-623, AC-624
- Arquivos: src/main/java/com/apiexternabackend/services/AutenticacaoService.java, src/main/java/com/apiexternabackend/resources/AuthResource.java, src/main/java/com/apiexternabackend/domains/dtos/TrocarSenhaRequestDTO.java, docs/erros.md, src/test/java/com/apiexternabackend/config/TrocarSenhaIntegrationTest.java
- Notas: decisões 1 a 3. Reaproveita `senhaAlteradaEm` (spec-22).

# Tasks: Relatório de usuários por situação

> feature: spec-29-relatorio-usuarios-situacao

## T-525 — Filtro por papel na listagem de usuários [pendente]

- Refs: US-479, AC-657
- Arquivos: src/main/java/com/apiexternabackend/resources/UsuarioAdminResource.java, src/main/java/com/apiexternabackend/services/UsuarioAdminService.java, src/main/java/com/apiexternabackend/repositories/UsuarioRepository.java, src/test/java/com/apiexternabackend/config/GestaoUsuariosIntegrationTest.java
- Notas: TDD (Aula 15), um ciclo vermelho/verde por critério.

## T-526 — Totais por situação [pendente]

- Refs: US-479, AC-658, AC-659
- Arquivos: src/main/java/com/apiexternabackend/resources/UsuarioAdminResource.java, src/main/java/com/apiexternabackend/services/UsuarioAdminService.java, src/main/java/com/apiexternabackend/repositories/UsuarioRepository.java, src/test/java/com/apiexternabackend/config/GestaoUsuariosIntegrationTest.java
- Notas: depende da T-525. A rota herda o `@PreAuthorize` da classe; AC-659 deve passar como caracterização.

## Documentos relacionados

[Spec](spec.md)

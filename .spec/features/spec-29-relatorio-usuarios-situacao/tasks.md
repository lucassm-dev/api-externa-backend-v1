# Tasks: Relatório de usuários por situação

> feature: spec-29-relatorio-usuarios-situacao

## T-525 — Filtro por papel na listagem de usuários [concluida]

- Refs: US-479, AC-657
- Arquivos: src/main/java/com/apiexternabackend/resources/UsuarioAdminResource.java, src/main/java/com/apiexternabackend/services/UsuarioAdminService.java, src/main/java/com/apiexternabackend/repositories/UsuarioRepository.java, src/test/java/com/apiexternabackend/config/GestaoUsuariosIntegrationTest.java, src/test/java/com/apiexternabackend/config/IdentidadeUsuarioIntegrationTest.java
- Notas: TDD (Aula 15), um ciclo vermelho/verde por critério. A varredura do AC-568 (spec-16) passa a ignorar parâmetros de rota `GET` (ASM-510).

## T-526 — Totais por situação [concluida]

- Refs: US-479, AC-658, AC-659
- Arquivos: src/main/java/com/apiexternabackend/resources/UsuarioAdminResource.java, src/main/java/com/apiexternabackend/services/UsuarioAdminService.java, src/main/java/com/apiexternabackend/repositories/UsuarioRepository.java, src/test/java/com/apiexternabackend/config/GestaoUsuariosIntegrationTest.java
- Notas: depende da T-525. A rota herda o `@PreAuthorize` da classe; AC-659 passou ao ser escrito (caracterização).

## Documentos relacionados

[Spec](spec.md)

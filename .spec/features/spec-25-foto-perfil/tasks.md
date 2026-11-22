# Tasks: Foto de perfil

> feature: spec-25-foto-perfil

## T-518 — Enviar, trocar, remover e ler a própria foto [concluida]
- Refs: US-472, AC-633, AC-634, AC-635, AC-636, AC-637, AC-638, AC-639
- Arquivos: src/main/java/com/apiexternabackend/services/FotoPerfilService.java, src/main/java/com/apiexternabackend/domains/FotoPerfil.java, src/main/java/com/apiexternabackend/repositories/FotoPerfilRepository.java, src/main/java/com/apiexternabackend/resources/AuthResource.java, src/main/java/com/apiexternabackend/services/AutenticacaoService.java, src/main/java/com/apiexternabackend/domains/dtos/UsuarioLogadoResponseDTO.java, src/main/java/com/apiexternabackend/resources/exceptions/GlobalExceptionHandler.java, src/main/resources/db/migration/V23__foto_perfil.sql, src/main/resources/application.properties, docs/erros.md, src/test/java/com/apiexternabackend/config/FotoPerfilIntegrationTest.java
- Notas: TDD em commits separados (Aula 15). Ciclos: 1 tamanho (AC-633), 2 formato (AC-634), 3 envio e leitura (AC-635), 4 remoção e fotoUrl (AC-637, AC-639); AC-636 e AC-638 entram como testes de caracterização.

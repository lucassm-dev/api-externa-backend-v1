# Tasks: Identidade de acesso (usuario) separada do perfil de investidor

> feature: spec-15-identidade-usuario

## T-498 — Separa usuario de investidor no domínio, na migração e no login [concluida]
- Refs: US-447, US-448, US-449, US-450, AC-552, AC-553, AC-554, AC-555, AC-557, AC-503, AC-505, AC-506, AC-507, AC-508
- Arquivos: src/main/resources/db/migration/V17__usuario_separado_do_investidor.sql, src/main/java/com/apiexternabackend/domains/Usuario.java, src/main/java/com/apiexternabackend/domains/enums/Papel.java, src/main/java/com/apiexternabackend/domains/enums/SituacaoUsuario.java, src/main/java/com/apiexternabackend/repositories/UsuarioRepository.java, src/main/java/com/apiexternabackend/domains/Investidor.java, src/main/java/com/apiexternabackend/repositories/InvestidorRepository.java, src/main/java/com/apiexternabackend/services/AutenticacaoService.java, src/main/java/com/apiexternabackend/services/InvestidorService.java, src/main/java/com/apiexternabackend/mappers/InvestidorMapper.java, src/main/java/com/apiexternabackend/resources/exceptions/AcessoNegadoException.java, src/test/java/com/apiexternabackend/config/IdentidadeUsuarioIntegrationTest.java, src/test/java/com/apiexternabackend/services/AutenticacaoServiceTest.java, src/test/java/com/apiexternabackend/services/InvestidorServiceTest.java, src/test/java/com/apiexternabackend/services/CarteiraServiceTest.java, src/test/java/com/apiexternabackend/services/OperacaoServiceTest.java
- Esforço: alto
- Notas: decisões 1 a 4 do Contexto. Uma tarefa só porque nada compila sem as outras partes: `Investidor` perde `email`/`senha` e todo teste que monta investidor quebra junto. Cadastro com `@Transactional` (AC-554). Bloqueado com senha certa → 403 `AUT-009` (AC-552); não havia exceção de negócio com 403, então nasceu `AcessoNegadoException` (o `GlobalExceptionHandler` já usa o status da exceção). Os testes de integração existentes passaram sem mudança, porque cadastram pela API.

## T-499 — Testa a migração V17 num Postgres real [pendente]
- Refs: AC-551, AC-556
- Arquivos: pom.xml, src/test/java/com/apiexternabackend/config/MigracaoUsuarioPostgresTest.java
- Notas: decisão 5. Testcontainers (escopo test) com Flyway até a V16, insere investidor com senha em BCrypt, migra para a V17 e faz login pelo `AutenticacaoService`. Depende da T-498.

## T-500 — Specs antigas apontam para a spec-15 [pendente]
- Refs: AC-505
- Arquivos: .spec/features/spec-10-vazamento-investidor-inativo/spec.md, .spec/features/autenticacao-investidor/spec.md
- Notas: suposições que falam de e-mail e senha em `investidor` (por exemplo ASM-440) passam a apontar para a spec-15.

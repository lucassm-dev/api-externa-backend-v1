# Tasks: ADMIN gerencia usuários

> feature: spec-18-gerenciar-usuarios

## T-505 — Bloquear e desbloquear usuário [concluida]
- Refs: US-460, AC-581, AC-582, AC-583, AC-584, AC-586
- Arquivos: src/main/resources/db/migration/V18__bloqueio_usuario.sql, src/main/java/com/apiexternabackend/domains/Usuario.java, src/main/java/com/apiexternabackend/domains/dtos/BloqueioRequestDTO.java, src/main/java/com/apiexternabackend/domains/dtos/UsuarioAdminResponseDTO.java, src/main/java/com/apiexternabackend/services/UsuarioAdminService.java, src/main/java/com/apiexternabackend/resources/UsuarioAdminResource.java, docs/erros.md, src/test/java/com/apiexternabackend/config/GestaoUsuariosIntegrationTest.java
- Notas: decisões 1 e 3. Colunas `motivo_bloqueio` (255) e `bloqueado_em`. Novos códigos AUT-011 (422, bloquear a si mesmo) e AUT-012 (404, usuário não encontrado) em docs/erros.md. Id do ADMIN vem do principal (ASM-475 da spec-16).

## T-506 — Bloqueio vale na requisição seguinte [concluida]
- Refs: US-460, AC-585
- Arquivos: src/main/java/com/apiexternabackend/config/SecurityConfig.java, src/main/java/com/apiexternabackend/config/JwtAuthenticationEntryPoint.java, src/main/java/com/apiexternabackend/repositories/UsuarioRepository.java, docs/erros.md, src/test/java/com/apiexternabackend/config/BloqueioImediatoIntegrationTest.java, src/test/java/com/apiexternabackend/config/MatrizAcessoIntegrationTest.java
- Notas: decisão 4. O conversor do JWT deixa de ser estático e consulta a situação (ASM-484); bloqueado lança exceção de autenticação que o entry point traduz para 401 AUT-010; excluído vira AUT-005 (ASM-485). Rodar a suíte inteira: todos os testes com token passam a precisar de usuário no banco ou de mock do repositório.

## T-507 — Listagem com indicadores [concluida]
- Refs: US-458, AC-576, AC-577, AC-578
- Arquivos: src/main/java/com/apiexternabackend/services/UsuarioAdminService.java, src/main/java/com/apiexternabackend/resources/UsuarioAdminResource.java, src/main/java/com/apiexternabackend/domains/dtos/UsuarioAdminResponseDTO.java, src/main/java/com/apiexternabackend/repositories/UsuarioRepository.java, src/main/java/com/apiexternabackend/repositories/CarteiraRepository.java, src/main/java/com/apiexternabackend/repositories/CarteiraAcaoRepository.java, src/test/java/com/apiexternabackend/config/GestaoUsuariosIntegrationTest.java
- Notas: decisão 2. Indicadores conforme ASM-487, ordem ASM-489. AC-578 cobre as quatro rotas (depende de T-505 e T-508 existirem).

## T-508 — Criar ADMIN [concluida]
- Refs: US-459, AC-579, AC-580
- Arquivos: src/main/java/com/apiexternabackend/domains/dtos/CriarAdminRequestDTO.java, src/main/java/com/apiexternabackend/services/UsuarioAdminService.java, src/main/java/com/apiexternabackend/resources/UsuarioAdminResource.java, src/test/java/com/apiexternabackend/config/GestaoUsuariosIntegrationTest.java
- Notas: reaproveitar a política de senha e a checagem de e-mail do AutenticacaoService (AUT-001/AUT-008).

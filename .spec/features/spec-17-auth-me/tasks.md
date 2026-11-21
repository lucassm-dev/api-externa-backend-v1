# Tasks: GET /auth/me com os dados e o papel do usuário logado

> feature: spec-17-auth-me

## T-504 — Rota GET /auth/me [pendente]
- Refs: US-456, US-457, AC-569, AC-570, AC-571, AC-572, AC-573, AC-574, AC-575
- Arquivos: src/main/java/com/apiexternabackend/config/SecurityConfig.java, src/main/java/com/apiexternabackend/resources/AuthResource.java, src/main/java/com/apiexternabackend/services/AutenticacaoService.java, src/main/java/com/apiexternabackend/domains/dtos/UsuarioLogadoResponseDTO.java, src/test/java/com/apiexternabackend/config/AuthMeIntegrationTest.java
- Notas: decisões 1 a 4 do Contexto. Uma tarefa só: a rota, o DTO e a liberação das rotas públicas não fazem sentido separados. O nome vem de `investidorRepository.findById` (vazio para ADMIN). Conta excluída ou inexistente lança a mesma resposta do entry point (401 `AUT-005`, "Token ausente ou inválido.").

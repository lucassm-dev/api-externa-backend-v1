# Tasks: Autenticacao investidor (SPEC-02)

> feature: autenticacao-investidor

## T-414 — Fundação: dependências, entidade, JWT, Spring Security, AutenticacaoService, /auth [concluida]
- Refs: US-001, US-002, US-004
- Arquivos: pom.xml, src/main/java/com/apiexternabackend/domains/Investidor.java, src/main/resources/db/migration/V11__add_senha_criado_em_investidor.sql, src/main/java/com/apiexternabackend/config/SecurityConfig.java, src/main/java/com/apiexternabackend/config/JwtService.java, src/main/java/com/apiexternabackend/config/JwtAuthenticationFilter.java, src/main/java/com/apiexternabackend/config/JwtAuthenticationEntryPoint.java, src/main/java/com/apiexternabackend/config/JwtAccessDeniedHandler.java, src/main/java/com/apiexternabackend/config/InvestidorPrincipal.java, src/main/java/com/apiexternabackend/services/AutenticacaoService.java, src/main/java/com/apiexternabackend/resources/AuthResource.java, src/main/java/com/apiexternabackend/domains/dtos/AuthCadastroRequestDTO.java, src/main/java/com/apiexternabackend/domains/dtos/AuthLoginRequestDTO.java, src/main/java/com/apiexternabackend/domains/dtos/AuthTokenResponseDTO.java, src/main/java/com/apiexternabackend/resources/exceptions/CredenciaisInvalidasException.java, .env, .env.example, src/main/resources/application-test.properties, docs/erros.md
- Notas: pré-requisito para todo o resto — feita sequencialmente, fora do plano paralelo, antes de liberar T-415..T-417. Ver design.md para o fluxo completo.

## T-415 — Isolamento de Carteira: investidorId vem do token, não do corpo [pendente]

- Refs: AC-006, AC-007, AC-443
- Arquivos: src/main/java/com/apiexternabackend/domains/dtos/CarteiraRequestDTO.java, src/main/java/com/apiexternabackend/services/CarteiraService.java, src/main/java/com/apiexternabackend/resources/CarteiraResource.java, src/test/java/com/apiexternabackend/services/CarteiraServiceTest.java, src/test/java/com/apiexternabackend/resources/CarteiraResourceTest.java
- Notas: `CarteiraService.buscarAtiva/buscarPorId/renomear/excluir/listarPorInvestidor` passam a exigir `Long investidorId` (do principal); carteira de outro investidor → `RecursoNaoEncontradoException("CAR-001", ...)`, igual a "não existe" (ASM-411).

## T-416 — Isolamento de Operação: idem, via carteira do investidor do token [pendente]

- Refs: AC-006, AC-007, AC-443
- Arquivos: src/main/java/com/apiexternabackend/services/OperacaoService.java, src/main/java/com/apiexternabackend/services/ConsultaOperacaoService.java, src/main/java/com/apiexternabackend/resources/OperacaoResource.java, src/test/java/com/apiexternabackend/services/OperacaoServiceTest.java, src/test/java/com/apiexternabackend/resources/OperacaoResourceTest.java
- Notas: `GET /operacoes` e `GET /carteiras/{id}/posicoes` passam a usar o investidor do token (não mais `@RequestParam`/livre). `PUT`/`DELETE /operacoes/{id}` passam a checar que a operação pertence a uma carteira do investidor logado.

## T-417 — Remove cadastro de Investidor sem senha; AutenticacaoServiceTest real [pendente]

- Refs: AC-001, AC-002, AC-003, AC-004, AC-005, AC-435, AC-436, AC-437
- Arquivos: src/main/java/com/apiexternabackend/services/InvestidorService.java, src/main/java/com/apiexternabackend/resources/InvestidorResource.java, src/main/java/com/apiexternabackend/domains/dtos/InvestidorRequestDTO.java (remover), src/test/java/com/apiexternabackend/services/InvestidorServiceTest.java, src/test/java/com/apiexternabackend/resources/InvestidorResourceTest.java, src/test/java/com/apiexternabackend/services/AutenticacaoServiceTest.java, src/test/java/com/apiexternabackend/resources/AuthResourceTest.java
- Notas: remove `POST /investidores`/`InvestidorService.cadastrar` e os testes @spec:AC-051/052/053 (retirados da spec `investidor`). Reescreve `AutenticacaoServiceTest` (hoje `@Disabled`, todos os métodos vazios) com asserts reais; cria `AuthResourceTest` novo.

## T-418 — Testes de segurança fim a fim + catálogo + mapeamento [pendente]

- Refs: AC-006, AC-007, AC-438, AC-439, AC-440, AC-441, AC-442
- Arquivos: src/test/java/com/apiexternabackend/config/SecurityIntegrationTest.java, docs/erros.md, docs/mapeamento-atual.html
- Notas: depende de T-414..T-417 concluídas. `@SpringBootTest` + `@AutoConfigureMockMvc` real (não `@WebMvcTest`) para exercitar o `SecurityFilterChain` de verdade: 401 sem token, 200 com token válido, 401 token expirado (gerar token com `exp` no passado), isolamento entre dois investidores de verdade (não mock).
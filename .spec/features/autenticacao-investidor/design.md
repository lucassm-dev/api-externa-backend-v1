# Design: Autenticacao investidor (SPEC-02)

> feature: autenticacao-investidor

## Dependências novas

- `spring-boot-starter-security` — filtro de segurança, `PasswordEncoder`, `SecurityFilterChain`
- `io.jsonwebtoken:jjwt-api`, `jjwt-impl`, `jjwt-jackson` (0.12.x) — geração/validação do JWT, sem trazer o módulo OAuth2 do Spring Security (desnecessário para token próprio stateless)

## Entidade `Investidor`

Campos novos: `senha` (String, hash BCrypt, `NOT NULL` — migration `V11`),
`criadoEm` (LocalDateTime, `NOT NULL DEFAULT now()`). `cpf` continua
(já existia, não estava no escopo literal do prompt mas é mantido).

```sql
-- V11__add_senha_criado_em_investidor.sql
ALTER TABLE investidor ADD COLUMN senha VARCHAR(255);
ALTER TABLE investidor ADD COLUMN criado_em TIMESTAMP NOT NULL DEFAULT now();
-- sem dado existente com senha (MVP sem usuários reais ainda) — sem backfill necessário
ALTER TABLE investidor ALTER COLUMN senha SET NOT NULL;
```

## Fluxo de cadastro/login

```
POST /auth/cadastro (público)
  AuthResource → AutenticacaoService.cadastrar(AuthCadastroRequestDTO)
    valida política de senha (8+, 1 letra, 1 número) → RegraVioladaException AUT-008
    valida e-mail único → RecursoDuplicadoException AUT-001
    valida CPF único → RecursoDuplicadoException AUT-002
    hash BCrypt, salva Investidor
    retorna InvestidorResponseDTO (sem senha)

POST /auth/login (público)
  AuthResource → AutenticacaoService.login(AuthLoginRequestDTO)
    busca por e-mail; se não existe OU senha não bate (BCryptPasswordEncoder.matches)
      → RegraVioladaException AUT-004, 401 (mensagem genérica, não revela qual campo errou)
    gera JWT (JwtService.gerar(investidorId, email)), expiraEm = agora + 24h
    retorna AuthTokenResponseDTO(token, tipo="Bearer", expiraEm)
```

## JWT

- Claims: `sub` = investidorId (String), `email`, `iat`, `exp` (24h)
- Assinatura: HMAC-SHA256, segredo em `JWT_SECRET` (`.env`, `.env.example`) — chave aleatória de 256+ bits, nunca commitada
- `JwtService`: `gerar(Long id, String email) -> String`, `validar(String token) -> Claims` (lança `ExpiredJwtException` ou `JwtException` genérica — ambas de `io.jsonwebtoken`, capturadas no filtro)

## Segurança — filtro e config

```
JwtAuthenticationFilter (OncePerRequestFilter, registrado antes de UsernamePasswordAuthenticationFilter)
  lê header "Authorization: Bearer <token>"
  sem header → segue a cadeia sem autenticar (SecurityConfig barra depois, vira 401 via entry point)
  com header:
    tenta validar via JwtService
    sucesso → seta SecurityContext com Authentication(principal=InvestidorPrincipal(id,email), authorities=[ROLE_INVESTIDOR])
    ExpiredJwtException → seta request attribute "tokenExpirado"=true, segue sem autenticar
    outra exceção (assinatura inválida, malformado) → segue sem autenticar

SecurityConfig (SecurityFilterChain)
  csrf desabilitado (API stateless, sem cookie de sessão)
  sessionCreationPolicy = STATELESS
  permitAll: /auth/**, /swagger-ui/**, /swagger-ui.html, /v3/api-docs/**
  anyRequest().authenticated()
  exceptionHandling:
    authenticationEntryPoint → JwtAuthenticationEntryPoint (401)
    accessDeniedHandler → JwtAccessDeniedHandler (403 — não usado nesta spec, só 1 papel, mas registrado pro padrão existir)
  addFilterBefore(JwtAuthenticationFilter, UsernamePasswordAuthenticationFilter)

JwtAuthenticationEntryPoint
  escreve StandardError manualmente (filtros rodam antes do DispatcherServlet — @RestControllerAdvice não os alcança)
  se request.getAttribute("tokenExpirado") == true → codigo AUT-006 "Token expirado, faça login novamente"
  senão → codigo AUT-005 "Token ausente ou inválido"
  ambos: status 401, mesmo formato do StandardError da SPEC-01 (timestamp, status, codigo, error, message, path)

JwtAccessDeniedHandler
  codigo AUT-007, status 403, mesmo formato
```

`InvestidorPrincipal` — `record InvestidorPrincipal(Long id, String email)`,
usado via `@AuthenticationPrincipal InvestidorPrincipal principal` nos
controllers que precisam do investidor logado.

## Isolamento de carteira/operação

- `CarteiraRequestDTO`: remove `investidorId`
- `CarteiraService.criar(dto, Long investidorId)` — investidorId vem do principal, não do DTO
- `CarteiraService.listarPorInvestidor(investidorId, pageable)` — investidorId vem do principal (`CarteiraResource` para de aceitar `@RequestParam Long investidorId`)
- `CarteiraService.buscarAtiva/buscarPorId/renomear/excluir` passam a exigir `Long investidorId` e lançam `RecursoNaoEncontradoException("CAR-001", ...)` quando a carteira existe mas pertence a outro investidor — **mesmo código/mensagem** de "carteira não encontrada" (ASM-411: não revela existência)
- `OperacaoService.comprar/vender/editar/excluir` já resolvem a carteira via `CarteiraService.buscarAtiva`/`buscarOperacao` — passam a receber o `investidorId` do principal e propagá-lo pra essas chamadas
- `OperacaoResource`/`CarteiraResource`: todos os endpoints ganham `@AuthenticationPrincipal InvestidorPrincipal principal`

## `POST /investidores` — removido

`InvestidorResource.cadastrar` e `InvestidorService.cadastrar` são removidos.
`InvestidorRequestDTO` também (não é mais usado — cadastro usa
`AuthCadastroRequestDTO`). `GET /investidores`, `GET /investidores/{id}` e
`DELETE /investidores/{id}` continuam, agora atrás de autenticação (sem
checar se é o próprio investidor — ver Fora de escopo da spec).

## Catálogo de erros — novos códigos AUT

| Código | Situação | Status | Exceção |
|---|---|---|---|
| AUT-004 | Login com e-mail ou senha incorretos | 401 | RegraVioladaException (ver nota¹) |
| AUT-005 | Token ausente, malformado ou com assinatura inválida | 401 | escrito direto pelo entry point, não passa pelo GlobalExceptionHandler |
| AUT-006 | Token expirado | 401 | idem |
| AUT-007 | Acesso negado (autenticado, sem permissão) | 403 | escrito pelo access denied handler |
| AUT-008 | Senha não atende a política mínima (8+ caracteres, 1 letra, 1 número) | 422 | RegraVioladaException |

¹ **Nota AUT-004:** `RegraVioladaException` hoje mapeia para 422
(`UNPROCESSABLE_ENTITY`), mas login errado precisa ser 401. Duas opções:
(a) dar a `RegraVioladaException` um construtor que aceita o `HttpStatus`
explicitamente para este caso pontual, ou (b) criar uma exceção nova
`CredenciaisInvalidasException` fixa em 401. Escolha: **(b)**, mais simples e
não abre a porta pra todo `RegraVioladaException` futuro escolher status
livremente (manteria o padrão fechado por design, como a SPEC-01 definiu).

## Testes de segurança

`@WebMvcTest` sozinho não carrega `SecurityFilterChain` por padrão; testes de
autenticação/autorização real (401 sem token, 200 com token válido, 401 token
expirado) usam `@SpringBootTest` + `@AutoConfigureMockMvc` com um perfil de
teste que usa um `JWT_SECRET` fixo (via `application-test.properties`).

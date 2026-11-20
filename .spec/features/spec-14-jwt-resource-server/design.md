# Design: JWT emitido e validado pelo resource server do Spring

> feature: spec-14-jwt-resource-server

Base: ADR-011 (decisão), ADR-009 (contrato de erro), ADR-001 (sem refresh).

## Antes e depois

| Peça | Hoje | Depois |
|---|---|---|
| Dependência | `jjwt-api/impl/jackson` 0.12.6 | `spring-boot-starter-oauth2-resource-server` |
| Emissão | `JwtService.gerar` com jjwt | `JwtService.gerar` com `JwtEncoder` (Nimbus, HS256) |
| Leitura | `JwtAuthenticationFilter` próprio | filtro de Bearer token do Spring + `JwtDecoder` |
| Claims | `sub`, `email`, `iat`, `exp` | `sub`, `iss`, `iat`, `exp`, `authorities` |
| Validade | 24 h | 60 min (`JWT_EXPIRATION_MS`) |
| Principal | `InvestidorPrincipal(id, email)` | `InvestidorPrincipal(id)` |
| Swagger | público em todo perfil | só no perfil `dev` |

## Decisões

### 1. Principal continua sendo `InvestidorPrincipal` (ASM-460)

Um `Converter<Jwt, AbstractAuthenticationToken>` registrado em
`oauth2ResourceServer(o -> o.jwt(j -> j.jwtAuthenticationConverter(...)))`
devolve um `UsernamePasswordAuthenticationToken` com principal
`new InvestidorPrincipal(Long.valueOf(jwt.getSubject()))` e as authorities
lidas da claim `authorities`. Os resources que usam
`@AuthenticationPrincipal InvestidorPrincipal` não mudam.

### 2. Chave, emissor e validade

- `JwtEncoder`: `NimbusJwtEncoder` com `ImmutableSecret` sobre o mesmo
  `jwt.secret` (HS256, cabeçalho `alg` explícito).
- `JwtDecoder`: `NimbusJwtDecoder.withSecretKey(...).macAlgorithm(HS256)`,
  com validador `JwtValidators.createDefaultWithIssuer(jwt.issuer)` — expiração
  com tolerância padrão de 60 s (ASM-462) e emissor obrigatório.
- Propriedades em `application.properties`, valendo para todo perfil:
  `jwt.issuer=${JWT_ISSUER:api-externa-backend}` e
  `jwt.expiration-ms=${JWT_EXPIRATION_MS:3600000}`. Saem as linhas de 24 h
  dos perfis `dev`, `prod` e `test`.

### 3. `AUT-005` x `AUT-006` no entry point (ASM-464)

O `JwtAuthenticationEntryPoint` é registrado também em
`oauth2ResourceServer(o -> o.authenticationEntryPoint(...))` — sem isso o
Spring usa o `BearerTokenAuthenticationEntryPoint` dele. Expirado é
reconhecido quando a causa da exceção é `JwtValidationException` com algum
erro cuja descrição contém "expired". Todo o resto é `AUT-005`. Some o
atributo `tokenExpirado` da requisição.

### 4. Swagger só no `dev`

`springdoc.api-docs.enabled=false` e `springdoc.swagger-ui.enabled=false` em
`application.properties`; `true` em `application-dev.properties`. As rotas do
Swagger continuam em `ROTAS_PUBLICAS` para o dev abrir sem token; fora do dev
elas não existem e respondem 404.

### 5. O que sai

`JwtAuthenticationFilter`, a parte de leitura do `JwtService` (`validar`,
`investidorIdDe`, `emailDe`, `expiracaoDe`) e as três dependências jjwt.
`AutenticacaoService` passa a ler a expiração do `Jwt` devolvido pelo
encoder em vez de decodificar o próprio token.

## Riscos

- Tokens antigos (sem `iss`) deixam de valer no deploy (ASM-465).
- Texto do erro de expiração do Spring pode mudar (ASM-464); o teste do
  AC-441 detecta.

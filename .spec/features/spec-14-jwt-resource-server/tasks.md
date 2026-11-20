# Tasks: JWT emitido e validado pelo resource server do Spring

> feature: spec-14-jwt-resource-server

## T-494 — Migra emissão e leitura do token para o resource server [pendente]
- Refs: AC-545, AC-546, AC-547, AC-548, AC-550, AC-439, AC-440, AC-441
- Arquivos: pom.xml, src/main/java/com/apiexternabackend/config/SecurityConfig.java, src/main/java/com/apiexternabackend/config/JwtService.java, src/main/java/com/apiexternabackend/config/InvestidorPrincipal.java, src/main/java/com/apiexternabackend/config/JwtAuthenticationEntryPoint.java, src/main/java/com/apiexternabackend/services/AutenticacaoService.java, src/main/resources/application.properties, src/main/resources/application-dev.properties, src/main/resources/application-prod.properties, src/main/resources/application-test.properties, src/test/java/com/apiexternabackend/services/AutenticacaoServiceTest.java, src/test/java/com/apiexternabackend/resources/AcaoResourceTest.java, src/test/java/com/apiexternabackend/resources/CarteiraResourceTest.java, src/test/java/com/apiexternabackend/resources/InvestidorResourceTest.java, src/test/java/com/apiexternabackend/resources/MercadoResourceTest.java, src/test/java/com/apiexternabackend/resources/OperacaoResourceTest.java, src/test/java/com/apiexternabackend/resources/exceptions/GlobalExceptionHandlerTest.java
- Notas: decisões 1, 2, 3 e 5 do design.md. Apaga `JwtAuthenticationFilter.java`. Uma tarefa só porque nenhuma parte compila sem as outras: o entry point usa o atributo do filtro e os testes usam o `InvestidorPrincipal` com e-mail.

## T-495 — Swagger só no perfil dev [pendente]
- Refs: AC-549
- Arquivos: src/main/resources/application.properties, src/main/resources/application-dev.properties
- Notas: decisão 4 do design.md.

## T-496 — Testes de integração do token novo [pendente]
- Refs: AC-545, AC-546, AC-547, AC-548, AC-549, AC-550, AC-441
- Arquivos: src/test/java/com/apiexternabackend/config/SecurityIntegrationTest.java
- Notas: o token expirado e o de outro emissor passam a ser gerados com o `JwtEncoder` do contexto (sem jjwt). AC-550 por `Class.forName("io.jsonwebtoken.Jwts")` esperando `ClassNotFoundException`.

## T-497 — Specs e exemplo de ambiente acompanham a migração [pendente]
- Refs: AC-546
- Arquivos: .spec/features/autenticacao-investidor/spec.md, .env.example
- Notas: ASM-413 (jjwt) vira `invalidada` apontando para a spec-14; `.env.example` ganha `JWT_EXPIRATION_MS` e `JWT_ISSUER` comentados com os padrões.

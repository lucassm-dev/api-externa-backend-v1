# Tasks: Papéis INVESTIDOR e ADMIN com matriz de acesso

> feature: spec-16-papeis-matriz-acesso

## T-501 — Papel real no token e login do ADMIN [concluida]
- Refs: US-451, US-455, AC-558, AC-568
- Arquivos: src/main/java/com/apiexternabackend/config/JwtService.java, src/main/java/com/apiexternabackend/services/AutenticacaoService.java, src/test/java/com/apiexternabackend/services/AutenticacaoServiceTest.java, src/test/java/com/apiexternabackend/config/IdentidadeUsuarioIntegrationTest.java, src/test/java/com/apiexternabackend/config/AuthMeIntegrationTest.java
- Notas: decisão 1. `JwtService.gerar` passa a receber o papel. Sai o filtro de papel do login e o teste da ASM-466 da spec-15 vira o oposto (ADMIN entra). O `AuthMeIntegrationTest` (spec-17, AC-571) gera token de ADMIN pelo `JwtService` e acompanha a nova assinatura. AC-568: teste de integração do cadastro com `"papel"` no corpo + varredura dos DTOs de entrada em `domains/dtos` por campo `papel`.

## T-502 — Matriz de acesso nas rotas [concluida]
- Refs: US-452, US-453, AC-559, AC-560, AC-561, AC-562, AC-563
- Arquivos: src/main/java/com/apiexternabackend/config/SecurityConfig.java, src/main/java/com/apiexternabackend/resources/CorretoraResource.java, src/main/java/com/apiexternabackend/resources/AcaoResource.java, src/main/java/com/apiexternabackend/resources/MercadoResource.java, src/main/java/com/apiexternabackend/resources/CarteiraResource.java, src/main/java/com/apiexternabackend/resources/OperacaoResource.java, src/main/java/com/apiexternabackend/resources/InvestidorResource.java, src/test/java/com/apiexternabackend/config/MatrizAcessoIntegrationTest.java, src/test/java/com/apiexternabackend/resources/CorretoraResourceTest.java, src/test/java/com/apiexternabackend/resources/AcaoResourceTest.java, src/test/java/com/apiexternabackend/resources/MercadoResourceTest.java, src/test/java/com/apiexternabackend/resources/CarteiraResourceTest.java, src/test/java/com/apiexternabackend/resources/OperacaoResourceTest.java, src/test/java/com/apiexternabackend/resources/InvestidorResourceTest.java
- Esforço: alto
- Notas: decisões 2 e 3. `AcaoResource` não ganha restrição de papel (só autenticado) até a #24. `@EnableMethodSecurity` no `SecurityConfig`; `@PreAuthorize` por classe ou método conforme a matriz. O teste de matriz usa tokens reais de cada papel e serviços externos mockados (CVM, cotação) para o 2xx do ADMIN. Os testes `@WebMvcTest` existentes passam a precisar do papel certo no token mockado; ajustar ao que quebrar. Depende da T-501 (papel no token).

## T-503 — Primeiro ADMIN pelas variáveis de ambiente [concluida]
- Refs: US-454, AC-564, AC-565, AC-566, AC-567
- Arquivos: src/main/java/com/apiexternabackend/config/AdminInicial.java, src/main/java/com/apiexternabackend/repositories/UsuarioRepository.java, src/main/resources/application.properties, .env.example, src/test/java/com/apiexternabackend/config/AdminInicialTest.java
- Notas: decisão 5. `ApplicationRunner` lendo `admin.email`/`admin.senha` (ASM-472). Log só com o e-mail. AC-565 por captura de saída (`OutputCaptureExtension`) e varredura de `src/main` por padrão de `admin.senha`.

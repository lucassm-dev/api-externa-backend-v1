# Tasks: SPEC-11 — CORS para o frontend Angular

> feature: spec-11-cors

## T-480 — Política de CORS por configuração e testes de integração [concluida]
- Refs: US-436, AC-510, AC-511, AC-512, AC-513, AC-514, AC-515, AC-516
- Arquivos: src/main/java/com/apiexternabackend/config/SecurityConfig.java, src/main/resources/application-dev.properties, src/main/resources/application-test.properties, src/test/java/com/apiexternabackend/config/CorsIntegrationTest.java
- Esforço: baixo
- Notas: origens lidas de `cors.allowed-origins` (lista, sem curinga). Métodos
  GET/POST/PUT/PATCH/DELETE/OPTIONS. Cabeçalhos `Authorization` e
  `Content-Type`. Credenciais desligadas. Confirmar que a consulta prévia
  (`OPTIONS`) não é barrada pelo `JwtAuthenticationFilter` nem pelas regras de
  autorização. Nada fora de configuração de segurança e teste.

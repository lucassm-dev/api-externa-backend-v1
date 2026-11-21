# Tasks: Esqueci minha senha com código por e-mail

> feature: spec-22-esqueci-senha

## T-515 — Pedido e redefinição de senha por código [concluida]
- Refs: US-468, US-469, AC-609, AC-610, AC-611, AC-612, AC-613, AC-614, AC-615, AC-616, AC-617, AC-618, AC-619
- Arquivos: src/main/java/com/apiexternabackend/services/RecuperacaoSenhaService.java, src/main/java/com/apiexternabackend/domains/CodigoRecuperacao.java, src/main/java/com/apiexternabackend/repositories/CodigoRecuperacaoRepository.java, src/main/java/com/apiexternabackend/domains/dtos/EsqueciSenhaRequestDTO.java, src/main/java/com/apiexternabackend/domains/dtos/RedefinirSenhaRequestDTO.java, src/main/java/com/apiexternabackend/resources/AuthResource.java, src/main/java/com/apiexternabackend/config/SecurityConfig.java, src/main/java/com/apiexternabackend/domains/Usuario.java, src/main/resources/db/migration/V21__recuperacao_senha.sql, docs/erros.md, src/test/java/com/apiexternabackend/config/RecuperacaoSenhaIntegrationTest.java
- Notas: decisões 1 a 4. SMTP simulado com mock do `JavaMailSender`.

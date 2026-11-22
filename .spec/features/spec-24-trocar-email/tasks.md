# Tasks: Trocar e-mail com confirmação por código

> feature: spec-24-trocar-email

## T-517 — Pedido e confirmação da troca de e-mail [concluida]
- Refs: US-471, AC-625, AC-626, AC-627, AC-628, AC-629, AC-630, AC-631, AC-632
- Arquivos: src/main/java/com/apiexternabackend/services/TrocaEmailService.java, src/main/java/com/apiexternabackend/services/RecuperacaoSenhaService.java, src/main/java/com/apiexternabackend/domains/CodigoRecuperacao.java, src/main/java/com/apiexternabackend/repositories/CodigoRecuperacaoRepository.java, src/main/java/com/apiexternabackend/domains/dtos/PedirTrocaEmailRequestDTO.java, src/main/java/com/apiexternabackend/domains/dtos/ConfirmarTrocaEmailRequestDTO.java, src/main/java/com/apiexternabackend/resources/AuthResource.java, src/main/resources/db/migration/V22__troca_email.sql, docs/erros.md, src/test/java/com/apiexternabackend/config/TrocaEmailIntegrationTest.java
- Notas: decisões 1 a 4. Reaproveita a tabela `codigo_recuperacao` com a coluna `novo_email`.

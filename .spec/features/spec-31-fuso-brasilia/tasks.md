# Tasks: Fuso de Brasília na aplicação

> feature: spec-31-fuso-brasilia

## T-530 — Fixar o fuso de Brasília na imagem [pendente]

- Refs: US-481, AC-666
- Arquivos: Dockerfile, docs/deploy.md, src/test/java/com/apiexternabackend/deploy/ConfiguracaoProducaoTest.java
- Notas: TDD, um ciclo vermelho/verde. O teste lê o `ENTRYPOINT` do `Dockerfile`, como os outros testes de `ConfiguracaoProducaoTest` leem o compose.

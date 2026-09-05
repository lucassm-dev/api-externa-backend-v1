# Tasks: SPEC-03 — Recadastro após exclusão e bloqueio com vínculo ativo

> feature: spec-03-recadastro-exclusao

## T-419 — Migration, entidades e repositórios (fundação) [concluida]
- Refs: US-415, US-417
- Arquivos: src/main/resources/db/migration/V12__unicidade_parcial_ativo.sql, src/main/java/com/apiexternabackend/domains/Acao.java, src/main/java/com/apiexternabackend/domains/Corretora.java, src/main/java/com/apiexternabackend/repositories/AcaoRepository.java, src/main/java/com/apiexternabackend/repositories/CorretoraRepository.java, src/main/java/com/apiexternabackend/repositories/CarteiraRepository.java, src/main/java/com/apiexternabackend/repositories/CarteiraAcaoRepository.java, docs/erros.md
- Notas: pré-requisito de compilação — feita antes de T-420/421/422. Ver design.md.

## T-420 — Ação: recadastro, busca individual e operação filtram ativo, bloqueio com posição [pendente]

- Refs: AC-444, AC-446, AC-447, AC-448, AC-449, AC-450, AC-451
- Arquivos: src/main/java/com/apiexternabackend/services/AcaoService.java, src/main/java/com/apiexternabackend/services/OperacaoService.java, src/test/java/com/apiexternabackend/services/AcaoServiceTest.java, src/test/java/com/apiexternabackend/resources/AcaoResourceTest.java, src/test/java/com/apiexternabackend/services/OperacaoServiceTest.java

## T-421 — Corretora: recadastro, bloqueio de exclusão com carteira ativa [pendente]

- Refs: AC-445, AC-446, AC-454, AC-455
- Arquivos: src/main/java/com/apiexternabackend/services/CorretoraService.java, src/test/java/com/apiexternabackend/services/CorretoraServiceTest.java, src/test/java/com/apiexternabackend/resources/CorretoraResourceTest.java

## T-422 — Carteira: bloqueio de exclusão com posição ativa [pendente]

- Refs: AC-452, AC-453
- Arquivos: src/main/java/com/apiexternabackend/services/CarteiraService.java, src/test/java/com/apiexternabackend/services/CarteiraServiceTest.java, src/test/java/com/apiexternabackend/resources/CarteiraResourceTest.java

## T-423 — Catálogo, mapeamento e auditoria final [pendente]

- Refs: US-415, US-416, US-417
- Arquivos: docs/erros.md, docs/mapeamento-atual.html
- Notas: depende de T-420..T-422 concluídas.
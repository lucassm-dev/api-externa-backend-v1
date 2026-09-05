# Tasks: SPEC-04 — Preço sugerido pela cotação, mas editável

> feature: spec-04-preco-editavel

## T-424 — Migration, entidade e DTOs (fundação) [concluida]
- Refs: US-418, US-419
- Arquivos: src/main/resources/db/migration/V13__rastreabilidade_preco_operacao.sql, src/main/java/com/apiexternabackend/domains/Operacao.java, src/main/java/com/apiexternabackend/domains/dtos/OperacaoRequestDTO.java, src/main/java/com/apiexternabackend/domains/dtos/OperacaoEditarDTO.java, src/main/java/com/apiexternabackend/domains/dtos/OperacaoResponseDTO.java, docs/erros.md

## T-425 — Comprar/vender: preço opcional, rastreabilidade, escala decimal [concluida]
- Refs: AC-456, AC-457, AC-458, AC-459, AC-461
- Arquivos: src/main/java/com/apiexternabackend/services/OperacaoService.java, src/test/java/com/apiexternabackend/services/OperacaoServiceTest.java, src/test/java/com/apiexternabackend/resources/OperacaoResourceTest.java

## T-426 — Editar operação: mesma validação de preço (corrige RN-OPE-05) [concluida]
- Refs: AC-464, AC-465
- Arquivos: src/main/java/com/apiexternabackend/services/OperacaoService.java, src/test/java/com/apiexternabackend/services/OperacaoServiceTest.java, src/test/java/com/apiexternabackend/resources/OperacaoResourceTest.java

## T-427 — Mapper (moeda + avisos de desvio), preço médio misto, mapeamento final [concluida]
- Refs: AC-460, AC-462, AC-463
- Arquivos: src/main/java/com/apiexternabackend/mappers/OperacaoMapper.java, src/test/java/com/apiexternabackend/services/PosicaoServiceTest.java, docs/mapeamento-atual.html
- Notas: depende de T-424..T-426 concluídas.

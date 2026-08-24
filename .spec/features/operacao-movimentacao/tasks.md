# Tasks: Operacao movimentacao

> feature: operacao-movimentacao

## T-400 — Domínio de operação e posição, repositórios e migration [pendente]

- Refs: US-401, US-403
- Arquivos: src/main/java/com/apiexternabackend/domains/Operacao.java, src/main/java/com/apiexternabackend/domains/enums/TipoOperacao.java, src/main/java/com/apiexternabackend/domains/CarteiraAcao.java, src/main/java/com/apiexternabackend/infra/converters/TipoOperacaoConverter.java, src/main/java/com/apiexternabackend/repositories/OperacaoRepository.java, src/main/java/com/apiexternabackend/repositories/CarteiraAcaoRepository.java, src/main/resources/db/migration/V6__create_operacao_posicao.sql
- Notas: Operacao = movimentação (carteira, acao, tipo, quantidade, precoUnitario, dataHora). CarteiraAcao = posição (quantidade, precoMedio). TipoOperacao COMPRA/VENDA.

## T-401 — DTOs e mappers de operação [pendente]

- Refs: AC-407, AC-409
- Arquivos: src/main/java/com/apiexternabackend/domains/dtos/OperacaoRequestDTO.java, src/main/java/com/apiexternabackend/domains/dtos/OperacaoResponseDTO.java, src/main/java/com/apiexternabackend/domains/dtos/CarteiraAcaoResponseDTO.java, src/main/java/com/apiexternabackend/domains/dtos/OperacaoEditarDTO.java, src/main/java/com/apiexternabackend/mappers/OperacaoMapper.java, src/main/java/com/apiexternabackend/mappers/CarteiraAcaoMapper.java
- Notas: Request com carteiraId, ticker/acaoId, quantidade (inteira). Response da movimentação com data/hora, tipo, ticker, quantidade, preço unitário e total.

## T-402 — Cálculo de posição e preço médio (recálculo do histórico) [pendente]

- Refs: AC-402, AC-405, AC-412, AC-413
- Arquivos: src/main/java/com/apiexternabackend/services/PosicaoService.java
- Notas: posição derivada das movimentações; preço médio ponderado; recálculo ao editar/excluir lançamento; posição sem quantidade deixa de existir.

## T-403 — Serviço de operação (compra e venda) [pendente]

- Refs: AC-305, AC-309, AC-401, AC-402, AC-403, AC-404, AC-405, AC-406, AC-411
- Arquivos: src/main/java/com/apiexternabackend/services/OperacaoService.java
- Notas: preço vem da cotação buscada no ato (compra e venda); valida mercado da carteira x ação e carteira ativa; não vende além da posição; venda que zera remove a posição; resultado realizado por carteira e por ticker; sem erro de saldo.

## T-404 — Consultas: histórico e rentabilidade não realizada [pendente]

- Refs: AC-409, AC-410
- Arquivos: src/main/java/com/apiexternabackend/services/ConsultaOperacaoService.java
- Notas: histórico de movimentações do investidor; rentabilidade não realizada = (cotação atual − preço médio) × quantidade, com horário da cotação.

## T-405 — Controller de operação [pendente]

- Refs: AC-407, AC-409, AC-412, AC-413
- Arquivos: src/main/java/com/apiexternabackend/resources/OperacaoResource.java
- Notas: POST /operacoes/compra, POST /operacoes/venda, GET /operacoes (histórico), PUT /operacoes/{id} (editar lançamento), DELETE /operacoes/{id} (excluir lançamento), GET /carteiras/{id}/posicoes.

## T-406 — Testes de operação [pendente]

- Refs: AC-305, AC-309, AC-401, AC-402, AC-403, AC-404, AC-405, AC-406, AC-407, AC-409, AC-410, AC-411, AC-412, AC-413
- Arquivos: src/test/java/com/apiexternabackend/services/OperacaoServiceTest.java, src/test/java/com/apiexternabackend/services/PosicaoServiceTest.java, src/test/java/com/apiexternabackend/resources/OperacaoResourceTest.java
- Notas: exemplo do preço médio (100@38 + 100@42 = 200@40); cotação mockada; um teste por critério.

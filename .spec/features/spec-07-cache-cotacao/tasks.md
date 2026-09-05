# Tasks: Cache de cotação com TTL e fallback em compra/venda

> feature: spec-07-cache-cotacao

## T-439 — Configuração do TTL em application.properties [pendente]

- Refs: AC-480
- Arquivos: src/main/resources/application.properties

## T-440 — CotacaoCacheService: TTL check, busca na fonte e persistência [pendente]

- Refs: AC-477, AC-478, AC-479, AC-481
- Arquivos: src/main/java/com/apiexternabackend/services/CotacaoCacheService.java
- Notas: centraliza a escolha do adapter (BR/US) e a checagem de TTL — usado por AcaoService e OperacaoService.

## T-441 — AcaoService.atualizarCotacao usa o cache + parâmetro forçar [pendente]

- Refs: AC-477, AC-478, AC-481, AC-485
- Arquivos: src/main/java/com/apiexternabackend/services/AcaoService.java

## T-442 — Endpoint PUT /acoes/{id}/atualizar-cotacao aceita ?forcar= [pendente]

- Refs: AC-481
- Arquivos: src/main/java/com/apiexternabackend/resources/AcaoResource.java

## T-443 — OperacaoService.comprar/vender usam o cache com fallback (Q-MAP-06) [pendente]

- Refs: AC-477, AC-478, AC-482, AC-483, AC-484
- Arquivos: src/main/java/com/apiexternabackend/services/OperacaoService.java

## T-444 — Aviso de cotação desatualizada na resposta de compra/venda [pendente]

- Refs: AC-482, AC-483
- Arquivos: src/main/java/com/apiexternabackend/services/OperacaoService.java

## T-445 — Testes de serviço: cache, TTL, forçar e fallback [pendente]

- Refs: AC-477, AC-478, AC-479, AC-480, AC-481, AC-482, AC-483, AC-484
- Arquivos: src/test/java/com/apiexternabackend/services/CotacaoCacheServiceTest.java, src/test/java/com/apiexternabackend/services/AcaoServiceTest.java, src/test/java/com/apiexternabackend/services/OperacaoServiceTest.java

## T-446 — Testes de resource: parâmetro forçar e aviso de fallback [pendente]

- Refs: AC-481, AC-482, AC-483, AC-485
- Arquivos: src/test/java/com/apiexternabackend/resources/AcaoResourceTest.java, src/test/java/com/apiexternabackend/resources/OperacaoResourceTest.java

## T-447 — Atualizar catálogo de erros e mapeamento vivo [pendente]

- Refs: AC-477, AC-478, AC-479, AC-480, AC-481, AC-482, AC-483, AC-484, AC-485
- Arquivos: docs/erros.md, docs/mapeamento-atual.html
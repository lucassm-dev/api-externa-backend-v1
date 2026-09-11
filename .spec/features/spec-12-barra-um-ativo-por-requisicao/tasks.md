# Tasks: SPEC-12 — Barra de cotações dentro do plano gratuito da brapi

> feature: spec-12-barra-um-ativo-por-requisicao

## T-481 — Uma requisição por ativo, lista curta e cache de 30 minutos [concluida]
- Refs: US-437, AC-517, AC-518, AC-519, AC-520
- Arquivos: src/main/java/com/apiexternabackend/services/BarraCotacoesService.java, src/main/resources/application.properties, src/test/java/com/apiexternabackend/services/BarraCotacoesServiceTest.java
- Esforço: baixo
- Notas: composição passa a ser ^BVSP, PETR4 e VALE3. Falha por ativo é isolada e o
  aviso nomeia quem faltou. Ajustar também o AC-498 da SPEC-09, que listava
  ITUB4, IFIX e IVVB11.

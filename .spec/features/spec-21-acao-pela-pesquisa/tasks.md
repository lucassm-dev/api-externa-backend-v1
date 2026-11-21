# Tasks: Ação entra no catálogo pela pesquisa

> feature: spec-21-acao-pela-pesquisa

## T-513 — Pesquisa inclui a ação; saem cadastro e exclusão [concluida]
- Refs: US-466, US-467, AC-602, AC-603, AC-604, AC-605, AC-606, AC-607, AC-608
- Arquivos: src/main/java/com/apiexternabackend/services/AcaoService.java, src/main/java/com/apiexternabackend/resources/AcaoResource.java, src/main/java/com/apiexternabackend/repositories/AcaoRepository.java, src/main/java/com/apiexternabackend/resources/exceptions/GlobalExceptionHandler.java, docs/erros.md, src/test/java/com/apiexternabackend/config/AcaoPelaPesquisaIntegrationTest.java
- Notas: decisões 1 e 2. `AcaoRequestDTO` e `PreRequisitoNaoAtendidoException` são apagados (sem uso). Integração com `BrapiAdapter` simulado para contar chamadas à fonte. Método não suportado passa a responder 405 em vez de cair no 500 genérico.

## T-514 — Migrar e aposentar critérios antigos [concluida]
- Refs: AC-201, AC-202, AC-203, AC-205, AC-209, AC-211, AC-429, AC-433, AC-446, AC-562, AC-426, AC-427, AC-428
- Arquivos: src/test/java/com/apiexternabackend/services/AcaoServiceTest.java, src/test/java/com/apiexternabackend/resources/AcaoResourceTest.java, src/test/java/com/apiexternabackend/resources/exceptions/GlobalExceptionHandlerTest.java, src/test/java/com/apiexternabackend/config/MatrizAcessoIntegrationTest.java
- Notas: decisão 3. O handler de erro passa a ser exercitado pelo `POST /carteiras`, porque não existe mais `POST /acoes`. Specs de origem ganham nota de remoção; spec-05 é apagada inteira.

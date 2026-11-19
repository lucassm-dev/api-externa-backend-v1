# Tasks: Investidor só enxerga e exclui a própria conta

> feature: spec-13-autorizacao-investidor

## T-490 — Service e resource de investidor recebem o investidor logado [concluida]
- Refs: AC-542, AC-543, AC-544
- Arquivos: src/main/java/com/apiexternabackend/services/InvestidorService.java, src/main/java/com/apiexternabackend/resources/InvestidorResource.java, src/main/java/com/apiexternabackend/repositories/InvestidorRepository.java
- Notas: mesmo padrão da `CarteiraResource` (`@AuthenticationPrincipal InvestidorPrincipal` → `principal.id()` no service). `buscarPorId` e `excluir` lançam o mesmo `RecursoNaoEncontradoException("AUT-003", ...)` de hoje quando o id não é o do logado; `listar` devolve página só com o logado, via `findByIdAndAtivoTrue(id, pageable)` no lugar do `findAllByAtivoTrue` (paginação correta também em `page>0`).

## T-491 — Testes de unidade de investidor na nova assinatura [concluida]
- Refs: AC-542, AC-543, AC-544
- Arquivos: src/test/java/com/apiexternabackend/services/InvestidorServiceTest.java, src/test/java/com/apiexternabackend/resources/InvestidorResourceTest.java
- Notas: os testes de AC-054, AC-055 e AC-422 passam a informar o investidor logado. Nenhum teste existente é apagado.

## T-492 — Teste de integração: investidor A contra investidor B [concluida]
- Refs: AC-542, AC-543, AC-544
- Arquivos: src/test/java/com/apiexternabackend/resources/InvestidorAutorizacaoIntegrationTest.java
- Notas: `@SpringBootTest` + `@AutoConfigureMockMvc` com JWT real (mesmo modelo do `SecurityIntegrationTest`), exigência da issue #16. Cadastra A e B por `/auth/cadastro`, usa o token de A contra os ids de B.

## T-493 — Specs antigas apontam para esta [concluida]
- Refs: AC-542
- Arquivos: .spec/features/investidor/spec.md, .spec/features/autenticacao-investidor/spec.md
- Notas: AC-054 e AC-055 passam a dizer "só a própria conta"; sai o item de fora de escopo correspondente na `investidor`; ASM-412 vira `invalidada` com resolução apontando para a spec-13.

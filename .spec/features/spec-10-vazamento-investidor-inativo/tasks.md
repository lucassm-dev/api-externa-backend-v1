# Tasks: Investidor excluído vazando por login, busca e unicidade

> feature: spec-10-vazamento-investidor-inativo

## T-470 — Migração V16: unicidade parcial de e-mail e CPF [pendente]

- Refs: AC-505, AC-506, AC-507
- Arquivos: src/main/resources/db/migration/V16__unicidade_parcial_investidor.sql
- Notas: derruba `uk_investidor_email`/`uk_investidor_cpf` e recria como índice único parcial `WHERE ativo = true`, mesmo padrão da `V12` (ticker/CNPJ).

## T-471 — Entidade Investidor sem unique incondicional [pendente]

- Refs: AC-505, AC-506
- Arquivos: src/main/java/com/apiexternabackend/domains/Investidor.java
- Notas: remove `unique = true` de `email` e `cpf` (ASM-440) — senão o Hibernate recria a constraint incondicional no H2 dos testes.

## T-472 — Repositório: consultas de investidor filtrando ativo [pendente]

- Refs: AC-503, AC-504, AC-505, AC-506, AC-507, AC-508
- Arquivos: src/main/java/com/apiexternabackend/repositories/InvestidorRepository.java
- Notas: `existsByEmailAndAtivoTrue`, `existsByCpfAndAtivoTrue`, `findByEmailAndAtivoTrue` no lugar das versões sem filtro.

## T-473 — Login e cadastro filtrando investidor ativo [pendente]

- Refs: AC-503, AC-505, AC-506, AC-507, AC-508
- Arquivos: src/main/java/com/apiexternabackend/services/AutenticacaoService.java

## T-474 — Busca individual de investidor filtrando ativo [pendente]

- Refs: AC-504
- Arquivos: src/main/java/com/apiexternabackend/services/InvestidorService.java

## T-475 — Remove consulta por ticker sem filtro de ativo [pendente]

- Refs: AC-509
- Arquivos: src/main/java/com/apiexternabackend/repositories/AcaoRepository.java

## T-476 — Testes de serviço: login, busca e unicidade [pendente]

- Refs: AC-503, AC-504, AC-505, AC-506, AC-507, AC-508, AC-509
- Arquivos: src/test/java/com/apiexternabackend/services/AutenticacaoServiceTest.java, src/test/java/com/apiexternabackend/services/InvestidorServiceTest.java

## T-477 — Testes de resource: 404 e 401 do investidor inativo [pendente]

- Refs: AC-503, AC-504
- Arquivos: src/test/java/com/apiexternabackend/resources/InvestidorResourceTest.java, src/test/java/com/apiexternabackend/resources/AuthResourceTest.java

## T-478 — Atualizar catálogo de erros e mapeamento vivo [pendente]

- Refs: AC-503, AC-504, AC-505, AC-506, AC-507, AC-508, AC-509
- Arquivos: docs/erros.md, docs/mapeamento-atual.html

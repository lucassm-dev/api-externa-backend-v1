# Tasks: Carteira

> feature: carteira

## T-300 — Entidade Carteira, repositório e migration [pendente]

- Refs: US-301, AC-303
- Arquivos: src/main/java/com/apiexternabackend/domains/Carteira.java, src/main/java/com/apiexternabackend/repositories/CarteiraRepository.java, src/main/resources/db/migration/V5__create_carteira.sql
- Notas: investidor_id, corretora_id (obrigatória), mercado (BR/US), nome, ativa (boolean, default true). findByInvestidorIdAndAtivaTrue.

## T-301 — DTOs e mapper da carteira [pendente]

- Refs: AC-301, AC-306, AC-307
- Arquivos: src/main/java/com/apiexternabackend/domains/dtos/CarteiraRequestDTO.java, src/main/java/com/apiexternabackend/domains/dtos/CarteiraResponseDTO.java, src/main/java/com/apiexternabackend/domains/dtos/CarteiraRenomearDTO.java, src/main/java/com/apiexternabackend/mappers/CarteiraMapper.java
- Notas: Request com investidorId, corretoraId, mercado, nome. Response com totais por moeda do mercado.

## T-302 — Serviço da carteira [pendente]

- Refs: AC-301, AC-302, AC-303, AC-304, AC-306, AC-307, AC-308, AC-310
- Arquivos: src/main/java/com/apiexternabackend/services/CarteiraService.java
- Notas: criar valida investidor e corretora existentes; múltiplas carteiras; renomear; exclusão lógica (ativa=false); totalização por moeda sem consolidar mercados; nome de inativa é reutilizável.

## T-303 — Controller da carteira [pendente]

- Refs: AC-301, AC-302, AC-306, AC-307, AC-308
- Arquivos: src/main/java/com/apiexternabackend/resources/CarteiraResource.java
- Notas: POST /carteiras, GET /carteiras?investidorId= (paginado), PATCH /carteiras/{id} (renomear), DELETE /carteiras/{id} (exclusão lógica).

## T-304 — Testes da carteira [pendente]

- Refs: AC-301, AC-302, AC-303, AC-304, AC-306, AC-307, AC-308, AC-310
- Arquivos: src/test/java/com/apiexternabackend/services/CarteiraServiceTest.java, src/test/java/com/apiexternabackend/resources/CarteiraResourceTest.java
- Notas: AC-305 e AC-309 (bloqueio de mercado/carteira inativa na operação) são testados na feature operacao-movimentacao.

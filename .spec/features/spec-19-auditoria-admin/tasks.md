# Tasks: Registro das ações administrativas

> feature: spec-19-auditoria-admin

## T-509 — Registro administrativo no bloqueio e desbloqueio [concluida]
- Refs: US-461, US-462, AC-587, AC-589, AC-590, AC-591, AC-592
- Arquivos: src/main/resources/db/migration/V19__registro_administrativo.sql, src/main/java/com/apiexternabackend/domains/RegistroAdministrativo.java, src/main/java/com/apiexternabackend/domains/enums/AcaoAdministrativa.java, src/main/java/com/apiexternabackend/domains/enums/AlvoAdministrativo.java, src/main/java/com/apiexternabackend/repositories/RegistroAdministrativoRepository.java, src/main/java/com/apiexternabackend/services/UsuarioAdminService.java, src/main/java/com/apiexternabackend/resources/UsuarioAdminResource.java, src/test/java/com/apiexternabackend/config/RegistroAdministrativoIntegrationTest.java
- Notas: decisão 3 e ASM-493. Repositório estende `Repository` (só `save`, `count`, `findAll`). Gravação na mesma transação do serviço. AC-590 com `@SpyBean` no repositório lançando exceção.

## T-510 — Registro na exclusão de corretora [concluida]
- Refs: US-461, US-462, AC-588, AC-589
- Arquivos: src/main/java/com/apiexternabackend/services/CorretoraService.java, src/main/java/com/apiexternabackend/resources/CorretoraResource.java, src/test/java/com/apiexternabackend/services/CorretoraServiceTest.java, src/test/java/com/apiexternabackend/resources/CorretoraResourceTest.java, src/test/java/com/apiexternabackend/config/RegistroAdministrativoIntegrationTest.java
- Notas: decisão 2. `excluir` passa a receber o id do ADMIN (principal) e ganha `@Transactional`; ajustar os testes que chamam `excluir(id)`.

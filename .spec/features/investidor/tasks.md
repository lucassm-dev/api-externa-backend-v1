# Tasks: Investidor

> feature: investidor

## T-050 — Entidade Investidor, repositório e migration [concluida]

- Refs: US-052
- Arquivos: src/main/java/com/apiexternabackend/domains/Investidor.java, src/main/java/com/apiexternabackend/repositories/InvestidorRepository.java, src/main/resources/db/migration/V1__create_investidor.sql
- Notas: campos id, nome, email (único), cpf (único). Repositório com existsByEmail/existsByCpf. Ganhou senha e criadoEm na SPEC-02 (migration V11).

## T-051 — DTOs e mapper do investidor [concluida]

- Refs: AC-054, AC-055
- Arquivos: src/main/java/com/apiexternabackend/domains/dtos/InvestidorResponseDTO.java, src/main/java/com/apiexternabackend/mappers/InvestidorMapper.java
- Notas: `InvestidorRequestDTO` existiu até a SPEC-02 (cadastro sem senha) e foi removido — cadastro migrou para `AuthCadastroRequestDTO` (feature `autenticacao-investidor`). Response não expõe dado sensível.

## T-052 — Tratamento de erro centralizado (RNF06) [concluida]

- Refs: US-052
- Arquivos: src/main/java/com/apiexternabackend/resources/exceptions/GlobalExceptionHandler.java, src/main/java/com/apiexternabackend/resources/exceptions/StandardError.java
- Notas: base compartilhada por todas as features — reescrita na SPEC-01 (hierarquia NegocioException com código interno, substituindo ResourceNotFoundException/DuplicateResourceException/BusinessException/ExternalServiceException).

## T-053 — Serviço do investidor [concluida]

- Refs: AC-054, AC-055
- Arquivos: src/main/java/com/apiexternabackend/services/InvestidorService.java
- Notas: cadastro (antigo `InvestidorService.cadastrar`, sem senha) foi removido na SPEC-02 — hoje o serviço só lista e busca. Cadastro é `AutenticacaoService.cadastrar`.

## T-054 — Controller do investidor [concluida]

- Refs: AC-054, AC-055
- Arquivos: src/main/java/com/apiexternabackend/resources/InvestidorResource.java
- Notas: GET /investidores (paginado), GET /investidores/{id}. `POST /investidores` foi removido na SPEC-02 — ver `POST /auth/cadastro`.

## T-055 — Testes do investidor [concluida]

- Refs: AC-054, AC-055
- Arquivos: src/test/java/com/apiexternabackend/services/InvestidorServiceTest.java, src/test/java/com/apiexternabackend/resources/InvestidorResourceTest.java
- Notas: testes de cadastro (AC-051/052/053, retirados desta spec) migraram para `AutenticacaoServiceTest`/`AuthResourceTest` na SPEC-02.

## T-056 — Configuração base do Feign [concluida]

- Refs: US-101, US-201
- Arquivos: src/main/java/com/apiexternabackend/config/FeignConfig.java, src/main/java/com/apiexternabackend/ApiExternaBackendApplication.java
- Notas: infra compartilhada (logging, error decoder) usada pelos clients de corretora e de ação. Fica na base para que cadastro-corretora e catalogo-acao possam rodar em paralelo, sem uma depender da outra. @EnableFeignClients já está na classe main.

# Tasks: Investidor

> feature: investidor

## T-050 — Entidade Investidor, repositório e migration [pendente]

- Refs: US-051, US-052
- Arquivos: src/main/java/com/apiexternabackend/domains/Investidor.java, src/main/java/com/apiexternabackend/repositories/InvestidorRepository.java, src/main/resources/db/migration/V1__create_investidor.sql
- Notas: campos id, nome, email (único), cpf (único). Repositório com existsByEmail/existsByCpf.

## T-051 — DTOs e mapper do investidor [pendente]

- Refs: AC-051, AC-054, AC-055
- Arquivos: src/main/java/com/apiexternabackend/domains/dtos/InvestidorRequestDTO.java, src/main/java/com/apiexternabackend/domains/dtos/InvestidorResponseDTO.java, src/main/java/com/apiexternabackend/mappers/InvestidorMapper.java
- Notas: Request com @NotBlank nome, @Email email, @CPF cpf (Hibernate Validator BR). Response não expõe dado sensível.

## T-052 — Tratamento de erro centralizado (RNF06) [pendente]

- Refs: AC-052, AC-053
- Arquivos: src/main/java/com/apiexternabackend/resources/exceptions/GlobalExceptionHandler.java, src/main/java/com/apiexternabackend/resources/exceptions/StandardError.java, src/main/java/com/apiexternabackend/resources/exceptions/ResourceNotFoundException.java, src/main/java/com/apiexternabackend/resources/exceptions/DuplicateResourceException.java, src/main/java/com/apiexternabackend/resources/exceptions/BusinessException.java, src/main/java/com/apiexternabackend/resources/exceptions/ExternalServiceException.java
- Notas: base compartilhada por todas as features. Trata validação (MethodArgumentNotValidException) e as exceções de negócio.

## T-053 — Serviço do investidor [pendente]

- Refs: AC-051, AC-052, AC-053
- Arquivos: src/main/java/com/apiexternabackend/services/InvestidorService.java
- Notas: cadastro com unicidade de email/cpf (DuplicateResource); formato validado no DTO.

## T-054 — Controller do investidor [pendente]

- Refs: AC-051, AC-054, AC-055
- Arquivos: src/main/java/com/apiexternabackend/resources/InvestidorResource.java
- Notas: POST /investidores, GET /investidores (paginado), GET /investidores/{id}.

## T-055 — Testes do investidor [pendente]

- Refs: AC-051, AC-052, AC-053, AC-054, AC-055
- Arquivos: src/test/java/com/apiexternabackend/services/InvestidorServiceTest.java, src/test/java/com/apiexternabackend/resources/InvestidorResourceTest.java
- Notas: um teste por critério, título com @spec:AC-0xx.

## T-056 — Configuração base do Feign [concluida]

- Refs: US-101, US-201
- Arquivos: src/main/java/com/apiexternabackend/config/FeignConfig.java, src/main/java/com/apiexternabackend/ApiExternaBackendApplication.java
- Notas: infra compartilhada (logging, error decoder) usada pelos clients de corretora e de ação. Fica na base para que cadastro-corretora e catalogo-acao possam rodar em paralelo, sem uma depender da outra. @EnableFeignClients já está na classe main.

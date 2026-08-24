/cos# Tasks: Cadastro corretora

> feature: cadastro-corretora

## T-100 — Entidade Corretora, repositório e migration [pendente]

- Refs: US-101, AC-104
- Arquivos: src/main/java/com/apiexternabackend/domains/Corretora.java, src/main/java/com/apiexternabackend/repositories/CorretoraRepository.java, src/main/resources/db/migration/V2__create_corretora.sql
- Notas: cnpj (único), razaoSocial, nomeFantasia, email, telefone, endereço (cep/logradouro/numero/complemento/bairro/cidade/uf), situacaoCadastral, validadaNaCvm, dataBaseCvm, dataCadastro. existsByCnpj/findByCnpj.

## T-101 — DTOs e mapper da corretora [pendente]

- Refs: AC-102, AC-108, AC-109, AC-110
- Arquivos: src/main/java/com/apiexternabackend/domains/dtos/CorretoraRequestDTO.java, src/main/java/com/apiexternabackend/domains/dtos/CorretoraResponseDTO.java, src/main/java/com/apiexternabackend/mappers/CorretoraMapper.java
- Notas: Request só com @CNPJ cnpj. Response completa, incluindo dataBaseCvm.

## T-103 — Integração BrasilAPI (CNPJ) [pendente]

- Refs: AC-102, AC-103, AC-111
- Arquivos: src/main/java/com/apiexternabackend/infra/client/cnpj/CnpjClient.java, src/main/java/com/apiexternabackend/infra/client/cnpj/dto/CnpjResponseDTO.java, src/main/java/com/apiexternabackend/infra/facade/CnpjFacade.java
- Notas: busca dados cadastrais; CNPJ inexistente vira erro tratado; fonte fora do ar → ExternalServiceException.

## T-104 — Integração ViaCEP (CEP) [pendente]

- Refs: AC-102, AC-111
- Arquivos: src/main/java/com/apiexternabackend/infra/client/cep/CepClient.java, src/main/java/com/apiexternabackend/infra/client/cep/dto/CepResponseDTO.java, src/main/java/com/apiexternabackend/infra/facade/CepFacade.java
- Notas: preenche endereço a partir do CEP; CEP inexistente/fonte fora → erro tratado.

## T-105 — Ingestão da base da CVM [pendente]

- Refs: AC-105, AC-106
- Arquivos: src/main/java/com/apiexternabackend/domains/CvmParticipante.java, src/main/java/com/apiexternabackend/repositories/CvmParticipanteRepository.java, src/main/resources/db/migration/V3__create_cvm_participante.sql, src/main/java/com/apiexternabackend/infra/client/cvm/CvmCorretoraClient.java, src/main/java/com/apiexternabackend/infra/client/cvm/dto/CvmCorretoraResponseDTO.java, src/main/java/com/apiexternabackend/services/CvmIngestaoService.java, src/main/java/com/apiexternabackend/config/CvmIngestaoScheduler.java
- Notas: caminho crítico (risco do PRD). Baixa cad_intermed.zip, descompacta, parseia CSV (Latin-1, ';'), faz upsert; guarda a data da base. Roda na subida (se vazia/velha) + @Scheduled diário.

## T-106 — Verificação de autorização na CVM (facade) [pendente]

- Refs: AC-105, AC-106, AC-107
- Arquivos: src/main/java/com/apiexternabackend/infra/facade/CvmFacade.java
- Notas: consulta a base local por CNPJ; distingue "não autorizada" de "não foi possível verificar" (base vazia/desatualizada); devolve a data da base.

## T-107 — Serviço da corretora [pendente]

- Refs: AC-101, AC-102, AC-103, AC-104, AC-105, AC-106, AC-107, AC-111
- Arquivos: src/main/java/com/apiexternabackend/services/CorretoraService.java
- Notas: valida formato do CNPJ antes de qualquer chamada externa; orquestra CnpjFacade + CvmFacade + CepFacade; unicidade; só salva se autorizada.

## T-108 — Controller da corretora [pendente]

- Refs: AC-102, AC-108, AC-109, AC-110
- Arquivos: src/main/java/com/apiexternabackend/resources/CorretoraResource.java
- Notas: POST /corretoras, GET /corretoras (paginado), GET /corretoras/{id}, GET /corretoras/cnpj/{cnpj}.

## T-109 — Testes da corretora [pendente]

- Refs: AC-101, AC-102, AC-103, AC-104, AC-105, AC-106, AC-107, AC-108, AC-109, AC-110, AC-111
- Arquivos: src/test/java/com/apiexternabackend/services/CorretoraServiceTest.java, src/test/java/com/apiexternabackend/infra/facade/CvmFacadeTest.java, src/test/java/com/apiexternabackend/resources/CorretoraResourceTest.java
- Notas: um teste por critério; as fontes externas são mockadas.

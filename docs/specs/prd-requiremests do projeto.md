# Sistema de Gerenciamento de 

## Regras:

1. Quero que utiliza esse padrão de arquitetura para meu projeto:

src
│   ├── main
│   │   ├── java
│   │   │   └── com
│   │   │       ├── apiexternabackend
│   │   │       ├── ApiExternaBackendApplication.java
│   │   │       ├── config
│   │   │       │   └── FeignConfig.java
│   │   │       ├── domains
│   │   │       │   ├── Acao.java
│   │   │       │   ├── Carteira.java
│   │   │       │   ├── CarteiraAcao.java
│   │   │       │   ├── Corretora.java
│   │   │       │   ├── dtos
│   │   │       │   │   ├── AcaoRequestDTO.java
│   │   │       │   │   ├── AcaoResponseDTO.java
│   │   │       │   │   ├── CarteiraAcaoResponseDTO.java
│   │   │       │   │   ├── CarteiraRequestDTO.java
│   │   │       │   │   ├── CarteiraResponseDTO.java
│   │   │       │   │   ├── CorretoraRequestDTO.java
│   │   │       │   │   ├── CorretoraResponseDTO.java
│   │   │       │   │   ├── OperacaoRequestDTO.java
│   │   │       │   │   └── OperacaoResponseDTO.java
│   │   │       │   ├── enums
│   │   │       │   │   └── TipoOperacao.java
│   │   │       │   └── Operacao.java
│   │   │       ├── infra
│   │   │       │   ├── adapter
│   │   │       │   │   ├── AlphaVantageAdapter.java
│   │   │       │   │   ├── BrapiAdapter.java
│   │   │       │   │   └── CotacaoAdapter.java
│   │   │       │   ├── client
│   │   │       │   │   ├── alphavantage
│   │   │       │   │   │   ├── AlphaVantageClient.java
│   │   │       │   │   │   └── dtos
│   │   │       │   │   │       ├── AlphaVantageOverviewDTO.java
│   │   │       │   │   │       ├── AlphaVantageQuoteDTO.java
│   │   │       │   │   │       └── AlphaVantageResponseDTO.java
│   │   │       │   │   ├── brapi
│   │   │       │   │   │   ├── BrapiClient.java
│   │   │       │   │   │   └── dtos
│   │   │       │   │   │       ├── BrapiResponseDTO.java
│   │   │       │   │   │       └── BrapiResultDTO.java
│   │   │       │   │   ├── cep
│   │   │       │   │   │   ├── CepClient.java
│   │   │       │   │   │   └── dto
│   │   │       │   │   │       └── CepResponseDTO.java
│   │   │       │   │   ├── cnpj
│   │   │       │   │   │   ├── CnpjClient.java
│   │   │       │   │   │   └── dto
│   │   │       │   │   │       └── CnpjResponseDTO.java
│   │   │       │   │   └── cvm
│   │   │       │   │       ├── CvmCorretoraClient.java
│   │   │       │   │       └── dto
│   │   │       │   │           └── CvmCorretoraResponseDTO.java
│   │   │       │   ├── converters
│   │   │       │   │   └── TipoOperacaoConverter.java
│   │   │       │   └── facade
│   │   │       │       ├── CepFacade.java
│   │   │       │       ├── CnpjFacade.java
│   │   │       │       └── CvmFacade.java
│   │   │       ├── mappers
│   │   │       │   ├── AcaoMapper.java
│   │   │       │   ├── CarteiraAcaoMapper.java
│   │   │       │   ├── CarteiraMapper.java
│   │   │       │   ├── CorretoraMapper.java
│   │   │       │   └── OperacaoMapper.java
│   │   │       ├── repositories
│   │   │       │   ├── AcaoRepository.java
│   │   │       │   ├── CarteiraAcaoRepository.java
│   │   │       │   ├── CarteiraRepository.java
│   │   │       │   ├── CorretoraRepository.java
│   │   │       │   └── OperacaoRepository.java
│   │   │       ├── resources
│   │   │       │   ├── AcaoResource.java
│   │   │       │   ├── CarteiraResource.java
│   │   │       │   ├── CorretoraResource.java
│   │   │       │   ├── exceptions
│   │   │       │   │   └── GlobalExceptionHandler.java
│   │   │       │   └── OperacaoResource.java
│   │   │       └── services
│   │   │           ├── AcaoService.java
│   │   │           ├── CarteiraService.java
│   │   │           ├── CorretoraService.java
│   │   │           └── OperacaoService.java
│   │   └── resources
│   │       ├── application-dev.properties
│   │       ├── application-test.properties
│   │       ├── application.properties
│   │       ├── static
│   │       └── templates
│   └── test
│       └── java
│           └── com
│               ├── apiexternabackend
│               │   └── ApiExternaBackendApplicationTests.java
│               ├── infra
│               │   └── facade
│               │       └── CvmFacadeTest.java
│               └── services
│                   ├── CarteiraServiceTest.java
│                   └── CorretoraServiceTest.java
└── target
    ├── api-externa-backend-0.0.1-SNAPSHOT.jar
    ├── api-externa-backend-0.0.1-SNAPSHOT.jar.original
    ├── classes
    │   ├── application-dev.properties
    │   ├── application-test.properties
    │   ├── application.properties
    │   └── com
    │       ├── ApiExternaBackendApplication.class
    │       ├── config
    │       │   └── FeignConfig.class
    │       ├── domains
    │       │   ├── Acao.class
    │       │   ├── Carteira.class
    │       │   ├── CarteiraAcao.class
    │       │   ├── Corretora.class
    │       │   ├── dtos
    │       │   │   ├── AcaoRequestDTO.class
    │       │   │   ├── AcaoResponseDTO.class
    │       │   │   ├── CarteiraAcaoResponseDTO.class
    │       │   │   ├── CarteiraRequestDTO.class
    │       │   │   ├── CarteiraResponseDTO.class
    │       │   │   ├── CorretoraRequestDTO.class
    │       │   │   ├── CorretoraResponseDTO.class
    │       │   │   ├── OperacaoRequestDTO.class
    │       │   │   └── OperacaoResponseDTO.class
    │       │   ├── enums
    │       │   │   └── TipoOperacao.class
    │       │   └── Operacao.class
    │       ├── infra
    │       │   ├── adapter
    │       │   │   ├── AlphaVantageAdapter.class
    │       │   │   ├── BrapiAdapter.class
    │       │   │   └── CotacaoAdapter.class
    │       │   ├── client
    │       │   │   ├── alphavantage
    │       │   │   │   ├── AlphaVantageClient.class
    │       │   │   │   └── dtos
    │       │   │   │       ├── AlphaVantageOverviewDTO.class
    │       │   │   │       ├── AlphaVantageQuoteDTO.class
    │       │   │   │       └── AlphaVantageResponseDTO.class
    │       │   │   ├── brapi
    │       │   │   │   ├── BrapiClient.class
    │       │   │   │   └── dtos
    │       │   │   │       ├── BrapiResponseDTO.class
    │       │   │   │       └── BrapiResultDTO.class
    │       │   │   ├── cep
    │       │   │   │   ├── CepClient.class
    │       │   │   │   └── dto
    │       │   │   │       └── CepResponseDTO.class
    │       │   │   ├── cnpj
    │       │   │   │   ├── CnpjClient.class
    │       │   │   │   └── dto
    │       │   │   │       └── CnpjResponseDTO.class
    │       │   │   └── cvm
    │       │   │       ├── CvmCorretoraClient.class
    │       │   │       └── dto
    │       │   │           └── CvmCorretoraResponseDTO.class
    │       │   ├── converters
    │       │   │   └── TipoOperacaoConverter.class
    │       │   └── facade
    │       │       ├── CepFacade.class
    │       │       ├── CnpjFacade.class
    │       │       └── CvmFacade.class
    │       ├── mappers
    │       │   ├── AcaoMapper.class
    │       │   ├── CarteiraAcaoMapper.class
    │       │   ├── CarteiraMapper.class
    │       │   ├── CorretoraMapper.class
    │       │   └── OperacaoMapper.class
    │       ├── repositories
    │       │   ├── AcaoRepository.class
    │       │   ├── CarteiraAcaoRepository.class
    │       │   ├── CarteiraRepository.class
    │       │   ├── CorretoraRepository.class
    │       │   └── OperacaoRepository.class
    │       ├── resources
    │       │   ├── AcaoResource.class
    │       │   ├── CarteiraResource.class
    │       │   ├── CorretoraResource.class
    │       │   ├── exceptions
    │       │   │   └── GlobalExceptionHandler.class
    │       │   └── OperacaoResource.class
    │       └── services
    │           ├── AcaoService.class
    │           ├── CarteiraService.class
    │           ├── CorretoraService.class
    │           └── OperacaoService.class
    ├── generated-sources
    │   └── annotations
    ├── generated-test-sources
    │   └── test-annotations
    ├── maven-archiver
    │   └── pom.properties
    ├── maven-status
    │   └── maven-compiler-plugin
    │       ├── compile
    │       │   └── default-compile
    │       │       ├── createdFiles.lst
    │       │       └── inputFiles.lst
    │       └── testCompile
    │           └── default-testCompile
    │               ├── createdFiles.lst
    │               └── inputFiles.lst
    ├── surefire-reports
    │   ├── com.apiexternabackend.ApiExternaBackendApplicationTests.txt
    │   ├── com.infra.facade.CvmFacadeTest.txt
    │   ├── com.services.CarteiraServiceTest.txt
    │   ├── com.services.CorretoraServiceTest.txt
    │   ├── TEST-com.apiexternabackend.ApiExternaBackendApplicationTests.xml
    │   ├── TEST-com.infra.facade.CvmFacadeTest.xml
    │   ├── TEST-com.services.CarteiraServiceTest.xml
    │   └── TEST-com.services.CorretoraServiceTest.xml
    └── test-classes
        └── com
            ├── apiexternabackend
            │   └── ApiExternaBackendApplicationTests.class
            ├── infra
            │   └── facade
            │       └── CvmFacadeTest.class
            └── services
                ├── CarteiraServiceTest.class
                └── CorretoraServiceTest.class

Esse projeto foi o meu passado vou utilizar o mesmo padrão.

2. Quero que utiliza padrões de projeto (Design Patterns) - Os princípios do S.O.L.I.D, como já utilizei o padrão facade para isolar a lógica da API ao inves de deixar tudo na Service, 

3. Validação no cadastro/login do usuário com autenticação

4. Validação na cvm da corretora.

## Tech Stack

- Docker para subir o container do banco de dados que será o PostgreSQL.
- Implementação do .env, implementa ele e o .env.example e coloca o .env no .gitignore, mais eu irei configurar chave de API, do banco pode configurar pra mim.
- Dependencias do Projeto:
	- Spring Web
	- Spring Data JPA
	- Flyway Migration - para versionar o banco de dados
	- Lombok - se achar que vai atrapalhar no código para manipular não utiliza.
	- Validation
	- H2 Database
	- PostgreSQL Driver
	- SpringDoc OpenAPI - Swagger para docuementar
	- Spring Security - para autentificação
se tiver alguma dependencia a mais para utilizar que acho coerente ao projeto, adiciona e me pergunta se é necessário e o motivo.
- Padrões de Projeto (Design Patterns) - principios do S.O.L.I.D
-  Java, Maven, Spring Boot 21.

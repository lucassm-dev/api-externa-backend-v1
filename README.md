# api-externa-backend-v1

Sistema de Simulação de Carteira de Ações — API REST em Java Spring Boot com
consumo de APIs externas (CNPJ, CEP, CVM, cotações BR/US).

## Stack

- Java 21, Spring Boot 3.3
- Spring Web, Spring Data JPA, Validation
- OpenFeign (clientes HTTP das APIs externas)
- Flyway (versionamento do banco)
- PostgreSQL (dev) / H2 (testes)
- SpringDoc OpenAPI (Swagger)
- MapStruct + Lombok

## Rodando

1. Copie `.env.example` para `.env` e preencha as chaves de API (`BRAPI_TOKEN`, `TWELVEDATA_API_KEY`).
2. Suba o banco: `docker compose up -d`
3. Rode a aplicação: `./mvnw spring-boot:run`
4. Swagger: http://localhost:8080/swagger-ui.html

## Testes

```
./mvnw test
```

## Estrutura

Arquitetura em camadas sob `com.apiexternabackend`: `resources` (controllers),
`services`, `repositories`, `domains` (entidades/dtos/enums), `mappers`, `infra`
(client/adapter/facade/converters para as APIs externas) e `config`.

## APIs externas e limitações

| Fonte | Uso | Plano gratuito | Atraso da cotação |
|---|---|---|---|
| BrasilAPI | Dados cadastrais por CNPJ | Sem chave, sem cota publicada | — |
| ViaCEP | Endereço por CEP | Sem chave | — |
| Dados Abertos CVM | Autorização da corretora (`cad_intermed.zip`) | Público | Base do último dia útil (atualização diária) |
| brapi.dev | Cotação de ações BR | 15.000 requisições/mês | ~30 minutos |
| Twelve Data | Cotação de ações US | 800 créditos/dia (8/min) | ~0,3 a 2 minutos após o fechamento do candle |

A cotação exibida em tela **não é ao vivo** e sempre vem acompanhada do horário em
que foi obtida (RN-Q01). Estouro de cota é tratado com mensagem específica de
"limite excedido" (RN-Q05).

## Especificações

O comportamento do sistema está especificado em `.spec/features/` (fluxo
onp-spec-driven). Documentos de produto em `docs/`.

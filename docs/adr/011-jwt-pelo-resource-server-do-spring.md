# ADR-011: JWT emitido e validado pelo resource server do Spring, com token de 60 minutos

- **Data**: 2026-11-18
- **Status**: Aceito
- **Decisores**: Lucas Mendes (dono do produto)
- **Tags**: autenticação, segurança, dependências

## Contexto e problema

O login emite JWT HS256 com a biblioteca jjwt 0.12.6. A emissão, a leitura do token e o filtro que autentica cada requisição (`JwtService`, `JwtAuthenticationFilter`) foram escritos à mão. O token carrega o id do investidor e o **e-mail** em claro, não tem emissor (`iss`) e vale 24 horas. O Swagger fica público também em produção.

A Aula 13 da disciplina usa o `spring-boot-starter-oauth2-resource-server`, que já traz `JwtEncoder`/`JwtDecoder` e o filtro de Bearer token. Com a chegada de papéis (ADR-013), o token precisa transportar authorities, e o código de segurança próprio passaria a crescer.

## Fatores de decisão

- Código de segurança mantido pelo framework é preferível a código próprio
- Payload de JWT é legível por qualquer um: não pode levar dado pessoal
- Sem refresh token (ADR-001), a validade é a única janela de exposição de um token vazado
- Alinhamento com o que a disciplina avalia

## Opções consideradas

- Migrar para o resource server do Spring (Nimbus)
- Manter jjwt e só corrigir as claims
- Não mudar nada

## Decisão

**Migrar para o resource server.** O token passa a levar só `sub` (id do usuário), `iss`, `iat`, `exp` e `authorities`; o e-mail sai. O emissor é validado na leitura. A validade padrão cai para **60 minutos**, configurável por variável de ambiente. Rotas públicas: cadastro e login; o Swagger fica disponível **só no perfil dev**.

A distinção entre token inválido (`AUT-005`) e expirado (`AUT-006`) continua no contrato de erro (ADR-009). Ela passa a ser feita lendo o erro OAuth2 no entry point, já que o filtro próprio deixa de existir.

### Consequências positivas

- Some o filtro escrito à mão, que engolia exceções para decidir o 401
- Token de outro emissor é recusado
- Nenhum dado pessoal no token
- Token vazado vale no máximo 1 hora

### Consequências negativas

- O investidor faz login de novo a cada hora — mais atrito, sem refresh (ADR-001 continua valendo)
- A separação AUT-005/AUT-006 depende da mensagem de erro do framework, que pode mudar numa atualização
- Testes que geram token pelo `JwtService` precisam ser reescritos

## Prós e contras

### Resource server do Spring ✅ Escolhida

- ✅ Filtro, validação de assinatura, expiração e emissor mantidos pelo Spring
- ✅ Conversão de authorities pronta para `@PreAuthorize`
- ❌ Expirado x inválido exige tratamento próprio

### Manter jjwt

- ✅ Já funciona e está testado
- ❌ Todo o caminho de segurança continua sendo código nosso

### Não mudar

- ✅ Custo zero
- ❌ E-mail exposto no token, 24h de validade, sem emissor

## Links

- ADR-001 — sessão sem renovação (continua valendo)
- ADR-009 — contrato de erro (AUT-005, AUT-006)
- ADR-013 — papéis que o token transporta
- `docs/aulas/AULA-13-LOGIN-COM-JWT.md`

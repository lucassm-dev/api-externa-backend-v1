# Spec: JWT emitido e validado pelo resource server do Spring

> feature: spec-14-jwt-resource-server
> status: rascunho

## Contexto

Issue #17. Hoje o token é emitido e validado com jjwt e um filtro escrito à
mão (`JwtService`, `JwtAuthenticationFilter`). O token leva o e-mail do
investidor em claro, não tem emissor e vale 24 horas. O Swagger é público
também em produção.

A direção já foi decidida no ADR-011 (migrar para o
`spring-boot-starter-oauth2-resource-server`, token de 60 minutos, sem dado
pessoal, emissor validado, Swagger só no perfil dev). Por ser mudança técnica
com decisão registrada, vai direto para spec + design, sem PRD nem RFC
(workshop PRD/RFC/TDD, seção 13, exceção 1). Tem `design.md` porque troca a
autenticação de todas as rotas e envolve decisões que a issue não fecha.

Continuam valendo, sem mudar de texto, os critérios já provados em
`autenticacao-investidor`: sem token → 401 `AUT-005` (AC-439), token válido
libera o acesso (AC-440) e token expirado → 401 `AUT-006` (AC-441). Os testes
deles passam a gerar o token pelo novo emissor.

## Histórias

### US-443 — Token não expõe dado pessoal e dura pouco

Como investidor, quero que o token de acesso não carregue meus dados
pessoais e expire rápido, para que um token vazado revele pouco e sirva por
pouco tempo.

#### AC-545 — O token não leva e-mail nem CPF

- **Dado** um investidor que acabou de fazer login
- **Quando** o conteúdo (payload) do token devolvido é decodificado
- **Então** ele traz apenas `sub` (id do investidor), `iss`, `iat`, `exp` e
  `authorities`, sem e-mail nem CPF

#### AC-546 — O token vale 60 minutos por padrão

- **Dado** a aplicação sem configuração específica de validade
- **Quando** o investidor faz login
- **Então** a resposta informa expiração 60 minutos depois da emissão, e o
  `exp` do token bate com esse horário (configurável por `JWT_EXPIRATION_MS`)

### US-444 — Só tokens legítimos abrem a API

Como dono do produto, quero que a API aceite apenas tokens assinados por ela
e emitidos por ela, para que um token forjado ou vindo de outro sistema não
abra nenhuma rota.

#### AC-547 — Token adulterado é recusado como inválido

- **Dado** um token válido que teve o conteúdo alterado depois de assinado
- **Quando** ele é usado numa rota protegida
- **Então** a API responde 401 com `AUT-005` (token ausente ou inválido)

#### AC-548 — Token de outro emissor é recusado como inválido

- **Dado** um token assinado com a mesma chave, mas com emissor (`iss`)
  diferente do da API
- **Quando** ele é usado numa rota protegida
- **Então** a API responde 401 com `AUT-005`

### US-445 — Documentação da API só em desenvolvimento

Como dono do produto, quero que o Swagger exista só no ambiente de
desenvolvimento, para não publicar o mapa da API em produção.

#### AC-549 — Swagger fora do perfil dev não responde

- **Dado** a aplicação rodando fora do perfil `dev`
- **Quando** alguém acessa `/swagger-ui.html` ou `/v3/api-docs` sem token
- **Então** a resposta é 401 ou 404, nunca a documentação

### US-446 — Segurança mantida pelo framework

Como pessoa desenvolvedora, quero que emissão e validação do token fiquem a
cargo do Spring, para não manter código de segurança próprio.

#### AC-550 — A biblioteca jjwt não faz mais parte da aplicação

- **Dado** a aplicação compilada
- **Quando** procuro as classes da jjwt (`io.jsonwebtoken`) no classpath
- **Então** elas não existem — a dependência saiu do `pom.xml`

## Fora de escopo

- Papéis reais no token (INVESTIDOR, ADMIN) — issue #19. Até lá o token
  leva `ROLE_INVESTIDOR` fixo (ASM-461).
- Separar login (`usuario`) do perfil (`investidor`) — issue #18. O `sub`
  continua sendo o id do investidor.
- Refresh token — continua fora (ADR-001).
- Revogar token antes de expirar (logout no servidor) — continua fora.
- Trocar o algoritmo para chave assimétrica (RS256) — HS256 com o mesmo
  `JWT_SECRET` de hoje.

## Suposições

| ID | Suposição | Status | Resolução |
|---|---|---|---|
| ASM-460 | O id do token chega aos resources como `InvestidorPrincipal(id)` por um conversor do `Jwt`; os resources não mudam. O e-mail sai do record, que não é usado em lugar nenhum. | confirmada | Dono do produto, 2026-11-19 |
| ASM-461 | Até a #19 a claim `authorities` leva `["ROLE_INVESTIDOR"]` fixo, a mesma authority que o filtro atual aplica à mão. | confirmada | Dono do produto, 2026-11-19 |
| ASM-462 | Mantém a tolerância padrão de 60 s do validador de expiração do Spring: token vencido há menos de 60 s ainda é aceito. | confirmada | Dono do produto, 2026-11-19 |
| ASM-463 | Emissor `api-externa-backend`, com override por `JWT_ISSUER`. | confirmada | Dono do produto, 2026-11-19 |
| ASM-464 | Expirado (`AUT-006`) x inválido (`AUT-005`) é decidido lendo a descrição do erro OAuth2 do validador de expiração ("Jwt expired at ..."). Uma atualização do Spring que mude esse texto faria expirado virar `AUT-005` — o AC-441 pega a regressão. | confirmada | Risco já aceito no ADR-011 (consequências negativas) |
| ASM-465 | Tokens emitidos antes do deploy (jjwt, sem `iss`) passam a ser recusados com `AUT-005`; o investidor faz login de novo uma vez. | confirmada | Dono do produto, 2026-11-19: sem janela de transição |

## Perguntas em aberto

Nenhuma — as decisões que a issue não fechava foram respondidas nas
ASM-460 a ASM-463.

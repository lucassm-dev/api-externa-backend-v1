# Spec: Autenticacao investidor

> feature: autenticacao-investidor
> status: em-andamento

## Contexto

**Entrando em implementação agora (SPEC-02), aprovado em 2026-09-04.** Dá ao
investidor uma identidade própria (cadastro com senha + login por e-mail) via
Spring Security + JWT stateless, e garante que carteiras e operações só sejam
visíveis/alteráveis pelo investidor dono. Catálogos de ação e corretora
continuam globais, fora deste isolamento — confirmado pelo Lucas.

O cadastro sem senha que existia em `POST /investidores` (feature `investidor`)
é **removido**: cadastro de investidor passa a ser sempre via
`POST /auth/cadastro`, com senha obrigatória. Ver decisão em `Fora de escopo`
da feature `investidor`.

## Histórias

### US-001 — Cadastro de investidor

Como visitante, quero criar uma conta com minhas credenciais, para que eu possa
ter carteiras que só eu acesso.

#### AC-001 — Cadastro com credenciais válidas cria a conta

- **Dado** que informo nome, e-mail, CPF e senha válidos, nenhum já em uso
- **Quando** envio `POST /auth/cadastro`
- **Então** a conta é criada (201) e passo a conseguir autenticar com essas credenciais

#### AC-002 — E-mail é único (RN-U01)

- **Dado** que já existe uma conta com um e-mail
- **Quando** tento cadastrar outra conta com o mesmo e-mail
- **Então** o cadastro é recusado com 409 e código `AUT-001`

#### AC-003 — Senha nunca é guardada em texto puro

- **Dado** que cadastrei uma conta com uma senha
- **Quando** o registro do investidor é persistido
- **Então** a senha armazenada é um hash BCrypt, não o texto digitado

#### AC-435 — CPF duplicado é recusado

- **Dado** que já existe uma conta com um CPF
- **Quando** tento cadastrar outra conta com o mesmo CPF
- **Então** o cadastro é recusado com 409 e código `AUT-002`

#### AC-436 — Senha fora da política mínima é recusada

- **Dado** uma senha com menos de 8 caracteres, ou sem nenhuma letra, ou sem
  nenhum número
- **Quando** tento cadastrar com essa senha
- **Então** o cadastro é recusado com 422 e código `AUT-008`, mensagem
  explicando a política (mínimo 8 caracteres, com letra e número)

### US-002 — Login

Como investidor cadastrado, quero autenticar com minhas credenciais, para que o
sistema saiba que sou eu e me dê acesso às minhas carteiras.

#### AC-004 — Login com credenciais corretas autentica

- **Dado** que tenho uma conta cadastrada
- **Quando** faço `POST /auth/login` com e-mail e senha corretos
- **Então** recebo 200 com um token JWT válido por 24 horas

#### AC-005 — Login com credenciais erradas é recusado

- **Dado** que tenho uma conta cadastrada
- **Quando** faço login com senha incorreta ou e-mail inexistente
- **Então** recebo 401 com código `AUT-004` e mensagem genérica ("e-mail ou
  senha inválidos") — a resposta é idêntica nos dois casos, não revela qual
  dado estava errado

### US-003 — Isolamento por investidor (RN-U02)

Como investidor, quero que apenas eu acesse minhas carteiras e movimentações,
para que nenhum outro usuário veja ou altere meus dados.

#### AC-006 — Investidor só acessa os próprios dados

- **Dado** que os investidores A e B existem e A tem uma carteira
- **Quando** B, autenticado, tenta ler, renomear, excluir ou operar a carteira
  de A (por id, adivinhando ou enumerando)
- **Então** o acesso é negado como se a carteira não existisse (404,
  `CAR-001`) — não revela que ela existe e pertence a outro investidor

#### AC-007 — Recurso protegido exige autenticação

- **Dado** uma requisição a um recurso de carteira/operação sem token
- **Quando** ela chega ao sistema
- **Então** é rejeitada com 401 antes de qualquer processamento de negócio

#### AC-443 — Carteira e operação usam o investidor do token, nunca um id do corpo

- **Dado** um investidor autenticado
- **Quando** ele cria uma carteira (`POST /carteiras`)
- **Então** ela é vinculada ao investidor do token — `CarteiraRequestDTO` não
  aceita mais `investidorId`, e `GET /carteiras` não aceita mais
  `investidorId` como query param (usa o token)

### US-004 — Autenticação stateless via JWT

Como mantenedor da API, quero autenticação sem estado de sessão no servidor,
para escalar sem sticky session e simplificar a infraestrutura.

#### AC-437 — Login expõe apenas o necessário

- **Dado** um login bem-sucedido
- **Quando** recebo a resposta
- **Então** ela contém `token`, `tipo` ("Bearer") e `expiraEm` (timestamp) —
  nunca a senha nem hash

#### AC-438 — Endpoints públicos funcionam sem token

- **Dado** `POST /auth/cadastro`, `POST /auth/login`, Swagger UI e o
  `/v3/api-docs`
- **Quando** chamados sem token
- **Então** respondem normalmente (não exigem autenticação)

#### AC-439 — Endpoint protegido sem token retorna 401

- **Dado** qualquer endpoint fora de `/auth/**` e do Swagger
- **Quando** chamado sem header `Authorization`
- **Então** retorna 401 com código `AUT-005`, no payload padronizado da SPEC-01

#### AC-440 — Endpoint protegido com token válido funciona normalmente

- **Dado** um token JWT válido e não expirado
- **Quando** chamo um endpoint protegido com `Authorization: Bearer <token>`
- **Então** a requisição é processada normalmente, como o investidor do token

#### AC-441 — Token expirado retorna 401 com código específico

- **Dado** um token JWT expirado (mais de 24h desde a emissão)
- **Quando** chamo um endpoint protegido com esse token
- **Então** retorna 401 com código `AUT-006` (distinto de `AUT-005`, token
  ausente/inválido/malformado)

#### AC-442 — Senha nunca aparece em resposta HTTP nem em log

- **Dado** qualquer chamada de cadastro, login ou consulta de investidor
- **Quando** inspeciono a resposta HTTP e os logs da aplicação
- **Então** a senha (texto puro ou hash) não aparece em nenhum dos dois

## Fora de escopo

- Recuperação de senha, verificação de e-mail, 2FA
- Refresh token (token expira em 24h e exige novo login — decisão do Lucas em 2026-09-04)
- Perfis/papéis (admin vs comum) — todo usuário é um investidor comum
- Restringir `GET /investidores` (lista todos) a "só eu" — continua listando
  todos os investidores ativos, agora exigindo token válido (qualquer um).
  Nome/e-mail não são dados sensíveis o bastante para bloquear nesta spec;
  senha nunca é exposta (AC-442)
- Cadastro/login não protegem os catálogos globais de ação e corretora
  (qualquer investidor autenticado consulta os mesmos)

## Suposições

| ID | Suposição | Status | Resolução |
|---|---|---|---|
| ASM-001 | Autenticação stateless via JWT (Bearer token), não sessão em servidor | confirmada | Decisão do Lucas — SPEC-02 |
| ASM-002 | O identificador de login é o e-mail do investidor | confirmada | Login será por e-mail, não username |
| ASM-003 | Cadastro guarda nome, e-mail, CPF (já existia) e agora senha | confirmada | CPF não estava no escopo literal do prompt SPEC-02, mas já é campo obrigatório único da entidade existente — mantido |
| ASM-004 | Hash de senha com BCrypt (padrão do Spring Security) | confirmada | `BCryptPasswordEncoder`, custo padrão (10) |
| ASM-411 | Carteira de outro investidor responde 404 (`CAR-001`), não 403 — esconde a existência em vez de recusar acesso a algo visível | confirmada | Evita enumeração: B não descobre que a carteira X existe e é de A. 403 fica reservado para autorização de papel/escopo (não usado nesta spec, só 1 papel existe) |
| ASM-412 | `GET /investidores` (lista todos) não é restrito a "ver só a si mesmo" nesta spec | confirmada | Fora de escopo explícito — nome/e-mail não justificam a complexidade agora |
| ASM-413 | Biblioteca JWT: `io.jsonwebtoken:jjwt` (jjwt-api/impl/jackson) — leve, sem depender do módulo OAuth2 do Spring Security | confirmada | Menor footprint que `spring-security-oauth2-resource-server` para um caso stateless simples de token próprio |

## Perguntas em aberto

Nenhuma — as decisões de produto (vínculo ao token, remoção do cadastro sem
senha, política de senha, expiração do token) foram respondidas em
2026-09-04 antes desta especificação.

Histórico (já respondidas antes da SPEC-02 entrar em implementação):

| ID | Pergunta | Status | Resposta |
|---|---|---|---|
| Q-001 | Confirma autenticação como parte do MVP? | respondida | Adiada para 2ª fase — agora é essa 2ª fase (SPEC-02) |
| Q-002 | O login é por e-mail ou por um "username" separado? | respondida | Por e-mail |

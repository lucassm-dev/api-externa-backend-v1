# Spec: Investidor

> feature: investidor
> status: rascunho

## Contexto

Cadastro mínimo de investidor **sem senha e sem login** — a autenticação completa
(cadastro com credenciais, login, isolamento) é 2ª fase (ver
`autenticacao-investidor`). Aqui o investidor existe só para ter um **id** que as
carteiras e operações referenciam; esse id é informado explicitamente nas
requisições do MVP. O e-mail já é o identificador natural que, na 2ª fase, vira o
login.

## Histórias

### US-051 — Cadastrar investidor

Como consumidor da API, quero cadastrar um investidor com nome, e-mail e CPF, para
ter um id a vincular às carteiras.

#### AC-051 — Cadastro com nome, e-mail e CPF cria o investidor

- **Dado** um nome, um e-mail ainda não usado e um CPF válido ainda não usado
- **Quando** cadastro o investidor
- **Então** ele é criado com um id próprio, sem exigir senha

#### AC-052 — E-mail e CPF são únicos

- **Dado** que já existe um investidor com um e-mail (ou um CPF)
- **Quando** tento cadastrar outro com o mesmo e-mail ou o mesmo CPF
- **Então** o cadastro é recusado e a mensagem informa qual campo já está em uso

#### AC-053 — E-mail ou CPF mal formatado é rejeitado

- **Dado** um e-mail com formato inválido ou um CPF inválido
- **Quando** tento cadastrar o investidor
- **Então** o cadastro é recusado, apontando o campo inválido

### US-052 — Consultar investidor

Como consumidor da API, quero listar e buscar investidores, para descobrir o id a
usar nas requisições de carteira e operação.

#### AC-054 — Listar investidores

- **Dado** que existem investidores cadastrados
- **Quando** solicito a listagem
- **Então** recebo os investidores de forma paginada, sem expor dado sensível

#### AC-055 — Buscar investidor por id

- **Dado** um investidor cadastrado
- **Quando** busco pelo id dele
- **Então** recebo seus dados; id inexistente retorna "não encontrado"

## Fora de escopo

- Senha, login, token, Spring Security (tudo na 2ª fase — `autenticacao-investidor`)
- Controle de acesso / isolamento com enforcement (2ª fase, RN-U02)
- Edição e exclusão de investidor

## Suposições

| ID | Suposição | Status | Resolução |
|---|---|---|---|
| ASM-051 | O investidor guarda nome, e-mail e CPF; nenhum campo de senha nesta fase | confirmada | Decisão do dono do produto: nome+e-mail+CPF, auth adiada para a 2ª fase |
| ASM-053 | O CPF é validado no formato/dígito verificador e é único no sistema | aberta | — |
| ASM-052 | O e-mail é único e será o identificador de login quando a 2ª fase chegar | confirmada | Login por e-mail (decisão do dono do produto) |

## Perguntas em aberto

| ID | Pergunta | Status | Resposta |
|---|---|---|---|
| Q-051 | Além de nome e e-mail, o investidor precisa de algum outro dado no MVP? | respondida | Nome, e-mail e CPF |

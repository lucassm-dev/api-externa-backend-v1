≠≠# Spec: Autenticacao investidor

> feature: autenticacao-investidor
> status: rascunho

## Contexto

**Esta feature NÃO entra no MVP — foi adiada para a 2ª fase do projeto.** O
investidor continua existindo no domínio (carteiras e movimentações pertencem a
ele), mas sem tela de cadastro/login nem Spring Security nesta fase. A spec fica
registrada para a 2ª fase.

Quando implementada, dá ao investidor uma identidade própria (cadastro + login
por e-mail) e garante que ele só enxerga e movimenta os próprios dados. No
enunciado é diferencial (Spring Security). Catálogos de ação e corretora são
**globais** e ficam fora deste isolamento.

## Histórias

### US-001 — Cadastro de investidor

Como visitante, quero criar uma conta com minhas credenciais, para que eu possa
ter carteiras que só eu acesso.

#### AC-001 — Cadastro com credenciais válidas cria a conta

- **Dado** que informo um identificador de login ainda não usado e uma senha válida
- **Quando** envio o cadastro
- **Então** a conta é criada e passo a conseguir autenticar com essas credenciais

#### AC-002 — Identificador de login é único (RN-U01)

- **Dado** que já existe uma conta com um identificador de login
- **Quando** tento cadastrar outra conta com o mesmo identificador
- **Então** o cadastro é recusado e a mensagem informa que o identificador já está em uso

#### AC-003 — Senha nunca é guardada em texto puro

- **Dado** que cadastrei uma conta com uma senha
- **Quando** o registro do investidor é persistido
- **Então** a senha armazenada é um hash, não o texto digitado (nenhuma consulta retorna a senha original)

### US-002 — Login

Como investidor cadastrado, quero autenticar com minhas credenciais, para que o
sistema saiba que sou eu e me dê acesso às minhas carteiras.

#### AC-004 — Login com credenciais corretas autentica

- **Dado** que tenho uma conta cadastrada
- **Quando** faço login com o identificador e a senha corretos
- **Então** recebo uma credencial de acesso válida (token/sessão) para as próximas requisições

#### AC-005 — Login com credenciais erradas é recusado

- **Dado** que tenho uma conta cadastrada
- **Quando** faço login com senha incorreta ou identificador inexistente
- **Então** o acesso é negado e a mensagem não revela qual dos dois estava errado

### US-003 — Isolamento por investidor (RN-U02)

Como investidor, quero que apenas eu acesse minhas carteiras e movimentações,
para que nenhum outro usuário veja ou altere meus dados.

#### AC-006 — Investidor só acessa os próprios dados

- **Dado** que os investidores A e B existem e A tem uma carteira
- **Quando** B, autenticado, tenta ler ou movimentar a carteira de A
- **Então** o acesso é negado e a carteira de A não aparece para B

#### AC-007 — Recurso protegido exige autenticação

- **Dado** uma requisição a um recurso de carteira/movimentação sem credencial de acesso
- **Quando** ela chega ao sistema
- **Então** é rejeitada por falta de autenticação, antes de qualquer processamento

## Fora de escopo

- Recuperação de senha, verificação de e-mail, 2FA
- Perfis/papéis (admin vs comum) — todo usuário é um investidor comum
- Cadastro/login não protegem os catálogos globais de ação e corretora (qualquer investidor autenticado consulta os mesmos)

## Suposições

| ID | Suposição | Status | Resolução |
|---|---|---|---|
| ASM-001 | Autenticação stateless via JWT (Bearer token), não sessão em servidor | aberta | — |
| ASM-002 | O identificador de login é o e-mail do investidor | confirmada | Login será por e-mail, não username (decisão do dono do produto) |
| ASM-003 | Cadastro guarda no mínimo: nome, e-mail (login) e senha | aberta | — |
| ASM-004 | Hash de senha com BCrypt (padrão do Spring Security) | aberta | — |

## Perguntas em aberto

| ID | Pergunta | Status | Resposta |
|---|---|---|---|
| Q-001 | Confirma autenticação como parte do MVP? O PRD (5.4) admite cortá-la se o prazo apertar, mantendo o investidor no domínio sem tela de login | respondida | NÃO entra no MVP — adiada para a 2ª fase do projeto |
| Q-002 | O login é por e-mail ou por um "username" separado? | respondida | Por e-mail |

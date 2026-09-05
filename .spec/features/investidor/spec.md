# Spec: Investidor

> feature: investidor
> status: em-andamento

## Contexto

**Atualizada em 2026-09-04 (SPEC-02).** O cadastro de investidor (antes
`POST /investidores`, sem senha) foi **removido e substituído** por
`POST /auth/cadastro` (feature `autenticacao-investidor`), que exige senha.
AC-051/052/053 (cadastro sem senha) foram retirados desta spec — o
comportamento equivalente (unicidade de e-mail/CPF, validação de formato)
agora vive em `autenticacao-investidor` (AC-001, AC-002, AC-435, AC-436).

Esta spec cobre o que sobrou: consultar investidores já cadastrados. Continua
existindo um investidor "dono" de cada carteira (a FK não mudou), mas quem
cria a conta é sempre via `/auth/cadastro`.

## Histórias

### US-052 — Consultar investidor

Como investidor autenticado, quero listar e buscar investidores, para
localizar dados básicos (nome/e-mail) de uma conta.

#### AC-054 — Listar investidores

- **Dado** que existem investidores cadastrados e o requisitante está autenticado
- **Quando** solicito a listagem
- **Então** recebo os investidores de forma paginada, sem expor senha nem hash

#### AC-055 — Buscar investidor por id

- **Dado** um investidor cadastrado e o requisitante autenticado
- **Quando** busco pelo id dele
- **Então** recebo seus dados; id inexistente retorna "não encontrado" (`AUT-003`)

## Fora de escopo

- Cadastro de investidor — migrado para `autenticacao-investidor` (`POST /auth/cadastro`)
- Senha, login, token — tudo em `autenticacao-investidor`
- Restringir a listagem a "só a própria conta" — ver ASM-412 em `autenticacao-investidor`
- Edição e exclusão de investidor pelo próprio dono (`DELETE /investidores/{id}` continua existindo sem checar se é o próprio investidor — fora de escopo desta spec)

## Suposições

| ID | Suposição | Status | Resolução |
|---|---|---|---|
| ASM-052 | O e-mail é único e é o identificador de login | confirmada | Login por e-mail (decisão do dono do produto) |
| ASM-053 | O CPF é validado no formato/dígito verificador e é único no sistema | aberta | — (constraint de unicidade existe no banco; validação de dígito verificador não foi implementada em nenhuma spec até agora) |

## Perguntas em aberto

Nenhuma.
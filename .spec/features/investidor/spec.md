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

Como investidor autenticado, quero listar e buscar a minha conta, para
consultar meus dados básicos (nome/e-mail). Contas de outros investidores
não aparecem (ver `spec-13-autorizacao-investidor`).

#### AC-054 — Listar investidores

- **Dado** que existem investidores cadastrados e o requisitante está autenticado
- **Quando** solicito a listagem
- **Então** recebo, de forma paginada, apenas a minha própria conta (AC-542),
  sem expor senha nem hash

#### AC-055 — Buscar investidor por id

- **Dado** um investidor cadastrado e o requisitante autenticado
- **Quando** busco pelo id dele
- **Então** recebo seus dados se o id for o meu; id inexistente ou de outro
  investidor retorna "não encontrado" (`AUT-003`, AC-543)

## Fora de escopo

- Cadastro de investidor — migrado para `autenticacao-investidor` (`POST /auth/cadastro`)
- Senha, login, token — tudo em `autenticacao-investidor`
- Edição de investidor pelo próprio dono
- Restrição de listagem, busca e exclusão à própria conta — feita em `spec-13-autorizacao-investidor`

## Suposições

| ID | Suposição | Status | Resolução |
|---|---|---|---|
| ASM-052 | O e-mail é único e é o identificador de login | confirmada | Login por e-mail (decisão do dono do produto) |
| ASM-053 | O CPF é validado no formato/dígito verificador e é único no sistema | aberta | — (constraint de unicidade existe no banco; validação de dígito verificador não foi implementada em nenhuma spec até agora) |

## Perguntas em aberto

Nenhuma.
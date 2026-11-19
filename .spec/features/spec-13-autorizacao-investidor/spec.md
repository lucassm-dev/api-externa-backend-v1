# Spec: Investidor só enxerga e exclui a própria conta

> feature: spec-13-autorizacao-investidor
> status: rascunho

## Contexto

Issue #16. `GET /investidores`, `GET /investidores/{id}` e
`DELETE /investidores/{id}` só exigem estar autenticado e não conferem quem
está chamando. Na prática, qualquer investidor logado lista todos os outros
(nome, e-mail, CPF) e consegue excluir a conta de outro investidor.

É falha de autorização horizontal (Aula 14, seção 3.4). A decisão já está
registrada no ADR-013 ("INVESTIDOR opera somente o que é seu; recurso de
outro investidor responde como inexistente"). Por isso esta feature vai
direto para spec + tarefas, sem RFC nem TDD: a mudança é pequena, reversível
e a direção já foi decidida (workshop PRD/RFC/TDD, seções 13 e 14).

Substitui o que ficou fora de escopo na `investidor` ("restringir a listagem
a só a própria conta") e na `autenticacao-investidor` (ASM-412).

## Histórias

### US-441 — Investidor só vê os próprios dados

Como investidor, quero que nenhum outro investidor consiga ver meu nome,
e-mail e CPF, para que meus dados pessoais não fiquem expostos a qualquer
pessoa com conta no sistema.

#### AC-542 — A listagem de investidores traz só a própria conta

- **Dado** que os investidores A e B estão ativos e A está autenticado
- **Quando** A lista os investidores (`GET /investidores`)
- **Então** A recebe uma página com um único investidor, ele mesmo, e o B
  não aparece (o formato paginado da resposta não muda)

#### AC-543 — Buscar outro investidor pelo id responde como inexistente

- **Dado** que os investidores A e B estão ativos e A está autenticado
- **Quando** A busca o B pelo id (`GET /investidores/{idDoB}`)
- **Então** A recebe "investidor não encontrado" (404 `AUT-003`), a mesma
  resposta de um id que nunca existiu, sem nenhum dado do B

### US-442 — Investidor só exclui a própria conta

Como investidor, quero que ninguém além de mim consiga excluir a minha
conta, para não perder o acesso por ação de outra pessoa.

#### AC-544 — Excluir a conta de outro investidor é recusado e não muda nada

- **Dado** que os investidores A e B estão ativos e A está autenticado
- **Quando** A tenta excluir a conta do B (`DELETE /investidores/{idDoB}`)
- **Então** A recebe "investidor não encontrado" (404 `AUT-003`) e a conta
  do B continua ativa: o B segue conseguindo consultar os próprios dados

## Fora de escopo

- Listagem completa de investidores para o administrador — é a issue #21
  (papel ADMIN, ADR-013).
- Separar login (`usuario`) do perfil (`investidor`) e migrar o JWT para o
  resource server do Spring — issues #17 e #18. Esta correção usa o
  `InvestidorPrincipal` que já existe.
- Carteiras e operações: já conferem o dono hoje (`CAR-001`/`OPE-001`).
- Corretoras e ações: são catálogo compartilhado, não pertencem a um
  investidor (ADR-014 e ADR-015).
- Revogar token de investidor excluído antes de expirar — continua como na
  ASM-442 da `spec-10-vazamento-investidor-inativo`.

## Suposições

| ID | Suposição | Status | Resolução |
|---|---|---|---|
| ASM-458 | Recurso de outro investidor responde 404 `AUT-003` com a mesma mensagem de id inexistente, e não 403 — um 403 confirmaria que a conta existe. | confirmada | ADR-013 e issue #16 ("responde como inexistente") |
| ASM-459 | `GET /investidores` continua existindo e paginado, filtrado para o investidor logado (página com um item), em vez de ser removido até a #21. Mantém o contrato para quem já consome a rota. | confirmada | Escolhida pelo dono do produto em 2026-11-18 (opção a) |

## Perguntas em aberto

Nenhuma — a forma da listagem foi decidida na ASM-459.

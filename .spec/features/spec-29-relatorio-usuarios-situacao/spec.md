# Spec: Relatório de usuários por situação

> feature: spec-29-relatorio-usuarios-situacao
> status: pronta

## Contexto

Issue #35 (PRD 15.5, ADR-013). Completa a spec-18: a listagem
`GET /admin/usuarios` (só ADMIN, filtro por situação, indicadores do ADR-013,
sem conteúdo de carteira) já existe. Falta filtrar por papel e mostrar os
totais por situação.

Decisões do dono do produto (2026-11-22):

1. Totais numa rota nova, `GET /admin/usuarios/totais`, que devolve
   `{"ATIVO": n, "BLOQUEADO": n}`; a listagem paginada não muda de formato.
2. `papel` é opcional na listagem e nos totais, combinado com `situacao`.
3. Usuários excluídos não entram nos totais, como já não entram na listagem.

## Histórias

### US-479 — ADMIN acompanha a situação das contas

Como ADMIN, quero filtrar os usuários por papel e ver quantos estão ativos e
bloqueados, para acompanhar a situação das contas.

#### AC-657 — Listagem filtra por papel

- **Dado** um investidor ativo, um investidor bloqueado e um ADMIN
- **Quando** o ADMIN lista com `papel=INVESTIDOR` e depois com `papel=INVESTIDOR&situacao=BLOQUEADO`
- **Então** a primeira lista traz só investidores, incluindo os dois, e nenhum ADMIN
- **E** a segunda traz só investidores bloqueados

#### AC-658 — Totais por situação, sem excluídos

- **Dado** dois investidores ativos, um investidor bloqueado, um investidor excluído e um ADMIN a mais
- **Quando** o ADMIN pede os totais com `papel=INVESTIDOR`
- **Então** o total de ativos sobe 2 e o de bloqueados sobe 1 em relação a antes deles existirem
- **E** a resposta só tem as chaves `ATIVO` e `BLOQUEADO`, sem conteúdo de carteira

#### AC-659 — Só o ADMIN vê os totais

- **Dado** um token de investidor e nenhum token
- **Quando** cada um pede os totais
- **Então** o investidor recebe 403 `AUT-007` e sem token recebe 401 `AUT-005`

## Fora de escopo

- Totais por papel numa resposta só (basta chamar com cada `papel`)
- Série histórica de bloqueios
- Exportar arquivo

## Suposições

| ID | Suposição | Status | Resolução |
|---|---|---|---|
| ASM-508 | Totais em rota nova `GET /admin/usuarios/totais`, com as duas situações sempre presentes (0 quando não há ninguém) | confirmada | Dono do produto, 2026-11-22: "Rota nova /totais" |
| ASM-509 | `papel` filtra listagem e totais; excluídos não entram nos totais | confirmada | Dono do produto, 2026-11-22: "Filtra por papel, sem excluídos" |

## Perguntas em aberto

Nenhuma.

## Documentos relacionados

[Tarefas](tasks.md) · [spec-18](../spec-18-gerenciar-usuarios/spec.md) · [ADR-013](../../../docs/adr/013-controle-de-acesso-por-papeis.md)

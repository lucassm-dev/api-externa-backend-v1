# Spec: SPEC-05 — Carteira obrigatória para cadastrar ação

> feature: spec-05-carteira-obrigatoria
> status: em-andamento

## Contexto

**Decisão fechada com o Lucas em 2026-09-04** (ver análise de impacto na
conversa): interpretação **(a)** — Ação continua catálogo global e
compartilhado entre investidores, como já estabelecido nas SPEC-01/03/04
(`ACA-002` duplicidade entre ativas, `ACA-003` bloqueio de exclusão com
posição em qualquer carteira). Esta spec só adiciona um **gate de UX**:
`POST /acoes` passa a exigir que o investidor autenticado já tenha pelo
menos uma carteira ativa — sem isso, o cadastro é recusado.

A interpretação alternativa (Ação vinculada a uma carteira específica) foi
descartada: exigiria dar dono à Ação, quebrando o catálogo compartilhado e
piorando o consumo de cota das APIs de cotação (cada carteira cadastraria e
buscaria cotação separadamente pro mesmo ticker).

## Histórias

### US-420 — Bloquear cadastro de ação sem carteira

Como operador do sistema, quero que o cadastro de ação exija que o
investidor já tenha uma carteira, para orientar o fluxo correto (criar
carteira antes de povoar o catálogo que ela vai usar).

#### AC-466 — Investidor sem carteira ativa não cadastra ação

- **Dado** um investidor autenticado sem nenhuma carteira ativa
- **Quando** ele chama `POST /acoes`
- **Então** o cadastro é recusado com 409 e código `ACA-004`

#### AC-467 — Investidor com carteira ativa cadastra normalmente

- **Dado** um investidor autenticado com pelo menos uma carteira ativa
- **Quando** ele chama `POST /acoes` com dados válidos
- **Então** o cadastro é aceito (201) — comportamento de hoje preservado

#### AC-468 — Mensagem de erro orienta o próximo passo

- **Dado** o bloqueio do AC-466
- **Quando** leio a mensagem de erro
- **Então** ela diz explicitamente para cadastrar uma carteira antes
  ("Cadastre uma carteira antes de cadastrar ações")

## Fora de escopo

- Qualquer outro gate por carteira (corretora, por exemplo, já não exige — o
  prompt só pede isso pra ação)
- Vincular a ação cadastrada à carteira que desbloqueou o cadastro — ela
  continua catálogo global, sem dono (decisão (a))

## Suposições

| ID | Suposição | Status | Resolução |
|---|---|---|---|
| ASM-420 | "Carteira ativa" aqui é qualquer carteira do investidor com `ativa=true`, independente de mercado (BR/US) ou de ter posições — só a existência importa | confirmada | O prompt fala em "ao menos uma carteira cadastrada", sem qualificar mercado nem conteúdo |

## Perguntas em aberto

Nenhuma — a interpretação foi decidida antes desta spec.
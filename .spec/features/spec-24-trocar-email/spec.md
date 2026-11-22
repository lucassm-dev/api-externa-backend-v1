# Spec: Trocar e-mail com confirmação por código

> feature: spec-24-trocar-email
> status: rascunho

## Contexto

Issue #28 (PRD 15.3), envio de e-mail pelo ADR-016. O usuário troca o e-mail
de acesso sem risco de perder a conta por um endereço digitado errado: o
e-mail só muda depois de confirmado o código enviado ao endereço novo. Usa o
mesmo mecanismo de código da spec-22.

Decisões de projeto (respondidas pelo dono do produto em 2026-11-21):

1. `POST /auth/email` (autenticado) com `novoEmail` e `senhaAtual`; responde
   202 e envia o código ao novo e-mail. Senha atual errada: `422 AUT-014`.
2. `POST /auth/email/confirmar` (autenticado) com `codigo`; responde 204 e só
   então troca o e-mail.
3. Mesmas regras de código da spec-22 (6 dígitos, 10 min, 5 tentativas, 3
   pedidos a cada 15 min, novo pedido invalida o anterior). Como o fluxo é
   autenticado, o limite de pedidos responde `422 AUT-015` e a falha de envio
   `503 EXT-012`, sem gravar nada.
4. O e-mail antigo não é avisado da troca.

## Histórias

### US-471 — Trocar o e-mail de acesso

Como usuário logado, quero trocar meu e-mail de acesso confirmando o novo
endereço, para não perder a conta por um erro de digitação.

#### AC-625 — Confirmação troca o e-mail

- **Dado** um usuário logado que pediu a troca para um e-mail livre
- **Quando** confirma com o código recebido no novo e-mail
- **Então** recebe 204, o login passa a ser pelo novo e-mail e o token atual continua válido

#### AC-626 — Sem confirmação o e-mail antigo continua valendo

- **Dado** um pedido de troca não confirmado
- **Quando** o usuário faz login
- **Então** o e-mail antigo funciona e o novo não

#### AC-627 — Novo e-mail já usado é recusado

- **Dado** um e-mail já usado por outro usuário não excluído
- **Quando** é pedido na troca, ou ocupado entre o pedido e a confirmação
- **Então** recebe 409 `AUT-001` e o e-mail não muda

#### AC-628 — Exige a senha atual

- **Dado** um usuário logado
- **Quando** pede a troca com a senha atual errada
- **Então** recebe 422 `AUT-014` e nenhum e-mail é enviado

#### AC-629 — Código segue as regras de expiração e tentativas

- **Dado** um código de troca de e-mail
- **Quando** está expirado, já foi usado ou recebeu 5 tentativas erradas
- **Então** a confirmação recebe 422 `AUT-013`

#### AC-630 — Falha no envio

- **Dado** o envio de e-mail fora do ar
- **Quando** a troca é pedida
- **Então** recebe 503 `EXT-012` e nenhum código fica gravado

#### AC-631 — Limite de pedidos

- **Dado** 3 pedidos nos últimos 15 minutos
- **Quando** o quarto chega
- **Então** recebe 422 `AUT-015`

#### AC-632 — Códigos dos dois fluxos não se misturam

- **Dado** um código de troca de e-mail
- **Quando** é usado para redefinir a senha
- **Então** recebe 422 `AUT-013`

## Fora de escopo

- Avisar o e-mail antigo da troca

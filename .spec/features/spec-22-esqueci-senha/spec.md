# Spec: Esqueci minha senha com código por e-mail

> feature: spec-22-esqueci-senha
> status: rascunho

## Contexto

Issue #26 (PRD 15.2), envio de e-mail pelo ADR-016. Quem esquece a senha não
tem como voltar a entrar. A partir desta spec, o usuário pede um código pelo
e-mail e, com o código válido, define uma senha nova.

Decisões de projeto (respondidas pelo dono do produto em 2026-11-21):

1. Código de 6 dígitos, válido por 10 minutos, no máximo 5 tentativas.
2. Pedir um código novo invalida o anterior.
3. No máximo 3 pedidos por e-mail a cada 15 minutos.
4. Código inválido, expirado ou já usado responde o mesmo erro, `422 AUT-013`.

## Histórias

### US-468 — Recuperar o acesso pelo e-mail

Como usuário que esqueceu a senha, quero receber um código no meu e-mail e
definir uma senha nova, para voltar a entrar.

#### AC-609 — Senha antiga para de funcionar e a nova funciona

- **Dado** um usuário ativo que recebeu um código válido
- **Quando** redefine a senha com esse código
- **Então** recebe 204; o login com a senha antiga é recusado e com a nova funciona

#### AC-610 — Token emitido antes da troca recebe 401

- **Dado** um token emitido antes da troca de senha
- **Quando** é usado depois da troca
- **Então** recebe 401; um token novo funciona

#### AC-611 — Código já usado é recusado

- **Dado** um código que já redefiniu a senha
- **Quando** é usado de novo
- **Então** recebe 422 `AUT-013`

#### AC-612 — Código expirado é recusado

- **Dado** um código com mais de 10 minutos
- **Quando** é usado
- **Então** recebe 422 `AUT-013`

#### AC-613 — Após 5 erros, nem o código certo vale

- **Dado** um código que já recebeu 5 tentativas erradas
- **Quando** o código certo é enviado
- **Então** recebe 422 `AUT-013`

#### AC-614 — Pedir novo código invalida o anterior

- **Dado** dois pedidos seguidos
- **Quando** o primeiro código é usado
- **Então** é recusado e o segundo funciona

### US-469 — O pedido não revela nada nem abre brecha

Como dono do produto, quero que o pedido de código não revele quem tem conta
e não possa ser usado para abuso.

#### AC-615 — E-mail inexistente responde igual ao existente

- **Dado** um e-mail sem conta
- **Quando** pede o código
- **Então** recebe o mesmo `202` vazio de um e-mail com conta, e nada é enviado

#### AC-616 — Usuário bloqueado não recupera a senha

- **Dado** um usuário bloqueado
- **Quando** pede o código ou tenta redefinir
- **Então** nenhum e-mail sai e a redefinição recebe 422 `AUT-013`

#### AC-617 — Mais de 3 pedidos em 15 minutos não enviam e-mail

- **Dado** 3 pedidos nos últimos 15 minutos
- **Quando** o quarto pedido chega
- **Então** responde `202` e nenhum e-mail sai

#### AC-618 — Falha no SMTP responde igual e não grava código

- **Dado** o envio de e-mail fora do ar
- **Quando** o código é pedido
- **Então** responde `202` vazio e nenhum código fica gravado

#### AC-619 — Código guardado como hash

- **Dado** um código enviado
- **Quando** o banco é lido
- **Então** o código não aparece em claro

## Fora de escopo

- Envio assíncrono e servidor SMTP em memória (GreenMail) nos testes
- Trocar a senha estando logado

## Suposições

| ID | Suposição | Status | Resolução |
|---|---|---|---|
| ASM-503 | O `iat` do JWT tem precisão de segundos: token emitido no mesmo segundo da troca continua valendo. | aberta | — |

## Perguntas em aberto

Nenhuma.

# Spec: Trocar senha no perfil

> feature: spec-23-trocar-senha
> status: rascunho

## Contexto

Issue #27 (PRD 15.3). O usuário logado troca a própria senha informando a
atual. A troca reaproveita o `senhaAlteradaEm` da spec-22: todo token emitido
antes dela deixa de valer.

Decisões de projeto (respondidas pelo dono do produto em 2026-11-21):

1. `PUT /auth/senha`, autenticado, com `senhaAtual` e `novaSenha`.
2. Senha atual errada responde `422 AUT-014`; nova senha fora da política, `AUT-008`.
3. A resposta devolve um token novo, no formato do login, para o dispositivo
   que trocou continuar logado; os demais são encerrados.

## Histórias

### US-470 — Trocar a própria senha

Como usuário logado, quero trocar minha senha informando a atual, para
manter minha conta segura e encerrar sessões em outros dispositivos.

#### AC-620 — Senha atual errada é recusada

- **Dado** um usuário logado
- **Quando** informa a senha atual errada
- **Então** recebe 422 `AUT-014` e a senha não muda

#### AC-621 — Nova senha gravada com BCrypt

- **Dado** um usuário logado com a senha atual certa
- **Quando** troca a senha
- **Então** recebe 200, a senha é gravada como hash BCrypt e só a nova funciona no login

#### AC-622 — Tokens anteriores à troca recebem 401

- **Dado** tokens emitidos antes da troca, inclusive o usado para trocar
- **Quando** são usados depois da troca
- **Então** recebem 401; o token devolvido pela troca funciona

#### AC-623 — Nova senha fora da política

- **Dado** um usuário logado
- **Quando** informa nova senha fora da política
- **Então** recebe 422 `AUT-008` e a senha não muda

#### AC-624 — Exige autenticação

- **Dado** uma requisição sem token
- **Quando** chama a troca de senha
- **Então** recebe 401

## Fora de escopo

- Impedir que a nova senha seja igual à atual

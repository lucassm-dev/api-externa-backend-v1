# Spec: Foto de perfil

> feature: spec-25-foto-perfil
> status: rascunho

## Contexto

Issue #30 (PRD 15.3), armazenamento decidido no ADR-017: a foto fica no
Postgres, em tabela própria, até 10 MB, em JPEG, PNG ou WebP detectados pela
assinatura do arquivo, guardada como veio.

Implementada em TDD (Aula 15): cada critério entra por um commit com o teste
falhando, seguido do commit que o faz passar.

Decisões de projeto (respondidas pelo dono do produto em 2026-11-21):

1. Rotas autenticadas, sempre sobre o usuário do token, sem id na URL:
   `PUT /auth/me/foto` (multipart, campo `arquivo`) envia ou troca e responde
   204; `DELETE /auth/me/foto` remove e responde 204; `GET /auth/me/foto`
   devolve a imagem com o tipo detectado, ou 404 sem foto.
2. Arquivo acima de 10 MB responde `422 AUT-016`; formato fora de JPEG, PNG e
   WebP responde `422 AUT-017`.
3. `GET /auth/me` ganha `fotoUrl`: `/auth/me/foto` com foto, `null` sem foto.
   Isso altera o AC-574 (spec-17), que passa a aceitar esse campo.

## Histórias

### US-472 — Manter a própria foto de perfil

Como usuário logado, quero enviar, trocar e remover a minha foto de perfil,
para ser reconhecido no sistema.

#### AC-633 — Arquivo acima de 10 MB é recusado

- **Dado** um usuário logado
- **Quando** envia uma imagem com mais de 10 MB
- **Então** recebe 422 `AUT-016` e nenhuma foto é gravada

#### AC-634 — Formato fora de JPEG, PNG e WebP é recusado

- **Dado** um usuário logado
- **Quando** envia um arquivo cujo conteúdo não é JPEG, PNG nem WebP, mesmo
  declarado como imagem
- **Então** recebe 422 `AUT-017` e nenhuma foto é gravada

#### AC-635 — Envio e leitura da foto

- **Dado** um usuário logado
- **Quando** envia uma imagem válida
- **Então** recebe 204 e `GET /auth/me/foto` devolve os mesmos bytes, com o
  tipo detectado

#### AC-636 — Trocar substitui a foto

- **Dado** um usuário que já tem foto
- **Quando** envia outra imagem válida
- **Então** `GET /auth/me/foto` devolve a nova

#### AC-637 — Remover a foto

- **Dado** um usuário com foto
- **Quando** a remove
- **Então** recebe 204 e `GET /auth/me/foto` responde 404

#### AC-638 — Cada usuário só mexe na própria foto

- **Dado** dois usuários, um deles com foto
- **Quando** o outro lê, troca ou remove a foto pelo próprio token
- **Então** a foto do primeiro continua igual; sem token, as rotas respondem 401

#### AC-639 — `GET /auth/me` informa a foto

- **Dado** um usuário logado
- **Quando** chama `GET /auth/me`
- **Então** `fotoUrl` é `/auth/me/foto` se ele tem foto e `null` se não tem

## Fora de escopo

- Redimensionar ou comprimir a imagem (ADR-017)
- Ver a foto de outro usuário

## Suposições

Nenhuma.

## Perguntas em aberto

Nenhuma.

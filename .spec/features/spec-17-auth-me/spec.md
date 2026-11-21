# Spec: GET /auth/me com os dados e o papel do usuário logado

> feature: spec-17-auth-me
> status: rascunho

## Contexto

Issue #20. Pedido original (issue, literal): "O frontend precisa mostrar no
topo da tela qual é o papel do usuário logado e abrir o perfil. `GET /auth/me`
devolvendo id, nome, e-mail e papel do usuário autenticado." Referência: PRD
15.3 (indicador do papel no canto superior de todas as telas e ícone que abre
o perfil).

Feature pequena: uma rota de leitura, sem migração. Pesquisa de concorrentes
não se aplica: "quem sou eu" a partir do token é um padrão consolidado.

Decisões de projeto (respondidas pelo dono do produto em 2026-11-21):

1. **`nome` vem do perfil de investidor.** O ADMIN só existe como `usuario`
   (spec-15, ADR-012), então para ele `nome` vem `null`; o frontend mostra o
   e-mail no lugar. Se o ADMIN ganha nome é decisão da #21.
2. **Conta excluída com token ainda válido recebe 401 `AUT-005`** ("Token
   ausente ou inválido."): o frontend trata como sessão encerrada.
3. **O papel vem como código** (`INVESTIDOR`, `ADMIN`); o rótulo da tela é do
   frontend.
4. **Só `/auth/cadastro` e `/auth/login` continuam públicas.** Hoje `/auth/**`
   inteiro é público no `SecurityConfig`, o que deixaria `/auth/me` aberto.

Relação com outras specs: a #19 (spec-16, ainda não mergeada) faz o ADMIN
conseguir login e define a matriz de acesso. `/auth/me` é para qualquer
usuário autenticado, então entra na matriz como "os dois papéis".

## Histórias

### US-456 — Saber quem está logado

Como usuário logado no frontend, quero consultar meus próprios dados e meu
papel, para que a tela mostre quem sou e o que posso fazer.

#### AC-569 — Investidor recebe os próprios dados e o papel

- **Dado** um investidor logado
- **Quando** ele chama `GET /auth/me` com o próprio token
- **Então** recebe 200 com `id`, `nome`, `email` e `papel` = `INVESTIDOR`,
  todos iguais aos do cadastro dele

#### AC-570 — Cada token devolve só o próprio dono

- **Dado** dois investidores logados, A e B
- **Quando** cada um chama `GET /auth/me` com o próprio token
- **Então** A recebe só os dados de A e B só os de B; nenhuma resposta traz
  dado do outro

#### AC-571 — ADMIN recebe os dados sem nome

- **Dado** um token válido de um usuário ADMIN
- **Quando** ele chama `GET /auth/me`
- **Então** recebe 200 com `id`, `email`, `papel` = `ADMIN` e `nome` nulo

### US-457 — Rota protegida e sem segredo

Como dono do produto, quero que `/auth/me` só responda a quem tem sessão
válida e nunca exponha a senha, para que a rota não vaze dado de acesso.

#### AC-572 — Sem token não há dados

- **Dado** nenhuma sessão
- **Quando** alguém chama `GET /auth/me` sem token
- **Então** recebe 401 `AUT-005`, sem dados de usuário

#### AC-573 — Conta excluída é tratada como sessão encerrada

- **Dado** um investidor que excluiu a própria conta, com o token emitido
  antes da exclusão ainda dentro da validade
- **Quando** ele chama `GET /auth/me` com esse token
- **Então** recebe 401 `AUT-005` ("Token ausente ou inválido.")

#### AC-574 — Resposta nunca traz a senha

- **Dado** um investidor logado
- **Quando** ele chama `GET /auth/me`
- **Então** a resposta tem só os campos `id`, `nome`, `email` e `papel` —
  nenhum campo de senha nem o hash

#### AC-575 — Cadastro e login continuam públicos

- **Dado** nenhuma sessão
- **Quando** alguém chama `POST /auth/cadastro` e `POST /auth/login`
- **Então** as duas rotas respondem sem exigir token (não 401)

## Fora de escopo

- Editar perfil (foto, e-mail, senha) — issues #27, #28 e #30.
- Nome para o ADMIN — decisão da #21.
- Barrar usuário bloqueado em toda requisição — issue #21. Até lá,
  bloqueado com token válido ainda recebe 200 aqui (ASM-482).
- Login do ADMIN e matriz de acesso — issue #19 (spec-16).

## Suposições

| ID | Suposição | Status | Resolução |
|---|---|---|---|
| ASM-479 | A resposta é um DTO novo (`UsuarioLogadoResponseDTO`) com `id`, `nome`, `email`, `papel`, sem reaproveitar `InvestidorResponseDTO`, que não tem papel. | aberta | — |
| ASM-480 | O teste do ADMIN (AC-571) usa um token gerado direto pelo `JwtService` para um `usuario` ADMIN gravado no banco, porque o login do ADMIN só abre com a #19. | aberta | — |
| ASM-481 | Conta excluída é detectada pela situação `EXCLUIDO` do `usuario` lido pelo id do token; usuário inexistente pelo id também responde 401 `AUT-005`. | aberta | — |
| ASM-482 | Bloqueado com token válido recebe 200 em `/auth/me` até a #21 (bloqueio imediato), coerente com a ASM-477 da spec-16. | aberta | — |
| ASM-483 | Os códigos desta spec começam em US-456/AC-569/ASM-479/T-504 porque a spec-16 (branch `feat/19`) já reserva US-451–455, AC-558–568, ASM-472–478 e T-501–503; o motor não enxerga branches não mergeadas. | confirmada | Conferido no git em 2026-11-21 |

## Perguntas em aberto

Nenhuma. As três decisões que a issue não fechava foram respondidas
(decisões 1 a 3 do Contexto).

# Spec: Identidade de acesso (usuario) separada do perfil de investidor

> feature: spec-15-identidade-usuario
> status: rascunho

## Contexto

Issue #18. Hoje `investidor` é a única identidade do sistema e guarda e-mail
e senha. Administrador e suporte precisam entrar sem ser investidores (sem
CPF, sem carteira). A direção foi decidida no ADR-012: a tabela `usuario`
guarda o acesso (e-mail, hash da senha, papel, situação) e `investidor` vira
o perfil (nome, CPF) com vínculo 1:1.

Decisões de projeto (respondidas pelo dono do produto em 2026-11-20):

1. **Id compartilhado.** `investidor.id` é o próprio `usuario.id` (`@MapsId`).
   A migração preserva os ids, o `sub` do token continua sendo o id do
   investidor e os resources (`InvestidorPrincipal`) não mudam.
2. **Situação do usuário** é `ATIVO`, `BLOQUEADO` ou `EXCLUIDO`. O e-mail é
   único só entre usuários não excluídos (índice parcial no Postgres), o que
   mantém a regra da SPEC-10 de reuso de e-mail.
3. **`investidor.ativo` continua existindo.** Excluir a conta marca
   `investidor.ativo = false` e `usuario.situacao = EXCLUIDO` na mesma
   transação. Consultas de investidor e o índice parcial de CPF não mudam.
4. **Login só aceita usuário `ATIVO`.** Bloqueado com a senha certa recebe
   aviso específico de conta bloqueada; com a senha errada, a mesma mensagem
   genérica de credencial inválida, para que ninguém descubra quais e-mails
   existem sem saber a senha. Quem bloqueia é a issue #21.
5. **A migração V17 é testada num Postgres real** (Testcontainers, só no
   escopo de teste). Os demais testes continuam em H2 com Flyway desligado.

Continuam valendo, sem mudar de texto, os critérios da SPEC-10 sobre reuso de
e-mail e CPF de conta excluída (AC-505, AC-506, AC-507) e o login de
investidor ativo (AC-503, AC-508). Os testes deles passam a rodar sobre
`usuario`.

## Histórias

### US-447 — Login de quem já tem conta continua igual

Como investidor que já tinha conta antes da mudança, quero continuar
entrando com o mesmo e-mail e a mesma senha, para não perceber a migração.

#### AC-551 — Investidor cadastrado antes da migração faz login com a mesma senha

- **Dado** um banco Postgres com as migrações até a V16 e um investidor
  ativo cadastrado com e-mail e senha
- **Quando** a migração V17 roda e esse investidor faz login com a mesma
  senha
- **Então** o login devolve um token cujo `sub` é o mesmo id que o
  investidor tinha antes da migração

#### AC-552 — Usuário bloqueado é avisado só quando acerta a senha

- **Dado** um usuário com situação `BLOQUEADO`
- **Quando** ele tenta fazer login com a senha correta e, depois, com uma
  senha errada
- **Então** com a senha correta a API responde 403 com `AUT-009` e a
  mensagem "Conta bloqueada. Procure o suporte."; com a senha errada,
  responde 401 com `AUT-004` e a mensagem genérica de e-mail ou senha
  inválidos, sem revelar o bloqueio

### US-448 — Autocadastro cria acesso e perfil juntos

Como pessoa se cadastrando, quero que minha conta de acesso e meu perfil de
investidor nasçam juntos, para nunca ficar com um login sem perfil (ou o
contrário).

#### AC-553 — Autocadastro cria usuário investidor e perfil com o mesmo id

- **Dado** um e-mail e um CPF ainda não usados
- **Quando** a pessoa faz o autocadastro
- **Então** existem um `usuario` com papel `INVESTIDOR` e situação `ATIVO` e
  um `investidor` com o mesmo id, e a senha guardada é um hash (não o texto)

#### AC-554 — Falha ao gravar o perfil não deixa usuário solto

- **Dado** um autocadastro em que a gravação do `investidor` falha
- **Quando** o cadastro é processado
- **Então** nenhum `usuario` novo fica gravado

### US-449 — Conta excluída libera e-mail e CPF

Como dono do produto, quero que a regra de reuso de e-mail e CPF de conta
excluída (SPEC-10) continue valendo com o e-mail morando em `usuario`.

#### AC-555 — Excluir a conta encerra o acesso

- **Dado** um investidor ativo
- **Quando** ele exclui a própria conta
- **Então** o `usuario` dele fica com situação `EXCLUIDO`, o `investidor`
  fica inativo e o login com o mesmo e-mail e senha responde 401 `AUT-004`

#### AC-556 — O banco só aceita e-mail repetido de conta excluída

- **Dado** um banco Postgres migrado até a V17 com um usuário `ATIVO`
- **Quando** se grava outro usuário com o mesmo e-mail, primeiro `ATIVO` e
  depois com o primeiro marcado como `EXCLUIDO`
- **Então** a primeira gravação é recusada pelo índice único e a segunda é
  aceita

### US-450 — Administrador nunca aparece como investidor

Como dono do produto, quero que administradores existam só como usuário,
para que nenhuma consulta de investidor os devolva.

#### AC-557 — Consulta de investidores não traz administrador

- **Dado** um `usuario` com papel `ADMIN` (sem perfil de investidor) e um
  investidor cadastrado
- **Quando** os investidores são consultados
- **Então** só o investidor aparece; o id do administrador não está no
  resultado

## Fora de escopo

- Papel real no token e matriz de acesso (INVESTIDOR, ADMIN) — issue #19. Até
  lá o token continua com `ROLE_INVESTIDOR` fixo (ASM-461).
- `GET /auth/me` — issue #20.
- Criar administrador, bloquear e desbloquear — issue #21. Esta spec só cria
  a coluna e faz o login respeitar `BLOQUEADO`.
- Papel `SUPORTE` — entra quando for especificado (ADR-013).

## Suposições

| ID | Suposição | Status | Resolução |
|---|---|---|---|
| ASM-466 | Até a issue #19, o login só emite token para papel `INVESTIDOR`; um `ADMIN` recebe 401 `AUT-004`. Sem isso, um administrador entraria com `ROLE_INVESTIDOR` fixo e sem perfil. Nenhum `ADMIN` existe antes da #21, então o efeito prático é nulo. | aberta | — |
| ASM-467 | O papel é guardado como texto (`INVESTIDOR`, `ADMIN`); `SUPORTE` entra no enum quando for especificado. | aberta | — |
| ASM-468 | A resposta do investidor (`InvestidorResponseDTO`) continua trazendo o e-mail, agora lido de `usuario`. Nenhum contrato da API muda. | aberta | — |
| ASM-469 | Na migração, investidor com `ativo = false` vira usuário `EXCLUIDO`; os demais viram `ATIVO`. A sequência de `usuario.id` começa depois do maior id migrado. | aberta | — |
| ASM-470 | O índice parcial de e-mail sai de `investidor` (V16) e passa para `usuario`; o de CPF fica em `investidor`, como está. | aberta | — |
| ASM-471 | Testcontainers precisa de Docker na máquina local e no CI; o runner `ubuntu-latest` do GitHub já tem. Sem Docker local, os testes da V17 falham em vez de pular. | aberta | — |

## Perguntas em aberto

Nenhuma. As quatro decisões que a issue não fechava foram respondidas
(decisões 1 a 5 do Contexto).

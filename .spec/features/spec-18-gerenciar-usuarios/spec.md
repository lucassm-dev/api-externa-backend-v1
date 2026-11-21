# Spec: ADMIN gerencia usuários — listar, criar ADMIN, bloquear e desbloquear

> feature: spec-18-gerenciar-usuarios
> status: rascunho

## Contexto

Issue #21, direção no ADR-013. A spec-16 criou o papel ADMIN, mas ele ainda
não tem como gerenciar quem usa o sistema. Hoje o bloqueado só é barrado no
login (spec-15) e um token já emitido continua valendo até expirar (ASM-477).

Decisões de projeto (respondidas pelo dono do produto em 2026-11-21):

1. **O motivo do bloqueio fica no próprio usuário**: colunas
   `motivo_bloqueio` e `bloqueado_em` em `usuario`, limpas ao desbloquear. O
   histórico de quem bloqueou quem fica com a #22.
2. **Listagem paginada sem excluídos**: `GET /admin/usuarios?situacao=`
   (`ATIVO` ou `BLOQUEADO`, opcional). Traz investidores e ADMINs.
3. **Um ADMIN bloqueia qualquer usuário, menos a si mesmo**, inclusive outro
   ADMIN.
4. **Bloqueado com token válido recebe 401 `AUT-010`** ("Conta bloqueada"),
   código distinto do token inválido.

### Rotas (todas só ADMIN)

| Operação | Rota |
|---|---|
| Listar | `GET /admin/usuarios?situacao=&page=&size=` |
| Criar ADMIN | `POST /admin/usuarios` `{email, senha}` |
| Bloquear | `PATCH /admin/usuarios/{id}/bloquear` `{motivo}` |
| Desbloquear | `PATCH /admin/usuarios/{id}/desbloquear` |

## Histórias

### US-458 — ADMIN enxerga quem usa o sistema

Como ADMIN, quero listar os usuários com indicadores de uso, para decidir
sobre bloqueios sem ver o conteúdo financeiro de ninguém.

#### AC-576 — Lista usuários com indicadores e filtro por situação

- **Dado** um investidor ativo com 2 carteiras ativas e posição aberta, um
  investidor bloqueado sem carteira, um ADMIN e um usuário excluído
- **Quando** o ADMIN lista sem filtro e depois com `situacao=BLOQUEADO`
- **Então** sem filtro vêm os três não excluídos, cada um com id, e-mail,
  nome (só investidor), papel, situação, motivo do bloqueio,
  `carteirasAtivas` e `temPosicaoAberta` (2 e `true` no primeiro); com o
  filtro vem só o bloqueado

#### AC-577 — Listagem não mostra conteúdo financeiro nem senha

- **Dado** um investidor com carteira, ações e operações
- **Quando** o ADMIN lista os usuários
- **Então** a resposta não contém ticker, quantidade, preço, valor, operação
  nem o hash da senha (a varredura do JSON não encontra nenhum desses campos
  e nenhum valor `$2`)

#### AC-578 — Só o ADMIN gerencia usuários

- **Dado** um token de investidor, um de ADMIN e nenhum token
- **Quando** cada um chama as quatro rotas de `/admin/usuarios`
- **Então** o ADMIN é atendido; o investidor recebe 403 `AUT-007`; sem token
  recebe 401 `AUT-005`

### US-459 — ADMIN cria outro ADMIN

Como ADMIN, quero criar outro ADMIN, para não depender de uma única conta.

#### AC-579 — ADMIN criado consegue entrar

- **Dado** um ADMIN logado
- **Quando** ele cria um ADMIN com e-mail novo e senha válida
- **Então** recebe 201 com id, e-mail, papel `ADMIN` e situação `ATIVO`, sem
  a senha; o novo ADMIN faz login e o token traz `ROLE_ADMIN`

#### AC-580 — Criação recusa e-mail em uso e senha fraca

- **Dado** um e-mail já usado por um usuário não excluído
- **Quando** o ADMIN cria um ADMIN com esse e-mail, e depois com e-mail novo
  e senha `abc`
- **Então** o primeiro recebe 409 `AUT-001` e o segundo 422 `AUT-008`; nenhum
  usuário é criado

### US-460 — ADMIN bloqueia e desbloqueia

Como ADMIN, quero bloquear e desbloquear usuários com efeito imediato, para
cortar o acesso de quem abusa do sistema.

#### AC-581 — Bloqueio grava situação e motivo, mesmo com carteira ativa

- **Dado** um investidor com carteira ativa
- **Quando** o ADMIN o bloqueia com o motivo "uso abusivo"
- **Então** recebe 200 com situação `BLOQUEADO` e o motivo; a carteira
  continua como estava

#### AC-582 — Bloqueio sem motivo é recusado

- **Dado** um investidor ativo
- **Quando** o ADMIN tenta bloqueá-lo sem motivo ou com motivo em branco
- **Então** recebe 400 de validação e o investidor continua `ATIVO`

#### AC-583 — ADMIN não bloqueia a si mesmo

- **Dado** um ADMIN logado
- **Quando** ele tenta bloquear o próprio id
- **Então** recebe 422 `AUT-011` e continua `ATIVO`

#### AC-584 — Bloqueado não entra; desbloqueado volta a entrar

- **Dado** um investidor bloqueado
- **Quando** ele tenta login, o ADMIN o desbloqueia e ele tenta de novo
- **Então** o primeiro login recebe 403 `AUT-009`; o desbloqueio responde 200
  com situação `ATIVO` e motivo vazio; o segundo login recebe token

#### AC-585 — Bloqueio vale na requisição seguinte

- **Dado** um investidor logado, com token ainda válido
- **Quando** o ADMIN o bloqueia e ele faz uma nova requisição com o mesmo
  token
- **Então** recebe 401 `AUT-010`; depois de desbloqueado, o mesmo token volta
  a ser aceito

#### AC-586 — Usuário inexistente ou excluído não é encontrado

- **Dado** um id que não existe e o id de um usuário excluído
- **Quando** o ADMIN tenta bloquear ou desbloquear cada um
- **Então** recebe 404 `AUT-012` nos dois casos

## Fora de escopo

- Registro de quem bloqueou/desbloqueou (#22).
- Papel SUPORTE e criação de SUPORTE (ADR futuro).
- Excluir usuário pelo ADMIN, trocar papel, editar e-mail ou senha de outro.
- Encerrar sessões de outra forma que não a checagem por requisição.

## Suposições

| ID | Suposição | Status | Resolução |
|---|---|---|---|
| ASM-484 | A checagem por requisição é um `SELECT` de `situacao` por id no conversor do JWT; o custo de uma consulta por requisição é aceitável sem cache (cache atrasaria o bloqueio). | aberta | — |
| ASM-485 | Usuário excluído com token ainda válido também recebe 401 na checagem por requisição (`AUT-005`, como token inválido), já que a conta não existe mais para o sistema. | aberta | — |
| ASM-486 | Bloquear quem já está bloqueado atualiza o motivo e responde 200; desbloquear quem está ativo responde 200 sem mudança (idempotente). | aberta | — |
| ASM-487 | `temPosicaoAberta` = alguma carteira ativa do investidor com ação de quantidade > 0; para ADMIN os indicadores são 0 e `false`. | aberta | — |
| ASM-488 | O motivo tem no máximo 255 caracteres; acima disso é 400 de validação. | aberta | — |
| ASM-489 | A ordem da listagem é por id crescente, com o tamanho de página padrão das outras consultas. | aberta | — |

## Perguntas em aberto

Nenhuma. As decisões que a issue não fechava foram respondidas (decisões 1 a
4 do Contexto).

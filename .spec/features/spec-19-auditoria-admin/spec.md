# Spec: Registro das ações administrativas

> feature: spec-19-auditoria-admin
> status: rascunho

## Contexto

Issue #22, direção no ADR-013 (auditoria). A spec-18 deu ao ADMIN o bloquear e
desbloquear, e a spec-16 deixou com ele a exclusão de corretora, mas nada
registra quem fez o quê. Esta spec cria esse registro.

Decisões de projeto (respondidas pelo dono do produto em 2026-11-21):

1. **A #22 cobre bloqueio, desbloqueio e exclusão de corretora.** A
   descontinuação de corretora só nasce na #23, que grava o próprio registro
   usando o que esta spec deixa pronto (o tipo de ação já existe aqui).
2. **O motivo da exclusão de corretora fica vazio**: o `DELETE /corretoras/{id}`
   mantém o contrato sem corpo. Só o bloqueio tem motivo obrigatório.
3. **Sem rota de consulta nesta issue.** Os registros só são gravados; a
   consulta pode entrar com os relatórios da v0.7.

### O registro

Tabela `registro_administrativo`: `id`, `admin_id` (quem), `acao`
(`BLOQUEIO`, `DESBLOQUEIO`, `EXCLUSAO_CORRETORA`, `DESCONTINUACAO_CORRETORA`),
`alvo_tipo` (`USUARIO` ou `CORRETORA`), `alvo_id` (em quem ou em quê),
`motivo` (pode ser vazio) e `registrado_em` (quando).

## Histórias

### US-461 — Toda ação administrativa deixa rastro

Como dono do produto, quero que cada ação administrativa fique registrada,
para saber quem fez o quê, em quem, quando e por quê.

#### AC-587 — Bloqueio e desbloqueio geram um registro cada

- **Dado** um ADMIN e um investidor ativo
- **Quando** o ADMIN bloqueia o investidor com o motivo "uso abusivo" e
  depois o desbloqueia
- **Então** existem exatamente dois registros novos: `BLOQUEIO` com o id do
  ADMIN, alvo `USUARIO` com o id do investidor, motivo "uso abusivo" e data;
  e `DESBLOQUEIO` com o mesmo ADMIN e alvo, sem motivo

#### AC-588 — Exclusão de corretora gera um registro

- **Dado** um ADMIN e uma corretora sem carteira ativa
- **Quando** o ADMIN exclui a corretora
- **Então** existe exatamente um registro novo `EXCLUSAO_CORRETORA` com o id
  do ADMIN, alvo `CORRETORA` com o id da corretora, motivo vazio e data

### US-462 — O registro é confiável

Como dono do produto, quero que o registro reflita só o que aconteceu e não
possa ser mexido, para poder confiar nele.

#### AC-589 — Ação recusada não gera registro

- **Dado** um ADMIN, uma corretora com carteira ativa e um id de usuário
  inexistente
- **Quando** o ADMIN tenta bloquear a si mesmo, bloquear sem motivo,
  bloquear o usuário inexistente e excluir a corretora com carteira ativa
- **Então** cada tentativa é recusada e a quantidade de registros não muda

#### AC-590 — Falha ao gravar o registro desfaz a ação

- **Dado** um investidor ativo e a gravação do registro falhando
- **Quando** o ADMIN tenta bloqueá-lo
- **Então** a requisição falha e o investidor continua `ATIVO`, sem motivo de
  bloqueio

#### AC-591 — A API não altera nem exclui registros

- **Dado** o código da aplicação
- **Quando** se procura rota ou operação que altere ou exclua registro
- **Então** nenhuma rota usa o registro, e o repositório do registro só sabe
  gravar e ler (não herda `delete` nem `saveAll` do `JpaRepository`)

#### AC-592 — Registro não guarda senha nem token

- **Dado** a tabela e a entidade do registro
- **Quando** se inspecionam os campos
- **Então** nenhum campo se chama ou guarda senha, hash ou token

## Fora de escopo

- Rota de consulta dos registros (v0.7).
- Gravar o registro da descontinuação (#23).
- Registrar a criação de ADMIN, o cadastro de corretora ou ações do
  investidor.

## Suposições

| ID | Suposição | Status | Resolução |
|---|---|---|---|
| ASM-490 | Bloquear quem já está bloqueado (ASM-486) e desbloquear quem já está ativo também geram registro: a ação foi pedida e concluída. | aberta | — |
| ASM-491 | O registro guarda só ids (do ADMIN e do alvo), sem copiar e-mail ou nome; se o alvo mudar depois, o registro aponta para o estado atual. | aberta | — |
| ASM-492 | `alvo_id` não tem chave estrangeira, porque aponta para tabelas diferentes conforme `alvo_tipo`; `admin_id` tem chave estrangeira para `usuario`. | aberta | — |
| ASM-493 | Um registro gravado não muda nunca: a entidade é marcada como imutável (`@Immutable`), além de não ter rota. Corrigir um registro errado exige acesso direto ao banco. | aberta | — |

## Perguntas em aberto

Nenhuma. As decisões que a issue não fechava foram respondidas (decisões 1 a
3 do Contexto).

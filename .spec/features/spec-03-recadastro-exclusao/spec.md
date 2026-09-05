# Spec: SPEC-03 — Recadastro após exclusão e bloqueio com vínculo ativo

> feature: spec-03-recadastro-exclusao
> status: em-andamento

## Contexto

**Bug relatado pelo Lucas:** cadastrou PETR4, excluiu, tentou cadastrar PETR4
de novo e o sistema recusou.

**Causa raiz confirmada (investigada nesta spec):** duas camadas do mesmo
problema.
1. **Camada de aplicação:** `AcaoService.cadastrar` chama
   `repository.existsByTicker(ticker)` — sem filtrar `ativo=true`. Uma ação
   excluída logicamente (`ativo=false`) continua "existindo" pra essa
   checagem, então o cadastro é recusado como duplicado. O mesmo padrão
   existe em `CorretoraService.cadastrar` (`existsByCnpj`, sem filtro).
2. **Camada de banco:** mesmo corrigindo a checagem da aplicação, a migration
   `V4__create_acao.sql` criou `CONSTRAINT uk_acao_ticker UNIQUE (ticker)` —
   uma constraint **incondicional**. O `INSERT` da ação recadastrada
   colidiria com a linha antiga (inativa) e falharia no banco, mesmo que a
   aplicação achasse que estava livre pra cadastrar. O mesmo vale para
   `uk_corretora_cnpj` em `corretora`.

**Correção, portanto, tem duas partes**: checagem de duplicidade passa a
filtrar `ativo=true` (services) **e** a constraint do banco precisa virar
um índice único parcial (`WHERE ativo = true`), não mais incondicional —
ver `design.md`.

Esta spec também fecha três pendências já registradas no mapeamento do
Bloco A e nas decisões de `docs/DECISOES-MAPEAMENTO.md`:
- **Q-MAP-07** (recadastro) — Ação e Corretora, cobertos acima. Carteira
  **não tem** esse bug: não existe constraint única em `nome` (RN-CAR-05
  já confirmava isso), então não há nada a corrigir nela para recadastro.
- **Q-MAP-08** (vazamento de ação excluída) — `GET /acoes/ticker/{ticker}`
  hoje retorna ação inativa com dados completos, e ela continua podendo ser
  comprada/vendida.
- **Q-MAP-01** (bloqueio de exclusão com vínculo ativo) — hoje nenhuma
  exclusão (ação, corretora, carteira) verifica se há posição/vínculo ativo.

`refactor-cnpj-delete` (spec anterior, que introduziu os `DELETE` lógicos)
tinha a pergunta **Q-404** em aberto: "as listagens já filtram por
`ativo=true`?". Resposta, confirmada nesta investigação: as listagens
(`GET /acoes`, `GET /corretoras`) **já filtram** corretamente
(`findAllByAtivoTrue`); o gap era só na **busca individual por ticker**.

## Histórias

### US-415 — Recadastro de ação e corretora após exclusão

Como operador do sistema, quero recadastrar um ticker/CNPJ que foi excluído
anteriormente, para não ficar travado por um registro que já não está ativo.

#### AC-444 — Recadastrar ticker de ação excluída funciona

- **Dado** que cadastrei PETR4, exclui, e nenhuma outra PETR4 ativa existe
- **Quando** cadastro PETR4 novamente
- **Então** o cadastro é aceito (201), criando uma **nova linha** — a antiga
  continua no banco, inativa

#### AC-445 — Recadastrar CNPJ de corretora excluída funciona

- **Dado** que cadastrei uma corretora, exclui, e nenhuma outra ativa com o
  mesmo CNPJ existe
- **Quando** cadastro o mesmo CNPJ novamente
- **Então** o cadastro é aceito (201), criando uma nova linha

#### AC-446 — Ticker/CNPJ duplicado entre registros ATIVOS continua bloqueado

- **Dado** uma ação (ou corretora) já ativa com um ticker (ou CNPJ)
- **Quando** tento cadastrar outra com o mesmo identificador, também ativa
- **Então** o cadastro é recusado (409), como já era antes

#### AC-447 — Histórico de operações do ticker antigo não é corrompido nem ressuscitado

- **Dado** um ticker excluído com operações antigas vinculadas, e um novo
  cadastro do mesmo ticker
- **Quando** consulto o histórico de operações
- **Então** as operações antigas continuam apontando para a ação antiga
  (inativa, outro id) — nunca para a ação recadastrada; a ação recadastrada
  começa sem posição nem histórico

### US-416 — Ação excluída não é visível nem operável

Como investidor, quero que uma ação excluída não apareça na busca individual
nem possa ser comprada/vendida, para não operar em cima de um catálogo que
já não existe mais.

#### AC-448 — Busca individual de ação inativa retorna 404

- **Dado** um ticker excluído (`ativo=false`)
- **Quando** chamo `GET /acoes/ticker/{ticker}`
- **Então** a API responde 404 (`ACA-001`), igual a um ticker que nunca existiu

#### AC-449 — Comprar/vender ação inativa é bloqueado

- **Dado** um ticker excluído
- **Quando** tento comprar ou vender essa ação
- **Então** a operação é recusada com erro padronizado (`ACA-001`, 404) —
  não usa a cotação nem cria operação

### US-417 — Bloqueio de exclusão com vínculo ativo (RN-Q-MAP-01)

Como operador do sistema, quero que o sistema impeça excluir ação, corretora
ou carteira que ainda tenha posição/vínculo ativo, para não deixar dado
órfão ou inconsistente.

#### AC-450 — Excluir ação com posição ativa é bloqueado

- **Dado** uma ação com quantidade > 0 em pelo menos uma carteira
- **Quando** tento excluí-la
- **Então** a exclusão é recusada (409), com mensagem informando quantas
  carteiras têm posição ativa nela

#### AC-451 — Excluir ação com posições zeradas é permitido

- **Dado** uma ação cujas posições estão todas zeradas (mesmo com histórico
  de operações)
- **Quando** tento excluí-la
- **Então** a exclusão é aceita (204)

#### AC-452 — Excluir carteira com posição ativa é bloqueado

- **Dado** uma carteira com quantidade > 0 em pelo menos uma ação
- **Quando** tento excluí-la
- **Então** a exclusão é recusada (409), com mensagem informando quantas
  posições ativas ela tem

#### AC-453 — Excluir carteira com posições zeradas é permitido

- **Dado** uma carteira sem nenhuma posição com quantidade > 0
- **Quando** tento excluí-la
- **Então** a exclusão é aceita (204)

#### AC-454 — Excluir corretora com carteira ativa vinculada é bloqueado

- **Dado** uma corretora com pelo menos uma carteira ativa apontando pra ela
- **Quando** tento excluí-la
- **Então** a exclusão é recusada (409), com mensagem informando quantas
  carteiras ativas dependem dela

#### AC-455 — Excluir corretora sem carteira ativa vinculada é permitido

- **Dado** uma corretora sem nenhuma carteira ativa (todas excluídas, ou
  nenhuma nunca existiu)
- **Quando** tento excluí-la
- **Então** a exclusão é aceita (204)

## Fora de escopo

- Reativar um registro excluído (recadastrar cria linha nova, não reativa a
  antiga) — decisão já registrada em `docs/DECISOES-MAPEAMENTO.md` (Q-MAP-07:
  "toda verificação de unicidade passa a considerar apenas registros ativos",
  não fala em reativação
- Exclusão de Carteira por falta de constraint única em nome — não há bug de
  recadastro aqui (RN-CAR-05 já cobria isso), só o bloqueio por posição ativa
  (US-417)
- Lucro realizado, cache de cotação, conversão de câmbio — outras specs

## Suposições

| ID | Suposição | Status | Resolução |
|---|---|---|---|
| ASM-414 | Para Corretora, o "vínculo ativo" que bloqueia exclusão é **carteira ativa vinculada** (existência), não "posição com quantidade > 0" — corretora não guarda posição diretamente, quem guarda é a carteira. Uma carteira ativa mas com posições zeradas ainda bloqueia a corretora | confirmada | Interpretação mais consistente com o bug original (RN-COR-06: "excluí corretora com carteira ativa vinculada e nada impediu") — o vínculo que importa pra corretora é a carteira existir e estar ativa, não o que ela contém |
| ASM-415 | O índice único parcial (`WHERE ativo = true`) só é aplicado via migration Flyway (perfil `dev`/produção, Postgres). No perfil de teste (H2, `ddl-auto=create-drop`, Flyway desligado), a unicidade só é garantida pela checagem da aplicação — os testes automatizados não exercitam a constraint de banco em si | confirmada | H2 nunca roda as migrations Flyway neste projeto (já era assim antes desta spec); testar a constraint de banco exigiria um teste de integração à parte contra Postgres real, fora do escopo dos testes JUnit atuais |

## Perguntas em aberto

Nenhuma — as decisões de produto relevantes (soft delete + filtro por
ativo, sem reativação) já estavam fechadas em `docs/DECISOES-MAPEAMENTO.md`
antes desta spec.
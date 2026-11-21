# Spec: Papéis INVESTIDOR e ADMIN com matriz de acesso

> feature: spec-16-papeis-matriz-acesso
> status: rascunho

## Contexto

Issue #19. Todo usuário autenticado recebe o mesmo papel (`ROLE_INVESTIDOR`
fixo no token, ASM-461) e pode fazer tudo, inclusive mexer no catálogo de
corretoras e de ações. A direção foi decidida no ADR-013: papéis com matriz
de acesso e menor privilégio. A spec-15 já separou `usuario` (com `papel`) do
perfil de investidor.

Decisões de projeto (respondidas pelo dono do produto em 2026-11-21):

1. **O token leva o papel real** do usuário (`ROLE_INVESTIDOR` ou
   `ROLE_ADMIN`), e o ADMIN passa a fazer login. Isso encerra a ASM-466 da
   spec-15.
2. **Autorização por método** (`@EnableMethodSecurity` + `@PreAuthorize` nos
   resources), conforme a matriz abaixo. Sem token continua 401 antes de
   qualquer regra de papel; papel errado responde 403 `AUT-007` (handler que
   já existe).
3. **Rotas de ação ficam como estão até a issue #24**, liberadas para os dois
   papéis (pesquisar, atualizar cotação, cadastrar e excluir). A #24 faz a
   pesquisa cadastrar sozinha o ticker que não está no catálogo e remove
   `POST /acoes` e `DELETE /acoes/{ticker}`; atualizar cotação continua do
   investidor. Revisão da decisão original (só ADMIN alteraria ações), feita
   pelo dono do produto em 2026-11-21 no gate de impacto: fechar o cadastro
   antes da #24 deixaria o investidor sem como comprar ticker novo.
4. **Bloqueio com efeito imediato fica para a issue #21**, junto com o
   bloquear/desbloquear. Até lá, bloqueado só é barrado no login (spec-15).
5. **Primeiro ADMIN vem de variáveis de ambiente**, criado na inicialização só
   se ainda não existir nenhum ADMIN. Sem as variáveis, a aplicação sobe sem
   ADMIN e loga um aviso. Se o e-mail já for de um investidor não excluído,
   não cria nem promove ninguém e loga um aviso.

### Matriz de acesso

| Operação | Rotas | INVESTIDOR | ADMIN |
|---|---|:-:|:-:|
| Consultar corretoras | `GET /corretoras`, `GET /corretoras/{id}`, `GET /corretoras/cnpj/{cnpj}` | ✅ | ✅ |
| Cadastrar e excluir corretora | `POST /corretoras`, `DELETE /corretoras/{id}` | ❌ | ✅ |
| Pesquisar ação | `GET /acoes`, `GET /acoes/ticker/{ticker}`, `GET /mercado/barra-cotacoes` | ✅ | ✅ |
| Atualizar cotação (a ação entra pela pesquisa desde a #24, spec-21) | `PUT /acoes/{id}/atualizar-cotacao` | ✅ | ✅ |
| Dados do usuário logado | `GET /auth/me` (spec-17) | ✅ | ✅ |
| Carteiras, operações e perfil próprios | `/carteiras/**`, `/operacoes/**`, `/investidores/**` | ✅ | ❌ |
| Gerenciar usuários | (nasce na #21) | ❌ | ✅ |

## Histórias

### US-451 — O token diz quem é quem

Como dono do produto, quero que o token carregue o papel real do usuário,
para que a API saiba o que cada um pode fazer.

#### AC-558 — Token leva o papel do usuário, e o ADMIN consegue entrar

- **Dado** um investidor e um ADMIN ativos, com a senha correta
- **Quando** cada um faz login
- **Então** os dois recebem token; o do investidor traz `authorities`
  `["ROLE_INVESTIDOR"]` e o do ADMIN traz `["ROLE_ADMIN"]`

### US-452 — Catálogo de corretoras só o ADMIN altera

Como dono do produto, quero que só o ADMIN altere o catálogo de corretoras,
para que um investidor não mude dado que todos usam. O catálogo de ações
continua aberto aos dois papéis até a #24 mudar como a ação entra nele.

#### AC-559 — Os dois papéis consultam corretoras

- **Dado** um token de investidor, um de ADMIN e nenhum token
- **Quando** cada um chama as rotas de consulta de corretoras
- **Então** investidor e ADMIN recebem 200; sem token recebe 401 `AUT-005`

#### AC-560 — Só o ADMIN cadastra e exclui corretora

- **Dado** um token de investidor, um de ADMIN e nenhum token
- **Quando** cada um chama `POST /corretoras` e `DELETE /corretoras/{id}`
- **Então** o ADMIN é atendido (2xx); o investidor recebe 403 `AUT-007`;
  sem token recebe 401 `AUT-005`

#### AC-561 — Os dois papéis pesquisam ações

- **Dado** um token de investidor, um de ADMIN e nenhum token
- **Quando** cada um chama as rotas de pesquisa de ação e a barra de
  cotações
- **Então** investidor e ADMIN recebem 200; sem token recebe 401 `AUT-005`

#### AC-562 — Os dois papéis atualizam cotação de ação

<!-- spec-21 (#24): POST /acoes e DELETE /acoes/{ticker} foram removidos; a ação entra pela pesquisa -->

- **Dado** um token de investidor, um de ADMIN e nenhum token
- **Quando** cada um chama `PUT /acoes/{id}/atualizar-cotacao`
- **Então** investidor e ADMIN são atendidos (nunca 403); sem token recebe
  401 `AUT-005`

### US-453 — Dado do investidor é só do investidor

Como investidor, quero que só eu acesse minhas carteiras, operações e
perfil, e que o ADMIN não veja o conteúdo delas (ADR-013, menor privilégio).

#### AC-563 — ADMIN não acessa carteiras, operações nem perfil de investidor

- **Dado** um token de investidor, um de ADMIN e nenhum token
- **Quando** cada um chama as rotas de `/carteiras`, `/operacoes` e
  `/investidores`
- **Então** o investidor recebe 200; o ADMIN recebe 403 `AUT-007`; sem token
  recebe 401 `AUT-005`

### US-454 — O primeiro ADMIN vem do ambiente

Como pessoa que opera o deploy, quero criar o primeiro ADMIN pelas variáveis
de ambiente, para não precisar de rota pública nem de senha no código.

#### AC-564 — Inicialização cria o ADMIN uma única vez

- **Dado** `ADMIN_EMAIL` e `ADMIN_SENHA` definidos e nenhum ADMIN no banco
- **Quando** a aplicação inicializa duas vezes
- **Então** existe exatamente um `usuario` ADMIN `ATIVO` com esse e-mail, e a
  senha guardada é um hash que confere com `ADMIN_SENHA`

#### AC-565 — A senha do ADMIN não aparece em log nem no código

- **Dado** `ADMIN_SENHA` definida
- **Quando** a aplicação inicializa e cria o ADMIN
- **Então** o texto da senha não aparece em nenhuma linha de log, e nenhum
  arquivo de `src/main` contém um valor padrão para ela

#### AC-566 — Sem as variáveis, sobe sem ADMIN e avisa

- **Dado** `ADMIN_EMAIL` ou `ADMIN_SENHA` ausentes
- **Quando** a aplicação inicializa
- **Então** ela sobe normalmente, nenhum ADMIN é criado e o log traz um aviso
  dizendo que nenhum ADMIN foi criado

#### AC-567 — E-mail de investidor não vira ADMIN

- **Dado** `ADMIN_EMAIL` igual ao e-mail de um investidor não excluído
- **Quando** a aplicação inicializa
- **Então** nenhum ADMIN é criado, o papel do investidor continua
  `INVESTIDOR` e o log traz um aviso para trocar o e-mail

### US-455 — Investidor nunca é promovido

Como dono do produto, quero que não exista caminho na API para trocar o
papel de um investidor, para que ninguém se promova a ADMIN.

#### AC-568 — Autocadastro ignora papel enviado e nenhuma entrada da API aceita papel

- **Dado** um autocadastro com `"papel": "ADMIN"` no corpo
- **Quando** o cadastro é feito
- **Então** o usuário criado tem papel `INVESTIDOR`, e nenhum DTO de entrada
  da API tem campo de papel

## Fora de escopo

- Rotas de gerenciar usuários (listar, criar ADMIN/SUPORTE, bloquear,
  desbloquear) e os testes 200/401/403 delas — issue #21.
- Bloqueio com efeito imediato em toda requisição — issue #21.
- `GET /auth/me` — issue #20.
- Descontinuar corretora — issue #23.
- Ação entrar no catálogo pela pesquisa e remoção de `POST`/`DELETE /acoes` —
  issue #24.
- Papel `SUPORTE` — ADR próprio, depois.

## Suposições

| ID | Suposição | Status | Resolução |
|---|---|---|---|
| ASM-472 | As variáveis são `ADMIN_EMAIL` e `ADMIN_SENHA`, lidas pelas propriedades `admin.email` e `admin.senha` sem valor padrão. | aberta | — |
| ASM-473 | A senha do ADMIN das variáveis segue a mesma política do autocadastro (8+, letra e número); fora dela, o ADMIN não é criado e o log avisa (sem mostrar a senha). | aberta | — |
| ASM-474 | "Já existe ADMIN" é qualquer `usuario` com papel `ADMIN` não excluído; com um ADMIN existente, as variáveis são ignoradas (trocar a senha por elas não é suportado). | aberta | — |
| ASM-475 | O principal continua `InvestidorPrincipal(id)` também para o ADMIN; o nome fica impreciso, mas nenhum resource de ADMIN usa o id ainda. Renomear entra quando a #21 precisar. | aberta | — |
| ASM-476 | Com o catálogo de ações só para ADMIN, o investidor não consegue comprar ticker que ainda não está no catálogo até um ADMIN cadastrar ou até a #24 (ação entra pela pesquisa). | invalidada | Dono do produto, 2026-11-21: ações ficam abertas aos dois papéis até a #24 (decisão 3) |
| ASM-477 | Até a #21, um usuário bloqueado com token já emitido continua acessando até o token expirar (no máximo 60 minutos). | invalidada | spec-18 (#21), AC-585: bloqueio vale na requisição seguinte |
| ASM-478 | Tokens emitidos antes do deploy trazem `ROLE_INVESTIDOR` e continuam válidos até expirar; como só investidores tinham login, o papel deles está correto. | aberta | — |

## Perguntas em aberto

Nenhuma. As decisões que a issue não fechava foram respondidas (decisões 1 a
5 do Contexto).

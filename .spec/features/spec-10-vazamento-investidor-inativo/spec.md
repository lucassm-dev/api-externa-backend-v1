# Spec: Investidor excluído vazando por login, busca e unicidade

> feature: spec-10-vazamento-investidor-inativo
> status: rascunho

## Contexto

O levantamento de filtros do sistema (07/09/2026) mostrou que o Investidor
ficou de fora da correção que a SPEC-03 aplicou a Ação, Corretora e Carteira
(Q-MAP-07/Q-MAP-08): a exclusão dele é lógica (`ativo=false`), mas quase
nenhuma consulta filtra por `ativo`. Na prática, um investidor "excluído"
continua conseguindo entrar no sistema e continua visível — e o e-mail/CPF
dele fica bloqueado para sempre, ao contrário de ticker e CNPJ, que podem ser
reaproveitados.

Junto disso, sobrou um método morto (`AcaoRepository.findByTicker`, sem
filtro de `ativo`) que é exatamente a armadilha que causou o bug 500 em
`GET /corretoras/cnpj/{cnpj}` corrigido em 06/09/2026: consulta que devolve
`Optional` sobre uma coluna que pode ter linha ativa **e** inativa com o
mesmo valor explode com `IncorrectResultSizeDataAccessException`.

As quatro correções andam juntas de propósito: permitir reaproveitar e-mail
(US-434) **exige** que login e unicidade filtrem por ativo (US-432/US-434),
senão a própria consulta de login passa a encontrar duas linhas e quebra com
o mesmo 500.

## Histórias

### US-432 — Investidor excluído não entra mais no sistema

Como dono do produto, quero que um investidor excluído não consiga mais fazer
login, para que "excluir" signifique de fato perder o acesso.

#### AC-503 — Login de investidor inativo é recusado

- **Dado** que um investidor foi excluído (`ativo=false`)
- **Quando** ele tenta fazer login com e-mail e senha corretos
- **Então** o login é recusado com 401 (`AUT-004`), com a mesma mensagem
  genérica de credencial inválida — não revela que a conta existe e foi
  excluída

### US-433 — Investidor excluído some das consultas

Como dono do produto, quero que um investidor excluído suma também da busca
individual, e não só da listagem, para não vazar dado de quem já saiu.

#### AC-504 — Buscar investidor excluído por id retorna 404

- **Dado** que um investidor foi excluído
- **Quando** consulto `GET /investidores/{id}` com o id dele
- **Então** recebo 404 (`AUT-003`), igual a um id que nunca existiu

### US-434 — E-mail e CPF liberados após a exclusão

Como investidor que saiu e quer voltar, quero poder me cadastrar de novo com
o mesmo e-mail e CPF, para não ficar bloqueado para sempre por uma conta que
já foi excluída — mesmo tratamento que ticker e CNPJ já têm.

#### AC-505 — Recadastro com e-mail de investidor excluído é permitido

- **Dado** que um investidor com e-mail X foi excluído
- **Quando** um novo cadastro é feito com o mesmo e-mail X
- **Então** o cadastro é aceito (201) e cria um registro novo, sem herdar
  nada do antigo

#### AC-506 — Recadastro com CPF de investidor excluído é permitido

- **Dado** que um investidor com CPF Y foi excluído
- **Quando** um novo cadastro é feito com o mesmo CPF Y
- **Então** o cadastro é aceito (201)

#### AC-507 — Duplicidade continua bloqueada entre investidores ativos

- **Dado** que existe um investidor **ativo** com e-mail X (ou CPF Y)
- **Quando** tento cadastrar outro com o mesmo e-mail (ou CPF)
- **Então** recebo 409 (`AUT-001`/`AUT-002`), como antes

#### AC-508 — Login continua funcionando com e-mail reaproveitado

- **Dado** que o e-mail X pertence a um investidor excluído **e** a um
  investidor ativo criado depois
- **Quando** faço login com e-mail X e a senha do investidor ativo
- **Então** o login é aceito e devolve o token do investidor **ativo** — a
  consulta não quebra por encontrar duas linhas

### US-435 — Consulta sem filtro de ativo não fica de armadilha no código

Como pessoa desenvolvedora, quero que não exista consulta por chave natural
sem filtro de `ativo`, para ninguém cair de novo no 500 que já aconteceu com
o CNPJ.

#### AC-509 — Não existe mais consulta por ticker sem filtro de ativo

- **Dado** o repositório de Ação
- **Quando** procuro um método de busca por ticker
- **Então** só existe a versão que filtra por `ativo=true` — a variante sem
  filtro (`findByTicker`), que não era usada por ninguém, foi removida

## Fora de escopo

- Reaproveitar histórico do investidor antigo no recadastro — o registro
  novo é independente, igual ao que já vale para ticker e CNPJ (Q-MAP-07).
- Excluir em cascata carteiras/operações do investidor excluído — a exclusão
  do investidor continua sendo só o flag, sem tocar no que está abaixo.
- Endpoint de reativação de investidor — não existe hoje e não entra aqui.

## Suposições

| ID | Suposição | Status | Resolução |
|---|---|---|---|
| ASM-439 | A unicidade de e-mail/CPF passa a valer só entre investidores ativos, com índice único parcial no banco (`WHERE ativo = true`), exatamente o padrão da migration `V12` para ticker e CNPJ. | aberta | — |
| ASM-440 | O `unique = true` das colunas `email` e `cpf` da entidade `Investidor` precisa sair, senão o Hibernate recria a constraint incondicional no schema de teste (H2, Flyway desligado) e o teste de recadastro falha — mesma pegadinha já enfrentada na SPEC-03. | aberta | — |
| ASM-441 | Login recusado por conta inativa devolve a mesma mensagem genérica de credencial inválida (`AUT-004`), sem código próprio, para não revelar que a conta existe — coerente com a semântica de não-enumeração já adotada em CAR-001/OPE-001. | aberta | — |
| ASM-442 | Um investidor inativo que ainda tenha token JWT válido emitido antes da exclusão continua passando pelo filtro até o token expirar (24h) — revogação de token em curso está fora do escopo desta spec. | aberta | — |

## Perguntas em aberto

Nenhuma — as quatro lacunas foram levantadas e aprovadas para correção nesta
conversa.

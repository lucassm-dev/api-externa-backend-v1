# Spec: SPEC-04 — Preço sugerido pela cotação, mas editável

> feature: spec-04-preco-editavel
> status: em-andamento

## Contexto

Hoje `POST /operacoes/compra` e `POST /operacoes/venda` sempre usam a
cotação buscada no ato — não é possível informar um preço manualmente (por
exemplo, para registrar uma compra antiga, ou corrigir um preço que a fonte
externa não capturou direito). Esta spec torna o `precoUnitario` **opcional**:
se vier, usa o informado; se não vier, busca a cotação atual (comportamento
de hoje preservado).

Fecha também **Q-MAP-04** (já decidida em `docs/DECISOES-MAPEAMENTO.md`):
valida o essencial (preço > 0, escala decimal) e **avisa sem bloquear**
quando o preço destoa muito da cotação de mercado — e corrige o bug
**RN-OPE-05** do mapeamento do Bloco A: `PUT /operacoes/{id}` (editar) hoje
não tem **nenhuma** validação de preço, aceita zero, negativo ou qualquer
valor absurdo, corrompendo silenciosamente o preço médio da posição.

## Histórias

### US-418 — Preço editável na compra e na venda

Como investidor, quero poder informar o preço unitário da minha compra/venda
em vez de usar sempre a cotação do momento, para registrar operações
antigas ou corrigir um preço que eu sei que está certo.

#### AC-456 — Compra/venda sem preço informado usa a cotação atual

- **Dado** uma compra ou venda sem `precoUnitario` no corpo
- **Quando** a operação é registrada
- **Então** o preço usado é a cotação atual buscada na fonte — comportamento
  de hoje, preservado

#### AC-457 — Compra/venda com preço informado usa o valor informado

- **Dado** uma compra ou venda com `precoUnitario` no corpo
- **Quando** a operação é registrada
- **Então** o preço usado é exatamente o informado — a fonte de cotação
  ainda é consultada (para registrar a cotação de mercado do momento e
  calcular o aviso de desvio), mas não define o preço da operação

#### AC-458 — Preço zero ou negativo é rejeitado

- **Dado** um `precoUnitario` menor ou igual a zero
- **Quando** tento registrar a compra/venda
- **Então** o cadastro é recusado com 400 e código `VAL-001`, apontando o
  campo `precoUnitario`

#### AC-459 — Preço com mais de 2 casas decimais é rejeitado

- **Dado** um `precoUnitario` com mais de 2 casas decimais significativas
  (ex.: 47.273) — BRL e USD usam 2 casas decimais (centavos/cents)
- **Quando** tento registrar a compra/venda
- **Então** o cadastro é recusado com 422 e código `OPE-005`

#### AC-460 — Preço muito distante da cotação gera aviso, não bloqueia

- **Dado** um `precoUnitario` informado manualmente que é 10x ou mais
  acima/abaixo da cotação de mercado do momento
- **Quando** a operação é registrada
- **Então** ela é aceita normalmente, e a resposta traz um aviso em
  `avisos[]` explicando a diferença (ex.: "O preço informado (R$ 4.750,00)
  está 100x acima da cotação atual (R$ 47,50). Confirme se está correto.")
  — nunca um erro

#### AC-461 — Operação registra se o preço foi automático ou manual, e a cotação do momento

- **Dado** qualquer compra ou venda registrada
- **Quando** consulto essa operação depois
- **Então** ela guarda se o preço veio da fonte (`precoManual=false`) ou foi
  informado manualmente (`precoManual=true`), e qual era a cotação de
  mercado no momento (`cotacaoNoMomento`) — mesmo quando o preço é manual

#### AC-462 — Resposta da operação informa a moeda do preço unitário

- **Dado** qualquer operação (ação BR ou US)
- **Quando** consulto a resposta
- **Então** o campo `moeda` explicita se `precoUnitario` está em BRL ou USD
  (herdado da ação) — nunca ambíguo

#### AC-463 — Preço médio recalculado corretamente misturando operações automáticas e manuais

- **Dado** uma compra automática de 100 unidades a R$38,00 (cotação) seguida
  de uma compra manual de 100 unidades a R$42,00 (preço informado)
- **Quando** consulto a posição
- **Então** o preço médio é R$40,00 — o cálculo (`PosicaoService`) já trata
  `precoUnitario` de forma uniforme, automático ou manual, sem necessidade
  de mudança nele; este critério prova que a mistura continua correta

### US-419 — Preço editável também na edição de operação (corrige RN-OPE-05)

Como operador do sistema, quero que `PUT /operacoes/{id}` valide o preço do
mesmo jeito que a compra/venda, para não corromper silenciosamente o preço
médio da posição.

#### AC-464 — Editar operação com preço zero ou negativo é rejeitado

- **Dado** um `PUT /operacoes/{id}` com `precoUnitario` ≤ 0
- **Quando** a edição é processada
- **Então** é recusada com 400 (`VAL-001`) — hoje (bug RN-OPE-05) isso é
  aceito e corrompe o preço médio recalculado

#### AC-465 — Editar operação com preço válido recalcula a posição

- **Dado** um `PUT /operacoes/{id}` com um novo `precoUnitario` válido
- **Quando** a edição é processada
- **Então** a posição é recalculada com o novo preço, e a operação editada
  passa a registrar `precoManual=true` e a cotação de mercado do momento da
  edição (`cotacaoNoMomento`, lida de `Acao.cotacaoAtual`)

## Fora de escopo

- Editar quantidade com validação de teto/aviso de desvio — só o preço tem
  aviso de desvio; quantidade já tem validação própria (`@Min(1)`)
- Mudar o teto de 10x depois de observar uso real — fica como está até
  surgir motivo pra revisar
- Histórico de avisos (a resposta recalcula o aviso a partir dos campos
  salvos; não existe uma tabela de avisos)

## Suposições

| ID | Suposição | Status | Resolução |
|---|---|---|---|
| ASM-416 | "Escala decimal coerente com a moeda" = no máximo 2 casas decimais significativas, igual para BRL e USD (ambas têm 2 casas de subunidade — centavos/cents) | confirmada | Simplifica a regra: mesma validação pras duas moedas, sem ramificação por `Mercado` |
| ASM-417 | O teto de aviso (10x) já foi decidido em `docs/DECISOES-MAPEAMENTO.md` (Q-MAP-04) — não é reaberto aqui | confirmada | Decisão anterior do Lucas |
| ASM-418 | Mesmo com preço manual informado, a fonte de cotação é sempre consultada (para registrar `cotacaoNoMomento` e calcular o aviso) — isso consome cota da API externa em toda operação, não só nas automáticas | confirmada | Necessário pra rastreabilidade (AC-461) e pro aviso de desvio (AC-460); sem isso não dá pra comparar o preço manual com nada |
| ASM-419 | Em `PUT /operacoes/{id}`, `cotacaoNoMomento` é lida de `Acao.cotacaoAtual` (valor já armazenado, sem nova chamada à fonte) — diferente de compra/venda, que sempre busca uma cotação nova | confirmada | Editar uma operação não deveria gastar cota externa de novo; `Acao.cotacaoAtual` já existe e é atualizado por `PUT /acoes/{id}/atualizar-cotacao` |

## Perguntas em aberto

Nenhuma — a política de validação e o teto de aviso já estavam decididos em
`docs/DECISOES-MAPEAMENTO.md` (Q-MAP-04) antes desta spec.

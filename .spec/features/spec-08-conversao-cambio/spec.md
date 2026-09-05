# Spec: Conversão de câmbio USD → BRL e consolidação da carteira

> feature: spec-08-conversao-cambio
> status: rascunho

## Contexto

O sistema permite carteiras com ações BR e US misturadas (Q-MAP-09), mas não
existe nenhuma conversão de câmbio — os valores ficam soltos na moeda
original, sem consolidação possível em reais. Esta spec cobre Q-MAP-10
(câmbio histórico no valor investido, câmbio atual no valor de mercado) e
corrige um efeito colateral do modelo multi-moeda: o `lucro-realizado` da
SPEC-06 soma valores brutos que, numa carteira BRL+USD, misturaria reais com
dólares.

Fonte de câmbio (Bloco B, já aprovado): AwesomeAPI como fonte primária
(`economia.awesomeapi.com.br`, campo `ask`), PTAX do Banco Central (Olinda)
como fallback. Se ambas falharem: prossegue com a última taxa em cache e um
aviso (decisão confirmada nesta conversa — mesmo padrão da SPEC-07/Q-MAP-06);
recusa a operação só se nunca houve taxa salva. Decompor o lucro em efeito
preço vs. efeito câmbio (item "desejável" do Q-MAP-10) fica **fora desta
spec** — decisão confirmada nesta conversa.

## Histórias

### US-426 — Câmbio histórico gravado por operação em USD

Como investidor, quero que toda compra/venda de ativo em USD grave a taxa de
câmbio do momento, para que o valor investido em reais reflita o câmbio real
de cada operação.

#### AC-486 — Operação em USD grava a taxa de câmbio do momento

- **Dado** que compro ou vendo uma ação em USD
- **Quando** a operação é registrada
- **Então** ela grava `taxaCambioNaOperacao` e `dataHoraTaxaCambio` com a
  cotação USD-BRL do momento

#### AC-487 — Operação em BRL usa câmbio 1 (cálculo uniforme)

- **Dado** que compro ou vendo uma ação em BRL
- **Quando** a operação é registrada
- **Então** `taxaCambioNaOperacao` é gravado como `1` — sem caminho especial
  no código de consolidação

#### AC-488 — Falha na fonte primária tenta o fallback

- **Dado** que a fonte primária de câmbio (AwesomeAPI) falha
- **Quando** registro uma operação em USD
- **Então** o sistema tenta a fonte de fallback (PTAX BCB) antes de desistir

#### AC-489 — Ambas as fontes falham, mas há cache: prossegue com aviso

- **Dado** que ambas as fontes de câmbio falham mas já existe uma taxa em
  cache
- **Quando** registro uma operação em USD
- **Então** a operação prossegue com a última taxa conhecida, com um aviso
  na resposta sobre a idade do dado

#### AC-490 — Ambas as fontes falham sem cache: operação recusada

- **Dado** que ambas as fontes de câmbio falham e nunca houve taxa em cache
- **Quando** registro uma operação em USD
- **Então** a operação é recusada (não há fallback possível)

### US-427 — Cache de câmbio com TTL

Como sistema, quero reaproveitar a taxa de câmbio dentro de um TTL, para não
estourar a cota das fontes externas.

#### AC-491 — Taxa dentro do TTL é reaproveitada

- **Dado** que a taxa foi obtida há menos que o TTL configurado
- **Quando** qualquer operação ou consolidação precisa da taxa
- **Então** o valor em cache é reaproveitado sem chamar a fonte externa

#### AC-492 — Taxa fora do TTL busca de novo

- **Dado** que a taxa está fora do TTL (ou nunca foi obtida)
- **Quando** é necessário obter a taxa
- **Então** a fonte é consultada e o cache é atualizado

### US-428 — Consolidação da carteira em BRL

Como investidor, quero ver o valor investido, o valor de mercado e o
lucro/prejuízo da minha carteira consolidados em reais, mesmo com ações em
BRL e USD misturadas, para entender minha posição financeira total.

#### AC-493 — Consolidado converte tudo para BRL

- **Dado** que uma carteira tem posições em BRL e USD
- **Quando** consulto o consolidado da carteira
- **Então** recebo o valor investido em BRL (câmbio histórico por operação),
  o valor de mercado em BRL (câmbio atual) e o lucro/prejuízo não realizado,
  todos em reais

#### AC-494 — Consolidado exibe a taxa e o horário usados

- **Dado** qualquer resposta de consolidado
- **Quando** ela é retornada
- **Então** exibe a taxa de câmbio atual usada e o horário em que foi obtida

#### AC-495 — Posição 100% BRL usa câmbio 1

- **Dado** que uma posição é inteiramente em BRL
- **Quando** o consolidado é calculado
- **Então** o câmbio aplicado é `1` — sem caminho especial no código

### US-429 — Lucro realizado consistente em carteiras multi-moeda

Como investidor, quero que o lucro realizado consolidado da carteira
(SPEC-06) seja expresso em reais de forma coerente, mesmo somando vendas de
ações BRL e USD, para não misturar moedas na mesma soma.

#### AC-496 — Lucro realizado soma valores convertidos para BRL

- **Dado** que uma carteira tem vendas de ações BRL e USD
- **Quando** consulto `GET /carteiras/{id}/lucro-realizado`
- **Então** o total e o detalhamento por ticker são convertidos para BRL
  (usando a taxa histórica de cada venda) antes de somar

## Fora de escopo

- Decompor o lucro em efeito-preço vs. efeito-câmbio — item "desejável" do
  Q-MAP-10, adiado por decisão do usuário nesta conversa.
- Suporte a moedas além de USD/BRL — só existem os mercados BR e US hoje.
- Corrigir retroativamente `taxaCambioNaOperacao` de operações antigas
  (ficam com o valor padrão da migração, `1`) — não há taxa histórica real
  para recuperar.

## Suposições

| ID | Suposição | Status | Resolução |
|---|---|---|---|
| ASM-429 | A consolidação é por carteira (`GET /carteiras/{id}/consolidado`), não um endpoint global entre carteiras do investidor — mesma escala das demais consultas (`posicoes`, `lucro-realizado`). | aberta | — |
| ASM-430 | O TTL do cache de câmbio é configurável separadamente do TTL de cotação de ação (`cambio.cache-ttl-minutos`), mesmo valor padrão (15min) mas chave própria. | aberta | — |
| ASM-431 | A decisão "última taxa conhecida, com aviso" (aprovada nesta conversa) vale tanto para compra/venda (aviso na operação) quanto para o consolidado (aviso no payload) quando a taxa atual usada estiver desatualizada. | confirmada | Validado com o usuário nesta conversa (AskUserQuestion). |
| ASM-432 | `custoTotalBrl` é armazenado em `CarteiraAcao`, recalculado no mesmo loop de `PosicaoService.recalcular` que já existe (paralelo ao `custoTotal` nativo) — não é uma tabela/cálculo separado. | aberta | — |
| ASM-433 | `lucroRealizadoBrl` é um novo campo persistido por operação de venda (paralelo a `lucroRealizado`), calculado como `lucroRealizado × taxaCambioNaOperacao` da própria venda. | aberta | — |

## Perguntas em aberto

Nenhuma — as duas decisões pendentes do prompt original (estratégia de
fallback quando as duas fontes de câmbio falham; decompor efeito-preço vs.
efeito-câmbio) foram resolvidas nesta conversa via pergunta direta ao
usuário.

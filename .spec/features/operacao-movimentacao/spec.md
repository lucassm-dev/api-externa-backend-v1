# Spec: Operacao movimentacao

> feature: operacao-movimentacao
> status: rascunho

## Contexto

Compra e venda de ação dentro de uma carteira, a preço de mercado corrente
(nunca digitado). Cada operação gera uma movimentação; a posição (quantidade +
preço médio) é o agregado **derivado do histórico** — por isso um lançamento pode
ser **editado ou excluído**, e a posição é **recalculada** a partir das
movimentações restantes (como Investidor10/StatusInvest). Não há caixa: o valor em
dinheiro é informativo. Cobre as seções 6.5 e 6.6 do PRD e as regras RN-P01 a
RN-P07 e RN-Q02/Q04.

> **Correção ao PRD:** a RN-P08 (movimentação imutável) foi **removida** por
> decisão do dono do produto. Lançamentos são corrigíveis com recálculo do preço
> médio, não por operação inversa.

## Histórias

### US-401 — Comprar ação

Como investidor, quero comprar uma ação numa carteira, para montar minha posição
simulada ao preço atual.

#### AC-401 — Compra usa a cotação do momento, nada digitado (RN-Q04, RN-Q02)

- **Dado** uma carteira e uma ação do mesmo mercado
- **Quando** compro uma quantidade
- **Então** o preço unitário usado é a cotação buscada no ato da operação, e não um valor informado por mim

#### AC-402 — Compras sucessivas geram preço médio ponderado (RN-P02)

- **Dado** que comprei 100 de um ticker a 38 e depois 100 a 42 na mesma carteira
- **Quando** consulto a posição
- **Então** a posição é 200 ao preço médio 40

#### AC-403 — Só é possível operar ação do mesmo mercado da carteira (RN-P01)

- **Dado** uma carteira de mercado BR
- **Quando** tento comprar uma ação de mercado US nessa carteira
- **Então** a operação é recusada por incompatibilidade de mercado

### US-402 — Vender ação

Como investidor, quero vender parte ou toda uma posição, para simular a saída e
apurar o resultado.

#### AC-404 — Não é possível vender mais que a posição atual (RN-P03)

- **Dado** uma posição de 100 de um ticker
- **Quando** tento vender 150
- **Então** a venda é recusada e a mensagem informa que a quantidade excede a posição

#### AC-405 — Venda que zera a posição a remove, preservando o histórico (RN-P04)

- **Dado** uma posição de 100 de um ticker
- **Quando** vendo as 100
- **Então** a posição some da carteira, mas as movimentações e o resultado acumulado permanecem

#### AC-406 — Venda alimenta o resultado realizado por carteira e por ticker

- **Dado** uma posição com preço médio conhecido
- **Quando** vendo parte dela à cotação do momento
- **Então** o resultado da operação (preço de venda − preço médio, vezes a quantidade) soma tanto no acumulado realizado da carteira quanto no acumulado realizado daquele ticker

### US-403 — Registro de movimentações

Como investidor, quero um histórico fiel e imutável das operações, para o preço
médio ser auditável (6.6).

#### AC-407 — Cada operação gera exatamente uma movimentação (RN-P07)

- **Dado** uma compra ou uma venda concluída
- **Quando** ela é registrada
- **Então** exatamente um registro de movimentação é criado

#### AC-409 — Histórico traz os campos exigidos (6.6)

- **Dado** movimentações registradas
- **Quando** listo o histórico do investidor
- **Então** cada linha mostra data/hora, tipo (compra/venda), ticker, quantidade, preço unitário e valor total

### US-406 — Corrigir lançamentos

Como investidor, quero editar ou excluir um lançamento errado, para que minha
posição reflita a realidade sem precisar de uma operação inversa.

#### AC-412 — Editar um lançamento recalcula a posição

- **Dado** uma posição formada por vários lançamentos de um ticker
- **Quando** edito a quantidade ou o preço de um desses lançamentos
- **Então** a quantidade e o preço médio da posição são recalculados a partir do histórico atualizado

#### AC-413 — Excluir um lançamento recalcula a posição

- **Dado** uma posição formada por vários lançamentos de um ticker
- **Quando** excluo um desses lançamentos
- **Então** o lançamento some do histórico e a posição é recalculada; se não sobrar quantidade, a posição deixa de existir

### US-404 — Rentabilidade não realizada

Como investidor, quero ver a rentabilidade não realizada da posição, para saber
como ela se comporta.

#### AC-410 — Rentabilidade não realizada usa a fórmula do PRD (RN-P05)

- **Dado** uma posição com preço médio e a cotação atual da ação
- **Quando** consulto a rentabilidade não realizada
- **Então** ela é (cotação atual − preço médio) × quantidade

### US-405 — Ausência de caixa

Como investidor, quero que o sistema nunca me barre por saldo, porque não existe
dinheiro na simulação (RN-P06).

#### AC-411 — Não existe erro de saldo insuficiente

- **Dado** qualquer compra em qualquer valor
- **Quando** a executo
- **Então** ela nunca é recusada por falta de saldo; o valor em dinheiro é apenas informativo

## Fora de escopo

- Proventos (dividendos/JCP) no cálculo de rentabilidade (PRD 8.3)
- Venda a descoberto (posição negativa) — PRD seção 4
- Apuração de IR/DARF e preço médio fiscal (PRD seção 4)
- Backtest / operar em data passada (PRD seção 4)

## Suposições

| ID | Suposição | Status | Resolução |
|---|---|---|---|
| ASM-401 | A posição (quantidade e preço médio) é derivada do histórico de movimentações, que é a fonte da verdade (PRD seção 9); editar/excluir lançamento dispara recálculo | confirmada | Decisão do dono do produto: RN-P08 removida, lançamentos corrigíveis com recálculo |
| ASM-402 | O custo usado no resultado realizado é o preço médio ponderado vigente na hora da venda | aberta | — |
| ASM-403 | A rentabilidade não realizada usa a última cotação conhecida da ação (pode estar defasada, com horário — RN-Q02) | aberta | — |
| ASM-404 | Quantidades são inteiras (sem fração de ação) | confirmada | Decisão do dono do produto: só quantidade inteira |

## Perguntas em aberto

| ID | Pergunta | Status | Resposta |
|---|---|---|---|
| Q-401 | O resultado realizado é por carteira (acumulado) ou também detalhado por ticker? | respondida | Por carteira e por ticker |
| Q-402 | Vender à cotação do momento também consome cota da API — aceitável, ou usar a última cotação salva? | respondida | Buscar no ato (compra e venda buscam a cotação na fonte no momento da operação) |
| Q-403 | Fração de ação é permitida (ações americanas costumam permitir)? | respondida | Não — só quantidade inteira |

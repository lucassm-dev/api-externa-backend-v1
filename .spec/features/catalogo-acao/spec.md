/cost# Spec: Catalogo acao

> feature: catalogo-acao
> status: rascunho

## Contexto

Catálogo **global** de ações (único por ticker, não por investidor). O investidor
cadastra uma ação por ticker e mercado; o sistema identifica o mercado, busca a
cotação na fonte correspondente (brapi para BR, Twelve Data para US) e registra o
momento em que a cotação foi obtida. Cobre RF07–RF11 e RF12. Toda cotação vem de
fonte externa — nada é digitado.

## Histórias

### US-201 — Cadastrar ação por ticker e mercado

Como investidor, quero cadastrar uma ação informando ticker e mercado, para que
ela exista no catálogo e possa ser comprada.

#### AC-201 — Ticker brasileiro válido é cadastrado com cotação e horário (RN-A03, RN-Q01)

- **Dado** um ticker brasileiro existente na fonte BR (brapi)
- **Quando** cadastro a ação no mercado BR
- **Então** a ação é salva com nome da empresa, moeda (BRL), cotação atual e a data/hora em que a cotação foi obtida

#### AC-202 — Ticker americano válido usa a fonte US (RN-A03)

- **Dado** um ticker americano existente na fonte US (Twelve Data)
- **Quando** cadastro a ação no mercado US
- **Então** a ação é salva com moeda USD, cotação e horário, consultando a fonte americana e não a brasileira

#### AC-203 — Ticker inexistente na fonte impede o cadastro (RN-A02, item 14)

- **Dado** um ticker que não existe na fonte do mercado informado
- **Quando** tento cadastrar
- **Então** o cadastro é recusado e a mensagem informa que o ticker não foi encontrado

#### AC-204 — Ticker duplicado é impedido (RN-A01, RF12)

- **Dado** que já existe uma ação cadastrada com um ticker
- **Quando** tento cadastrar outra com o mesmo ticker
- **Então** o cadastro é recusado por duplicidade

#### AC-205 — O sistema direciona a busca conforme o mercado (RN-A03, RN-A06)

- **Dado** um cadastro de ação com mercado BR e outro com mercado US
- **Quando** cada um é processado
- **Então** o BR consulta a fonte brasileira e o US a americana — nunca a fonte errada para o mercado

### US-202 — Listar e buscar ações

Como investidor, quero listar e localizar ações do catálogo, para consultá-las e
operá-las.

#### AC-206 — Listar ações (RF09)

- **Dado** que existem ações cadastradas
- **Quando** solicito a listagem
- **Então** recebo as ações de forma paginada

#### AC-207 — Buscar ação por ticker (RF10)

- **Dado** uma ação cadastrada
- **Quando** busco pelo ticker
- **Então** recebo os dados dela; ticker não cadastrado retorna "não encontrado"

### US-203 — Atualizar cotação

Como investidor, quero atualizar a cotação de uma ação cadastrada, para consultar
um preço mais recente.

#### AC-208 — Atualizar cotação busca preço novo e registra novo horário (RF11, RN-Q02)

- **Dado** uma ação cadastrada com uma cotação e um horário
- **Quando** solicito atualizar a cotação
- **Então** o sistema busca o preço atual na fonte do mercado e grava a nova cotação com a nova data/hora

### US-204 — Frescor e resiliência da cotação

Como investidor, quero saber quão atual é a cotação e não ver a tela quebrar
quando a fonte falha, para confiar no que o sistema mostra.

#### AC-209 — Toda cotação exibida acompanha o horário de obtenção (RN-Q01)

- **Dado** uma ação com cotação
- **Quando** consulto a ação
- **Então** a resposta traz a cotação junto da data/hora em que foi obtida

#### AC-210 — Fonte indisponível retorna a última cotação conhecida (RN-Q05)

- **Dado** uma ação já cadastrada e a fonte de cotação indisponível
- **Quando** consulto/atualizo a cotação
- **Então** o sistema retorna a última cotação conhecida com seu horário, em vez de falhar a operação inteira

#### AC-211 — Limite de requisições da fonte é tratado com mensagem própria (item 14, RN-Q05)

- **Dado** que a cota gratuita da fonte externa foi excedida
- **Quando** uma operação precisa buscar cotação
- **Então** o sistema responde com uma mensagem específica de "limite excedido", não um erro genérico ou inesperado

## Fora de escopo

- Histórico de cotações (diferencial não incluído — PRD 5.3)
- Proventos: dividendos e JCP (cortados pela regra de gratuidade — PRD 8.3)
- Backtest / cotação em data passada (PRD seção 4)
- Câmbio e variação do dólar (PRD 5.3)

## Suposições

| ID | Suposição | Status | Resolução |
|---|---|---|---|
| ASM-201 | Fonte BR: brapi.dev; fonte US: Twelve Data (Alpha Vantage removida de vez) | confirmada | Decisão do dono do produto: só Twelve Data no US |
| ASM-202 | O mercado (BR/US) é informado pelo investidor no cadastro, não inferido só pelo formato do ticker | aberta | — |
| ASM-203 | A moeda é derivada do mercado (BR→BRL, US→USD), não digitada | aberta | — |
| ASM-204 | Cada fonte de cotação é isolada atrás de um Adapter comum de cotação (Item 15) | aberta | — |

## Perguntas em aberto

| ID | Pergunta | Status | Resposta |
|---|---|---|---|
| Q-201 | Qual o atraso das cotações no plano gratuito de cada fonte? O item 9 do enunciado exige documentar (pendência do Apêndice A) | respondida | brapi: atraso ~30 min, 15.000 req/mês. Twelve Data: atraso ~0,3–2 min após o fechamento do candle, 800 créditos/dia (8/min). Documentado no README |
| Q-202 | Fora do horário de pregão, usar o último fechamento é suficiente (RN-Q03)? | respondida | Sim — exibe o último fechamento disponível, com o horário de obtenção |
| Q-203 | O código-legado tinha AlphaVantageAdapter; confirma trocar de vez por Twelve Data no US? | respondida | Sim — só Twelve Data no US, Alpha Vantage removida |

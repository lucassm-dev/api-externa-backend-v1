# Spec: Carteira

> feature: carteira
> status: rascunho

## Contexto

Carteira de **acompanhamento**, sem dinheiro real: não há depósito, saque nem
saldo. O investidor cria quantas carteiras quiser; cada uma pertence a ele, está
associada a uma corretora e a **um único mercado** (BR ou US). Carteira brasileira
totaliza em reais, americana em dólares, e os dois nunca se somam. É diferencial
do enunciado promovido a MVP no PRD (6.4).

**Sem autenticação no MVP** (adiada para a 2ª fase — ver `autenticacao-investidor`):
o investidor é cadastrado sem senha e seu **id vai explícito na requisição** ao
criar carteira e operar. A listagem filtra pelo id informado, mas o sistema **não
impede** no MVP que se informe o id de outro investidor — o isolamento com
enforcement de segurança chega junto com o login, na 2ª fase.

## Histórias

### US-301 — Criar carteira

Como investidor, quero criar carteiras de acompanhamento, para simular teses
diferentes separadamente.

#### AC-301 — Criar carteira definindo um único mercado

- **Dado** que informo o id de um investidor cadastrado, um mercado (BR ou US) e uma corretora
- **Quando** crio a carteira
- **Então** ela é criada vinculada a esse investidor, ao mercado escolhido e à corretora informada

#### AC-302 — Um investidor pode ter várias carteiras

- **Dado** que já tenho uma carteira
- **Quando** crio outra
- **Então** ambas coexistem sob a minha conta, cada uma com seu mercado

#### AC-303 — Carteira exige uma corretora existente

- **Dado** que informo uma corretora que não está cadastrada
- **Quando** tento criar a carteira
- **Então** a criação é recusada e a mensagem informa que a corretora não existe

### US-302 — Totalização por mercado

Como investidor, quero que cada carteira totalize na sua própria moeda, para não
misturar reais com dólares.

#### AC-304 — BR totaliza em reais, US em dólares, nunca somados

- **Dado** uma carteira BR e uma carteira US, cada uma com posições
- **Quando** consulto os totais de cada carteira
- **Então** a BR é expressa em BRL e a US em USD, e não existe um total consolidado somando as duas moedas

#### AC-305 — Só é possível associar ação do mesmo mercado da carteira (RN-P01)

- **Dado** uma carteira de mercado BR
- **Quando** uma operação tenta incluir uma ação de mercado US nessa carteira
- **Então** a operação é recusada por incompatibilidade de mercado

### US-303 — Listagem por investidor

Como consumidor da API, quero listar as carteiras de um investidor pelo id dele,
para ver só o que é daquele investidor.

#### AC-306 — Listagem filtra pelo id do investidor informado

- **Dado** que os investidores A e B têm carteiras
- **Quando** listo as carteiras informando o id de A
- **Então** só aparecem as carteiras de A, nunca as de B

> O **enforcement de segurança** (impedir que alguém informe o id de outro
> investidor) depende do login e fica na 2ª fase (feature `autenticacao-investidor`,
> RN-U02). No MVP isto é apenas um filtro por id, não um controle de acesso.

### US-304 — Renomear e desativar carteira

Como investidor, quero renomear e desativar carteiras, para organizar minhas
simulações sem perder o histórico.

#### AC-307 — Renomear carteira

- **Dado** uma carteira minha
- **Quando** altero o nome dela
- **Então** o novo nome é gravado, sem afetar posições, movimentações ou totais

#### AC-308 — Excluir carteira é exclusão lógica

- **Dado** uma carteira minha com posições e movimentações
- **Quando** a excluo
- **Então** ela é marcada como inativa e some das listagens, dos totais e da rentabilidade; as movimentações e posições permanecem no banco vinculadas a ela

#### AC-309 — Carteira inativa não recebe novas operações

- **Dado** uma carteira inativa
- **Quando** tento comprar ou vender nela
- **Então** a operação é recusada porque a carteira está inativa

#### AC-310 — Nome de carteira inativa é reutilizável

- **Dado** uma carteira inativa chamada "X"
- **Quando** crio uma nova carteira chamada "X"
- **Então** a criação é permitida — o nome da inativa não bloqueia

## Fora de escopo

- Saldo em dinheiro, depósito, saque, erro de "saldo insuficiente" (RN-P06)
- Telas de análise e composição da carteira (PRD 5.3)
- Compra/venda e cálculo de posição (ficam na feature operacao-movimentacao)
- Trocar o mercado de uma carteira depois de criada
- Restaurar uma carteira inativa (não há restauração pelo usuário no MVP)

## Suposições

| ID | Suposição | Status | Resolução |
|---|---|---|---|
| ASM-301 | Toda carteira é obrigatoriamente associada a uma corretora (modelo conceitual do PRD, seção 9) | confirmada | Decisão do dono do produto: corretora obrigatória |
| ASM-302 | O mercado da carteira é imutável após a criação | aberta | — |
| ASM-303 | Não há limite de quantidade de carteiras por investidor | aberta | — |
| ASM-304 | No MVP existe um investidor cadastrado (sem senha) e seu id é informado na requisição; o cadastro/login completo é 2ª fase | confirmada | Decisão do dono do produto: auth adiada, id do investidor vai na requisição |

## Perguntas em aberto

| ID | Pergunta | Status | Resposta |
|---|---|---|---|
| Q-301 | A associação com a corretora é obrigatória mesmo, ou pode existir carteira sem corretora? | respondida | Obrigatória |
| Q-302 | Uma carteira pode ser renomeada/excluída? Se excluída, o que acontece com as movimentações? | respondida | Renomear livre; excluir = exclusão lógica (inativa, some de listagens/totais/rentabilidade, movimentações preservadas, não recebe operação, nome reutilizável, sem restauração no MVP) |

# ADR-018: Filtros no servidor para o extrato de operações

- **Data**: 2026-11-21
- **Status**: Aceito
- **Decisores**: Lucas Mendes (dono do produto)
- **Tags**: dados, relatórios, api
- **Substitui**: ADR-010, na parte de filtros

## Contexto e problema

O ADR-010 assumiu que as listagens eram paginadas e sem filtro no servidor, e
recusou filtrar no cliente: filtrar só a página carregada esconde operações que
existem e faz o investidor achar que o sistema perdeu o lançamento. O próprio
ADR-010 previu que ampliar o backend viria num ADR que o substituísse.

Os relatórios do investidor (PRD 15.4, issues #32 e #33) exigem exatamente esse
recorte: movimentações num período, de um ticker, com lucro ou com prejuízo.
Falta fechar a lista de filtros, se o relatório exporta arquivo e como "lucro ou
prejuízo" é medido.

## Fatores de decisão

- O filtro tem de valer sobre todas as operações do investidor, não sobre a
  página carregada (o motivo do ADR-010 continua de pé)
- Resultado estável: a mesma consulta, sem operação nova, devolve a mesma coisa
- Reaproveitar o que a operação já guarda, sem modelo novo
- Escopo contido para a v0.6

## Opções consideradas

### Lucro ou prejuízo

| Opção | Estabilidade | Onde já existe |
|---|---|---|
| **Só realizado** | Fixo desde a venda | `operacao.lucro_realizado`, calculado pela SPEC-06 no preço médio |
| Realizado e em aberto | O em aberto muda a cada cotação | Pertence à posição, não à movimentação |

### Exportação

| Opção | Custo |
|---|---|
| **Sem arquivo, só a tela** | Nenhum além do filtro |
| CSV | Pouco código, outro formato de resposta para manter e testar |
| PDF | Biblioteca nova, layout e testes de documento |

## Decisão

**O extrato `GET /operacoes` passa a aceitar filtros opcionais, aplicados no
servidor antes da paginação, todos combinados com E.**

| Parâmetro | Efeito |
|---|---|
| `dataInicio`, `dataFim` | Data da operação dentro do intervalo, inclusive nas duas pontas; cada um pode vir sozinho |
| `ticker` | Operações de um ticker |
| `carteiraId` | Operações de uma carteira do próprio investidor |
| `tipo` | `COMPRA` ou `VENDA` |
| `resultado` | `LUCRO` (lucro realizado maior que zero) ou `PREJUIZO` (menor que zero) |

- **Lucro ou prejuízo é só o realizado**, o da venda pelo preço médio que a
  SPEC-06 já grava em cada operação. Por isso `resultado` devolve apenas vendas,
  e venda com resultado zero não entra em nenhum dos dois.
- **Sem exportação de arquivo.** O relatório é a tela sobre a resposta JSON
  paginada. Exportar fica para quando for pedido, num ADR próprio.
- **O recorte respeita a posse**: o investidor só vê as próprias operações, e
  `carteiraId` de carteira alheia não devolve nada de outra pessoa.
- Sem nenhum filtro, a resposta continua igual à de hoje.

### O que continua valendo do ADR-010

**Sem série histórica.** Os gráficos de desempenho seguem como retratos do
agora, sem evolução no tempo, até outra decisão. O ADR-010 passa a
"Substituído parcialmente" e aponta para este.

### Consequências positivas

- Os relatórios de #32 e #33 saem do próprio extrato, sem rota nova
- O filtro é verdadeiro: vale para todas as operações, não para a página
- O resultado de uma consulta não muda sozinho com a cotação
- Nenhuma coluna nova: tudo já está gravado na operação

### Consequências negativas

- "Com lucro ou prejuízo" não mostra posições ainda abertas que estejam no
  vermelho ou no azul; quem quiser isso olha as posições
- Sem arquivo, levar o relatório para uma planilha exige copiar da tela
- Mais combinações de parâmetros para testar no mesmo endpoint

## Links

- ADR-010 (substituído na parte de filtros)
- PRD 15.4 (relatórios do investidor)
- SPEC-06 (lucro realizado)
- Issues #31 (este ADR), #32 (período e ticker) e #33 (lucro ou prejuízo)

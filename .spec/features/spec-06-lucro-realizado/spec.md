# Spec: Lucro realizado e exclusão segura de operação

> feature: spec-06-lucro-realizado
> status: rascunho

## Contexto

Hoje uma venda não registra o lucro obtido — o número se perde assim que a
posição zera ou é recomprada (CALC-07 do mapeamento). Além disso, excluir uma
operação apaga a linha do banco (hard delete), o que é irreversível e não
deixa rastro. Esta spec cobre Q-MAP-02 (lucro realizado por venda + consultas
agregadas por carteira/ticker) e Q-MAP-03 (exclusão vira soft delete, com
recálculo correto da posição e do lucro realizado acumulado).

## Histórias

### US-421 — Lucro realizado por venda

Como investidor, quero que cada venda registre o lucro obtido naquele
momento, para acompanhar meu desempenho histórico mesmo depois de recomprar
ou zerar a posição.

#### AC-469 — Venda grava preço médio e lucro do momento

- **Dado** que tenho uma posição com preço médio de compra X e quantidade
  suficiente
- **Quando** registro uma venda com preço unitário Y e quantidade Q
- **Então** a operação de venda salva `precoMedioCompraNoMomento = X` e
  `lucroRealizado = (Y − X) × Q`

#### AC-470 — Lucro realizado total da carteira

- **Dado** que existem vendas ativas em uma carteira (de tickers diferentes)
- **Quando** consulto o lucro realizado dessa carteira
- **Então** recebo o total somado apenas das vendas ATIVAS daquela carteira

#### AC-471 — Lucro realizado agrupado por ticker

- **Dado** que existem vendas ativas de tickers diferentes na mesma carteira
- **Quando** consulto o lucro realizado dessa carteira
- **Então** recebo também o total agrupado por ticker, na mesma resposta

### US-422 — Exclusão sem perda de rastreabilidade

Como investidor, quero excluir uma operação lançada por engano sem apagar o
histórico definitivamente, para manter rastreabilidade e não corromper
cálculos de outras operações.

#### AC-472 — Exclusão marca inativa, não apaga

- **Dado** que uma operação ativa existe
- **Quando** eu a excluo
- **Então** ela é marcada inativa (permanece na tabela) e some do histórico e
  das listagens

#### AC-473 — Operação já excluída não pode ser reoperada

- **Dado** que uma operação já foi excluída (inativa)
- **Quando** tento editá-la ou excluí-la novamente
- **Então** recebo 404 com o código OPE-001 (mesma semântica de operação
  inexistente)

### US-423 — Recálculo correto após editar ou excluir

Como investidor, quero que excluir ou editar uma operação recalcule
corretamente a posição e o lucro realizado acumulado, para que os números
continuem confiáveis mesmo depois de uma correção.

#### AC-474 — Excluir uma venda recalcula a posição e o lucro restante

- **Dado** uma sequência compra → venda → venda na mesma posição
- **Quando** excluo a primeira venda
- **Então** a posição (quantidade, preço médio) e o `lucroRealizado` da
  venda restante são recalculados considerando só as operações ativas
  restantes

#### AC-475 — Editar uma venda recalcula o próprio lucro

- **Dado** uma venda já registrada com `lucroRealizado` calculado
- **Quando** edito a quantidade ou o preço unitário dessa venda
- **Então** o `lucroRealizado` dessa operação é recalculado usando o novo
  valor e o preço médio de compra histórico daquele ponto da sequência

#### AC-476 — Editar uma compra recalcula vendas posteriores em cascata

- **Dado** um histórico compra → venda, onde a venda já tem
  `lucroRealizado` calculado
- **Quando** a compra é editada (preço ou quantidade)
- **Então** o `lucroRealizado` da venda posterior é recalculado para
  refletir o novo preço médio de compra

## Fora de escopo

- Converter `lucroRealizado` para BRL quando a venda é em USD — depende da
  conversão de câmbio (Q-MAP-10), fica para a spec de câmbio.
- Bloquear edição que resultaria em quantidade negativa em algum ponto
  intermediário da sequência — gap pré-existente em `editar()` (não
  introduzido por esta spec), fora de escopo aqui.
- Lucro realizado consolidado entre múltiplas carteiras do investidor — só
  por carteira e por ticker dentro de uma carteira, conforme Q-MAP-02.

## Suposições

| ID | Suposição | Status | Resolução |
|---|---|---|---|
| ASM-421 | O endpoint de lucro realizado retorna total e detalhamento por ticker num único payload (`GET /carteiras/{id}/lucro-realizado`), não dois endpoints separados. | aberta | — |
| ASM-422 | `precoMedioCompraNoMomento` e `lucroRealizado` ficam nulos para operações de COMPRA — só fazem sentido em VENDA. | aberta | — |
| ASM-423 | O recálculo (`PosicaoService.recalcular`) passa a ser a única fonte de verdade para `lucroRealizado`: ele reprocessa toda a série ativa ordenada por data e regrava o campo em cada venda, em vez de calculá-lo só uma vez no momento do insert. Isso é o que faz o recálculo em cascata (AC-476) funcionar sem lógica especial em `editar()`. | aberta | — |
| ASM-424 | A query de "lucro realizado" filtra por `tipo = VENDA` e `ativo = true`; não existe conceito de lucro realizado para COMPRA. | aberta | — |

## Perguntas em aberto

Nenhuma — Q-MAP-02 e Q-MAP-03 já fecham as decisões de produto necessárias
para esta spec.

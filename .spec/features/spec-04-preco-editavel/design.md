# Design: SPEC-04 — Preço sugerido pela cotação, mas editável

> feature: spec-04-preco-editavel

## Migration

```sql
-- V13__rastreabilidade_preco_operacao.sql
ALTER TABLE operacao ADD COLUMN preco_manual BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE operacao ADD COLUMN cotacao_no_momento NUMERIC(18,4);
```

`cotacao_no_momento` fica nullable — operações lançadas antes desta spec não
têm esse dado retroativo; a resposta simplesmente não calcula aviso pra elas
(sem cotação de referência, não tem com o que comparar).

## Entidade `Operacao`

Novos campos: `precoManual` (Boolean, default false), `cotacaoNoMomento`
(BigDecimal, nullable).

## DTOs

```java
// OperacaoRequestDTO — precoUnitario vira opcional
@Positive(message = "Preço unitário deve ser maior que zero")
private BigDecimal precoUnitario; // sem @NotNull

// OperacaoEditarDTO — ganha a mesma validação de preço
@Positive(message = "Preço unitário deve ser maior que zero")
private BigDecimal precoUnitario; // já existia, sem validação — bug RN-OPE-05

// OperacaoResponseDTO — dois campos novos
private String moeda;           // herdado de Acao.moeda
private List<String> avisos;    // computado no mapper, nunca persistido
```

`@Positive` do Jakarta Validation só dispara quando o campo não é `null` —
preço ausente continua passando (usa cotação), preço zero/negativo vira 400
`VAL-001` automaticamente pelo `GlobalExceptionHandler` já existente.

## `OperacaoService.comprar`/`vender`

```java
CotacaoResultado cotacao = adapterPara(acao.getMercado()).buscarCotacao(acao.getTicker()); // sempre busca (AC-461/ASM-418)

boolean precoManual = dto.getPrecoUnitario() != null;
BigDecimal precoEfetivo = precoManual ? dto.getPrecoUnitario() : cotacao.preco();

validarEscalaDecimal(precoEfetivo); // OPE-005, 422 — só entra aqui se passou no @Positive

operacao.setPrecoUnitario(precoEfetivo);
operacao.setPrecoManual(precoManual);
operacao.setCotacaoNoMomento(cotacao.preco());
```

`validarEscalaDecimal`: `if (preco.stripTrailingZeros().scale() > 2) throw new RegraVioladaException("OPE-005", ...)`.

## `OperacaoService.editar`

```java
if (novoPreco != null) {
    validarEscalaDecimal(novoPreco); // OPE-005
    operacao.setPrecoUnitario(novoPreco);
    operacao.setPrecoManual(true);
    operacao.setCotacaoNoMomento(operacao.getAcao().getCotacaoAtual()); // ASM-419, sem nova chamada externa
}
```

## Aviso de desvio (10x) — calculado no `OperacaoMapper`, nunca persistido

```java
default List<String> calcularAvisos(Operacao op) {
    if (!Boolean.TRUE.equals(op.getPrecoManual()) || op.getCotacaoNoMomento() == null
            || op.getCotacaoNoMomento().signum() == 0) {
        return List.of();
    }
    BigDecimal razao = op.getPrecoUnitario().divide(op.getCotacaoNoMomento(), 4, RoundingMode.HALF_UP);
    if (razao.compareTo(BigDecimal.TEN) >= 0 || razao.compareTo(new BigDecimal("0.1")) <= 0) {
        String direcao = razao.compareTo(BigDecimal.ONE) > 0 ? "acima" : "abaixo";
        BigDecimal multiplo = razao.compareTo(BigDecimal.ONE) > 0 ? razao : BigDecimal.ONE.divide(razao, 0, RoundingMode.HALF_UP);
        return List.of("O preço informado (" + op.getPrecoUnitario() + ") está " + multiplo
                + "x " + direcao + " da cotação atual (" + op.getCotacaoNoMomento() + "). Confirme se está correto.");
    }
    return List.of();
}
```

(MapStruct não gera lógica condicional complexa via `expression` de forma
legível — melhor um método `default` na própria interface do mapper, ou uma
chamada a partir do `OperacaoService` que popula o DTO depois do
`mapper.toResponse()`. Decisão de implementação: método `default` no
mapper, mantém a lógica de apresentação fora do service.)

## Catálogo de erros — novo código

| Código | Situação | Status | Exceção |
|---|---|---|---|
| OPE-005 | Preço unitário com mais de 2 casas decimais (BRL/USD) | 422 | RegraVioladaException |

`VAL-001` (preço ≤ 0) já existe — nenhum código novo pra esse caso, só passa
a se aplicar também a `precoUnitario`.
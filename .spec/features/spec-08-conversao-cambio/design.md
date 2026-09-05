# Design: Conversão de câmbio USD → BRL

> feature: spec-08-conversao-cambio

## Componentes novos

```
infra/client/awesomeapi/AwesomeApiCambioClient.java   (Feign — GET /json/last/USD-BRL)
infra/client/awesomeapi/dtos/AwesomeApiCotacaoDTO.java
infra/client/bcb/BcbPtaxClient.java                    (Feign — OData PTAX)
infra/client/bcb/dtos/BcbPtaxResponseDTO.java
infra/facade/CambioFacade.java                         (tenta AwesomeAPI, cai para PTAX, senão lança IntegracaoExternaException)
services/CambioCacheService.java                       (cache TTL global, in-memory — não é por Acao como CotacaoCacheService)
```

`CambioFacade` segue o padrão já usado em `CvmFacade`/`CepFacade`/`CnpjFacade`:
tenta a fonte primária, captura falha, tenta o fallback, e só then lança
`IntegracaoExternaException` se as duas falharem. Não existe uma interface
`CambioAdapter` com múltiplas implementações selecionáveis (como
`CotacaoAdapter` para BR/US) porque aqui não há "qual mercado" — é sempre
USD-BRL, com fallback sequencial dentro da própria facade.

`CambioCacheService` é global (não por entidade) porque só existe um par de
moedas no sistema (USD-BRL). Guarda o último resultado obtido em um campo de
instância (`volatile`), com TTL próprio (`cambio.cache-ttl-minutos`). Retorna
um `CambioObtido(CambioResultado resultado, boolean desatualizado)` — mesma
forma do `CotacaoObtida` já usado em `OperacaoService` (SPEC-07), para que o
chamador saiba se deve anexar um aviso.

## Mudanças em componentes existentes

- **`Operacao`**: `taxaCambioNaOperacao` (BigDecimal, not null, default 1),
  `dataHoraTaxaCambio` (LocalDateTime, nullable), `lucroRealizadoBrl`
  (BigDecimal, nullable — só em vendas, paralelo a `lucroRealizado`).
- **`CarteiraAcao`**: `custoTotalBrl` (BigDecimal, not null, default 0) — o
  equivalente em BRL do `custoTotal` nativo que já existe implicitamente no
  cálculo de `precoMedio` dentro de `PosicaoService.recalcular`.
- **`OperacaoService.comprar`/`vender`**: depois de resolver o preço efetivo,
  se `acao.getMoeda().equals("USD")` busca a taxa via
  `CambioCacheService.obterTaxaAtual()` (mesmo padrão de fallback do
  `obterCotacaoComFallback` já existente); se `"BRL"`, grava `taxaCambioNaOperacao
  = BigDecimal.ONE` e `dataHoraTaxaCambio = agora` — sem branch na
  consolidação depois.
- **`PosicaoService.recalcular`**: o loop que já existe (e que a SPEC-06
  estendeu para gravar `lucroRealizado`) ganha mais dois acúmulos em
  paralelo: `custoTotalBrl` (mesma lógica de soma/redução proporcional do
  `custoTotal` nativo, mas multiplicando por `taxaCambioNaOperacao` de cada
  operação) e `lucroRealizadoBrl` (gravado em cada venda, `lucroRealizado ×
  taxaCambioNaOperacao` daquela venda).
- **`ConsultaOperacaoService.lucroRealizado`**: passa a somar
  `lucroRealizadoBrl` em vez de `lucroRealizado` bruto — corrige a mistura
  de moedas numa carteira BR+US (AC-496).
- **`ConsultaOperacaoService` (novo método) `consolidado(carteiraId,
  investidorId)`**: busca todas as posições ativas da carteira,
  soma `custoTotalBrl` (valor investido), calcula valor de mercado somando
  `quantidade × cotacaoAtual × taxaCambioAtualDaAcao` (taxa atual vem de
  `CambioCacheService.obterTaxaAtual()`, chamada uma vez por consulta e
  reaproveitada para todas as posições em USD; posições BRL usam `1`),
  devolve `lucro = valorDeMercado - valorInvestido`, mais a taxa atual usada
  e o horário.
- **`OperacaoResource`**: novo endpoint `GET /carteiras/{id}/consolidado`.
- **`OperacaoResponseDTO`**: ganha `taxaCambioNaOperacao` (exposto — mesma
  lógica de transparência já aplicada a `cotacaoNoMomento`/`moeda`).

## Migração

`V15__cambio_operacao_carteira_acao.sql`:
```sql
ALTER TABLE operacao ADD COLUMN taxa_cambio_na_operacao NUMERIC(18,6) NOT NULL DEFAULT 1;
ALTER TABLE operacao ADD COLUMN data_hora_taxa_cambio TIMESTAMP;
ALTER TABLE operacao ADD COLUMN lucro_realizado_brl NUMERIC(18,4);
ALTER TABLE carteira_acao ADD COLUMN custo_total_brl NUMERIC(18,4) NOT NULL DEFAULT 0;
```
Operações antigas ficam com `taxa_cambio_na_operacao = 1` (não há taxa
histórica real para recuperar — ver Fora de escopo). Na próxima
compra/venda/edição, `PosicaoService.recalcular` reprocessa o histórico e
`custo_total_brl` passa a refletir a realidade a partir daí.

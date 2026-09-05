# Design: SPEC-03 — Recadastro após exclusão e bloqueio com vínculo ativo

> feature: spec-03-recadastro-exclusao

## Migration — de constraint incondicional para índice único parcial

```sql
-- V12__unicidade_parcial_ativo.sql
ALTER TABLE acao DROP CONSTRAINT uk_acao_ticker;
CREATE UNIQUE INDEX uk_acao_ticker_ativo ON acao (ticker) WHERE ativo = true;

ALTER TABLE corretora DROP CONSTRAINT uk_corretora_cnpj;
CREATE UNIQUE INDEX uk_corretora_cnpj_ativo ON corretora (cnpj) WHERE ativo = true;
```

Efeito: `ticker`/`cnpj` só precisam ser únicos **entre os registros ativos**.
Duas linhas inativas (ou uma ativa + N inativas) com o mesmo valor convivem
sem violar a constraint.

**Entidades:** remove `unique = true` de `Acao.ticker` e `Corretora.cnpj`
(`@Column`) — hoje isso faz o Hibernate recriar a constraint incondicional
no schema de teste (H2, `ddl-auto=create-drop`), que nem olha pra Flyway.
Sem o `unique=true` ali, H2 não impõe nenhuma constraint de unicidade — a
proteção nos testes fica 100% pela checagem da aplicação
(`existsByTickerAndAtivoTrue`/`existsByCnpjAndAtivoTrue`), que é exatamente
o que os testes de serviço exercitam (ver ASM-415).

## Checagem de duplicidade — services

```java
// AcaoService.cadastrar
if (repository.existsByTickerAndAtivoTrue(ticker)) { ... ACA-002, 409 }

// CorretoraService.cadastrar
if (repository.existsByCnpjAndAtivoTrue(cnpj)) { ... COR-002, 409 }
```

Novos métodos de repositório: `AcaoRepository.existsByTickerAndAtivoTrue`,
`CorretoraRepository.existsByCnpjAndAtivoTrue`.

## Busca individual e operação — filtro por ativo

```java
// AcaoService.buscarPorTicker
repository.findByTickerAndAtivoTrue(ticker)   // já existe, só passa a ser usado aqui
    .orElseThrow(() -> new RecursoNaoEncontradoException("ACA-001", ...));

// OperacaoService.buscarAcao (usado por comprar/vender)
acaoRepository.findByTickerAndAtivoTrue(ticker)
    .orElseThrow(() -> new RecursoNaoEncontradoException("ACA-001", ...));
```

O histórico de operações (`GET /operacoes`) continua resolvendo
ticker/nome normalmente — ele lê `operacao.getAcao()` direto pela FK, que
nunca é apagada (soft delete), só fica com `ativo=false`. Nenhuma mudança
necessária ali.

## Bloqueio de exclusão com vínculo ativo

Novos métodos em `CarteiraAcaoRepository`:

```java
long countByAcaoIdAndQuantidadeGreaterThan(Long acaoId, Integer zero);
long countByCarteiraIdAndQuantidadeGreaterThan(Long carteiraId, Integer zero);
```

Novo método em `CarteiraRepository`:

```java
long countByCorretoraIdAndAtivaTrue(Long corretoraId);
```

```java
// AcaoService.excluir
long vinculos = carteiraAcaoRepository.countByAcaoIdAndQuantidadeGreaterThan(acao.getId(), 0);
if (vinculos > 0) throw new RegraVioladaException("ACA-003",
    "Ação possui posição ativa em " + vinculos + " carteira(s) — exclusão bloqueada");

// CarteiraService.excluir
long posicoes = carteiraAcaoRepository.countByCarteiraIdAndQuantidadeGreaterThan(carteira.getId(), 0);
if (posicoes > 0) throw new RegraVioladaException("CAR-002",
    "Carteira possui " + posicoes + " posição(ões) ativa(s) — exclusão bloqueada");

// CorretoraService.excluir
long carteirasAtivas = carteiraRepository.countByCorretoraIdAndAtivaTrue(corretora.getId());
if (carteirasAtivas > 0) throw new RegraVioladaException("COR-004",
    "Corretora possui " + carteirasAtivas + " carteira(s) ativa(s) vinculada(s) — exclusão bloqueada");
```

Novos códigos no catálogo (`docs/erros.md`): `ACA-003`, `CAR-002`, `COR-004`
— todos 422 (`RegraVioladaException`), consistente com o padrão da SPEC-01.

## Impacto no catálogo de erros existente

- `ACA-002` (duplicado) e `COR-002` (duplicado) continuam os mesmos códigos
  — só muda a condição que os dispara (agora filtrando `ativo`)
- `CAR-001` (não encontrada) continua igual

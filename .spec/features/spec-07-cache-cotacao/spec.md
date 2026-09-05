# Spec: Cache de cotação com TTL e fallback em compra/venda

> feature: spec-07-cache-cotacao
> status: rascunho

## Contexto

Hoje toda compra, venda e atualização de cotação chama a API externa (brapi/
Twelve Data) sem nenhum cache — arriscando estourar a cota gratuita das fontes
à toa, já que o dado costuma ser reaproveitável por um tempo. Esta spec cobre
Q-MAP-05 (cache reaproveitando os campos `cotacaoAtual`/`dataHoraCotacao` já
existentes em `Acao`, com TTL de 15 minutos — validado com o usuário nesta
conversa) e Q-MAP-06 (compra/venda passam a prosseguir com a última cotação
conhecida quando a fonte estiver com cota estourada ou indisponível, em vez de
falhar — decisão que a SPEC-01 explicitamente deixou para esta spec).

## Histórias

### US-424 — Cache de cotação com TTL configurável

Como sistema, quero reaproveitar a cotação salva enquanto ela estiver dentro
do TTL, para não estourar a cota gratuita das APIs externas à toa.

#### AC-477 — Cotação dentro do TTL é reaproveitada sem chamar a fonte

- **Dado** que a cotação de uma ação foi salva há menos que o TTL configurado
- **Quando** compro, vendo ou atualizo a cotação (sem forçar)
- **Então** o valor salvo é reaproveitado e a fonte externa NÃO é chamada

#### AC-478 — Cotação fora do TTL busca na fonte e atualiza o cache

- **Dado** que a cotação de uma ação foi salva há mais que o TTL configurado
  (ou nunca foi salva)
- **Quando** compro, vendo ou atualizo a cotação
- **Então** a fonte externa é chamada e `cotacaoAtual`/`dataHoraCotacao` são
  atualizados

#### AC-479 — Resposta com cotação sempre traz o timestamp

- **Dado** qualquer resposta que contenha cotação
- **Quando** ela é retornada
- **Então** o timestamp da cotação (`dataHoraCotacao`) está presente

#### AC-480 — TTL é configurável externamente

- **Dado** que o TTL está definido em `application.properties`
  (`cotacao.cache-ttl-minutos`)
- **Quando** o valor é alterado
- **Então** o comportamento de cache muda sem precisar recompilar código

#### AC-481 — Forçar atualização ignora o cache

- **Dado** que a cotação está dentro do TTL
- **Quando** chamo `PUT /acoes/{id}/atualizar-cotacao?forcar=true`
- **Então** a fonte externa é chamada mesmo assim, ignorando o cache

### US-425 — Fallback de compra/venda com fonte indisponível ou cota estourada

Como investidor, quero conseguir comprar/vender mesmo quando a fonte de
cotação está fora do ar ou com cota estourada, para não ter minhas operações
travadas por um problema que não é meu.

#### AC-482 — Cota estourada em compra/venda prossegue com aviso

- **Dado** que a fonte de cotação retorna cota estourada ao comprar/vender
- **Quando** a operação é processada
- **Então** ela prossegue usando a última cotação conhecida, com um aviso na
  resposta sobre a idade do dado

#### AC-483 — Fonte indisponível em compra/venda prossegue com aviso

- **Dado** que a fonte de cotação está indisponível ao comprar/vender
- **Quando** a operação é processada
- **Então** ela prossegue da mesma forma (última cotação conhecida + aviso)

#### AC-484 — Sem cotação salva e fonte falhando, operação é recusada

- **Dado** que a ação nunca teve cotação salva (nunca foi buscada) e a fonte
  está indisponível ou com cota estourada
- **Quando** comprar/vender é chamado sem preço manual
- **Então** a operação é recusada (não há fallback possível)

#### AC-485 — Atualizar cotação continua respondendo 429 em cota estourada

- **Dado** que `PUT /acoes/{id}/atualizar-cotacao` (sem forçar) encontra cota
  estourada
- **Quando** processado
- **Então** continua respondendo 429 explícito — o fallback é exclusivo de
  compra/venda, não muda o comportamento fixado na SPEC-01

## Fora de escopo

- Cache compartilhado entre múltiplas instâncias da aplicação — o "cache" é
  só o campo `dataHoraCotacao` já persistido no banco, sem camada distribuída
  nova (nem Redis, nem Caffeine — requisito explícito de Q-MAP-05).
- TTL diferenciado por mercado ou por fonte — mesmo valor para BR e US nesta
  spec.
- Alterar o comportamento de `atualizar-cotacao` em cota estourada — continua
  429 (ver AC-485), fixado na SPEC-01.

## Suposições

| ID | Suposição | Status | Resolução |
|---|---|---|---|
| ASM-425 | O TTL de 15 minutos vale igualmente para BR e US — não há TTL diferenciado por mercado (fora de escopo, ver acima). | confirmada | Validado com o usuário nesta conversa (AskUserQuestion). |
| ASM-426 | "Resposta que contém cotação" (AC-479) refere-se ao campo `dataHoraCotacao` já existente em `AcaoResponseDTO` — não introduz um campo `atualizadoEm` renomeado. | aberta | — |
| ASM-427 | O parâmetro de forçar atualização é um query param `forcar` (boolean, default `false`) em `PUT /acoes/{id}/atualizar-cotacao`. | aberta | — |
| ASM-428 | A checagem de TTL é por ação (cada ticker tem seu próprio relógio de cache via `dataHoraCotacao`), não um cache global único. | aberta | — |

## Perguntas em aberto

Nenhuma — Q-MAP-05 (TTL validado nesta conversa) e Q-MAP-06 já fecham as
decisões de produto necessárias para esta spec.

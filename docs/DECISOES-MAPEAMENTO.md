# Decisões — Perguntas em Aberto do Mapeamento

> Respostas do Lucas às 8 perguntas levantadas no Bloco A (mapeamento).
> Cada decisão está fechada. Implemente conforme descrito, sem reabrir a discussão.
> Se alguma decisão entrar em conflito com algo que você encontrar no código, **pare e avise** antes de improvisar.

---

## Resumo rápido

| # | Decisão | Afeta |
|---|---|---|
| Q-MAP-01 | Bloquear exclusão só com posição ativa (qtd > 0) | SPEC-03 |
| Q-MAP-02 | Gravar o lucro realizado em cada operação de venda | Nova SPEC |
| Q-MAP-03 | Operação: soft delete + recálculo da posição | Nova SPEC |
| Q-MAP-04 | Preço: validar o básico, **avisar** (não bloquear) se destoar do mercado | SPEC-04 |
| Q-MAP-05 | Cache reaproveitando `dataHoraCotacao` já existente na tabela `Acao` | Nova SPEC |
| Q-MAP-06 | 429 só na atualização de cotação; operação segue com preço antigo | SPEC-01 |
| Q-MAP-07 | Corrigir o bug de recadastro em Ação, Corretora **e** Carteira | SPEC-03 |
| Q-MAP-08 | Corrigir o vazamento de ação excluída junto com o SPEC-03 | SPEC-03 |
| Q-MAP-09 | Carteira aceita ações BR e EUA juntas (sem restrição) | SPEC-08 |
| Q-MAP-10 | Câmbio histórico no investido, atual no valor de mercado | SPEC-08 |

---

## Q-MAP-01 — Bloqueio de exclusão

**Decisão:** bloquear a exclusão **apenas quando houver posição ativa** (quantidade > 0).

- Posição zerada com histórico de operações → **pode excluir**
- Posição com quantidade > 0 → **bloqueia** com erro padronizado (SPEC-01)
- O **histórico de operações nunca é apagado**, mesmo quando a exclusão é permitida

Vale para Ação, Corretora e Carteira. Mensagem de erro deve dizer o que impede e quantos vínculos existem.

---

## Q-MAP-02 — Lucro realizado

**Decisão:** gravar o lucro **na própria operação de venda**, no momento em que ela acontece.

Campos a persistir na operação de venda:

```
precoMedioCompraNoMomento   → preço médio da posição quando a venda ocorreu
lucroRealizado              → (precoUnitarioVenda − precoMedioCompraNoMomento) × quantidade
```

**Por quê:** é um fato histórico, não um número derivado. Não "estraga" quando a posição zera nem quando a ação é recomprada.

**Consultas do dashboard:**
- Lucro realizado da carteira → `SUM(lucroRealizado)` das vendas daquela carteira
- Lucro realizado por ticker → o mesmo, agrupado por ticker
- Ambos os filtros devem existir no endpoint do dashboard

**Ponto de atenção:** ao editar ou excluir (soft delete) uma venda, o `lucroRealizado` daquela operação precisa ser recalculado ou zerado. Ver Q-MAP-03.

---

## Q-MAP-03 — Exclusão de operação

**Decisão:** trocar hard delete por **soft delete**, e **recalcular a posição** depois.

- `DELETE /operacoes/{id}` marca a operação como inativa (padrão do resto do sistema)
- Operações inativas **somem** das listagens e dos cálculos
- Após excluir, o sistema **recalcula** a partir das operações ativas: `quantidadeAtual`, `precoMedioCompra`, `valorTotalInvestido` e o lucro realizado acumulado
- O mesmo recálculo se aplica quando uma operação é **editada**

⚠️ O recálculo é a parte crítica desta decisão — mais importante que o flag de soft delete. Não entregue a spec sem teste que prove o recálculo correto.

---

## Q-MAP-04 — Validação de preço

**Decisão:** validar o essencial e **avisar sem bloquear** quando o preço destoar do mercado.

**Rejeita (erro 400/422):**
- Preço menor ou igual a zero
- Escala decimal incoerente com a moeda
- Quantidade menor ou igual a zero

**Aceita, mas devolve aviso na resposta:**
- Preço muito distante da cotação atual (proponha o múltiplo — sugestão inicial: 10x acima ou abaixo)
- Formato sugerido do aviso: *"O preço informado (R$ 4.750,00) está 100x acima da cotação atual (R$ 47,50). Confirme se está correto."*

**Por que não bloquear:** cadastrar operação antiga é caso de uso legítimo — uma compra de 2020 pode estar 5x distante da cotação de hoje. Bloquear impediria o usuário de registrar o histórico real.

O aviso deve vir num campo próprio da resposta (ex.: `avisos[]`), não como erro.

---

## Q-MAP-05 — Cache de cotação

**Decisão:** reaproveitar os campos que **já existem** na tabela `Acao` — `cotacaoAtual` e `dataHoraCotacao`.

**Regra:** antes de chamar a API externa, verificar a idade de `dataHoraCotacao`.
- Dentro do TTL → usa o valor salvo, **não chama a API**
- Fora do TTL → busca na API e atualiza os dois campos

**TTL sugerido: 15 minutos.** O plano gratuito da brapi já entrega dado com ~30 min de atraso, então TTL menor queima cota sem entregar dado mais fresco. **Valide esse número e me justifique** antes de implementar.

**Requisitos:**
- TTL configurável via `application.yml`, não hardcoded
- Deve existir uma forma de **forçar atualização** ignorando o cache (parâmetro no endpoint de atualizar cotação)
- Toda resposta que contenha cotação deve trazer `atualizadoEm`
- **Sem dependência nova** (nada de Caffeine/Redis) — a estrutura já existe no banco

---

## Q-MAP-06 — Comportamento com cota estourada

**Decisão:** distinguir os dois cenários.

| Situação | Comportamento |
|---|---|
| `PUT /acoes/{id}/atualizar-cotacao` com cota estourada | **429** padronizado, informando quando a cota renova |
| Comprar/vender com cota estourada | **Prossegue** usando a última cotação conhecida, com aviso da idade do dado |

**Por que funciona:** com o cache da Q-MAP-05 sempre existe um preço salvo, e com o preço editável (SPEC-04) o usuário sempre pode digitar o valor manualmente. O sistema nunca trava.

Em qualquer resposta com dado potencialmente velho, incluir `atualizadoEm` e um aviso explícito.

---

## Q-MAP-07 — Bug de recadastro

**Decisão:** corrigir em **Ação, Corretora e Carteira**, na mesma spec.

**Causa raiz:** as verificações de duplicidade (`existsByTicker`, `existsByCnpj`, e equivalente da carteira) não filtram `ativo = true`. Um registro excluído logicamente continua bloqueando o recadastro.

**Correção:** toda verificação de unicidade passa a considerar **apenas registros ativos**.

**Teste obrigatório para cada entidade:** cadastrar → excluir → cadastrar o mesmo identificador → deve funcionar, e o registro novo **não pode herdar** o histórico do antigo.

---

## Q-MAP-08 — Ação excluída ainda visível e operável

**Decisão:** corrigir **junto com o Q-MAP-07**, na mesma spec (SPEC-03).

**Problema:** `GET /acoes/ticker/{ticker}` retorna ações com `ativo = false`, e essas ações continuam podendo ser compradas e vendidas.

**Correção:**
- Endpoints públicos de busca e listagem retornam **apenas ações ativas**
- Tentar comprar ou vender ação inativa → erro padronizado (SPEC-01)

⚠️ **Cuidado importante:** o histórico de operações precisa continuar conseguindo resolver o nome/ticker de ações já excluídas — senão a tela de operações fica com linhas vazias. A correção é **bloquear novas operações e esconder das listagens**, não tornar o registro irrecuperável internamente. Garanta isso com teste.

---

# Impacto no plano de specs

As decisões acima criam trabalho que não estava previsto. Proposta de reorganização:

```
SPEC-01  Padronização de erros ............ + Q-MAP-06 (429 vs 502)
   ↓
SPEC-02  Investidor + Security + JWT ...... sem mudança
   ↓
SPEC-03  Correção do ciclo de exclusão .... AMPLIADA
         ├─ Q-MAP-07 recadastro (3 entidades)
         ├─ Q-MAP-08 vazamento de inativas
         └─ Q-MAP-01 bloqueio com posição ativa
   ↓
SPEC-04  Preço editável .................. + Q-MAP-04 (avisos)
   ↓
SPEC-05  Carteira obrigatória ............ sem mudança
   ↓
SPEC-06  Lucro realizado .................. NOVA (Q-MAP-02 + Q-MAP-03)
   ↓
SPEC-07  Cache de cotação ................. NOVA (Q-MAP-05)
   ↓
SPEC-08  Conversão USD → BRL ............. era SPEC-06
   ↓
SPEC-09  Barra de cotações (ticker) ....... era SPEC-07
   ↓
SPEC-10+ Backlog aprovado no B.2
```

**Antes de começar:** me apresente essa reorganização com sua opinião. Se achar que a ordem deveria ser outra (por exemplo, cache antes do lucro realizado), argumente. As regras de trabalho do prompt original continuam valendo — **uma spec por vez, com parada e revisão minha ao final de cada uma**.

---

# Q-MAP-09 — Carteira multi-mercado (DECIDIDO)

**Decisão:** a carteira **aceita ações brasileiras e americanas na mesma carteira**. Não haverá restrição de mercado ou moeda por carteira.

**Justificativa do Lucas:** uma corretora brasileira permite investir em ações americanas — via BDR (negociada na B3, em reais) ou via conta internacional oferecida pela própria corretora. Restringir seria artificial.

**Não implemente:** separação de carteira por mercado, campo `moeda` na carteira, ou qualquer bloqueio de cadastro por mercado.

## Q-MAP-10 — Qual câmbio usar (DECIDIDO)

**Decisão:** **taxa histórica no valor investido, taxa atual no valor de mercado.**

### O que gravar

Toda operação com ativo em USD passa a gravar a taxa de câmbio do momento:

```
taxaCambioNaOperacao   → cotação USD-BRL no instante da compra/venda
dataHoraTaxaCambio     → quando essa taxa foi obtida
```

Isso vale também para as **vendas**, para que o `lucroRealizado` da Q-MAP-02 possa ser expresso em BRL de forma coerente.

### Como calcular na consolidação

| Valor consolidado | Câmbio usado | Fórmula |
|---|---|---|
| **Valor total investido (BRL)** | Histórico, por operação | `SOMA(quantidade × precoUnitario × taxaCambioNaOperacao)` |
| **Valor de mercado (BRL)** | Atual | `quantidadeAtual × cotacaoAtual × taxaCambioAtual` |
| **Lucro/prejuízo (BRL)** | — | `valorDeMercado − valorInvestido` |

### Por que assim

O valor investido representa **quanto saiu do bolso** — é um fato passado e não deve mudar sozinho quando o dólar mexe. O valor de mercado representa **quanto vale hoje** — esse sim precisa do câmbio atual.

Com essa separação, o lucro em reais captura naturalmente os dois efeitos: a variação do preço da ação **e** a variação do câmbio.

### Regras que continuam valendo

- Preço médio, quantidade e valor investido **por posição** permanecem na moeda original do ativo. **Nunca converter no nível da posição.**
- A conversão acontece **apenas na consolidação** (totais da carteira e dashboard).
- Todo valor consolidado deve exibir **a taxa atual usada e o horário em que foi obtida**.
- Ativos em BRL têm taxa 1 — não precisam de conversão, mas o cálculo deve ser uniforme para não criar caminho especial no código.

### Desejável (não obrigatório)

Se for simples, decompor o lucro no dashboard em **efeito preço** e **efeito câmbio**. Isso responde a pergunta "ganhei porque a ação subiu ou porque o dólar subiu?". **Proponha antes de implementar** — não faça por conta própria.

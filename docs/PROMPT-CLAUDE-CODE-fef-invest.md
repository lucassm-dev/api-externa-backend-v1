




# Prompt de Trabalho — api-externa-backend-v1 (FEF Invest)

> **Como usar:** cole este arquivo inteiro em uma conversa **nova** do Claude Code, na raiz do projeto `api-externa-backend-v1`.
> Ele contém 3 blocos: **(A) Mapeamento**, **(B) Decisões que preciso aprovar** e **(C) Implementações em specs**.
> Execute os blocos **na ordem** e **pare nos checkpoints**.

---

## 0. Contexto do projeto

- **Repositório:** `lucassm-dev/api-externa-backend-v1`
- **Domínio:** Simulação/gestão de carteira de ações (corretoras validadas na CVM, ações BR e EUA, operações de compra e venda, cotações).
- **Stack:** Java 21, Spring Boot 3.3, Spring Web, Spring Data JPA, Validation, OpenFeign, Flyway, PostgreSQL (dev) / H2 (testes), SpringDoc OpenAPI, MapStruct, Lombok.
- **Arquitetura:** camadas sob `com.apiexternabackend` — `resources` (controllers), `services`, `repositories`, `domains` (entidades/dtos/enums), `mappers`, `infra` (client/adapter/facade/converters), `config`.
- **APIs externas já integradas:** BrasilAPI (CNPJ), ViaCEP (CEP), Dados Abertos CVM (`cad_intermed.zip`), brapi.dev (cotação BR), Twelve Data (cotação US).
- **Metodologia:** `onp-spec-driven` (skill instalada em `.claude/skills/onp-spec-driven/`, estrutura em `.spec/`, config em `onpspec.config.json`).
- **Grafo de código:** `graphify` (grafo em `graphify-out/`).

---

## ⚠️ REGRAS DE TRABALHO (valem para TODO este prompt)

1. **Nunca faça tudo de uma vez.** Uma feature por vez. Ao terminar cada uma: **PARE**, me avise e me entregue um roteiro de verificação passo a passo (comandos exatos, requisições `.http`/curl, o que devo ver na tela e o que significa "deu certo").
2. **Só avance para a próxima feature depois que eu escrever "aprovado"** (ou eu apontar correções).
3. **Toda API externa que você me indicar precisa ter plano gratuito viável.** Se o plano gratuito não cobrir o caso de uso, me avise ANTES de implementar.
4. **Não invente regra de negócio.** Se encontrar ambiguidade, registre como *suposição* ou *pergunta em aberto* na spec e me pergunte.
5. **Não altere comportamento existente sem me avisar.** Se uma implementação nova quebrar uma regra atual, me mostre o conflito e espere decisão.
6. **Cada tarefa = 1 commit rastreável.** Conventional Commits, em português.
7. **Teste pulado não conta como pronto.** Todo critério de aceite precisa de teste que passa.
8. **Sempre rode `graphify update .`** depois de modificar código, para manter o grafo atualizado.
9. **Fale comigo em português.** Código, nomes de classe e commits podem seguir o padrão já existente no projeto.

---

# BLOCO A — MAPEAMENTO DO QUE EXISTE HOJE

> **Objetivo:** antes de escrever uma linha de código novo, quero enxergar tudo que o sistema já faz.
> Não implemente nada neste bloco. Apenas leia, execute e documente.

## A.1 — Levantamento

Use as ferramentas nesta ordem de preferência (da mais barata para a mais cara em contexto):

1. `graphify query "<pergunta>"` para navegar o código (se `graphify-out/graph.json` existir).
   - Se o grafo não existir ou estiver desatualizado, rode `graphify .` / `graphify update .` primeiro.
   - Use `graphify path "<A>" "<B>"` para entender relacionamentos e `graphify explain "<conceito>"` para focar em um tema.
2. Leia `.spec/features/` e `docs/` — pode haver regra já especificada (ex.: RN-Q01, RN-Q05 citadas no README).
3. Leia o código-fonte das camadas `services`, `domains` e `infra` para extrair as regras que **não** estão documentadas.
4. **Suba o projeto e teste de verdade:**
   - `docker compose up -d`
   - `./mvnw spring-boot:run`
   - Explore o Swagger em `http://localhost:8080/swagger-ui.html`
   - Use o `api-requests.http` existente para exercitar os fluxos (cadastrar corretora, cadastrar ação, criar carteira, comprar, vender, excluir)
   - Rode `./mvnw test` e registre o que passa/falha hoje
   - Se faltar chave de API no `.env`, me avise **antes** de tentar (não invente token)

## A.2 — O que quero receber

Três listas completas, **numeradas e com código de identificação**, cada item apontando o arquivo e a linha onde ele vive:

### Lista 1 — Regras de negócio
Formato de cada item:

| Código | Regra (em português, linguagem de negócio) | Onde está no código | Está especificada em `.spec/`? |
|---|---|---|---|

Cubra no mínimo: corretora (validação CVM, CNPJ, situação cadastral), ação (cadastro, ticker, mercado BR/EUA, moeda), carteira (criação, vínculo com corretora), operação (compra, venda, quantidade), exclusões (o que bloqueia excluir o quê) e cotações (frescor, cache, tratamento de cota estourada).

### Lista 2 — Cálculos
Para cada cálculo, quero a **fórmula escrita por extenso** + um **exemplo numérico** que você mesmo validou rodando o sistema:

| Código | Cálculo | Fórmula | Exemplo numérico real | Onde está no código |
|---|---|---|---|---|

Cubra no mínimo: preço médio de compra, preço médio de venda, quantidade atual, valor total investido, lucro/prejuízo (realizado e não realizado), percentual de variação, lucro médio por ação, e qualquer conversão de moeda que já exista.

**Atenção especial:** deixe explícito o que acontece com o lucro quando a posição é zerada e depois recomprada. Quero saber se o histórico se perde.

### Lista 3 — Movimentações (mudanças de estado)
Para cada operação que altera dados, quero o "antes → depois":

| Código | Movimentação | O que muda no banco | Regras que a bloqueiam | Onde está no código |
|---|---|---|---|---|

Cubra: compra, venda, recompra após zerar, exclusão de ação, exclusão de corretora, exclusão de carteira, atualização de cotação.

## A.3 — Entrega do mapeamento (relatório visual)

Gere um arquivo **`docs/mapeamento-atual.html`** — página única, HTML + CSS inline, sem dependências externas — contendo as três listas acima, e **abra no meu navegador** (`open docs/mapeamento-atual.html` no macOS).

Requisitos do relatório:
- Índice no topo com âncoras para cada seção
- Uma tabela por lista, com busca simples por texto (input + filtro em JS puro)
- Destaque visual (badge colorido) para itens marcados como ⚠️ **inconsistência encontrada** ou ❓ **dúvida para o Lucas**
- Seção final "Perguntas em aberto" com tudo que você não conseguiu determinar sozinho
- Sem CDN, sem `localStorage`, sem chamadas de rede — o arquivo tem que abrir offline

### ⛔ CHECKPOINT 1 — PARE AQUI

Depois de gerar e abrir o relatório:
- Me diga em 5 linhas o que mais te chamou atenção (inconsistências, regra faltando, cálculo suspeito)
- Espere eu revisar e responder **"aprovado"** ou apontar correções
- **Não comece o Bloco B sozinho**

---

# BLOCO B — DECISÕES QUE PRECISO APROVAR

> Depois do meu "aprovado" no Bloco A, trabalhe este bloco. Continua sendo **pesquisa e proposta, não implementação**.

## B.1 — API de câmbio USD → BRL

Preciso converter para reais as movimentações feitas com ações do mercado americano. Já pré-selecionei duas opções gratuitas — **valide se ainda estão ativas e gratuitas hoje**, e me recomende uma:

**Opção 1 — AwesomeAPI (economia.awesomeapi.com.br)**
- Endpoint: `GET https://economia.awesomeapi.com.br/json/last/USD-BRL`
- Sem chave funciona (resposta com cache de ~1 minuto); com cadastro gratuito sobe o limite (documentação fala em até 100.000 requisições)
- Retorna `bid` (compra), `ask` (venda), `high`, `low`, `pctChange`, `timestamp`
- Também oferece `USD-BRLPTAX` (cotação PTAX do Banco Central) e histórico diário

**Opção 2 — PTAX oficial do Banco Central (Olinda / Dados Abertos BCB)**
- Público, sem chave, sem cota
- É a taxa oficial de referência — mas só tem valor em dias úteis e é do fechamento, não intradiário

**O que quero que você me proponha:**
- Qual usar como **fonte principal** e qual como **fallback**
- Qual campo usar na conversão (`bid`, `ask` ou PTAX) e **por quê** — e me explique a diferença em linguagem simples
- Se a taxa deve ser **gravada junto com a operação** (taxa histórica do dia da compra) ou **buscada sempre ao exibir** (taxa de hoje). Me mostre o impacto de cada escolha no cálculo de lucro.
- Estratégia de cache para não estourar cota

## B.2 — Barra de cotações do topo (ticker de mercado)

**O que eu quero, exatamente:** aquela faixa horizontal que fica no topo do **investidor10.com.br** e do **statusinvest.com.br**, mostrando o mercado em tempo quase real. Formato de cada item: **símbolo + preço + variação % colorida** (verde para alta, vermelho para queda).

Exemplo do que aparece nessa faixa:

```
USD R$ 5,10 -0,61% │ EUR R$ 5,92 -0,34% │ IBOV 185.188,12 -0,01% │ IFIX 3.761,48 +0,20%
│ BTC R$ 415,49 K +5,23% │ IVVB11 R$ 446,99 +1,20% │ ITUB4 R$ 41,94 +1,18% │ PETR4 R$ 47,50 -1,31%
```

Repare que **não é dado fundamentalista** — é uma mistura de **moedas, índices, cripto, ETF e ações**, todos com preço e variação do dia.

### A boa notícia: dá para fazer tudo com a brapi, que você já usa

Todos esses tipos de ativo saem da **mesma API e do mesmo token** que o projeto já tem configurado (`BRAPI_TOKEN`, plano gratuito de 15.000 req/mês):

| Item da faixa | Endpoint brapi | Observação |
|---|---|---|
| **IBOV** | `GET /api/quote/^BVSP` | O ticker do Ibovespa na brapi é `^BVSP` |
| **IFIX** e outros índices | `GET /api/quote/<ticker>` | ⚠️ **Confirme se o IFIX está na lista** — consulte `/api/available` para ver todos os índices suportados |
| **ITUB4, PETR4, IVVB11** | `GET /api/quote/PETR4,ITUB4,IVVB11` | Aceita **vários tickers na mesma chamada**, separados por vírgula — economiza cota |
| **USD, EUR** | `GET /api/v2/currency?currency=USD-BRL,EUR-BRL` | |
| **BTC** | `GET /api/v2/crypto?coin=BTC&currency=BRL` | |

Campos que interessam na resposta: `regularMarketPrice`, `regularMarketChange`, `regularMarketChangePercent`, `regularMarketTime`, `symbol`/`shortName` e `logo` (a brapi serve ícone SVG das empresas).

**Detalhe importante sobre índices:** a documentação da brapi avisa que, para índices como `^BVSP`, **a maioria dos campos além de cotação e histórico retorna `null`**. Para a faixa isso não é problema — preço e variação bastam.

### Tarefas de pesquisa do B.2

1. **Confirme na doc atual da brapi** quais desses endpoints estão liberados no **plano gratuito** (`/api/v2/currency` e `/api/v2/crypto` exigem token; verifique se o free cobre). **Se algum não estiver no free, me avise e proponha alternativa gratuita** — para moeda, a AwesomeAPI do B.1 já resolve USD e EUR sem token.
2. Verifique se **IFIX** existe na brapi. Se não existir, me proponha de onde tirar (ou sugira substituir por outro índice disponível).
3. **Calcule o consumo de cota** da faixa: se atualizar a cada X segundos para N usuários, quantas requisições por mês? Cabe nos 15.000 do free? **Me traga o número antes de implementar** e proponha a estratégia (cache no backend + 1 chamada agregada compartilhada por todos os usuários, provavelmente).
4. Lembre que **PETR4, VALE3, ITUB4 e MGLU3 são liberadas sem token e sem limite** (sandbox) — use isso para desenvolver e testar sem gastar cota.

### Tarefa extra: fundamentos da ação (opcional, decidir depois)

Além da faixa, a mesma brapi expõe indicadores fundamentalistas no endpoint de cotação:

```
GET /api/quote/PETR4?modules=defaultKeyStatistics,financialData,summaryProfile&dividends=true&token=SEU_TOKEN
```

Campos: `forwardPE` (P/L), `priceToBook` (P/VP), `bookValue` (VPA), `dividendYield` (DY), `marketCap`, `beta`, `returnOnEquity` (ROE), `sector`.

⚠️ **A brapi anuncia "fundamentos anuais" a partir do plano pago (Startup).** Confirme exatamente o que o plano gratuito entrega desses módulos. **Se o free não cobrir, me diga e NÃO implemente** — isso é desejável, não obrigatório.

### Varredura de funcionalidades

Analise **investidor10.com.br** e **statusinvest.com.br** e me traga funcionalidades que fariam sentido numa carteira de investimentos. **Restrição:** só o que der para alimentar com dado **gratuito**.

**Formato da entrega:**

| Ideia / funcionalidade | O que resolve pro usuário | Fonte de dado (grátis?) | Consumo de cota | Esforço (P/M/G) | Depende de quê |
|---|---|---|---|---|---|

Ordene por **melhor relação valor/esforço** e marque sua recomendação de top 3.

### ⛔ CHECKPOINT 2 — PARE AQUI

Me entregue as recomendações do B.1 e B.2 e **espere minha decisão**. Eu vou escolher o que entra no escopo. **Não crie spec de nada do Bloco B sem meu aval explícito.**

---

# BLOCO C — IMPLEMENTAÇÕES

> Só comece depois do meu "aprovado" no Checkpoint 2.
> Aqui você usa o fluxo completo do **onp-spec-driven**: especificar → projetar → tarefas → plano → executar → auditar.
> **Uma spec por vez.** Ao final de cada uma: auditoria mecânica + roteiro de verificação para mim + PARE.

## Ordem de execução (respeite as dependências)

```
SPEC-01  Padronização de erros ......... base para todas as outras
   ↓
SPEC-02  Investidor + Security + JWT ... depende do padrão de erro
   ↓
SPEC-03  Recadastrar ação excluída ..... correção de bug
   ↓
SPEC-04  Preço editável na operação .... afeta os cálculos
   ↓
SPEC-05  Carteira obrigatória .......... depende de investidor (02)
   ↓
SPEC-06  Conversão USD → BRL ........... depende da decisão B.1
   ↓
SPEC-07  Barra de cotações (ticker) .... depende da decisão B.2
   ↓
SPEC-08+ Funcionalidades novas ......... só as que eu aprovar no B.2
```

---

## SPEC-01 — Tratamento e padronização de erros

**Por que primeiro:** todas as specs seguintes vão retornar erro em algum momento. Se o padrão não existir antes, cada uma inventa o seu.

**O que preciso:**
- `@RestControllerAdvice` global centralizando o tratamento
- **Payload de erro padronizado e único** para toda a API. Proponha o formato (sugestão de base: RFC 7807 / *Problem Details*) contendo no mínimo: timestamp, status HTTP, código interno do erro, mensagem para o usuário final em português, caminho da requisição e (quando for validação) a lista de campos com problema
- **Catálogo de códigos de erro internos** por domínio, versionado em `docs/erros.md`. Sugestão de prefixos: `COR-xxx` (corretora), `ACA-xxx` (ação), `CAR-xxx` (carteira), `OPE-xxx` (operação), `AUT-xxx` (autenticação), `EXT-xxx` (API externa)
- Mapeamento correto de status HTTP: 400 (payload inválido), 401, 403, 404, 409 (conflito de regra de negócio), 422, 429 (cota de API externa estourada), 502/503 (API externa fora do ar)
- Hierarquia de exceções de negócio (ex.: `NegocioException` → `RecursoNaoEncontradoException`, `RegraVioladaException`, `IntegracaoExternaException`)
- **Migrar as regras que hoje retornam erro genérico** para o novo padrão — use o mapeamento do Bloco A como checklist
- **Nunca vazar stacktrace nem detalhe interno** na resposta

**Critérios de aceite (escreva testes para cada um):**
- Toda exceção de negócio conhecida retorna o payload padronizado
- Erro de validação de payload lista os campos inválidos
- Cota estourada de API externa retorna 429 com mensagem específica
- API externa indisponível retorna 502/503 e **não** 500
- Nenhuma resposta de erro contém stacktrace

---

## SPEC-02 — Cadastro e Login de Investidor + Spring Security + JWT

**Escopo:**

1. **Entidade `Investidor`**
   - Campos mínimos: id, nome, email (único), senha (hash), data de cadastro, status ativo
   - Senha com **BCrypt** — nunca em texto puro, nunca em log, nunca no DTO de resposta
   - Migration Flyway nova (não edite migration já aplicada)

2. **Endpoints públicos**
   - `POST /auth/cadastro` — cria investidor
   - `POST /auth/login` — devolve o token JWT
   - Validação de email e política mínima de senha (me proponha a política antes de codar)

3. **Spring Security + JWT**
   - Todos os endpoints protegidos por padrão; libere explicitamente apenas `/auth/**`, Swagger e actuator health
   - Filtro de autenticação JWT, `SessionCreationPolicy.STATELESS`
   - **Segredo do JWT vem do `.env`** (adicione a chave no `.env.example`) — nunca commitado
   - Defina e me explique o tempo de expiração do token
   - 401 para token ausente/inválido/expirado e 403 para acesso negado, ambos no padrão da SPEC-01

4. **Vínculo de dados ao investidor logado** ⚠️ *ponto que muda muita coisa*
   - Carteira, corretora, ação e operações passam a pertencer a um investidor?
   - **PARE E ME PERGUNTE** antes de decidir. Minha intuição: **carteira e operações são do investidor**; **ação e corretora são catálogo compartilhado** — mas quero ver sua análise do impacto (migrations, dados existentes, consultas) antes de fechar.

**Critérios de aceite:**
- Cadastro com email duplicado retorna 409 com código `AUT-xxx`
- Login com credencial errada retorna 401 e **não** revela se o email existe
- Endpoint protegido sem token retorna 401
- Endpoint protegido com token válido funciona normalmente
- Senha nunca aparece em resposta nem em log
- Token expirado retorna 401 com código específico

---

## SPEC-03 — Recadastrar ação que foi excluída

**Bug reportado por mim:** cadastrei PETR4, excluí, e ao tentar cadastrar PETR4 de novo o sistema não deixa.

**Antes de implementar:** investigue e me diga a **causa raiz**. Hipóteses a checar — constraint `unique` no ticker com registro que continua no banco, exclusão lógica sem filtro nas consultas, cache do ticker, ou registro órfão em tabela relacionada.

**Comportamento esperado:**
- Excluí PETR4 → consigo cadastrar PETR4 novamente, sem erro
- O histórico de operações antigas **não pode ser corrompido nem ressuscitado** por engano no novo cadastro
- Se a exclusão for lógica (soft delete), toda consulta precisa filtrar os excluídos
- Me apresente as duas abordagens (**hard delete** vs **soft delete + reativação**) com prós e contras, e **espere eu escolher** antes de implementar

**Critérios de aceite:**
- Cadastrar → excluir → cadastrar o mesmo ticker funciona
- Operações antigas do ticker excluído não aparecem no novo cadastro
- Excluir ação que está em carteira ativa continua bloqueado (confirme essa regra no mapeamento do Bloco A)

---

## SPEC-04 — Preço da ação sugerido pela cotação, mas editável

**Hoje:** a operação usa a cotação atual buscada automaticamente.
**Quero:** o sistema **sugere** a cotação atual, e eu **posso alterar** o preço unitário na compra e na venda.

**Detalhes:**
- Endpoint de compra/venda aceita `precoUnitario` **opcional**: se vier, usa o informado; se não vier, busca a cotação atual
- Validações: preço > 0, escala decimal coerente com a moeda, limite superior sensato (me proponha um teto para evitar erro de digitação — ex.: alerta se estiver muito distante da cotação de mercado)
- **Rastreabilidade:** grave na operação se o preço foi automático ou informado manualmente, e qual era a cotação de mercado no momento
- **Impacto nos cálculos:** preço médio, lucro e variação passam a usar o preço efetivo da operação. Revise cada cálculo da Lista 2 do Bloco A e me mostre o que muda.
- Junto disso, endereçe a **inconsistência de moeda**: se a ação é EUA, o preço informado está em USD — deixe explícito no contrato da API qual moeda está sendo enviada

**Critérios de aceite:**
- Compra sem `precoUnitario` usa a cotação atual (comportamento de hoje preservado)
- Compra com `precoUnitario` usa o valor informado
- Preço zero ou negativo é rejeitado com erro padronizado
- Preço médio recalculado corretamente misturando operações automáticas e manuais (teste com exemplo numérico)

---

## SPEC-05 — Não permitir cadastrar ação sem carteira

**Regra:** o investidor só pode cadastrar ação se já tiver ao menos uma carteira cadastrada.

**Antes de implementar, me confirme a interpretação.** Há duas leituras possíveis:
- **(a)** Ação é catálogo global e a regra é apenas um *gate* de UX: bloqueia o cadastro se o investidor não tem carteira
- **(b)** Ação passa a ser vinculada a uma carteira específica no momento do cadastro

Elas têm impactos muito diferentes no modelo de dados. **Me pergunte qual eu quero e espere a resposta** — não escolha sozinho.

**Critérios de aceite (assumindo a interpretação que eu escolher):**
- Investidor sem carteira tenta cadastrar ação → erro 409 com código `ACA-xxx` e mensagem clara ("Cadastre uma carteira antes de cadastrar ações")
- Investidor com carteira cadastra normalmente
- A mensagem de erro orienta o próximo passo

---

## SPEC-06 — Conversão de movimentações USD → BRL

**Só implemente com a decisão do B.1 fechada.**

**Escopo:**
- Client/adapter da API de câmbio escolhida, seguindo o padrão de `infra` já usado no projeto (client → adapter → facade → converter)
- Cache da taxa (defina TTL e me justifique) para não estourar cota
- Fallback quando a API de câmbio estiver fora: me proponha a estratégia (última taxa conhecida? erro explícito? bloqueia a operação?) e **espere minha decisão**
- Aplicação da conversão nos totais consolidados da carteira (valor investido, lucro/prejuízo)
- **Sempre exibir junto:** a taxa usada e o horário em que foi obtida — mesma filosofia da regra RN-Q01 que já existe para cotações

**Critérios de aceite:**
- Posição em ação EUA aparece convertida no total consolidado em BRL
- Taxa e horário de obtenção retornam no payload
- API de câmbio indisponível → comportamento definido, sem 500 genérico
- Cache funciona (segunda chamada dentro do TTL não bate na API externa — prove com teste)

---

## SPEC-07 — Barra de cotações do topo (ticker de mercado)

**Só implemente com a decisão do B.2 fechada** (endpoints confirmados no plano gratuito e cálculo de cota aprovado).

**O que é:** um endpoint que alimenta a faixa horizontal do topo da tela, no estilo Investidor10 / StatusInvest — moedas, índices, cripto, ETF e ações com preço e variação do dia.

**Escopo do backend:**

- **Endpoint único agregado:** `GET /mercado/ticker` (ou nome equivalente no padrão do projeto) — devolve tudo que a faixa precisa em **uma só chamada do frontend**
- **Composição configurável:** a lista de símbolos da faixa vem de configuração (`application.yml` ou tabela), não hardcoded no código. Composição inicial sugerida: `USD-BRL`, `EUR-BRL`, `^BVSP`, `IFIX` (se disponível), `BTC`, `IVVB11`, `ITUB4`, `PETR4`
- **Agregação no `infra`:** um facade que orquestra as chamadas aos endpoints da brapi (`/api/quote`, `/api/v2/currency`, `/api/v2/crypto`) seguindo o padrão client → adapter → facade → converter já usado no projeto
- **Batching:** agrupe os tickers de ações/índices numa **única chamada** (`/api/quote/PETR4,ITUB4,IVVB11,^BVSP`) em vez de uma por símbolo
- **Cache compartilhado e obrigatório** ⚠️ — a faixa é igual para todos os usuários, então **uma consulta serve todo mundo**. Defina o TTL (sugestão: 60s, alinhado com o delay de ~30min da brapi no free — não adianta cachear menos que o refresh da fonte) e **me justifique o número**
- **Degradação graciosa:** se um símbolo falhar, os outros continuam aparecendo. A faixa nunca derruba a página inteira por causa de um item
- **Payload padronizado** por item, independente do tipo de ativo:
  ```
  simbolo, rotulo, tipo (MOEDA|INDICE|CRIPTO|ETF|ACAO),
  preco, moeda, variacaoPercentual, variacaoAbsoluta,
  atualizadoEm
  ```
- **Sempre devolver `atualizadoEm`** — mesma filosofia da RN-Q01 que já existe: o dado não é ao vivo e o usuário precisa saber de quando ele é
- Cota estourada da brapi → 429 padronizado da SPEC-01, **e a faixa serve o último valor cacheado** em vez de sumir

**Critérios de aceite:**
- Uma chamada ao endpoint devolve todos os símbolos configurados com preço e variação
- Segunda chamada dentro do TTL **não** bate na brapi (prove com teste que conta as chamadas ao client)
- Símbolo indisponível não derruba os demais itens da resposta
- Cada item traz `atualizadoEm` preenchido
- Alterar a lista de símbolos na configuração muda a resposta **sem recompilar regra de negócio**
- Cota estourada retorna dado cacheado + indicação de que está degradado

**Fora de escopo aqui:** o CSS/animação da faixa no frontend Angular. Este projeto é o backend — entregue o endpoint e o contrato documentado no Swagger.

---

## SPEC-08+ — Funcionalidades novas (backlog)

Só entram aqui as ideias do **B.2 que eu aprovar explicitamente**. Não crie spec antecipada.

---

# ENTREGA DE CADA SPEC (checklist obrigatório)

Ao terminar uma spec, me entregue **nesta ordem**:

1. ✅ **O que foi feito** — resumo em 5 linhas, sem jargão desnecessário
2. ✅ **Arquivos alterados/criados** — lista com uma linha de explicação cada
3. ✅ **Auditoria mecânica do onp-spec** — a prova: cada critério de aceite ligado ao teste que o comprova, com o exit code
4. ✅ **Roteiro de verificação passo a passo para mim**, no formato:
   ```
   PASSO 1 — Suba o ambiente
     Comando: docker compose up -d && ./mvnw spring-boot:run
     Esperado: aplicação sobe sem erro na porta 8080

   PASSO 2 — <ação>
     Requisição: <curl ou bloco do api-requests.http>
     Esperado: status <código>, corpo com <campo> = <valor>
     Se der errado: <o que provavelmente aconteceu>
   ```
5. ✅ **O que mudou nas regras/cálculos** mapeados no Bloco A — e atualize `docs/mapeamento-atual.html`
6. ✅ **Suposições que você fez** e perguntas ainda em aberto
7. ✅ **`graphify update .` executado**
8. ⛔ **PARE e espere meu "aprovado"**

---

# PRIMEIRA AÇÃO

Comece **agora** pelo **BLOCO A**. Não toque em nada dos Blocos B e C ainda.

Antes de rodar qualquer coisa, me diga em 3 linhas o plano de ataque do Bloco A — e se faltar alguma chave de API no `.env` para subir o projeto, **me avise primeiro**.

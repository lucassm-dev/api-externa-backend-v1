# PRD — Sistema de Simulação de Carteira de Ações

**Versão:** 0.4 (rascunho para revisão)
**Data:** 21/08/2026
**Contexto acadêmico:** Trabalho de Programação em Java Spring — gestão de ações com consumo de APIs externas
**Próxima etapa:** especificação técnica via SDD

---

## 0. Como ler este documento

Este PRD descreve **o que o produto faz e por quê**. Ele não decide stack, arquitetura, padrões de projeto, nomes de endpoint, códigos de status ou modelagem física de tabelas — tudo isso pertence à especificação.

Onde uma decisão técnica tem consequência visível para quem usa o sistema, ela aparece aqui **na forma da consequência**, não da técnica. Por exemplo: o PRD diz que a cotação exibida pode estar defasada e sempre mostra a hora em que foi obtida; a spec decide se isso vem de cache, de tabela ou de chamada direta.

**Legenda:** `[D]` decidido · `[P]` proposto por este rascunho, precisa da sua confirmação · `[A]` em aberto

### 0.1. Precedência entre documentos

Dois documentos governam este projeto: o enunciado do trabalho (`requirements-investimentos.md`) e este PRD.

**Regra geral: o enunciado tem precedência em tudo que exige explicitamente.** Nada neste PRD remove um requisito do enunciado. Onde o PRD vai além, é acréscimo, não substituição.

Onde os dois divergem em **interpretação** — não em exigência — vale este PRD. Os pontos conhecidos:

| Ponto | O que diz o enunciado | O que diz este PRD | Resolução |
|---|---|---|---|
| Corretora na entidade Ação | Sugere o campo, marcado como opcional | Mantém o campo sem semântica de propriedade (RN-A04) | O campo existe. Nenhum cálculo o usa |
| Corretora reprovada na CVM | RN03 deixa a escolha ao grupo: bloquear ou marcar "não validada" | Bloqueia (RN-C03) | Escolha já feita. Não reabrir |
| Carteira | Aparece como diferencial | Faz parte do MVP | Acréscimo ao escopo obrigatório |
| Autenticação | Aparece como diferencial | Faz parte do MVP | Acréscimo ao escopo obrigatório |
| Frescor da cotação | O contexto fala em "tempo real ou quase" | Snapshot com horário de obtenção (RN-Q01) | O próprio enunciado confirma o snapshot: a entidade guarda data e hora da cotação e existe uma operação separada de atualizá-la |

`[A]` **Itens marcados `[A]` não devem ser preenchidos por conta própria.** São perguntas em aberto, não lacunas a completar. Se a especificação precisar de um deles, a resposta certa é perguntar, não assumir.

---

**Mudanças da v0.3 para a v0.4**

- Nova seção 0.1, com regra de precedência e mapa de divergências com o enunciado
- Novo Apêndice B, com as alternativas descartadas

**Mudanças da v0.2 para a v0.3**

- Removidas as seções de stack, requisitos não-funcionais técnicos e padrões de projeto
- Regras de negócio reescritas sem códigos de status HTTP
- Modelo de dados virou **modelo conceitual de domínio**, sem detalhes de persistência
- Limites de API movidos para o Apêndice A, como insumo da especificação
- Nova seção 10, com as restrições que o enunciado impõe e que não são escolha sua

---

## 1. Visão geral

Sistema web para **simular a evolução de uma carteira de ações sem dinheiro real**. O investidor cria carteiras, registra compras e vendas a preço de mercado corrente, e acompanha o histórico das movimentações e o desempenho das posições.

Não conecta com corretora, não executa ordem real, não espelha custódia.

`[D]` **Ação é o único ativo do sistema.**

---

## 2. Regra de escopo: gratuidade `[D]`

**Nenhuma funcionalidade do produto depende de plano pago de API.** Se o dado necessário só existe em plano pago, a funcionalidade não entra — não é adiada, é cortada.

Essa regra tem precedência sobre desejos de produto. Sua consequência mais visível é a remoção dos proventos (seção 8.3).

---

## 3. Problema e público

### 3.1. Problema

Quem já investe não tem onde responder, de graça e sem risco, à pergunta *"o que aconteceria se eu montasse esta carteira?"*. Investir de verdade é caro para testar uma tese. Planilha não atualiza preço sozinha. E as ferramentas de mercado analisam ativos individuais, não uma carteira hipotética montada agora.

### 3.2. Persona primária `[D]`

**Investidor com alguma experiência, testando uma tese.** Já tem conta em corretora e já comprou ação. Quer montar carteiras hipotéticas e acompanhar como se comportam daqui pra frente. Tolera — e prefere — tela densa de números.

**Consequência de design:** a interface segue a densidade do StatusInvest, não um app com onboarding guiado.

### 3.3. Persona secundária `[P]`

**Iniciante curioso.** Atendido como efeito colateral da simplicidade do sistema, não como público que dirige decisões.

---

## 4. Não-objetivos `[D]`

Fora do produto — não são "ainda não", são "não":

- Executar ordem real ou conectar com corretora
- Espelhar a custódia real do investidor
- **Backtest** — simular compra em data passada. Toda posição começa no preço de hoje
- **Proventos (dividendos e JCP)** — cortados pela regra de gratuidade, ver 8.3
- Apuração de IR, DARF ou informe de rendimentos
- Venda a descoberto
- Recomendação de investimento
- Qualquer ativo que não seja ação

---

## 5. Escopo do MVP

O MVP é o que será entregue como trabalho. Ele é **maior que o mínimo exigido pelo enunciado** — o excedente cai em itens que o professor lista como diferenciais e que pontuam.

### 5.1. O que você definiu `[D]`

| # | Funcionalidade |
|---|---|
| 1 | Cadastro e login do investidor, com autenticação |
| 2 | Cadastro de corretora com validação na CVM — não validada, não cadastra |
| 3 | Cadastro de carteira do investidor |
| 4 | Compra e venda de ação, com preço vindo da cotação atual |
| 5 | Tela de lançamentos e movimentações, mostrando as compras e vendas efetuadas |

### 5.2. O que o enunciado obriga e não está na lista acima `[A]`

> **Precisa entrar no MVP mesmo assim.**

- **Cadastro, listagem, busca por ticker e atualização de cotação da ação.** As RF07 a RF11 exigem essas operações explicitamente. Comprar uma ação pressupõe que ela exista no catálogo, mas o professor cobra as operações como funcionalidade própria.
- **Mercado americano.** O item 4.2 e a RN06 exigem ação brasileira **e** americana.
- **Listagem de corretoras e busca por CNPJ** (RF05 e RF06).

### 5.3. Fora do MVP `[P]`

- Telas de análise e composição da carteira
- Notícias de mercado e variação do dólar
- Histórico de cotações

### 5.4. Plano de corte

Este MVP é ambicioso para um trabalho de disciplina: autenticação, quatro fontes externas, ingestão da base da CVM e um log de movimentações.

Se o prazo apertar, o item mais seguro para simplificar é a **autenticação** — o enunciado a trata como diferencial, e o conceito de investidor pode continuar existindo no domínio sem tela de login.

---

## 6. Funcionalidades

### 6.1. Investidor e autenticação

Cadastro com credenciais próprias e login. Toda carteira pertence a um investidor; toda movimentação pertence a uma carteira.

`[D]` **Autenticação é obrigatória no seu MVP**, embora o enunciado a trate como diferencial.

`[P]` Corretora e ação **não** pertencem a investidor. Qualquer investidor autenticado consulta o mesmo catálogo.

### 6.2. Corretora

O investidor informa apenas o CNPJ. O sistema preenche o resto a partir de fontes públicas: dados cadastrais da empresa, verificação de autorização na CVM e endereço pelo CEP.

`[D]` Corretora não autorizada **não é cadastrada**.

### 6.3. Ação

Cadastro por ticker e mercado. O sistema identifica o mercado, obtém a cotação na fonte correspondente e registra o momento em que ela foi obtida.

### 6.4. Carteira

`[D]` **Carteira de acompanhamento, sem saldo em dinheiro.** Não há depósito, saque nem erro de saldo insuficiente.

`[D]` O investidor pode ter **quantas carteiras quiser**.

`[D]` Cada carteira pertence a **um único mercado**. Carteira brasileira totaliza em reais, americana em dólares, e os dois números nunca se somam.

### 6.5. Compra e venda

**Compra:** o investidor escolhe a ação e a quantidade. `[D]` O preço vem da **cotação do momento da operação** — nada é digitado.

**Venda:** reduz a quantidade da posição. O resultado da operação vai para o acumulado realizado da carteira.

> **Nota de vocabulário.** "Compra" aqui não movimenta caixa, porque não existe caixa. O valor em dinheiro que aparece na movimentação é informativo — quantidade vezes preço — e não debita nada.

### 6.6. Lançamentos e movimentações

Histórico de todas as compras e vendas do investidor: data e hora, tipo, ticker, quantidade, preço unitário e valor total.

`[D]` **O lançamento é corrigível.** Uma operação registrada pode ser editada ou excluída; a posição e o preço médio são recalculados a partir do histórico de movimentações (como Investidor10/StatusInvest). *(Correção da v0.4: a RN-P08 anterior, que tornava a movimentação imutável, foi removida por decisão do dono do produto.)*

---

## 7. Regras de negócio

### 7.1. Investidor

| ID | Regra | Status |
|---|---|---|
| RN-U01 | O identificador de login é único no sistema | `[P]` |
| RN-U02 | Um investidor só enxerga e movimenta as próprias carteiras | `[P]` |

### 7.2. Corretora

| ID | Regra | Status |
|---|---|---|
| RN-C01 | CNPJ inválido no formato é rejeitado antes de consultar qualquer fonte externa | `[P]` |
| RN-C02 | CNPJ não encontrado na base da Receita impede o cadastro | `[P]` |
| RN-C03 | Corretora **não autorizada** na CVM não é cadastrada, e o sistema informa o motivo | `[D]` |
| RN-C04 | Quando o sistema **não consegue verificar** a autorização, a corretora também não é cadastrada — mas a mensagem diz que a verificação falhou, e não que a corretora foi reprovada | `[D]` |
| RN-C05 | CNPJ é único no sistema inteiro | `[D]` — enunciado |
| RN-C06 | O CEP é verificado em fonte pública antes do cadastro ser salvo | `[D]` — enunciado |

> **RN-C04 existe porque reprovar e não conseguir verificar são coisas diferentes.** Um investidor que vê "corretora não autorizada" quando na verdade a base local está velha recebe uma informação falsa sobre uma empresa real. A distinção é de produto, não de implementação.

### 7.3. Ação

| ID | Regra | Status |
|---|---|---|
| RN-A01 | Ticker é único no sistema. O catálogo de ações é **global**, não por investidor | `[D]` — enunciado |
| RN-A02 | Ação só é cadastrada se o ticker existir na fonte do seu mercado | `[D]` — enunciado |
| RN-A03 | O sistema distingue ativo brasileiro de americano e consulta a fonte adequada a cada um | `[D]` — enunciado |
| RN-A04 | A corretora associada à ação é **referência do cadastro**. Não indica propriedade e não entra em nenhum cálculo | `[D]` |

> **Atenção à RN-A04.** Como o ticker é único, existe uma só PETR4 para todos os investidores. Se alguém ler essa associação como "dono", o cálculo da carteira de outro investidor sai errado. A corretora que vale para qualquer cálculo é sempre a da **carteira**.

### 7.4. Cotação

| ID | Regra | Status |
|---|---|---|
| RN-Q01 | Toda cotação exibida é acompanhada do momento em que foi obtida | `[D]` |
| RN-Q02 | A cotação exibida em listagens **pode estar defasada**. A cotação usada em compra e venda é obtida no ato da operação | `[D]` |
| RN-Q03 | Fora do horário de pregão, vale o último fechamento disponível | `[P]` |
| RN-Q04 | Nenhum preço é digitado por usuário. Toda cotação vem de fonte externa | `[D]` |
| RN-Q05 | Quando a fonte de cotação não responde, o sistema exibe a última cotação conhecida com seu horário, em vez de falhar a tela inteira | `[P]` |

### 7.5. Carteira, posição e movimentação

| ID | Regra | Status |
|---|---|---|
| RN-P01 | Só é possível operar ação do mesmo mercado da carteira | `[D]` |
| RN-P02 | Compras sucessivas do mesmo ticker geram **preço médio ponderado**. Ex.: 100 a R$ 38 e 100 a R$ 42 resultam em 200 a R$ 40 | `[P]` |
| RN-P03 | Não é possível vender quantidade maior que a posição atual | `[P]` |
| RN-P04 | Venda que zera a posição a remove da carteira, preservando as movimentações e o resultado acumulado | `[P]` |
| RN-P05 | Rentabilidade não realizada = (cotação atual − preço médio) × quantidade | `[P]` |
| RN-P06 | Não há saldo em dinheiro. Não existe erro de "saldo insuficiente" | `[D]` |
| RN-P07 | Toda compra e toda venda gera exatamente um registro de movimentação | `[P]` |
| RN-P08 | ~~Movimentação registrada não é editada nem excluída~~ **REMOVIDA** — lançamento é editável/excluível com recálculo do preço médio | `[D]` |

---

## 8. Dados externos

Esta seção define **de onde vem cada dado e o que é gratuito**. Os números que sustentam essas decisões estão no Apêndice A.

### 8.1. Fontes que entram

| Dado | Fonte | Gratuito |
|---|---|---|
| Dados cadastrais por CNPJ | API pública de consulta de CNPJ | Sim |
| Endereço por CEP | ViaCEP ou equivalente | Sim |
| Autorização de corretora na CVM | Dados Abertos da CVM (ver 8.2) | Sim |
| Cotação de ação brasileira | brapi.dev | Sim, com cota mensal |
| Cotação de ação americana | Twelve Data | Sim, com cota diária |
| Câmbio (fora do MVP) | Fonte pública gratuita | Sim |

**Alpha Vantage foi descartada.** O plano gratuito caiu para 25 requisições por dia, o que é pouco até para desenvolver e quase garante falha na demonstração. Twelve Data resolve o mesmo problema com folga muito maior.

### 8.2. A validação na CVM

O Portal de Dados Abertos da CVM publica os dados cadastrais de participantes intermediários — corretoras entre eles — referentes ao último dia útil. **Não é uma consulta por CNPJ pela rede: é um conjunto de dados que o sistema precisa manter localmente.**

Consequências de produto:

- A verificação reflete a situação **do último dia útil**, não do instante da consulta
- A interface deve informar a data da base usada na verificação
- Uma corretora autorizada hoje de manhã pode ser recusada até a próxima atualização da base — e a RN-C04 garante que a mensagem não a acuse de irregular

`[A]` Existem APIs REST comerciais que embrulham esse mesmo dado. São pagas, portanto fora, pela regra da seção 2.

### 8.3. O que ficou de fora: proventos

`[D]` **Dividendos e JCP saem do produto.** O dado só existe em plano pago.

**O que se perde:** a rentabilidade exibida mede apenas a variação de preço. Uma ação pode ficar parada no preço o ano inteiro e ainda ter distribuído dinheiro aos acionistas — e o sistema não vai mostrar isso.

Para a persona primária, que conhece o conceito, essa é uma limitação real. `[P]` **Ela precisa aparecer na interface como aviso explícito**, não ficar implícita num número menor do que deveria.

`[A]` Se aparecer fonte gratuita que cubra **os dois mercados**, a decisão pode ser revista. Fonte gratuita para só um mercado não serve — criaria carteiras com regras de cálculo diferentes.

### 8.4. Consumo de fontes externas

`[P]` As cotas gratuitas são limitadas, então o produto assume que:

- A cotação exibida em tela **não é ao vivo**, e isso é comunicado ao usuário pelo horário de obtenção (RN-Q01)
- O momento da compra ou venda é a única situação em que vale buscar preço novo (RN-Q02)
- Estourar a cota é cenário previsto, com mensagem própria, não erro inesperado (RN-Q05)

---

## 9. Modelo conceitual de domínio

Entidades e relações do problema. **Não é modelagem de banco** — cardinalidades, chaves e persistência são assunto da especificação.

```
Investidor  1 ──── N  Carteira
                        │  pertence a um mercado (BR ou US)
                        │  associada a uma Corretora
                        │
                        ├── N  Posicao ──── 1  Acao
                        │        quantidade e preço médio
                        │
                        └── N  Movimentacao ── 1  Acao
                                 compra ou venda, quantidade,
                                 preço unitário, momento
```

**Notas conceituais**

- **Ação e corretora são catálogos globais**, consequência direta da unicidade de ticker e CNPJ exigida pelo enunciado. Nenhuma das duas pertence a um investidor.
- **A movimentação é a fonte da verdade; a posição é o agregado.** Quantidade e preço médio derivam do histórico de movimentações. Como isso se resolve tecnicamente é decisão da spec.
- **A cotação pertence à ação**, não à posição. Duas carteiras com PETR4 olham o mesmo preço.

---

## 10. Restrições herdadas do enunciado

Não são escolhas de produto nem decisões suas — são condições dadas pelo trabalho. Ficam registradas para rastreabilidade e passam à especificação sem discussão.

| Restrição | Origem |
|---|---|
| Java com Spring Boot | RNF01 |
| Arquitetura em camadas | RNF02 |
| H2 e PostgreSQL — `[A]` confirmar com o professor se é via perfis distintos | RNF03 |
| Respostas em JSON | RNF04 |
| Tratamento de erro centralizado | RNF06 |
| Padrão de projeto que isole o serviço de terceiro | Item 15 |
| Mínimo de três integrações externas reais | Item 14 |
| Tratar falhas: fonte fora do ar, ticker inexistente, CNPJ inválido, CEP inexistente, cota excedida | Item 14 |

---

## 11. Métricas de sucesso `[A]`

Nenhuma métrica foi decidida. Propostas:

**Acadêmicas**

- Todas as funcionalidades mínimas do enunciado funcionando na apresentação
- Os cinco cenários de falha do item 14 demonstráveis ao vivo
- Cobertura de teste nos pontos de integração

**De produto**

- Percentual de investidores que criam uma segunda carteira
- Percentual de carteiras ainda consultadas trinta dias depois

---

## 12. Riscos

| Risco | Impacto | Mitigação |
|---|---|---|
| **Ingestão da base da CVM mais trabalhosa que o previsto** | Alto — a validação de corretora é obrigatória | Começar o projeto por ela. É o caminho crítico |
| **Base da CVM desatualizada recusa corretora legítima** | Médio | RN-C04, mais exibição da data da base |
| **Cota da fonte americana estourada antes da apresentação** | Médio | Twelve Data em vez de Alpha Vantage, mais persistência da cotação |
| **Cota da fonte brasileira estourada** | Médio | Reaproveitamento de cotação entre investidores |
| **Escopo do MVP maior que o prazo** | Alto | Plano de corte da seção 5.4 |
| **Associação corretora-ação lida como propriedade** | Médio — erro silencioso de cálculo | RN-A04 explícita e teste com dois investidores no mesmo ticker |
| **Ausência de proventos frustra a persona primária** | Baixo | Aviso explícito na interface |

---

## 13. Decisões em aberto

1. **Operações de ação precisam entrar no MVP** — não estavam na sua lista, mas o enunciado exige (seção 5.2)
2. **Mercado americano no MVP** — idem, obrigatório
3. **H2 e PostgreSQL ao mesmo tempo?** Confirmar com o professor
4. **Fonte gratuita de proventos para os dois mercados** — se existir, a decisão de corte pode ser revista
5. **Métricas de sucesso** — nada definido
6. **Atraso das cotações no plano gratuito de cada fonte** — verificar e documentar, como o item 9 do enunciado pede

---

## 14. Rastreabilidade com o enunciado

| Requisito | Onde está | No MVP? |
|---|---|---|
| RF01 — Cadastrar corretora por CNPJ | 6.2 | Sim |
| RF02 — Dados cadastrais em fonte externa | 6.2 | Sim |
| RF03 — Validar autorização em fonte pública | 6.2, 8.2, RN-C03, RN-C04 | Sim |
| RF04 — Endereço pelo CEP | 6.2, RN-C06 | Sim |
| RF05 — Listar corretoras | 5.2 | Sim |
| RF06 — Buscar corretora por id e CNPJ | 5.2 | Sim |
| RF07 — Cadastrar ação com ticker e mercado | 6.3 | Sim |
| RF08 — Consultar cotação em fonte externa | 6.3, RN-Q01 | Sim |
| RF09 — Listar ações | 5.2 | Sim |
| RF10 — Buscar ação por ticker | 5.2 | Sim |
| RF11 — Atualizar cotação | RN-Q02 | Sim |
| RF12 — Impedir duplicidade | RN-C05, RN-A01 | Sim |
| Diferencial — carteiras | 6.4 | Sim |
| Diferencial — autenticação | 6.1 | Sim |
| Diferencial — cache de consultas externas | 8.4 | Sim |
| Diferencial — histórico de cotações | — | Não |
| Item 15 — isolamento de serviço externo | Seção 10 | Sim |

---

## Apêndice A — Limites das fontes gratuitas

Levantamento de 21/08/2026. **Limites de API mudam com frequência — confirme na página oficial antes de fechar a especificação.**

| Fonte | Plano gratuito | Observação |
|---|---|---|
| brapi.dev | 15.000 requisições por mês, 1 ticker por chamada | Planos pagos aumentam a cota, reduzem o atraso e incluem dividendos |
| Twelve Data | 800 créditos por dia, 8 por minuto | Cobre ações americanas, câmbio e cripto no plano Basic |
| Alpha Vantage | 25 requisições por dia, 5 por minuto | **Descartada.** O limite já foi 500, depois 100 |
| ViaCEP / BrasilAPI | Sem chave, sem cota publicada | — |
| Dados Abertos da CVM | Conjunto público, sem cota | Arquivo atualizado com base no último dia útil |

**Pendências para a especificação**

- Confirmar o atraso das cotações no plano gratuito de cada fonte. As referências divergem, e o item 9 do enunciado exige documentar essa limitação
- Confirmar qual API de CNPJ será usada e se exige chave
- Confirmar formato, endereço e frequência de atualização do conjunto de dados da CVM

---

## Apêndice B — Alternativas descartadas

Registro do que **já foi considerado e rejeitado**. Não são lacunas nem oportunidades de melhoria: são decisões fechadas. Repropor qualquer uma delas é retrabalho.

| Decisão tomada | Alternativa descartada | Motivo |
|---|---|---|
| Carteira simulada | Registro de operações reais do investidor | Traria custódia, preço médio fiscal e apuração de IR — outro produto |
| Sem saldo em dinheiro | Conta com depósito, saldo e erro de saldo insuficiente | O modelo de acompanhamento resolve o mesmo problema com muito menos regra |
| Preço vem da fonte externa no ato da operação | Investidor digita o preço de entrada | O enunciado proíbe cadastrar dado fictício sem integração |
| Sem backtest | Simular compra em data passada | Exige série histórica e é outra funcionalidade, com outro custo |
| Carteiras separadas por mercado | Consolidar tudo em reais, ou exibir dois totais lado a lado | Evita colocar câmbio dentro do cálculo de rentabilidade |
| Bloquear corretora reprovada | Salvar marcada como "não validada" | Escolha do grupo entre as duas opções que a RN03 permite |
| Twelve Data para o mercado americano | Alpha Vantage | 25 requisições por dia inviabiliza desenvolvimento e demonstração |
| Proventos fora do produto | Proventos numa fase posterior | O dado só existe em plano pago, e a regra de gratuidade prevalece |
| Público primário: investidor experiente | Iniciante que nunca investiu | Define a densidade da interface e o vocabulário do produto |
| Ação como catálogo global | Ação por investidor | Consequência da unicidade de ticker exigida pelo enunciado |

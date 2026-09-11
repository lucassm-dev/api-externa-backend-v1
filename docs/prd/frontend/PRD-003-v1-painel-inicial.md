# PRD-003 v1 — Painel inicial

**Status:** proposto · **Data:** 08/09/2026 · **Depende de:** PRD-002 · **Relacionado:** ADR-005, ADR-006

## Problema

Depois de entrar, o investidor precisa saber em cinco segundos duas coisas: **como estão minhas carteiras** e **o que faço agora**. Sem essa tela, ele cai direto numa lista de cadastros e o produto parece um CRUD, não uma ferramenta de acompanhamento.

## Quem usa

Todo investidor logado, em toda sessão. É a tela mais vista do produto.

## Histórias

**H-003.1 — Ver o retrato do dia.** Como investidor, quero ver logo ao entrar quanto tenho investido, quanto vale hoje e qual o resultado, somando todas as minhas carteiras.

**H-003.2 — Ver o mercado.** Como investidor, quero ver os principais índices, o dólar e alguns ativos de referência assim que entro, para me situar antes de olhar minha carteira.

**H-003.3 — Entrar direto numa carteira.** Como investidor com várias carteiras, quero ver cada uma resumida e ir para a que me interessa em um clique.

**H-003.4 — Ser conduzido no primeiro acesso.** Como investidor recém-cadastrado, quero que a tela me diga qual é o próximo passo, em vez de me mostrar zeros.

## Comportamento esperado

A tela tem quatro blocos, nesta ordem de prioridade visual:

**1. Barra de mercado.** Faixa no topo com índices e câmbio: símbolo, preço e variação percentual, com o horário da última atualização. É um dado de contexto, não do investidor — aparece igual para todo mundo. Quando uma fonte falha, o item some da barra e um aviso discreto explica que parte dos dados não está disponível; a barra nunca derruba a tela.

`[D]` **A barra se atualiza a cada trinta minutos, e não a cada quinze.** Diferente da cotação das ações do investidor, cada item da barra custa uma consulta própria à fonte externa, e a cota gratuita é a mesma que as compras e vendas usam. Trinta minutos mantêm a barra dentro de 19% da cota mensal; quinze consumiriam quase tudo e deixariam as operações sem consulta.

**Consequência para a tela:** o horário exibido na barra vai passar dos quinze minutos com frequência — é o comportamento normal dela, não uma falha. A marcação visual de dado desatualizado descrita no PRD-009 **não se aplica à barra**: ela vale para cotação de ativo e taxa de câmbio usadas em decisão, não para o dado de contexto do topo da tela. Marcar a barra de laranja a maior parte do tempo treinaria o investidor a ignorar exatamente o sinal que precisa funcionar na tela de desempenho.

A barra mostra o horário; quem quiser saber a idade do dado, lê ali.

**2. Resumo consolidado — de uma carteira por vez.** `[D]` Valor investido, valor de mercado e resultado não realizado **de uma carteira selecionada**, não da soma de todas. Um seletor no topo do bloco escolhe qual, e a escolha é lembrada entre visitas.

A decisão acompanha o backend, que consolida por carteira e não tem um total geral do investidor. Somar no frontend produziria um número que ninguém consegue conferir contra nenhuma tela do sistema.

Os valores em dólar são convertidos para real, com a taxa de câmbio usada e o horário dela visíveis (ver ADR-004). Resultado positivo e negativo têm tratamento visual distinto — e não apenas por cor, porque o produto tem tema claro e escuro.

**3. Carteiras.** Um cartão por carteira: nome, corretora, valor de mercado, resultado, e atalho para abrir. `[D]` Ordenadas por **ordem de cadastro, a mais recente primeiro** — é a única ordem que o servidor consegue aplicar de verdade (valor de mercado é calculado no momento da leitura, não é um campo armazenado) e é estável: a carteira não muda de lugar sozinha quando o preço oscila.

> A carteira não guarda data de criação; a ordem de cadastro é a ordem do identificador. Se um dia o produto precisar exibir "criada em", isso é campo novo no backend.

**4. Últimas movimentações.** `[D]` As **cinco** operações mais recentes do investidor, com atalho para o extrato completo. Cinco linhas cabem sem empurrar as carteiras para fora da primeira dobra; quem quer mais vai ao extrato.

### Primeiro acesso

Investidor sem nenhuma corretora vê, no lugar dos blocos 2, 3 e 4, um único caminho: "Comece cadastrando a corretora onde você investe" com o botão que leva ao cadastro de corretora. O bloco 1 aparece normalmente — dá vida à tela e não depende de dados do investidor.

Investidor com corretora mas sem carteira: o próximo passo vira "crie sua primeira carteira".
Investidor com carteira mas sem operação: "registre sua primeira compra".

A tela sempre mostra exatamente **um** próximo passo, nunca uma lista de pendências.

## Estados

| Situação | O que aparece |
|---|---|
| Sem corretora | Convite a cadastrar corretora, ocupando o corpo da tela |
| Sem carteira | Convite a criar carteira |
| Com carteira, sem operação | Carteiras listadas zeradas + convite a registrar a primeira compra |
| Carregando | Esqueleto dos blocos, não spinner de tela inteira |
| Câmbio indisponível | Consolidado aparece com a última taxa conhecida + aviso do horário dela |
| Barra de mercado parcial | Itens que vieram aparecem; aviso discreto nomeando os que não |
| Barra com mais de 15 minutos | Situação normal — mostra o horário, sem marcação de desatualizado |

## Critérios de aceite

- O consolidado exibido é o de uma carteira selecionada, do investidor logado, e nunca inclui dado de outro investidor
- A carteira selecionada no consolidado é lembrada entre visitas
- Toda cotação e toda taxa de câmbio na tela aparece acompanhada do horário em que foi obtida
- Falha em qualquer fonte externa degrada o bloco afetado, nunca a tela
- A barra exibe o horário da atualização, mas nunca a marcação visual de dado desatualizado
- Investidor sem dados vê um convite acionável, nunca uma tabela vazia sem explicação
- Cada carteira listada abre a tela da carteira em um clique

## Fora do escopo do v1

Personalização dos blocos, widgets configuráveis, notícias, alertas de preço, comparação com benchmark.

## Perguntas em aberto

Nenhuma.

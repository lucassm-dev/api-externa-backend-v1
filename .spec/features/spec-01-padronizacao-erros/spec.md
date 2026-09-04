# Spec: SPEC-01 — Padronização e tratamento de erros

> feature: spec-01-padronizacao-erros
> status: rascunho

## Contexto

Hoje o tratamento de erro é ad-hoc: quatro exceções sem código interno
(`ResourceNotFoundException`, `DuplicateResourceException`, `BusinessException`,
`ExternalServiceException`), sem distinção entre "cota de API externa estourada"
e "API externa fora do ar" (ambas viram 502), e sem um catálogo de códigos que
o consumidor da API possa usar para tratar erro por tipo. Esta é a base para
todas as specs seguintes, que vão depender de um payload de erro previsível.

## Histórias

### US-412 — Payload de erro padronizado para exceções de negócio conhecidas

Como consumidor da API (frontend ou integração), quero que toda exceção de
negócio conhecida gere uma resposta no mesmo formato, para que eu possa tratar
erro de forma genérica sem depender de parsing de mensagem.

#### AC-426 — Exceção de negócio conhecida retorna payload padronizado

- **Dado** uma chamada que dispara uma exceção de negócio conhecida (recurso
  não encontrado, recurso duplicado, regra violada ou integração externa)
- **Quando** a API responde
- **Então** o corpo traz `timestamp`, `status`, `codigo` (ex.: `ACA-002`),
  `error` (categoria em português), `message` (mensagem final ao usuário) e
  `path` — e o status HTTP corresponde ao tipo da exceção

#### AC-427 — Erro de validação de payload lista os campos inválidos

- **Dado** uma requisição com corpo inválido (ex.: `POST /acoes` sem ticker)
- **Quando** a validação de bean falha
- **Então** a resposta é 400 com `fieldErrors` listando cada campo e a
  mensagem de validação correspondente

#### AC-428 — Nenhuma resposta de erro contém stacktrace ou detalhe interno

- **Dado** qualquer exceção, mapeada ou não mapeada (bug inesperado)
- **Quando** a API responde
- **Então** o corpo nunca contém stacktrace, nome de classe interna ou
  mensagem de exceção não traduzida; exceção não mapeada vira 500 genérico
  com código `SYS-001`, logada com stacktrace completo apenas no servidor

### US-413 — Diferenciação entre cota externa estourada e fonte externa indisponível

Como consumidor da API, quero saber se um erro de fonte de cotação é "tente
de novo mais tarde por cota" ou "a fonte caiu agora", para agir diferente em
cada caso (esperar vs avisar o usuário).

#### AC-429 — Cota estourada da fonte de cotação retorna 429

- **Dado** que a fonte de cotação (brapi ou Twelve Data) responde HTTP 429
  (limite de requisições excedido)
- **Quando** a aplicação tenta buscar cotação
- **Então** a exceção resultante mapeia para 429, com código `EXT-009` e
  mensagem explícita de limite de requisições

#### AC-430 — Fonte de cotação indisponível (não é cota) retorna 502/503, nunca 500

- **Dado** que a fonte de cotação está fora do ar (timeout, 5xx genérico)
- **Quando** a aplicação tenta buscar cotação
- **Então** a exceção resultante mapeia para 503, com código `EXT-010`, e a
  API nunca retorna 500 para essa causa

#### AC-431 — `PUT /acoes/{id}/atualizar-cotacao` com cota estourada retorna 429 explícito

- **Dado** uma ação já cadastrada e a fonte de cotação com cota estourada
- **Quando** chamo `PUT /acoes/{id}/atualizar-cotacao`
- **Então** a API responde 429 com código `EXT-009` — não silencia o erro
  devolvendo a cotação antiga sem avisar

#### AC-432 — `PUT /acoes/{id}/atualizar-cotacao` com fonte indisponível preserva o comportamento atual (AC-210)

- **Dado** uma ação já cadastrada e a fonte de cotação indisponível (não é
  cota, é queda mesmo)
- **Quando** chamo `PUT /acoes/{id}/atualizar-cotacao`
- **Então** a API responde 200 com a última cotação conhecida — este
  comportamento já existia (RN-Q05/AC-210 de `catalogo-acao`) e **não muda**

### US-414 — Catálogo de códigos internos de erro por domínio

Como desenvolvedor consumindo a API (frontend), quero um catálogo documentado
de códigos de erro por domínio, para tratar cada erro por código em vez de
depender do texto da mensagem.

#### AC-433 — Toda resposta de erro de negócio traz um código do catálogo

- **Dado** qualquer exceção de negócio mapeada (`NegocioException` e
  subtipos)
- **Quando** a API responde
- **Então** o campo `codigo` do payload existe em `docs/erros.md`, com
  prefixo do domínio correto (`COR-`, `ACA-`, `CAR-`, `OPE-`, `AUT-`, `EXT-`,
  `VAL-` para validação, `SYS-` para erro inesperado)

#### AC-434 — Falha de infraestrutura ao verificar CVM não é confundida com "não autorizada"

- **Dado** que a fonte CVM (Dados Abertos CVM / BrasilAPI) está indisponível
  durante o cadastro de uma corretora (não é que ela respondeu "não
  autorizada" — é que não respondeu)
- **Quando** chamo `POST /corretoras`
- **Então** a API responde 503 com código `EXT-007` — distinto do 422
  `COR-003` usado quando a CVM responde e diz que a corretora não está
  autorizada. **Decisão confirmada com o Lucas em 2026-09-04**: separa os
  dois casos (antes, ambos caíam em 422 genérico)

## Fora de escopo

- 401/403 e qualquer handler de autenticação/autorização — não há Spring
  Security configurado ainda; entra na SPEC-02
- Fallback de compra/venda usando última cotação conhecida quando a cota
  estoura (Q-MAP-06) — fica para a SPEC-07, junto do cache de cotação, por
  decisão já registrada em `docs/DECISOES-MAPEAMENTO.md`
- Qualquer novo código de erro para features ainda não implementadas
  (ex.: `OPE-` para preço editável da SPEC-04) — o catálogo cresce spec a
  spec, não é escrito de uma vez

## Suposições

| ID | Suposição | Status | Resolução |
|---|---|---|---|
| ASM-408 | Exceção não mapeada (bug/erro de programação) retorna 500 genérico com código `SYS-001` e mensagem fixa em português, sem nenhum detalhe da exceção original — só o log do servidor tem o stacktrace | confirmada | Decorre direto de AC-428 (nunca vazar stacktrace); nenhum critério do prompt original cobria esse caminho, mas é exigido para o critério "nenhuma resposta com stacktrace" valer para qualquer erro, não só os mapeados |
| ASM-409 | Erro de validação (`400`) usa um único código genérico `VAL-001` — o detalhe fica em `fieldErrors`, não em códigos por campo | confirmada | Simplicidade: um erro de validação já é auto-descritivo pela lista de campos; criar um código por campo inválido seria abstração sem uso real |
| ASM-410 | Mensagem de 429 não informa o horário exato de renovação da cota — nem a brapi nem a Twelve Data expõem esse dado no corpo do erro | confirmada | O texto de AC-431/Q-MAP-06 original mencionava "informando quando a cota renova"; sem esse dado na fonte, a mensagem fica genérica ("tente novamente mais tarde") |

## Perguntas em aberto

Nenhuma — a única decisão de produto pendente (status do erro de CVM
indisponível) foi respondida durante a especificação (ver AC-434).

# Spec: Cadastro corretora

> feature: cadastro-corretora
> status: rascunho

## Contexto

O investidor cadastra uma corretora informando **apenas o CNPJ**. O sistema
preenche o resto a partir de fontes públicas: dados cadastrais da empresa
(Receita/equivalente), endereço pelo CEP e verificação de autorização na CVM.
Corretora não autorizada não é cadastrada. O catálogo de corretoras é **global**
(único por CNPJ, não por investidor). Cobre RF01–RF06 e RF12 do enunciado.

## Histórias

### US-101 — Cadastrar corretora a partir do CNPJ

Como investidor, quero cadastrar uma corretora só com o CNPJ, para que o sistema
busque e valide os dados oficiais sem eu digitá-los.

#### AC-101 — CNPJ mal formatado é rejeitado antes de qualquer chamada externa (RN-C01)

- **Dado** um CNPJ inválido no formato
- **Quando** tento cadastrar a corretora
- **Então** o cadastro é recusado imediatamente e **nenhuma** fonte externa é consultada

#### AC-102 — CNPJ válido busca dados cadastrais e endereço e salva (RF02, RF04, RN-C06)

- **Dado** um CNPJ válido, existente na Receita, de instituição autorizada na CVM e com CEP consultável
- **Quando** cadastro a corretora
- **Então** o sistema preenche razão social, nome fantasia e endereço (via CEP) a partir das fontes e persiste a corretora

#### AC-103 — CNPJ não encontrado na Receita impede o cadastro (RN-C02)

- **Dado** um CNPJ bem formatado que não existe na base da Receita
- **Quando** tento cadastrar
- **Então** o cadastro é recusado e a mensagem informa que o CNPJ não foi encontrado

#### AC-104 — CNPJ duplicado é impedido (RN-C05, RF12)

- **Dado** que já existe uma corretora cadastrada com um CNPJ
- **Quando** tento cadastrar outra com o mesmo CNPJ
- **Então** o cadastro é recusado por duplicidade

### US-102 — Validação na CVM

Como investidor, quero que só corretoras autorizadas na CVM sejam cadastradas,
para que eu não registre uma instituição irregular.

#### AC-105 — Corretora não autorizada na CVM não é cadastrada (RN-C03)

- **Dado** um CNPJ válido cuja instituição **não consta como autorizada** na base da CVM
- **Quando** tento cadastrar
- **Então** o cadastro é recusado e a mensagem informa que a corretora não é autorizada

#### AC-106 — Falha ao verificar a CVM não vira "reprovada" (RN-C04)

- **Dado** que a verificação na base da CVM **não pôde ser realizada** (base indisponível/incompleta)
- **Quando** tento cadastrar
- **Então** o cadastro é recusado, mas a mensagem diz que a **verificação falhou** — não que a corretora foi reprovada

#### AC-107 — A resposta informa a data da base da CVM usada (8.2)

- **Dado** um cadastro que passou pela verificação da CVM
- **Quando** o resultado é retornado
- **Então** ele inclui a data da base da CVM (último dia útil) que embasou a verificação

### US-103 — Listar e buscar corretoras

Como investidor, quero listar e localizar corretoras já cadastradas, para
consultá-las e associá-las a carteiras.

#### AC-108 — Listar corretoras (RF05)

- **Dado** que existem corretoras cadastradas
- **Quando** solicito a listagem
- **Então** recebo as corretoras de forma paginada

#### AC-109 — Buscar corretora por id (RF06)

- **Dado** uma corretora cadastrada com um id
- **Quando** busco por esse id
- **Então** recebo os dados dela; id inexistente retorna "não encontrado"

#### AC-110 — Buscar corretora por CNPJ (RF06)

- **Dado** uma corretora cadastrada
- **Quando** busco pelo CNPJ dela
- **Então** recebo os dados dela; CNPJ não cadastrado retorna "não encontrado"

### US-104 — Resiliência das fontes externas (item 14)

Como investidor, quero uma mensagem clara quando uma fonte externa falha, para
não confundir indisponibilidade com dado inválido.

#### AC-111 — Fonte externa fora do ar é tratada, não quebra a aplicação

- **Dado** que uma das fontes (Receita/CEP/CVM) está indisponível durante o cadastro
- **Quando** tento cadastrar
- **Então** recebo um erro tratado e específico da falha, e a corretora não é salva pela metade

## Fora de escopo

- Edição/atualização dos dados de uma corretora já cadastrada
- Preenchimento manual de campos oficiais sem validação (proibido por RN02 do enunciado)
- Exclusão de corretora
- Associação corretora↔carteira (fica na feature carteira)

## Suposições

| ID | Suposição | Status | Resolução |
|---|---|---|---|
| ASM-101 | Fonte de dados por CNPJ: BrasilAPI, sem chave | confirmada | Decisão do dono do produto: BrasilAPI (gratuita, sem token) |
| ASM-102 | Fonte de CEP: ViaCEP | aberta | — |
| ASM-103 | Verificação da CVM via dataset "Participantes Intermediários: Informação Cadastral" (cad_intermed.zip, dados.cvm.gov.br/dados/INTERMED/CAD/DADOS/), atualização diária/último dia útil, ingerido localmente | confirmada | Dataset oficial localizado; encoding/separador a confirmar na implementação |
| ASM-104 | O cliente de cada fonte externa é isolado atrás de um Facade/Adapter (Item 15 do enunciado) | aberta | — |
| ASM-105 | A base da CVM é ingerida na subida da app (se vazia/desatualizada) e reimportada 1x/dia por tarefa agendada (@Scheduled) | confirmada | Decisão do dono do produto: subida + job diário |

## Perguntas em aberto

| ID | Pergunta | Status | Resposta |
|---|---|---|---|
| Q-101 | Qual API de CNPJ exatamente, e exige chave? (pendência do Apêndice A do PRD) | respondida | BrasilAPI, sem chave |
| Q-102 | Formato, endereço e frequência de atualização do dataset da CVM? (pendência do Apêndice A) | respondida | Dataset "Participantes Intermediários" (cad_intermed.zip em dados.cvm.gov.br/dados/INTERMED/CAD/DADOS/), CSV zipado, atualização diária (último dia útil) |
| Q-103 | Como/quando a base local da CVM é atualizada — job agendado, carga manual, na subida da app? | respondida | Na subida da app + job diário agendado |
| Q-104 | O campo de e-mail/telefone da corretora vem de alguma fonte ou fica vazio? | respondida | Vêm da BrasilAPI quando disponíveis; nulos quando não (nunca digitados) |

# Spec: Refatoração CNPJ no Facade + DELETE nas features

> feature: refactor-cnpj-delete
> status: pronta

## Contexto

A lógica de normalização e validação de CNPJ está embutida em `CorretoraService`,
que é responsabilidade do facade de integração. Além disso, os recursos Corretora,
Investidor e Ação não possuem endpoint DELETE, tornando impossível remover registros
pela API. A exclusão segue o padrão já estabelecido pela Carteira: exclusão lógica
(campo `ativo`).

## Histórias

### US-408 — Validação de CNPJ encapsulada no CnpjFacade

Como desenvolvedor, quero que a normalização e validação de CNPJ estejam no
`CnpjFacade`, para que a `CorretoraService` não carregue responsabilidade de
formato e os testes de validação não dependam do serviço completo.

#### AC-415 — CNPJ formatado é normalizado para 14 dígitos

- **Dado** um CNPJ no formato `00.000.000/0000-00`
- **Quando** `CnpjFacade.normalizar(cnpj)` é chamado
- **Então** retorna uma string com exatamente 14 dígitos, sem pontuação

#### AC-416 — CNPJ com dígitos verificadores válidos passa a validação

- **Dado** um CNPJ de 14 dígitos com dígitos verificadores corretos
- **Quando** `CnpjFacade.validar(cnpj)` é chamado
- **Então** nenhuma exceção é lançada

#### AC-417 — CNPJ com dígitos verificadores inválidos é rejeitado

- **Dado** um CNPJ de 14 dígitos com dígitos verificadores errados
- **Quando** `CnpjFacade.validar(cnpj)` é chamado
- **Então** uma `BusinessException` é lançada com mensagem indicando dígitos inválidos

#### AC-418 — CNPJ com comprimento diferente de 14 dígitos é rejeitado

- **Dado** uma string de dígitos com tamanho ≠ 14
- **Quando** `CnpjFacade.validar(cnpj)` é chamado
- **Então** uma `BusinessException` é lançada com mensagem indicando formato inválido

#### AC-419 — CorretoraService delega validação ao CnpjFacade sem duplicar lógica

- **Dado** que `CnpjFacade.validar()` lança exceção para CNPJ inválido
- **Quando** `CorretoraService.cadastrar()` é chamado com esse CNPJ
- **Então** a exceção propaga sem que `CorretoraService` reimplemente a lógica de dígito

---

### US-409 — Exclusão lógica de Corretora

Como operador do sistema, quero excluir logicamente uma corretora pelo ID,
para que ela deixe de aparecer nas listagens sem perder o histórico.

#### AC-420 — Corretora ativa é desativada e some das listagens

- **Dado** uma corretora cadastrada e ativa
- **Quando** `DELETE /corretoras/{id}` é chamado
- **Então** a API retorna 204 e a corretora não aparece em `GET /corretoras`

#### AC-421 — DELETE em corretora inexistente ou inativa retorna 404

- **Dado** que o ID não existe ou a corretora já está inativa
- **Quando** `DELETE /corretoras/{id}` é chamado
- **Então** a API retorna 404

---

### US-410 — Exclusão lógica de Investidor

Como operador do sistema, quero excluir logicamente um investidor pelo ID,
para que ele deixe de aparecer nas listagens sem perder o histórico.

#### AC-422 — Investidor ativo é desativado e some das listagens

- **Dado** um investidor cadastrado e ativo
- **Quando** `DELETE /investidores/{id}` é chamado
- **Então** a API retorna 204 e o investidor não aparece em `GET /investidores`

#### AC-423 — DELETE em investidor inexistente ou inativo retorna 404

- **Dado** que o ID não existe ou o investidor já está inativo
- **Quando** `DELETE /investidores/{id}` é chamado
- **Então** a API retorna 404

---

### US-411 — Exclusão lógica de Ação

Como operador do sistema, quero excluir logicamente uma ação pelo ticker,
para que ela deixe de aparecer no catálogo sem perder o histórico.

#### AC-424 — Ação ativa é desativada e some do catálogo

- **Dado** uma ação cadastrada e ativa
- **Quando** `DELETE /acoes/{ticker}` é chamado
- **Então** a API retorna 204 e a ação não aparece em `GET /acoes`

#### AC-425 — DELETE em ação inexistente ou inativa retorna 404

- **Dado** que o ticker não existe ou a ação já está inativa
- **Quando** `DELETE /acoes/{ticker}` é chamado
- **Então** a API retorna 404

---

## Fora de escopo

- Reativar registros excluídos logicamente
- Exclusão em cascata (carteiras de um investidor excluído permanecem)
- Endpoint DELETE para Carteira e Operação (já existem)

## Suposições

| ID | Suposição | Status | Resolução |
|---|---|---|---|
| ASM-405 | Exclusão lógica usa campo `ativo BOOLEAN DEFAULT TRUE` adicionado via migration | confirmada | Usuário confirmou padrão de exclusão lógica |
| ASM-406 | `GET /corretoras`, `GET /investidores` e `GET /acoes` filtram apenas registros ativos | aberta | — |
| ASM-407 | `DELETE /acoes/{ticker}` usa o ticker como identificador (não ID numérico) | aberta | — |

## Perguntas em aberto

| ID | Pergunta | Status | Resposta |
|---|---|---|---|
| Q-404 | As listagens (`GET`) já filtram por `ativo = true` ou precisamos adicionar esse filtro? | aberta | — |

# Spec: SPEC-11 — CORS para o frontend Angular

> feature: spec-11-cors
> status: pronta

## Contexto

O frontend Angular roda em outra origem (`http://localhost:4200`) e é bloqueado
pelo navegador ao chamar a API em `http://localhost:8080`, porque o backend não
declara nenhuma política de CORS. Hoje o desenvolvimento só funciona com o proxy
do Angular CLI, que mascara o problema fazendo o navegador enxergar tudo na
mesma origem — o que quebra assim que o frontend for publicado num endereço real.

Esta feature declara a política de CORS sem afrouxar nada da autenticação.

## Histórias

### US-436 — Consumir a API a partir do frontend em outra origem

Como investidor usando o frontend Angular publicado em outra origem, quero que o
navegador permita as chamadas à API, para que o sistema funcione sem depender de
um proxy de desenvolvimento.

#### AC-510 — Consulta prévia do navegador passa sem token

- **Dado** uma rota que exige autenticação e uma origem permitida
- **Quando** o navegador faz a consulta prévia (requisição `OPTIONS` de
  preflight) **sem token**
- **Então** a resposta é de sucesso e traz os cabeçalhos de permissão de CORS,
  em vez de 401

#### AC-511 — A resposta autoriza explicitamente a origem que chamou

- **Dado** uma requisição vinda de uma origem permitida
- **Quando** ela chega a qualquer rota da API
- **Então** a resposta traz `Access-Control-Allow-Origin` com aquela origem

#### AC-512 — Origem não listada não recebe permissão

- **Dado** uma requisição vinda de uma origem que não está na lista
- **Quando** ela chega à API
- **Então** a resposta não traz cabeçalho de permissão para aquela origem, e o
  navegador bloqueia a leitura

#### AC-513 — Renomear carteira é permitido entre origens

- **Dado** uma origem permitida
- **Quando** o navegador consulta previamente se pode usar o método `PATCH`
  (usado em `PATCH /carteiras/{id}`, a renomeação de carteira)
- **Então** a resposta lista `PATCH` entre os métodos permitidos

#### AC-514 — A permissão de origem não abre rota protegida

- **Dado** uma origem permitida
- **Quando** uma requisição real chega **sem token** a uma rota que exige
  autenticação
- **Então** ela continua sendo recusada com 401 e código `AUT-005`, exatamente
  como antes desta feature

#### AC-515 — A lista de origens vem de configuração, não do código

- **Dado** uma origem definida na configuração da aplicação
- **Quando** a aplicação sobe com um valor diferente do padrão
- **Então** é esse valor configurado que passa a ser aceito, sem alteração de
  código-fonte

#### AC-516 — A API não pede credenciais entre origens

- **Dado** uma requisição vinda de uma origem permitida
- **Quando** a resposta é montada
- **Então** ela não traz `Access-Control-Allow-Credentials`, porque a
  autenticação viaja no cabeçalho `Authorization` e o sistema não usa cookie de
  sessão

## Fora de escopo

- Alterar regra de negócio, service, resource ou DTO
- Alterar as rotas públicas (`/auth/**` e Swagger) ou o comportamento do JWT
- Definir a origem de produção dentro do repositório
- Cabeçalhos expostos ao cliente além do que o padrão já expõe
- Tempo de cache da consulta prévia (`maxAge`) customizado

## Suposições

| ID | Suposição | Status | Resolução |
|---|---|---|---|
| ASM-443 | A origem de desenvolvimento é `http://localhost:4200`, porta padrão do Angular CLI | confirmada | Declarado no prompt de origem da feature (`docs/PROMPT-CORS-backend.md`) |
| ASM-444 | Desligar credenciais entre origens é seguro porque o token viaja em `Authorization` e não existe cookie de sessão | confirmada | Confirmado na configuração atual: sessão stateless, nenhum cookie emitido |
| ASM-445 | `Authorization` e `Content-Type` bastam como cabeçalhos de requisição para todas as chamadas do frontend | confirmada | Nenhum endpoint da API exige cabeçalho customizado; revisado contra os sete resources |
| ASM-446 | Não configurar tempo de cache da consulta prévia é aceitável — vale o padrão do framework | confirmada | Decisão desta spec. Se o volume de preflight incomodar em produção, vira ajuste posterior, não bloqueia a entrega |

## Perguntas em aberto

| ID | Pergunta | Status | Resposta |
|---|---|---|---|
| Q-MAP-11 | Qual é a origem do frontend em produção? | respondida | Não entra no repositório. A propriedade é lida do ambiente no deploy; o repositório só carrega o valor de desenvolvimento |
| Q-MAP-12 | A política de CORS deve valer também para as rotas públicas (`/auth/**` e Swagger)? | respondida | Sim. O login é a primeira chamada que o frontend faz — sem CORS em `/auth/**`, nada funciona. A política é única para toda a API |

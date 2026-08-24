# Trabalho de Programação em Java Spring

# Desenvolvimento de um software de gestão de ações com consumo de APIs externas

## 1. Contextualização

No mercado financeiro, sistemas de apoio à gestão de investimentos precisam integrar informações de diferentes fontes, como dados cadastrais de instituições, localização geográfica e cotações de ativos financeiros em tempo real ou quase em tempo real.

Neste trabalho, os alunos deverão desenvolver uma aplicação utilizando Java com Spring Boot para simular um software de gestão de ações, permitindo o cadastro e a consulta de corretoras e ativos financeiros, com integração a APIs públicas para validação e obtenção de dados externos.

## 2. Objetivo geral

Desenvolver uma API REST em Java Spring Boot para gerenciar corretoras e ações financeiras, realizando integração com serviços externos para validação e enriquecimento de dados.

## 3. Objetivos específicos

**Ao final do trabalho, o aluno deverá ser capaz de:**

- desenvolver uma aplicação em camadas com Spring Boot;
- consumir APIs externas utilizando cliente HTTP;
- validar dados de entrada com regras de negócio;
- persistirinformações em banco de dados relacional;
- tratar exceções e respostas de erro;
- documentar endpoints da aplicação;
- aplicar boas práticas de organização de código.

## 4. Descrição do sistema

**O sistema deverá permitir o gerenciamento de:**

### 4.1. Corretoras

O usuário poderá cadastrar corretoras de valores mobiliários, sendo necessário:

- informar o CNPJ da instituição;
- buscar automaticamente, por meio de uma API pública, os dados cadastrais básicos da empresa na Receita Federal ou serviço equivalente;
- validar se a instituição informada corresponde a uma entidade autorizada/compatível com atuação no mercado financeiro, com base em consulta à CVM ou fonte pública equivalente;
- informar ou consultar o CEP da corretora por meio de API pública;
- armazenar os dados validados no sistema.

### 4.2. Ações

O usuário poderá cadastrar ativos financeiros com base em seu ticker.

O sistema deverá:

- permitir o cadastro de ações brasileiras e americanas;
- identificar o mercado do ativo;
- consultar a cotação do papel em API apropriada, conforme o ticker informado;
- armazenar informações básicas do ativo, como:
    - ticker;
    - nome da empresa, quando disponível;
    - mercado de origem; 
    - moeda;
    - cotação atual ou mais recente;
    - data/hora da cotação obtida.

## 5. Requisitos funcionais

O sistema deverá contemplar, no
mínimo, os seguintes requisitos:

- RF01 - Cadastrar corretora a partir do CNPJ.
- RF02 - Consultar automaticamente os dados cadastrais da corretora em API externa.
- RF03 - Validar se a corretora é uma instituição compatível com registro/autorização no mercado financeiro por meio de consulta a fonte pública.
- RF04 - Consultar e preencher dados de endereço a partir do CEP informado.
- RF05 - Listar corretoras cadastradas.
- RF06 - Buscar corretora por id e por CNPJ. 
- RF07 - Cadastrar ação informando ticker e mercado.
- RF08 - Consultar cotação da ação em API externa apropriada.
- RF09 - Listar ações cadastradas.
- RF10 - Buscar ação por ticker.
- RF11 - Atualizar a cotação de uma ação já cadastrada.
- RF12 - Impedir cadastro duplicado de corretora por CNPJ e de ação por ticker.

## 6. Requisitos não funcionais

- RNF01 - A aplicação deverá ser desenvolvida em Java com Spring Boot.
- RNF02 - O projeto deverá seguir arquitetura em camadas, contendo, no mínimo:
    - controller;
    - service;
    - repository;
    - model/entity;
    - dto.
- RNF03 - O banco de dados deverá ser H2 e PostgreSQL.
- RNF04 - As APIs deverão retornar respostas em formato JSON.
- RNF05 - O código deverá apresentar organização, legibilidade e padronização.
- RNF06 - O tratamento de erros deve ser implementado de forma centralizada.

## 7. Regras de negócio

- RN01 - Uma corretora só poderá ser cadastrada se o CNPJ for válido no formato e existir na base consultada.
- RN02 - cadastro da corretora deverá registrar os dados vindos da consulta externa, não sendo permitido preencher manualmente campos principais sem tentativa de validação.
- RN03 - Caso a instituição não seja identificada como participante válida do mercado financeiro conforme critério adotado no trabalho, o sistema deverá impedir o cadastro ou marcar explicitamente a corretora como “não validada”, conforme decisão do grupo.
- RN04 - O CEP informado deverá ser validado em API pública antes do salvamento.
- RN05 - Uma ação só poderá ser cadastrada se o ticker existir na API de cotação correspondente.
- RN06 - O sistema deverá distinguir ativos brasileiros e americanos, direcionando a busca para a API adequada.
- RN07 - Não será permitido cadastrar duas ações com o mesmo ticker.

## 8. Sugestão de entidades

**Corretora**

- id
- cnpj
- razaoSocial
- nomeFantasia
- email
- telefone
- cep
- logradouro
- numero
- complemento
- bairro
- cidade
- uf
- situacaoCadastral
- validadaNaCvm
- dataCadastro

**Acao**

- id
- ticker
- nomeEmpresa
- mercado
- moeda
- cotacaoAtual
- dataHoraCotacao
- corretoraRelacionada (opcional, se desejarem vincular)

## 9. APIs sugeridas

Os grupos poderão utilizar APIs públicas ou gratuitas para fins acadêmicos. 

**Exemplos:**

- API pública para consulta de CNPJ
- API pública para consulta de CEP
- API pública para validação de instituição ligada ao mercado financeiro
- API de cotação de ações brasileiras
- API de cotação de ações americanas

**Observação:** cada grupo deverá documentar quais APIs utilizou, justificando sua escolha e apontando limitações, como autenticação, limite de requisições e disponibilidade.

## 10. Endpoints mínimos esperados

**Corretoras:**

- POST /corretoras
- GET /corretoras
- GET /corretoras/{id}
- GET /corretoras/cnpj/{cnpj}

**Ações:**

- POST /acoes
- GET /acoes
- GET /acoes/{id}
- GET /acoes/ticker/{ticker}
- PUT /acoes/{id}/atualizar-cotacao

## 11. Diferenciais

**Serão considerados diferenciais:**

- uso de Feign Client ou WebClient;
- testes unitários e/ou de integração;
- paginação nas listagens;
- logs estruturados;
- cache para consultas externas;
- dashboard simples com HTML/Thymeleaf ou frontend separado;
- associação entre corretoras e carteiras de ações;
- histórico de cotações;
- autenticação com Spring Security.

## 12. Entregáveis

**Cada aluno deverá entregar:**

- código-fonte completo;
- arquivo README com documentação explicando como e quais apis foram utilizadas;
- documentação das APIs externas utilizadas;
- coleção de testes no Postman ou Insomnia;
- diagrama simplificado das entidades;
- apresentação prática do sistema em funcionamento.

## 14. Restrições

- Não será permitido cadastrar dados completamente fictícios sem integração com APIs externas.
- O sistema deve demonstrar claramente pelo menos três integrações externas reais.

O grupo deverá tratar cenários de falha, como:

- API fora do ar;
- ticker inexistente;
- CNPJ inválido;
- CEP inexistente;
- limite de requisições excedido.

## 15. Objetivo Extra

Pensar na aplicação de padrão de projetos de forma que promova o isolamento do serviço de terceiro (API) dentro do sistema.

### 15.1. -  APIs Externas
- brapi.dev - ações brasileiras
- alphavantage.co - ações bolsa americana
- twelve data - ações bolsa americana
- brasil api - diversas apis para dados brasileiros
- viacep - cep das cidades brasileiras


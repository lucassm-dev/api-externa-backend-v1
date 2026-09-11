# Prompt — habilitar CORS no backend

Prompt para colar numa sessão do Claude Code aberta na raiz do backend
(`api-externa-backend-v1`). Escrito em 08/09/2026, quando o frontend Angular
começou e esbarrou no bloqueio do navegador.

---

Preciso habilitar CORS neste backend Spring Boot. Hoje ele não tem nenhuma
configuração de CORS, e o frontend Angular rodando em `http://localhost:4200`
é bloqueado pelo navegador ao chamar a API em `http://localhost:8080`.

Use a skill `onp-spec-driven` para isso — quero spec, tarefas e testes, não
uma edição solta.

## Contexto do código

- A configuração de segurança está em
  `src/main/java/com/apiexternabackend/config/SecurityConfig.java`, com
  `SecurityFilterChain`, CSRF desabilitado, sessão stateless e um
  `JwtAuthenticationFilter` registrado antes do
  `UsernamePasswordAuthenticationFilter`.
- Rotas públicas hoje: `/auth/**` e as do Swagger. Todo o resto exige token.
- A autenticação é por JWT no cabeçalho `Authorization`. **Não há cookie de
  sessão**, então a API não precisa de credenciais entre origens.
- Os testes de segurança existentes estão em
  `src/test/java/com/apiexternabackend/config/SecurityIntegrationTest.java`.

## O que precisa acontecer

1. **Origens vêm de configuração, não do código.** Uma propriedade
   (ex.: `cors.allowed-origins`) lida pelo `SecurityConfig`, com valor de
   desenvolvimento em `application-dev.properties`. Origem de produção não
   entra no código nem no repositório.

2. **Nada de `*` na lista de origens.** A API devolve dados financeiros de
   investidor autenticado; a lista é explícita.

3. **Métodos liberados: GET, POST, PUT, PATCH, DELETE, OPTIONS.** O `PATCH`
   é obrigatório — é o que renomeia carteira (`PATCH /carteiras/{id}`). Já vi
   configuração de CORS quebrar só por esquecer ele.

4. **Cabeçalhos de requisição: `Authorization` e `Content-Type`**, no mínimo.

5. **`allowCredentials` fica desligado.** O token vai no cabeçalho, não em
   cookie. Ligar isso sem necessidade impede o uso de curinga e amplia a
   superfície à toa. Se você concluir que precisa ligar, me explique por quê
   antes de fazer.

6. **A requisição de preflight (`OPTIONS`) precisa passar sem token.** Confirme
   que o `JwtAuthenticationFilter` e as regras de autorização não a barram —
   um preflight que responde 401 quebra todas as chamadas do frontend, e o
   erro que aparece no navegador não diz nada sobre autenticação.

## Testes que quero junto

Em `SecurityIntegrationTest`, ou num teste novo ao lado dele:

- Preflight `OPTIONS` numa rota autenticada, vindo da origem permitida, **sem
  token**, responde sucesso e traz os cabeçalhos de CORS
- Requisição real vinda da origem permitida traz o cabeçalho
  `Access-Control-Allow-Origin` com aquela origem
- Requisição vinda de origem **não** listada não recebe o cabeçalho de
  permissão
- Os testes de segurança que já existem continuam passando — CORS não pode
  abrir rota que era autenticada

## Limites

- Não mexa em regra de negócio, service, resource ou DTO. Isto é só
  configuração de segurança e teste.
- Não mude as rotas públicas nem o comportamento do JWT.
- Ao terminar, rode `./mvnw test` e me mostre a saída antes de dizer que
  funcionou.
- Depois de tudo passando, rode `graphify update .`.

## Como vou validar

Vou subir o Angular em `localhost:4200` e fazer login. Se o preflight passar
e o `POST /auth/login` responder, está resolvido.

# CORS para frontend local

## Objetivo

Permitir que o frontend Angular em `http://localhost:4200` consuma a API Spring Boot durante o desenvolvimento local.

## Implementação

Adicionar uma configuração MVC global em `com.apiexternabackend.config.CorsConfig` que implementa `WebMvcConfigurer`.

- Aplicar a política a todas as rotas (`/**`).
- Permitir exclusivamente a origem `http://localhost:4200`.
- Permitir os métodos `GET`, `POST`, `PUT`, `PATCH`, `DELETE` e `OPTIONS`.
- Aceitar os cabeçalhos da requisição (`*`).
- Não habilitar credenciais, pois a aplicação não usa Spring Security ou autenticação baseada em cookies.

## Fora de escopo

- URLs de produção e parametrização por perfil.
- Spring Security e autenticação.
- Alterações nos controllers.

## Verificação

Compilar e executar os testes Maven existentes para validar que a configuração é descoberta pelo Spring e não introduz regressões.

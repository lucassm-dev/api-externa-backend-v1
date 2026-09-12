# Imagem do backend: Maven compila, JRE roda. A imagem final não carrega Maven
# nem código-fonte — só o jar e uma JRE.

FROM maven:3.9-eclipse-temurin-21 AS construcao

WORKDIR /app

# O pom sozinho primeiro: enquanto as dependências não mudarem, esta camada vem
# do cache e o build não baixa o repositório inteiro de novo.
COPY pom.xml ./
RUN mvn -B -q dependency:go-offline

COPY src ./src
# Os testes rodam no CI (.github/workflows/ci.yml), com banco de verdade. Repetir
# aqui exigiria subir Postgres dentro do build da imagem.
RUN mvn -B -q clean package -DskipTests


FROM eclipse-temurin:21-jre-alpine AS producao

# Processo de aplicação não precisa ser root.
RUN addgroup -S spring && adduser -S spring -G spring

WORKDIR /app
COPY --from=construcao --chown=spring:spring /app/target/*.jar app.jar

USER spring
EXPOSE 8080

# /v3/api-docs é rota pública (SecurityConfig.ROTAS_PUBLICAS): serve de sinal de
# vida sem precisar de actuator nem de token.
#
# Baixa o corpo e joga fora, em vez de --spider: --spider fecha a conexão depois
# do cabeçalho, e o Spring — no meio da escrita do documento — registra um
# AsyncRequestNotUsableException a cada sondagem. Log limpo vale os poucos KB.
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=5 \
  CMD wget --quiet --tries=1 -O /dev/null http://localhost:8080/v3/api-docs || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]

# api-externa-backend-v1

Backend Spring (Java) para integração com API externa.

## Stack
- Java / Spring Boot
- Maven ou Gradle (definir)

## Estrutura do projeto
- `src/main/java` — código da aplicação
- `src/main/resources` — configs (`application.yml`)
- `src/test/java` — testes
- `docs/prd/` — PRDs e especificações de features

## Convenções
- (preencher: pacotes, camadas, padrões de nomenclatura)

## Comandos
- Build: `./mvnw clean install` (ou `./gradlew build`)
- Rodar: `./mvnw spring-boot:run`
- Testes: `./mvnw test`

## graphify

This project has a knowledge graph at graphify-out/ with god nodes, community structure, and cross-file relationships.

Rules:
- For codebase questions, first run `graphify query "<question>"` when graphify-out/graph.json exists. Use `graphify path "<A>" "<B>"` for relationships and `graphify explain "<concept>"` for focused concepts. These return a scoped subgraph, usually much smaller than GRAPH_REPORT.md or raw grep output.
- If graphify-out/wiki/index.md exists, use it for broad navigation instead of raw source browsing.
- Read graphify-out/GRAPH_REPORT.md only for broad architecture review or when query/path/explain do not surface enough context.
- After modifying code, run `graphify update .` to keep the graph current (AST-only, no API cost).

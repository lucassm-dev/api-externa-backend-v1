# Spec: Fuso de Brasília na aplicação

> feature: spec-31-fuso-brasilia
> status: implementada

## Contexto

Issue #61, levantada na #36. A aplicação grava datas com `LocalDateTime.now()`,
no fuso da JVM. Se o container rodar em UTC, extrato, operações e relatório de
picos ficam 3 horas à frente do horário de Brasília.

Decisões do dono do produto (2026-11-22):

1. O fuso fica na imagem, não no servidor: o `ENTRYPOINT` do `Dockerfile`
   sobe a JVM com `-Duser.timezone=America/Sao_Paulo`. A JVM traz a própria
   base de fusos, então a imagem alpine não precisa de tzdata.
2. O deploy no homelab nunca ficou no ar, então não há dado gravado a ajustar.

## Histórias

### US-481 — Horários no fuso de Brasília em qualquer servidor

Como investidor, quero ver os horários das minhas operações no horário de
Brasília, para que o extrato bata com o que eu fiz, seja qual for o fuso do
servidor.

#### AC-666 — A imagem roda no horário de Brasília

- **Dado** a imagem de produção gerada pelo `Dockerfile`
- **Quando** o container sobe a aplicação
- **Então** a JVM roda no fuso `America/Sao_Paulo` (`-Duser.timezone` no `ENTRYPOINT`), seja qual for o fuso do host
- **E** o `docs/deploy.md` explica que o fuso vem da imagem

## Fora de escopo

- Configurar o fuso do servidor do homelab e o deploy em si
- Ajustar dados já gravados (não há dados de produção)
- Gravar datas com fuso (`OffsetDateTime`/`Instant`) no lugar de `LocalDateTime`
- `./mvnw spring-boot:run` local, que usa o fuso da máquina

## Suposições

| ID | Suposição | Status | Resolução |
|---|---|---|---|
| ASM-515 | O fuso é fixado na imagem com `-Duser.timezone`, não no servidor | confirmada | Dono do produto, 2026-11-22: "abordagem do fuso fora do servidor" |
| ASM-516 | Não há dados de produção a ajustar, porque o deploy no homelab não ficou no ar | confirmada | Dono do produto, 2026-11-22: "não deu certo o deploy" |

## Perguntas em aberto

| ID | Pergunta | Status | Resposta |
|---|---|---|---|
| Q-407 | O deploy no homelab entra nesta issue? | respondida | Não, fica fora do escopo por enquanto (dono do produto, 2026-11-22) |

## Documentos relacionados

[Tarefas](tasks.md) · [spec-30](../spec-30-relatorio-picos-requisicoes/spec.md) · [Guia de deploy](../../../docs/deploy.md)

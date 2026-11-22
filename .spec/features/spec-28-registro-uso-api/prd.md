<!-- onp-spec:documentation-contract=v1 -->

# PRD: Registro de uso da API por usuário

> feature: spec-28-registro-uso-api
> status: revisado

[ADR](adr.md) · [TDD](design.md) · [Spec](spec.md) · [Tarefas](tasks.md) · [Pesquisa](research.md)

## Pedido original do cliente

> Registrar as requisições à API por usuário: quando e qual tipo de operação. Não registrar corpo da requisição, senha nem token. Definir por quanto tempo os registros ficam guardados.

Fonte: issue #34 (milestone v0.7 — Relatórios do administrador), referência PRD
15.5 do [PRD do sistema](../../../docs/prd/PRD-sistema-investimentos-acoes.md).

## Problema, público e contexto

Palavras do PRD 15.5: "Picos de requisições por usuário, para controle de uso".
O relatório (#36) precisa de dados que o sistema não coleta hoje. Público: o
administrador, que lê o relatório; o investidor, cujo uso é registrado.

Interpretação da equipe: "tipo de operação" é o método HTTP somado ao padrão da
rota (ex.: `GET /carteiras/{id}/posicoes`), que diz o que foi feito sem expor os
valores enviados.

## Resultados desejados e métricas

| Resultado | Linha de base | Meta | Fonte |
|---|---|---|---|
| Requisições autenticadas com registro | 0% (nada é coletado) | 100%, salvo falha de gravação logada | Testes AC-651/AC-653 |
| Atraso causado pelo registro | — | A resposta não espera a gravação | Teste AC-654 |
| Registros com mais de 90 dias | — | 0 após o descarte diário | Teste AC-656 |

## Requisitos e prioridades

| Requisito | Palavras do cliente / origem | Prioridade | Critérios na spec |
|---|---|---|---|
| Registrar requisição por usuário: quando e o quê | Issue #34 | Alta | AC-651 |
| Não guardar corpo, senha nem token | Issue #34 | Alta | AC-652 |
| Só requisições autenticadas | Dono do produto, 2026-11-22 | Alta | AC-653 |
| Não deixar a requisição mais lenta | Critério da issue #34 | Alta | AC-654, AC-655 |
| Guardar por 90 dias, descarte diário | Dono do produto, 2026-11-22 | Alta | AC-656 |

## Escopo e exclusões

Entra: registro, gravação assíncrona e descarte. Fica de fora: o relatório e a
consulta dos registros (#36), rate limit e registro de requisições sem token.

## Regras de produto e domínios

- PRD 15.1/15.5: relatório do administrador mostra uso e situação das contas,
  nunca o conteúdo das carteiras. O registro guarda o padrão da rota, não ids
  nem filtros, então não revela conteúdo de carteira.
- [ADR-011](../../../docs/adr/011-jwt-pelo-resource-server-do-spring.md) e
  [ADR-013](../../../docs/adr/013-controle-de-acesso-por-papeis.md): a
  autenticação continua a mesma; o registro só lê o usuário já autenticado.
- Domínios: autenticação (cadeia de filtros) e auditoria administrativa
  (spec-19, mesmo cuidado de não guardar segredo).

## Dependências, riscos e suposições

- Risco: fila do executor assíncrono crescer sob pico; mitigação no [TDD](design.md#6-riscos-e-mitigação).
- Suposições ASM-504 a ASM-507 na [spec](spec.md#suposições).

## Validação com o cliente

| Pergunta | Resposta literal | Origem | Data |
|---|---|---|---|
| Por quanto tempo guardar? Registrar só autenticadas? | "90 dias, só autenticadas, pode fazer os commits" | Chat Claude Code, sessão anterior ao `/clear` | 2026-11-22 |
| Abordagem: filtro depois da autenticação, gravação assíncrona, sem corpo/query/token/IP | Aceita como ponto de partida | Mesma sessão | 2026-11-22 |

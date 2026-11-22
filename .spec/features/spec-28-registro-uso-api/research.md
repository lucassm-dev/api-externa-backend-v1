<!-- onp-spec:documentation-contract=v1 -->

# Pesquisa de produto: Registro de uso da API por usuário

> feature: spec-28-registro-uso-api
> status: revisado

[PRD](prd.md) · [ADR](adr.md) · [TDD](design.md) · [Spec](spec.md) · [Tarefas](tasks.md)

## Pergunta e hipótese

Pergunta: qual prazo de guarda e quais campos atendem ao relatório de picos sem
guardar dado pessoal além do necessário? Hipótese: método + padrão da rota +
status + usuário + data e hora bastam para o #36, e 90 dias cobrem a análise de
picos recentes. A pesquisa pode mudar o prazo e os campos.

## Concorrentes e alternativas

| Produto / alternativa | Comportamento observado | Fonte primária | Data da consulta | Limitações |
|---|---|---|---|---|
| Marco Civil da Internet, art. 15 | Provedor de aplicação com fins econômicos guarda registros de acesso (data, hora, IP) por 6 meses | [Lei 12.965/2014](https://www.planalto.gov.br/ccivil_03/_ato2011-2014/2014/lei/l12965.htm) | Texto conhecido; não reconsultado nesta sessão | Projeto acadêmico, sem fim econômico: obrigação não se aplica hoje |
| LGPD, arts. 6º (III, necessidade) e 16 | Dado pessoal é eliminado ao fim da finalidade; coleta limitada ao mínimo necessário | [Lei 13.709/2018](https://www.planalto.gov.br/ccivil_03/_ato2015-2018/2018/lei/l13709.htm) | Texto conhecido; não reconsultado nesta sessão | Não fixa prazo; o prazo é decisão do controlador |
| Fazer nada | #36 sem dados | — | — | Bloqueia a milestone v0.7 |

Não houve consulta a concorrentes comerciais: o pedido é interno (relatório do
administrador) e a decisão de prazo já foi tomada pelo dono do produto.

## Evidências e dados

Não há dado de uso real: o sistema não coleta nada hoje, e esse é o problema.
Volume e latência abaixo são estimativas, marcadas como tal.

## Viabilidade matemática e estatística

Estimativa de volume (hipótese, sem dado real): 50 usuários × 200 requisições
por dia × 90 dias = 900 mil linhas. Cada linha tem cerca de 100 bytes de dados
mais índice, ou seja, na ordem de 100–200 MB no PostgreSQL. É aceitável para o
homelab ([deploy](../../../docs/deploy.md)). Se o uso for 10 vezes maior, a
tabela passa de 1 GB e vale rever o prazo ou agregar por hora.

## Proposta e alternativas ao pedido

Recomendação: seguir com 90 dias e os campos mínimos. Guardar IP, como pede o
Marco Civil para serviço comercial, contraria o "não guardar além do necessário"
e não é preciso para o #36. Fica registrado no ADR como gatilho de revisão.

## Decisão e experimento

Decisão do dono do produto, 2026-11-22: "90 dias, só autenticadas". Validação:
os testes AC-651 a AC-656 e, depois do deploy, contar as linhas por dia para
conferir a estimativa de volume.

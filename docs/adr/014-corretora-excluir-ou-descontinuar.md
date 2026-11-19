# ADR-014: Corretora tem duas saídas — excluir cadastro errado ou descontinuar

- **Data**: 2026-11-18
- **Status**: Aceito
- **Decisores**: Lucas Mendes (dono do produto)
- **Tags**: domínio, catálogo, frontend

## Contexto e problema

Com o ADR-013, só o administrador mantém corretoras. Hoje a única saída é excluir, bloqueada se houver carteira ativa (`COR-004`, ADR-007). Surgiu a pergunta: o administrador deveria poder excluir mesmo com carteiras, depois de uma confirmação?

No mercado, corretoras deixam de existir: são liquidadas pelo Banco Central, incorporadas ou mudam de nome. As ações do investidor não são da corretora — ficam no CPF dele na B3 — e ele as transfere para outra corretora. O histórico ("comprei na corretora X") continua verdadeiro depois que X fecha.

## Fatores de decisão

- O histórico de onde cada operação aconteceu não pode desaparecer
- O administrador não pode ficar travado por uma corretora que saiu do mercado
- Cadastro errado precisa ter conserto

## Opções consideradas

- Duas ações: excluir (sem vínculo) e descontinuar (com vínculo)
- Excluir mesmo com carteiras, após confirmação
- Manter só a exclusão bloqueada

## Decisão

**Duas ações distintas.**

- **Excluir** — para cadastro errado. Continua bloqueada enquanto houver carteira vinculada (regra atual, ADR-007).
- **Descontinuar** — para corretora que saiu do mercado. Some da lista ao criar carteira; carteiras existentes seguem funcionando e mostram que a corretora foi descontinuada. Antes de confirmar, o administrador vê quantas carteiras serão afetadas. Fica registrada na auditoria (ADR-013).

Excluir com carteiras foi recusado: apagaria o histórico de onde as operações foram feitas, e nenhuma confirmação torna isso correto.

### Consequências positivas

- Histórico preservado mesmo quando a corretora acaba
- O administrador sempre tem uma ação possível
- A confirmação mostra o impacto real, em vez de um "tem certeza?" genérico

### Consequências negativas

- Mais um estado de corretora para tratar nas telas e nas consultas
- Investidor com carteira em corretora descontinuada não tem, por ora, como mover a carteira para outra — transferência fica para feature futura

## Links

- ADR-002 — catálogo compartilhado
- ADR-007 — exclusão lógica com bloqueio por vínculo (complementado por este)
- ADR-013 — administrador e auditoria
- PRD seção 15.1

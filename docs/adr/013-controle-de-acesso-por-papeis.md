# ADR-013: Controle de acesso por papéis, com dono dos dados e bloqueio imediato

- **Data**: 2026-11-18
- **Status**: Aceito
- **Decisores**: Lucas Mendes (dono do produto)
- **Tags**: segurança, autorização, lgpd, frontend

## Contexto e problema

Todo usuário autenticado recebe o mesmo papel e pode fazer tudo. Isso tem dois efeitos: o catálogo compartilhado de corretoras (ADR-002) pode ser alterado por qualquer investidor, e `GET /investidores` e `DELETE /investidores/{id}` não conferem quem chama — **qualquer investidor lista e exclui a conta dos outros**. O produto passa a ter administrador e, depois, suporte.

## Fatores de decisão

- Menor privilégio: cada papel recebe só o necessário (LGPD, art. 6º, III)
- Dado financeiro do investidor não é necessário para administrar o sistema
- Bloquear precisa ter efeito real, não só no próximo login
- Toda ação administrativa precisa deixar rastro

## Opções consideradas

- Papéis com matriz de acesso, verificação de dono e auditoria
- Papéis simples (ADMIN pode tudo, inclusive ver carteiras)
- Manter papel único

## Decisão

**Três papéis: `INVESTIDOR`, `ADMIN` e `SUPORTE` (este só no futuro).**

- **INVESTIDOR** — opera somente o que é seu (carteiras, operações, perfil). Toda consulta e alteração confere o dono; recurso de outro investidor responde como inexistente.
- **ADMIN** — mantém corretoras (ADR-014) e usuários: lista, cria ADMIN/SUPORTE, bloqueia e desbloqueia. **Não vê o conteúdo das carteiras**, só indicadores por usuário (carteiras ativas, se tem posição aberta).
- **Investidor nasce só pelo autocadastro** e nunca é promovido. O primeiro ADMIN vem das variáveis de ambiente.
- **Bloqueio**: motivo obrigatório, vale para quem tem carteira ativa, e **vale na hora** — cada requisição confere no banco se o usuário continua ativo. Ninguém bloqueia a si mesmo.
- **Auditoria**: bloqueio, desbloqueio, exclusão e descontinuação de corretora gravam quem, o quê, em quem, quando e por quê.

Recusada a opção de ADMIN ver carteiras: não serve a nenhuma tarefa dele, e quem decide se uma conta pode ser excluída é a regra do sistema, não o olho do administrador. O suporte terá acesso de leitura restrito a um atendimento aberto, decidido em ADR próprio.

### Consequências positivas

- Fecha o furo de listar e excluir conta alheia
- Catálogo de corretoras protegido
- Conta comprometida pode ser travada imediatamente

### Consequências negativas

- Uma consulta ao banco por requisição, pela chave primária
- Toda rota precisa de teste de 200, 401 e 403
- Ninguém consegue ver uma carteira para ajudar o investidor até o suporte existir

## Links

- ADR-002, ADR-011, ADR-012, ADR-014
- PRD seção 15.1
- `docs/aulas/AULA-14-CONTROLE-DE-ACESSO (1).md`

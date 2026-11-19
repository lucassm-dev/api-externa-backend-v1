# ADR-012: Identidade de acesso separada do perfil de investidor

- **Data**: 2026-11-18
- **Status**: Aceito
- **Decisores**: Lucas Mendes (dono do produto)
- **Tags**: domínio, banco-de-dados, autenticação

## Contexto e problema

Hoje a única identidade do sistema é `Investidor`: a mesma tabela guarda nome, CPF, e-mail e senha, e todo login é de um investidor. O produto passa a ter pessoas que entram no sistema **sem investir**: o administrador agora e o suporte em seguida (ADR-013). Elas precisam de e-mail, senha, papel e situação, mas não têm carteira e não fazem sentido com CPF obrigatório.

## Fatores de decisão

- O terceiro papel (suporte) já está decidido; o modelo precisa recebê-lo sem remendo
- CPF obrigatório é regra do investidor, não de quem administra
- Consultas de investidor não podem passar a devolver administradores
- Bloqueio, troca de senha e e-mail (PRD 15.2 e 15.3) são assuntos de acesso, não de perfil

## Opções consideradas

- Tabela `usuario` (acesso) com `investidor` como perfil 1:1
- Coluna `papel` na tabela `investidor`
- Não mudar (sem administrador)

## Decisão

**Separar.** `usuario` guarda tudo o que é de acesso: e-mail, hash da senha, papel, situação (ativo/bloqueado). `investidor` guarda o perfil de quem investe (nome, CPF) e aponta para o seu `usuario`. Carteiras continuam ligadas ao investidor. Administrador e suporte existem só como `usuario`.

O autocadastro cria os dois registros juntos. O e-mail e a senha atuais migram de `investidor` para `usuario` numa migração Flyway, preservando os ids.

### Consequências positivas

- O suporte entra depois criando um usuário com outro papel, sem mexer no modelo
- CPF continua obrigatório para investidor
- Listar investidores nunca devolve administrador
- Regras de acesso ficam num lugar só

### Consequências negativas

- Migração de dados agora, com risco de quebrar o login existente se mal feita
- Toda consulta que hoje parte do investidor logado passa a resolver usuário → investidor
- Unicidade de e-mail muda de tabela; as regras de reuso de e-mail após exclusão (SPEC-10) precisam ser reescritas sobre `usuario`

## Prós e contras

### `usuario` + `investidor` 1:1 ✅ Escolhida

- ✅ Cada tabela com uma responsabilidade
- ✅ Pronto para o suporte
- ❌ Migração e um join a mais

### Coluna `papel` em `investidor`

- ✅ Mudança mínima
- ❌ Administrador vira "investidor" sem CPF
- ❌ Toda consulta de investidor precisa filtrar papel

### Não mudar

- ✅ Nenhum custo
- ❌ Não existe administrador para manter o catálogo

## Links

- ADR-013 — papéis que motivam a separação
- PRD seção 15.1 a 15.3
- SPEC-10 — reuso de e-mail e CPF de conta excluída

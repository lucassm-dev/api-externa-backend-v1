# ADR-015: Ação entra no catálogo na hora da pesquisa

- **Data**: 2026-11-18
- **Status**: Aceito
- **Decisores**: Lucas Mendes (dono do produto)
- **Tags**: domínio, catálogo, dados-externos, frontend

## Contexto e problema

Hoje o investidor cadastra a ação informando ticker e mercado; o backend confere o ticker na fonte de cotação e grava. O cadastro exige carteira ativa (`ACA-004`, ADR-003) e a ação pode ser excluída por qualquer um (`ACA-003`).

Com papéis (ADR-013), a pergunta é quem cadastra ação. Nem o investidor nem o administrador agregam nada ao cadastro: o dado vem inteiro da fonte externa. O enunciado do trabalho, porém, exige "cadastrar ação informando ticker e mercado" (RF07), ticker existente na fonte (RN05) e sem duplicidade (RN07).

## Fatores de decisão

- O cadastro manual não acrescenta informação além do ticker e do mercado
- RF07, RN05 e RN07 precisam continuar atendidos
- Exigir carteira para cadastrar ação é contraintuitivo (o próprio ADR-003 reconhece)

## Opções consideradas

- Pesquisa por ticker e mercado inclui a ação no catálogo quando ela ainda não existe
- Administrador cadastra as ações
- Manter o cadastro pelo investidor

## Decisão

**A pesquisa inclui.** Ao pesquisar ticker e mercado, se a ação está no catálogo ela é devolvida; se não está, o sistema consulta a fonte do mercado (RN06), grava e devolve. Ticker inexistente na fonte não entra (RN05); duplicidade é impossível porque a pesquisa reaproveita o que já existe (RN07). Qualquer usuário autenticado pode pesquisar.

Saem: o cadastro explícito de ação, a exclusão de ação e o pré-requisito de carteira (`ACA-004`). Este ADR substitui o item 2 do ADR-003; a ordem corretora → carteira → operação continua.

### Consequências positivas

- Um passo a menos para o investidor
- Ninguém precisa de permissão para "cadastrar" algo que vem da fonte
- Catálogo nunca tem ação inventada

### Consequências negativas

- Pesquisa passa a escrever no banco — um GET com efeito colateral
- Erro de digitação de ticker válido (ex.: PETR3 no lugar de PETR4) entra no catálogo e não sai, já que ninguém exclui ação
- Cada pesquisa de ticker novo consome cota das fontes gratuitas

## Links

- ADR-003 — item 2 substituído por este
- ADR-005 — cotação como snapshot com cache
- ADR-007 — exclusão de ação deixa de existir
- Enunciado: RF07, RN05, RN06, RN07

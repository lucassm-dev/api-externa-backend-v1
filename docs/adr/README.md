# ADRs — decisões que amarram o frontend

Decisões já tomadas e implementadas no backend, registradas aqui porque cada uma
restringe o que as telas podem prometer.

| ADR | Decisão | Impacto principal no frontend |
|---|---|---|
| [001](001-sessao-por-token-de-prazo-fixo-sem-renovacao.md) | Sessão por token de prazo fixo, sem renovação | Cadastro e login são dois passos; expiração devolve ao login |
| [002](002-corretora-e-acao-como-catalogo-compartilhado.md) | Corretora e ação são catálogo compartilhado | "Corretoras" e "Ações", nunca "minhas" |
| [003](003-ordem-obrigatoria-corretora-carteira-acao.md) | Ordem obrigatória corretora → carteira → ação → operação | Painel mostra um próximo passo; cadastro de ação bloqueado sem carteira |
| [004](004-carteira-multimercado-com-consolidacao-em-moeda-unica.md) | Carteira multimercado com consolidação em moeda única | Nenhuma tela afirma um mercado por carteira; taxa e horário sempre visíveis |
| [005](005-cotacao-como-snapshot-com-cache.md) | Cotação é snapshot datado, com cache | Todo preço vem com horário; sem tempo real, sem polling |
| [006](006-degradacao-com-avisos-em-vez-de-falha.md) | Degradação com avisos em vez de falha | Três níveis: informação, aviso, erro — aviso nunca parece erro |
| [007](007-exclusao-logica-com-bloqueio-por-vinculo-ativo.md) | Exclusão lógica com bloqueio por vínculo ativo | Sem lixeira, sem desfazer; bloqueio sempre explicado |
| [008](008-preco-de-mercado-como-padrao-com-override-manual.md) | Preço de mercado por padrão, manual por opt-in | Formulário mostra preço e total antes de confirmar |
| [009](009-contrato-unico-de-erro-com-codigo-por-dominio.md) | Contrato único de erro com código por domínio | Frontend trata por código, escreve os próprios textos |
| [010](010-consultas-paginadas-sem-filtro-e-sem-serie-historica.md) | Consultas paginadas, sem filtro e sem série histórica — **filtros substituídos pelo ADR-018** | Extrato sem filtros no v1; nenhum gráfico de evolução |

Decididas em 2026-11-18, implementação pendente:

| ADR | Decisão | Impacto principal no frontend |
|---|---|---|
| [011](011-jwt-pelo-resource-server-do-spring.md) | JWT pelo resource server do Spring, token de 60 min | Login de novo a cada hora; Swagger só em dev |
| [012](012-identidade-separada-do-perfil-de-investidor.md) | Identidade (`usuario`) separada do perfil (`investidor`) | Nenhum direto; base para admin e suporte |
| [013](013-controle-de-acesso-por-papeis.md) | Papéis INVESTIDOR/ADMIN/SUPORTE, dono dos dados, bloqueio imediato | Menus por papel; indicador de papel no topo; admin sem tela de carteira |
| [014](014-corretora-excluir-ou-descontinuar.md) | Corretora: excluir cadastro errado ou descontinuar | Aviso de corretora descontinuada; confirmação mostra carteiras afetadas |
| [015](015-acao-entra-no-catalogo-pela-pesquisa.md) | Ação entra no catálogo pela pesquisa | Sem tela de cadastro de ação; pesquisa por ticker e mercado |

Decididas em 2026-11-21, implementação pendente:

| ADR | Decisão | Impacto principal no frontend |
|---|---|---|
| [016](016-envio-de-email-por-smtp-gratuito.md) | E-mail por SMTP gratuito (Gmail com senha de app), credenciais em variáveis de ambiente | Pedido de código sempre responde igual; em fluxo autenticado, falha de envio vira 503 `EXT-012` |
| [017](017-foto-de-perfil-no-banco.md) | Foto de perfil no Postgres, em tabela própria (`bytea`), até 2 MB, JPEG/PNG/WebP detectados pela assinatura | Backup continua um único `pg_dump`; banco cresce até 2 MB por usuário |
| [018](018-filtros-no-extrato-de-operacoes.md) | Extrato `GET /operacoes` com filtros no servidor (período, ticker, carteira, tipo, lucro ou prejuízo realizado); sem exportação; substitui o ADR-010 na parte de filtros | Relatórios saem do extrato; série histórica continua fora |

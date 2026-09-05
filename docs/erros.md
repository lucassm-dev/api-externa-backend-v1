# Catálogo de códigos de erro

Toda resposta de erro da API segue o mesmo formato (`StandardError`):

```json
{
  "timestamp": "2026-09-04T19:30:00Z",
  "status": 404,
  "codigo": "ACA-001",
  "error": "Not Found",
  "message": "Ação não encontrada: PETR4",
  "path": "/acoes/ticker/PETR4",
  "fieldErrors": []
}
```

`fieldErrors` só é preenchido em erro de validação (`VAL-001`, 400). Nenhuma
resposta de erro traz stacktrace ou nome de classe interna — exceção não
mapeada (bug) vira `SYS-001` (500) genérico.

## Hierarquia de exceções

```
NegocioException (abstrata)
├── RecursoNaoEncontradoException   → 404
├── RecursoDuplicadoException       → 409
├── RegraVioladaException           → 422
├── IntegracaoExternaException      → 429 (limiteExcedido=true) ou 503 (limiteExcedido=false)
└── CredenciaisInvalidasException   → 401 (login incorreto — único caso de negócio fixo em 401)
```

401/403 de autenticação/autorização (token ausente, expirado, acesso negado)
**não** passam pela hierarquia acima — são escritos direto pelo
`JwtAuthenticationEntryPoint`/`JwtAccessDeniedHandler` no filtro de
segurança, antes do Spring MVC processar a requisição (ver `AUT-005/006/007`).

Implementação: `src/main/java/com/apiexternabackend/resources/exceptions/`.

## COR — Corretora

| Código | Situação | Status HTTP | Exceção |
|---|---|---|---|
| COR-001 | Corretora não encontrada | 404 | RecursoNaoEncontradoException |
| COR-002 | Corretora já cadastrada (CNPJ duplicado) | 409 | RecursoDuplicadoException |
| COR-003 | Regra de cadastro violada: CNPJ com formato/dígitos inválidos, CNPJ não encontrado na Receita, CEP não encontrado, ou corretora não autorizada na CVM (a fonte respondeu e disse que não está OK) | 422 | RegraVioladaException |
| COR-004 | Exclusão bloqueada: existe carteira ativa vinculada à corretora | 422 | RegraVioladaException |

## ACA — Ação

| Código | Situação | Status HTTP | Exceção |
|---|---|---|---|
| ACA-001 | Ação não encontrada (inclui ação existente porém inativa — busca individual e operação nunca enxergam ação excluída) | 404 | RecursoNaoEncontradoException |
| ACA-002 | Ação já cadastrada (ticker duplicado **entre ativas** — ticker de ação excluída pode ser reutilizado) | 409 | RecursoDuplicadoException |
| ACA-003 | Exclusão bloqueada: ação tem posição ativa (quantidade > 0) em pelo menos uma carteira | 422 | RegraVioladaException |

## CAR — Carteira

| Código | Situação | Status HTTP | Exceção |
|---|---|---|---|
| CAR-001 | Carteira não encontrada (ou inativa, ou de outro investidor — SPEC-02) | 404 | RecursoNaoEncontradoException |
| CAR-002 | Exclusão bloqueada: carteira tem posição ativa (quantidade > 0) em pelo menos uma ação | 422 | RegraVioladaException |

## OPE — Operação

| Código | Situação | Status HTTP | Exceção |
|---|---|---|---|
| OPE-001 | Operação não encontrada | 404 | RecursoNaoEncontradoException |
| OPE-002 | Incompatibilidade de mercado entre carteira e ação | 422 | RegraVioladaException |
| OPE-003 | Sem posição na ação para vender | 422 | RegraVioladaException |
| OPE-004 | Quantidade de venda excede a posição atual | 422 | RegraVioladaException |
| OPE-005 | Preço unitário com mais de 2 casas decimais (BRL/USD usam 2 casas de subunidade) | 422 | RegraVioladaException |

## AUT — Investidor / autenticação

> Desde a SPEC-02, `Investidor` tem cadastro com senha (`POST /auth/cadastro`)
> e login com JWT (`POST /auth/login`). AUT-005/006/007 são escritos direto
> pelo `JwtAuthenticationEntryPoint`/`JwtAccessDeniedHandler` — não passam
> pelo `GlobalExceptionHandler` porque acontecem no filtro de segurança,
> antes do Spring MVC.

| Código | Situação | Status HTTP | Exceção |
|---|---|---|---|
| AUT-001 | E-mail já cadastrado | 409 | RecursoDuplicadoException |
| AUT-002 | CPF já cadastrado | 409 | RecursoDuplicadoException |
| AUT-003 | Investidor não encontrado | 404 | RecursoNaoEncontradoException |
| AUT-004 | Login com e-mail ou senha incorretos (mensagem genérica, não revela qual campo errou) | 401 | CredenciaisInvalidasException |
| AUT-005 | Token ausente, malformado ou com assinatura inválida | 401 | `JwtAuthenticationEntryPoint` |
| AUT-006 | Token expirado | 401 | `JwtAuthenticationEntryPoint` |
| AUT-007 | Acesso negado (autenticado, sem permissão) | 403 | `JwtAccessDeniedHandler` |
| AUT-008 | Senha fora da política mínima (8+ caracteres, com letra e número) | 422 | RegraVioladaException |

## EXT — Integração externa

| Código | Situação | Status HTTP | Exceção |
|---|---|---|---|
| EXT-007 | Falha de infraestrutura ao consultar fonte externa durante o cadastro de corretora (CNPJ/Receita, CEP/ViaCEP ou CVM indisponíveis — a fonte não respondeu, diferente de COR-003 onde ela respondeu e disse "não autorizada") | 503 | IntegracaoExternaException (limiteExcedido=false) |
| EXT-008 | Ticker não encontrado na fonte de cotação (brapi/Twelve Data) | 422 | RegraVioladaException |
| EXT-009 | Limite de requisições da fonte de cotação excedido | 429 | IntegracaoExternaException (limiteExcedido=true) |
| EXT-010 | Fonte de cotação indisponível (não é limite de cota) | 503 | IntegracaoExternaException (limiteExcedido=false) |

## VAL — Validação de payload

| Código | Situação | Status HTTP |
|---|---|---|
| VAL-001 | Corpo da requisição inválido — detalhe de cada campo em `fieldErrors` | 400 |

## SYS — Erro interno inesperado

| Código | Situação | Status HTTP |
|---|---|---|
| SYS-001 | Exceção não mapeada (bug). Mensagem genérica ao cliente; stacktrace completo só no log do servidor | 500 |

## Notas

- `EXT-007` cobre hoje CNPJ, CEP e CVM juntos porque as três falhas acontecem
  dentro do mesmo fluxo (`POST /corretoras`) e todas significam a mesma coisa
  para quem consome a API: "não deu pra completar o cadastro por falha de
  infraestrutura externa, tente de novo". Se algum consumidor precisar
  distinguir qual das três falhou, isso vira um código mais granular depois —
  não foi necessário até aqui.
- Cota de API externa estourada (`EXT-009`) nunca derruba compra/venda hoje —
  ver Fora de escopo da SPEC-01: esse fallback é da SPEC-07 (cache de
  cotação), junto da decisão Q-MAP-06.
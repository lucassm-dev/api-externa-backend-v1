# ADR-016: Envio de e-mail por SMTP gratuito

- **Data**: 2026-11-21
- **Status**: Aceito
- **Decisores**: Lucas Mendes (dono do produto)
- **Tags**: infraestrutura, e-mail, gratuidade, segurança

## Contexto e problema

Esqueci minha senha (PRD 15.2, #26) e trocar e-mail com confirmação (#28)
precisam mandar um código ao e-mail do usuário. Hoje o sistema não envia
e-mail nenhum. A escolha do serviço precisa respeitar a regra de gratuidade
(PRD seção 2): nenhuma funcionalidade depende de plano pago.

O projeto não tem domínio próprio. Sem domínio, não dá para configurar SPF e
DKIM, e isso elimina ou piora boa parte das opções.

## Fatores de decisão

- Gratuito de forma permanente, sem cartão de crédito
- Funcionar sem domínio próprio
- Chegar à caixa de entrada, e não ao spam, de quem usa Gmail e Outlook
- Volume baixo: poucos códigos por dia num trabalho acadêmico
- Trocar de serviço sem mexer no código
- Credenciais fora do Git

## Opções consideradas

Limites gratuitos conferidos em 2026-11-21:

| Opção | Limite gratuito | Sem domínio próprio | Integração |
|---|---|---|---|
| **Gmail SMTP com senha de app** | 500 e-mails/dia (conta pessoal) | Funciona: remetente `@gmail.com` assinado pelo próprio Google | SMTP |
| **Brevo** | 300 e-mails/dia, sem cartão; rodapé "Sent with Brevo" | Só com remetente avulso verificado; um `From: @gmail.com` enviado por servidor de terceiros falha no DMARC e tende a cair no spam | SMTP ou API |
| **Resend** | 100 e-mails/dia e 3.000/mês; ao estourar, o envio pausa sem cobrança | Não serve: sem domínio verificado só envia para o próprio dono da conta | API ou SMTP |

## Decisão

**Gmail SMTP com senha de app, pelo `spring-boot-starter-mail`.**

- O backend fala SMTP genérico (host, porta, usuário, senha, remetente). O
  Gmail é só a configuração atual. Trocar para Brevo, ou para outro
  provedor quando houver domínio, é trocar variáveis de ambiente, sem mudar
  o código.
- **Credenciais**: `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`,
  `MAIL_PASSWORD` (a senha de app, nunca a senha da conta) e `MAIL_FROM`
  ficam em variáveis de ambiente, com o `.env` fora do Git e exemplos vazios
  no `.env.example`, como o `JWT_SECRET` e o `ADMIN_SENHA`. A conta é
  dedicada ao sistema, não uma conta pessoal.
- **Limite**: 500 por dia é muito acima do uso esperado. Para proteger a
  cota e evitar abuso, cada funcionalidade que envia código limita os
  pedidos por e-mail num intervalo curto (o número exato fica com a spec de
  cada uma).
- **Quando o envio falha** (Gmail fora, cota estourada, credencial errada):
  - No **esqueci minha senha**, a resposta continua igual à de sucesso,
    porque responder diferente revelaria que o e-mail tem conta (PRD 15.2).
    A falha vai para o log, sem o código e sem o endereço completo.
  - Em fluxos **autenticados** (trocar e-mail), a falha responde
    `503 EXT-012` ("serviço de e-mail indisponível, tente mais tarde") e
    nenhuma alteração pendente é gravada.
  - Nos dois casos o código não é considerado enviado, e o usuário pode
    pedir de novo.
- **Testes** usam um servidor SMTP em memória (GreenMail). Nenhum teste
  envia e-mail de verdade.

Recusada a Resend: sem domínio verificado ela não entrega para terceiros.
Recusada a Brevo como padrão: sem domínio, o remetente `@gmail.com` passando
por outro servidor falha no DMARC, e o plano gratuito ainda coloca o
rodapé da marca. Ela continua sendo a alternativa documentada se o Gmail
bloquear a conta.

### Consequências positivas

- Custo zero, sem cartão, e funciona hoje, sem domínio
- Melhor chance de cair na caixa de entrada entre as opções sem domínio,
  porque o e-mail sai assinado pelo próprio Google
- Código preso ao SMTP, não a um provedor: a troca é só configuração
- Testes sem rede e sem credencial

### Consequências negativas

- O remetente é um endereço `@gmail.com`, sem a marca do produto
- O Google pode suspender o envio da conta por até 24 horas se suspeitar de
  abuso, e nesse período nenhum código sai
- A senha de app depende de verificação em duas etapas na conta e é revogada
  se a senha da conta mudar
- O envio é síncrono: um SMTP lento atrasa a resposta do pedido

## Links

- PRD seção 2 (gratuidade) e 15.2 (esqueci minha senha)
- Issues #25 (este ADR), #26 (esqueci minha senha) e #28 (trocar e-mail)
- [Resend: cotas e limites da conta](https://resend.com/docs/knowledge-base/account-quotas-and-limits)
- [Brevo: limites do plano gratuito](https://help.brevo.com/hc/en-us/articles/208580669)
- [Google: limites de envio do Gmail](https://support.google.com/a/answer/166852)

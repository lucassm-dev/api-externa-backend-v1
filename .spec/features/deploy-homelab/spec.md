# Spec: Deploy no homelab

> feature: deploy-homelab
> status: em-implementacao

## Contexto

O sistema (banco, API e frontend) sobe hoje por `docker compose up --build`, mas
o compose foi pensado para desenvolvimento: publica o PostgreSQL (5432) e a API
(8080) em todas as interfaces, e a API roda com o perfil `dev` (`show-sql=true`).

O objetivo é manter a aplicação no ar 24/7 num servidor Ubuntu x64 de casa
(i3 de 2ª geração, 8 GB de RAM, HD de 1 TB), acessível só pela tailnet do
Tailscale, e treinar deploy. Os dois repositórios (públicos) são clonados lado a
lado no servidor e as imagens são construídas lá mesmo. O deploy é disparado à
mão por SSH, com um script que já serve para um GitHub Action no futuro.

Decisões tomadas no brainstorming:

- configuração de produção como override (`docker-compose.prod.yml`) sobre o
  compose base, que continua intacto para desenvolvimento;
- acesso por `tailscale serve` na porta do frontend, sem exposição à internet;
- backup diário do banco por `pg_dump`, com retenção de 7 dias, no próprio HD.

## Histórias

### US-438 — Configuração de produção separada da de desenvolvimento

Como dono do homelab, quero que o sistema em produção só exponha o frontend e
rode com configuração enxuta, para que o banco e a API não fiquem acessíveis na
rede e o servidor modesto não se esgote.

#### AC-521 — Banco sem porta publicada em produção

- **Dado** o compose base mesclado com `docker-compose.prod.yml`
- **Quando** a definição do serviço `postgres` é lida
- **Então** ela não publica nenhuma porta no host

#### AC-522 — Só o frontend publica porta em produção

- **Dado** o compose base mesclado com `docker-compose.prod.yml`
- **Quando** as portas publicadas de todos os serviços são lidas
- **Então** `backend` não publica nenhuma porta e `frontend` é o único serviço
  com porta publicada

#### AC-523 — API em perfil de produção e com memória limitada

- **Dado** o override de produção
- **Quando** a definição do serviço `backend` é lida
- **Então** ela define `SPRING_PROFILES_ACTIVE=prod` e um limite de memória
  para o container

#### AC-524 — Log de container com rotação

- **Dado** o override de produção
- **Quando** a configuração de log de `postgres`, `backend` e `frontend` é lida
- **Então** cada um usa o driver `json-file` com `max-size` e `max-file`
  definidos

#### AC-525 — Perfil de produção não despeja SQL no log

- **Dado** o arquivo `application-prod.properties`
- **Quando** a propriedade `spring.jpa.show-sql` é lida
- **Então** o valor é `false`

### US-439 — Deploy por um único comando

Como dono do homelab, quero rodar um script que atualiza os dois repositórios,
reconstrói e sobe o sistema, e me diz com código de saída se deu certo, para
fazer deploy à mão hoje e por GitHub Action amanhã.

#### AC-526 — Sem `.env` o deploy nem começa

- **Dado** o diretório do backend sem arquivo `.env`
- **Quando** `deploy/deploy.sh` é executado
- **Então** ele sai com código diferente de 0, informa a falta do `.env` e não
  chama `git` nem `docker compose up`

#### AC-527 — Mudança local não commitada aborta o deploy

- **Dado** um dos dois repositórios com alteração não commitada
- **Quando** `deploy/deploy.sh` é executado
- **Então** ele sai com código diferente de 0, informa qual repositório está
  sujo e não descarta a alteração

#### AC-528 — Docker Compose antigo aborta o deploy

- **Dado** um Docker Compose com versão anterior a 2.24 (sem suporte a `!reset`)
- **Quando** `deploy/deploy.sh` é executado
- **Então** ele sai com código diferente de 0 e informa a versão mínima exigida

#### AC-529 — Caminho feliz atualiza e sobe com o override

- **Dado** pré-checagens satisfeitas e serviços que ficam saudáveis
- **Quando** `deploy/deploy.sh` é executado sem variáveis extras
- **Então** os dois repositórios são alinhados a `origin/main`, o compose é
  chamado com `docker-compose.yml` e `docker-compose.prod.yml` e `--build`, o
  script imprime o commit implantado de cada repositório e sai com código 0

#### AC-530 — Branch escolhida por variável

- **Dado** a variável `BRANCH=develop`
- **Quando** `deploy/deploy.sh` é executado
- **Então** os dois repositórios são alinhados a `origin/develop`

#### AC-531 — Serviço que não fica saudável falha o deploy

- **Dado** um serviço que não chega a `healthy` dentro do prazo de espera
- **Quando** `deploy/deploy.sh` é executado
- **Então** ele sai com código diferente de 0 e imprime o log recente do
  serviço doente

#### AC-532 — Verificação final pelo nginx falha o deploy

- **Dado** serviços saudáveis, mas `/saude` sem resposta 200 ou uma rota da API
  respondendo 502 pelo nginx
- **Quando** `deploy/deploy.sh` é executado
- **Então** ele sai com código diferente de 0

#### AC-533 — Funciona chamado de qualquer diretório

- **Dado** o shell posicionado fora do repositório
- **Quando** `deploy/deploy.sh` é chamado pelo caminho absoluto
- **Então** ele encontra o `.env`, os arquivos de compose e o repositório do
  frontend como se tivesse sido chamado de dentro do backend

### US-440 — Backup diário do banco

Como dono do homelab, quero um dump diário do banco com retenção curta, para
recuperar os dados se algo der errado sem lotar o HD.

#### AC-534 — Backup gera dump compactado e datado

- **Dado** o banco em execução
- **Quando** `deploy/backup.sh` é executado
- **Então** surge no diretório de backups um arquivo `.sql.gz` com a data e a
  hora no nome, com o conteúdo produzido pelo `pg_dump`

#### AC-535 — Dumps com mais de 7 dias são removidos

- **Dado** dumps no diretório de backups com 8 dias e com 1 dia de idade
- **Quando** `deploy/backup.sh` é executado
- **Então** o dump de 8 dias é apagado e o de 1 dia é mantido

#### AC-536 — Falha do dump não deixa arquivo enganoso

- **Dado** um `pg_dump` que termina com erro
- **Quando** `deploy/backup.sh` é executado
- **Então** ele sai com código diferente de 0 e nenhum arquivo novo fica no
  diretório de backups

## Fora de escopo

- Exposição pública (Tailscale Funnel, domínio próprio, Cloudflare Tunnel)
- GitHub Action de deploy (o script fica pronto para ele, o workflow não)
- Registry de imagens e build multi-arquitetura
- Rollback automático — o rollback é manual e documentado
- Cópia do backup para fora do servidor
- Mudanças no frontend
- Monitoramento e alertas

## Suposições

| ID | Suposição | Status | Resolução |
|---|---|---|---|
| ASM-450 | O Docker Compose do servidor é 2.24 ou mais novo | aberta | Confirmar com `docker compose version` no preparo do servidor; o `deploy.sh` aborta se não for (AC-528) |
| ASM-451 | O que só existe no servidor — primeiro deploy real, `tailscale serve`, volta após reboot e cron do backup — é validado por checklist manual em `docs/deploy.md`, fora dos critérios de aceite | confirmada | Decisão do usuário: JUnit para o que é verificável no repositório, checklist para o que depende do servidor |
| ASM-452 | Os scripts são testados com `docker`, `git` e `curl` falsos no `PATH`; os testes exigem `bash` no ambiente | confirmada | macOS e o runner `ubuntu-latest` do CI têm `bash` |
| ASM-453 | 768 MB de limite para o container da API bastam | aberta | Conferir consumo real com `docker stats` após o primeiro deploy |
| ASM-454 | Migrations do Flyway só andam para frente: rollback de código não reverte o banco | confirmada | Registrado no guia de rollback; reverter dados é restaurar backup |
| ASM-455 | A API sobe no container sem arquivo `.env` (o `spring-dotenv` não exige o arquivo) | confirmada | O compose atual já roda assim, com segredos só por variável de ambiente |

## Perguntas em aberto

| ID | Pergunta | Status | Resposta |
|---|---|---|---|
| Q-DEP-001 | Como o código chega ao servidor? | respondida | Git clone dos dois repositórios e build no próprio servidor |
| Q-DEP-002 | Quem acessa o sistema? | respondida | Só dispositivos da tailnet, via `tailscale serve` |
| Q-DEP-003 | Como o deploy é disparado? | respondida | Manual por SSH agora; script pronto para GitHub Action depois |
| Q-DEP-004 | Backup entra no escopo? | respondida | Sim, diário, retenção de 7 dias, sem cópia externa |
| Q-DEP-005 | Os repositórios são públicos? | respondida | Sim — clone por HTTPS, sem deploy key |

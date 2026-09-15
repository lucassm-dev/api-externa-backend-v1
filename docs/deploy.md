# Guia de deploy — homelab

Este guia cobre o que fica **fora dos critérios de aceite** (ASM-451): tudo
que só existe no servidor real — preparo inicial, primeiro deploy, acesso pela
tailnet, cron do backup e o que fazer quando algo dá errado. O comportamento
dos scripts (`deploy/deploy.sh`, `deploy/backup.sh`) e do
`docker-compose.prod.yml` está coberto por teste em
`.spec/features/deploy-homelab/spec.md` — aqui é o checklist manual.

Alvo: Ubuntu 22.04+ x64, i3 de 2ª geração, 8 GB de RAM, HD de 1 TB, acessível
só pela tailnet do Tailscale.

## 1. Preparo do servidor (uma vez)

- [ ] Instalar Docker Engine + Docker Compose plugin (`docker compose version`
  precisa ser **2.24 ou mais novo** — `deploy.sh` aborta se não for, AC-528).
- [ ] Habilitar o Docker no boot: `sudo systemctl enable docker`.
- [ ] Colocar o usuário de deploy no grupo `docker` (evita `sudo` em todo
  comando): `sudo usermod -aG docker $USER` e relogar.
- [ ] Instalar o Tailscale e autenticar (`sudo tailscale up`) — o servidor
  precisa aparecer na tailnet.
- [ ] Criar o diretório de trabalho: `sudo mkdir -p /opt/investimentos && sudo
  chown $USER:$USER /opt/investimentos`.
- [ ] Clonar os dois repositórios lado a lado, por HTTPS (são públicos, sem
  deploy key):
  ```bash
  cd /opt/investimentos
  git clone https://github.com/lucassm-dev/api-externa-backend-v1.git
  git clone https://github.com/lucassm-dev/api-externa-frontend-v2.git
  ```
- [ ] Criar o `.env` do backend a partir do `.env.example`, com segredos
  gerados (não reaproveitar os de desenvolvimento):
  ```bash
  cd api-externa-backend-v1
  cp .env.example .env
  # JWT_SECRET: openssl rand -base64 48
  # POSTGRES_PASSWORD: openssl rand -base64 24
  # FRONTEND_PATH=../api-externa-frontend-v2   (repos lado a lado no servidor)
  chmod 600 .env
  ```
  Sem esse arquivo o deploy nem começa (AC-526). O `deploy.sh` já passa o
  caminho do frontend ao compose (AC-538), mas o rollback e o restore chamam o
  compose direto — por isso o `FRONTEND_PATH` do `.env` também precisa apontar
  para o repositório vizinho.

## 2. Primeiro deploy

```bash
cd /opt/investimentos/api-externa-backend-v1
deploy/deploy.sh
```

- [ ] Confirmar que os três containers (`investimentos-db`,
  `investimentos-api`, `investimentos-web`) sobem e ficam `healthy`.
- [ ] Confirmar que o script imprime o commit implantado de cada repositório e
  sai com código 0. A verificação final exige `401` em `/corretoras` pelo
  nginx (AC-539): é a prova de que o proxy alcança a API — qualquer outro
  código, inclusive `200`, falha o deploy.
- [ ] Rodar as migrações do Flyway confirmando no log da API que não há erro
  (o `SPRING_PROFILES_ACTIVE=prod` do override já reduz o log a não
  despejar SQL — AC-525).

## 3. Acesso pela tailnet

Só o frontend publica porta (AC-522); banco e API ficam só na rede interna do
compose. O acesso de fora do servidor é via `tailscale serve`:

```bash
sudo tailscale serve --bg 8081
```

- [ ] Confirmar com `tailscale serve status` que a porta 8081 está servida.
- [ ] Acessar `https://<nome-da-maquina>.<tailnet>.ts.net` de outro
  dispositivo da tailnet e ver o frontend responder.
- [ ] Confirmar que o serviço volta sozinho após reboot: `sudo systemctl
  status tailscaled` habilitado, e reexecutar `tailscale serve --bg 8081`
  (ou registrar como serviço, se preferir) — hoje é passo manual pós-boot.

### Banco e Swagger sem porta publicada

Como `postgres` e `backend` não publicam porta (AC-521, AC-522), o acesso
direto é só via `docker exec`/túnel SSH, nunca pela rede:

```bash
# psql direto no container
docker exec -it investimentos-db psql -U <POSTGRES_USER> -d <POSTGRES_DB>

# Swagger, por túnel SSH até o IP do container na rede do compose
# (o backend não publica porta no host, mas o IP do container é roteável
#  a partir do próprio servidor Linux)
IP=$(ssh <usuario>@<servidor> "docker inspect -f '{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}' investimentos-api")
ssh -L 8080:$IP:8080 <usuario>@<servidor>
# depois abra http://localhost:8080/swagger-ui.html na máquina local
```

O jeito mais simples de inspecionar o backend sem editar o compose é
`docker compose exec backend sh` ou `docker logs -f investimentos-api`.

## 4. Cron do backup

```bash
crontab -e
```

Adicionar (dump diário às 3h da manhã):

```
0 3 * * * /opt/investimentos/api-externa-backend-v1/deploy/backup.sh >> /var/log/investimentos-backup.log 2>&1
```

- [ ] Rodar `deploy/backup.sh` manualmente uma vez e confirmar que aparece um
  `.sql.gz` datado em `/opt/investimentos/backups` (AC-534).
- [ ] Confirmar, depois de alguns dias, que dumps com mais de 7 dias somem
  sozinhos (AC-535, retenção via `RETENCAO_DIAS`).
- [ ] Confirmar que o cron roda com o ambiente certo (`docker`, `git` no
  `PATH` do cron — cron tem `PATH` mínimo; se falhar, use caminho absoluto
  para os binários ou defina `PATH` no crontab).

## 5. Rollback manual

Não há rollback automático (fora de escopo). Passos:

1. No repositório afetado, `git checkout <commit-anterior>` (ou `git reset
   --hard` para o commit desejado).
2. Subir de novo com o compose de produção:
   ```bash
   docker compose -f docker-compose.yml -f docker-compose.prod.yml up -d --build
   ```
3. **O Flyway só migra para frente** (ASM-454): reverter o código não reverte
   migrações já aplicadas no banco. Se o rollback de código depende de uma
   migração anterior, restaure o backup (seção 6) em vez de tentar reverter o
   schema.

## 6. Restore de backup

```bash
# para o backend para não escrever durante o restore
docker compose -f docker-compose.yml -f docker-compose.prod.yml stop backend

gunzip -c /opt/investimentos/backups/investimentos-<data>.sql.gz | \
  docker compose -f docker-compose.yml -f docker-compose.prod.yml exec -T postgres \
  psql -U <POSTGRES_USER> -d <POSTGRES_DB>

docker compose -f docker-compose.yml -f docker-compose.prod.yml start backend
```

- [ ] Confirmar que a API volta a responder e os dados batem com o backup
  restaurado.

## 7. Variáveis usadas pelos scripts

| Variável | Script | Padrão | Para que serve |
|---|---|---|---|
| `BRANCH` | `deploy.sh` | `main` | branch alinhada em cada `git fetch/reset` |
| `FRONTEND_DIR` | `deploy.sh` | `../api-externa-frontend-v2` | onde está o repo do frontend |
| `HEALTH_TIMEOUT` / `HEALTH_INTERVAL` | `deploy.sh` | `180` / `5` (segundos) | prazo de espera por `healthy` |
| `FRONTEND_PORT` | `deploy.sh` | `8081` | porta local checada no smoke test |
| `BACKUP_DIR` | `backup.sh` | `/opt/investimentos/backups` | onde os dumps ficam |
| `RETENCAO_DIAS` | `backup.sh` | `7` | idade máxima antes de apagar o dump |

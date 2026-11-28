# ADR-017: Foto de perfil guardada no banco

- **Data**: 2026-11-21
- **Status**: Aceito
- **Decisores**: Lucas Mendes (dono do produto)
- **Tags**: armazenamento, perfil, homelab, segurança

## Contexto e problema

O perfil terá foto, que o usuário envia, troca e remove (PRD 15.3, #30).
O sistema ainda não guarda nenhum arquivo. É preciso decidir onde guardar as
imagens, qual o tamanho máximo e quais formatos aceitar.

O deploy roda no homelab com Docker Compose (`docs/deploy.md`). O único dado
persistente é o volume do Postgres, e o backup diário (`deploy/backup.sh`) é
um `pg_dump` desse banco. A regra de gratuidade (PRD seção 2) vale também
aqui.

## Fatores de decisão

- Entrar no backup e no restore que já existem, sem passo novo
- Deploy simples: um volume, um serviço de dados
- Custo zero e nada fora do homelab
- Volume baixo: uma foto por usuário, poucos usuários num trabalho acadêmico
- Não confiar no que o cliente diz sobre o arquivo

## Opções consideradas

| Opção | Backup e restore | Deploy | Dependência externa |
|---|---|---|---|
| **Banco, em tabela própria (`bytea`)** | Já coberto pelo `pg_dump` | Nada muda | Nenhuma |
| **Disco do servidor (volume Docker)** | `backup.sh` precisa copiar o volume; restore em dois passos, que podem ficar fora de sincronia com o banco | Volume novo no compose | Nenhuma |
| **Serviço externo gratuito** (Cloudinary, Supabase Storage) | Fica com o provedor | Credencial e chamada HTTP no envio | Conta, limites do plano grátis e dado fora do homelab |

## Decisão

**Guardar a foto no Postgres, em uma tabela própria.**

- **Tabela `foto_perfil`**: `usuario_id` (chave primária e estrangeira, uma
  foto por usuário), `conteudo` (`bytea`), `tipo` (o media type detectado) e
  `atualizada_em`. Fica fora de `usuario` para que as consultas de usuário,
  que rodam a cada requisição autenticada, não carreguem a imagem.
- **Tamanho máximo**: 10 MB. Acima disso, a foto é recusada.
- **Formatos**: JPEG, PNG e WebP. O formato é detectado pelos primeiros bytes
  do arquivo (a assinatura), e não pela extensão nem pelo `Content-Type`
  enviado pelo cliente. Arquivo que não bate com nenhuma das três assinaturas
  é recusado.
- **Sem processamento**: a imagem é guardada como veio, sem redimensionar nem
  comprimir. O limite de 10 MB já segura o crescimento; redimensionar fica a
  cargo do frontend, se ele quiser.
- **Entrega**: a imagem sai por um endpoint da API, com o `Content-Type`
  guardado na tabela. A forma exata (rotas e resposta do `GET /auth/me`) fica
  com a spec da #30.

Recusado o disco do servidor: o ganho de deixar o banco menor não compensa um
backup a mais e um restore que pode deixar banco e arquivos fora de sincronia.
Recusado o serviço externo: traz conta, credencial e limites de plano para
uma funcionalidade pequena, e tira o dado do homelab.

### Consequências positivas

- Backup e restore continuam um único `pg_dump`, sem mudança no deploy
- A foto some junto com o usuário na mesma transação, sem arquivo órfão
- Nenhuma dependência nova, nem biblioteca nem serviço
- Formato validado pelo conteúdo, não pelo que o cliente declara

### Consequências negativas

- O banco cresce até 10 MB por usuário com foto, e o dump cresce junto
- Cada exibição da foto passa pela API e pelo banco, sem CDN
- Sem redimensionamento, uma foto de 10 MB é entregue inteira mesmo onde
  aparece pequena
- Migrar para disco ou serviço externo depois exige mover os dados existentes

## Links

- PRD seção 2 (gratuidade) e 15.3 (perfil)
- Issues #29 (este ADR) e #30 (foto de perfil)
- `docs/deploy.md` e `deploy/backup.sh`

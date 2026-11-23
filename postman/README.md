# Coleção Postman — Bloodlink

Valida a API inteira **pelo gateway** (`http://localhost:8000`), nunca direto nos serviços.

## Importar
1. Postman → **Import** → selecione os dois arquivos desta pasta:
   - `Bloodlink.postman_collection.json`
   - `Bloodlink-Local.postman_environment.json`
2. Escolha o ambiente **Bloodlink - Local** (canto superior direito).
3. Preencha no ambiente (só estas duas; o resto o script preenche):
   - `adminEmail` e `adminSenha`: os valores `BLOODLINK_ADMIN_EMAIL` / `BLOODLINK_ADMIN_SENHA` do seu `.env`.

## Antes de rodar
- Infra no ar (Postgres, Redis, RabbitMQ, MinIO), os 6 serviços e o gateway por último.
- Migrations tenant V14–V16 aplicadas nos centros (a coleção cria um centro novo; o provisionamento roda as migrations dele).
- MailHog (http://localhost:8025) mostra os e-mails de confirmação, lembrete e alerta de estoque.

## Como rodar
Rode **pasta por pasta**, na ordem (botão direito na pasta → *Run folder*), com **delay de 300 ms** entre requisições
(o gateway limita 10 req/s por IP e 1 req/s nas rotas públicas, e responde 429 se passar).

| Pasta | O que valida |
|---|---|
| 00 Gateway | saúde; `/internal/**` não exposto |
| 01 Auth (plataforma) | login, senha errada, aba errada, refresh |
| 02 Centros | criar centro, schema, lista pública, perfil, renomear |
| 03 Equipe | cria admin/recepção/enfermeiro, logins, troca de senha |
| 04 Doadores | cadastro pela equipe, ficha, impedimentos, convite, permissões |
| 05 Autocadastro | autocadastro público, ativação, login do doador |
| 06 Área do doador | perfil, ficha, agendar/cancelar, documentos |
| 07 Agenda e check-in | agenda, fila, triagem, conclusão, intervalo mínimo |
| 08 Estoque | lotes, baixas, parâmetros, movimentações |
| 09 Reservas | reservas e alerta de estoque crítico |
| 10 Documentos | upload/listagem/download da equipe |
| 11 Painel | painel e permissões |
| 12 Esqueci minha senha | recuperação, token de uso único, sessões revogadas |
| 13 Segurança | 401/403, cabeçalhos forjados, token adulterado |
| 14 Plataforma | listagens e migrations |
| 99 Encerramento | **desativa** o centro de teste, bloqueio de autocadastro, logout |

## Passos manuais (links de e-mail só saem no log em desenvolvimento)
1. Pasta **05**: rode só a 1ª requisição (*Autocadastro com ficha completa*). No **log do Auth/API** procure
   `/ativar-conta?token=...`, copie o token para a variável de ambiente `ativacaoToken` e rode o resto da pasta.
2. Pasta **12**: rode *Esqueci a senha (e-mail existente)*, copie o token de `/redefinir-senha?token=...` do log
   para `resetToken` e continue.
3. Requisições de **upload** (06 e 10): clique em *Body → form-data → arquivo* e selecione um PDF/PNG/JPG pequeno.

## Observações
- Access token vale 15 min. Se aparecer 401 no meio da execução, rode de novo o login da pasta correspondente.
- O fluxo B da pasta 07 (check-in/conclusão) só funciona em **dia útil** (agenda de segunda a sexta, 08–16h):
  a coleção avisa no console quando hoje não é dia útil.
- Cada execução usa um `runId` novo (e-mails/CPFs únicos). Para recomeçar do zero, limpe `runId` no ambiente.
- O estoque usa O+ para o teste de alerta crítico: a baixa de 15 bolsas deixa o tipo no limiar.

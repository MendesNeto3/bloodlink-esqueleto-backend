# Bloodlink — esqueleto de arquitetura (sem implementação)

Este projeto contém **só a estrutura**: os 9 módulos Maven, cada `pom.xml`
com as dependências corretas já configuradas, `application.yml`/
`docker-compose.yml`, as migrations SQL, e a árvore de pacotes completa
(hexagonal: `domain/ → application/port/{in,out}/ → application/service/ →
adapter/{in,out}/`) — mas **nenhuma classe Java está implementada**.

As pastas de pacote que ficariam vazias têm um arquivo `.gitkeep` só pra
sobreviverem no zip/git (apague-os conforme for criando as classes reais).

## Por onde começar

O roadmap completo, com a ordem de implementação, as classes que cada
etapa precisa, e o propósito de cada uma, está no quadro do Trello:

**https://trello.com/b/vvCH8Q2z/bloodlink-backend**

Siga a ordem do Backlog de cima pra baixo — cada cartão pressupõe que os
anteriores já foram feitos (ex.: você precisa do `bloodlink-common` e do
`bloodlink-tenant-starter` prontos antes de começar o `bloodlink-estoque`).

## Estrutura dos módulos

| Módulo | Tipo | Porta |
|---|---|---|
| `bloodlink-common` | biblioteca | — |
| `bloodlink-tenant-starter` | biblioteca | — |
| `bloodlink-auth-support` | biblioteca | — |
| `bloodlink-gateway` | serviço | 8000 |
| `bloodlink-centro` | serviço | 8084 |
| `bloodlink-api` | serviço | 8080 |
| `bloodlink-estoque` | serviço | 8082 |
| `bloodlink-checkin` | serviço | 8083 |
| `bloodlink-notificacao` | serviço | 8081 |

## Abrindo no IntelliJ

`File → Open`, aponte pra esta pasta raiz (onde está o `pom.xml` pai). O
IntelliJ reconhece os 9 módulos automaticamente. Como não há nenhuma classe
`@SpringBootApplication` ainda, os módulos não vão rodar até você criar a
classe principal de cada um (primeiro cartão de cada serviço no Trello).

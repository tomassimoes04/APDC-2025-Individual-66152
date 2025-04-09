# APDC 2024/2025 - Exercício Individual

Este repositório contém a implementação do Exercício de Avaliação Individual da unidade curricular de Aplicações para a Cloud (ADC), Edição 2024/2025.

**Aluno:** Tomás Simões
**Número:** 66152

## Descrição do Projeto

O projeto consiste numa API REST desenvolvida em Java com Maven, utilizando o Google App Engine (Standard Environment - Java 21) e o Google Cloud Datastore como base de dados. A API implementa um sistema de gestão de utilizadores (com diferentes roles e estados), autenticação baseada em tokens, e gestão de folhas de obra, conforme os requisitos especificados no enunciado do exercício.

## Funcionalidades Implementadas (Operações Obrigatórias)

*   **OP1:** Registo de novas contas de utilizador (`/rest/register/`).
*   **OP2:** Login de utilizadores (`/rest/login/`), retornando um AuthToken.
*   **OP3:** Alteração de Role de utilizadores (`/rest/user/changerole`) por ADMIN/BACKOFFICE.
*   **OP4:** Alteração de Estado da Conta (`/rest/user/changestate`) por ADMIN/BACKOFFICE.
*   **OP5:** Remoção de Contas de Utilizador (`/rest/user/{username}`) por ADMIN/BACKOFFICE (com limpeza de tokens).
*   **OP6:** Listagem de Utilizadores (`/rest/user/listusers`) com visibilidade baseada no role do requisitante.
*   **OP7:** Alteração de Atributos da Conta (`/rest/user/updateatts`) com permissões e restrições por role.
*   **OP8:** Alteração da Própria Password (`/rest/user/changepwd`).
*   **OP9:** Logout da Sessão (`/rest/login/logout`) invalidando o token no Datastore.
*   **OP10:** Criação/Atualização de Folhas de Obra (`/rest/worksheet/`) por BACKOFFICE/ADMIN e Atualização do Estado da Obra (`/rest/worksheet/updatestate`) pelo PARTNER associado.

## Tecnologias Utilizadas

*   **Linguagem:** Java 21
*   **Framework Web/API:** JAX-RS (Jersey 3.1.x)
*   **Build Tool:** Apache Maven
*   **Plataforma Cloud:** Google App Engine (Standard Environment - Java 21 Runtime)
*   **Base de Dados:** Google Cloud Datastore (usando a API `google-cloud-datastore`)
*   **Autenticação:** Tokens de portador (Bearer Tokens) simples (UUID) guardados no Datastore.
*   **Hashing de Password:** SHA-512 (via Apache Commons Codec)
*   **Outras:** Gson (JSON), Jakarta EE Web API.

## Como Compilar e Fazer Deploy

**Pré-requisitos:**
*   JDK 21 instalado.
*   Apache Maven instalado.
*   Google Cloud SDK (`gcloud`) instalado e autenticado (`gcloud auth login`, `gcloud auth application-default login`).

**Compilar:**
Navegue até à pasta raiz do projeto no terminal e execute:
```bash
mvn clean package


Isto irá compilar o código e criar o ficheiro .war na pasta target/.

Fazer Deploy para Google App Engine:

Configure o seu ID de projeto Google Cloud no ficheiro pom.xml (dentro do plugin appengine-maven-plugin). Certifique-se que o projeto GCP existe e tem as APIs necessárias (App Engine Admin, Cloud Datastore, Cloud Build) ativadas.

Configure o projeto ativo na gcloud: gcloud config set project SEU_PROJECT_ID

Execute o comando de deploy:

mvn package appengine:deploy
IGNORE_WHEN_COPYING_START
content_copy
download
Use code with caution.
Bash
IGNORE_WHEN_COPYING_END

A aplicação estará disponível na URL fornecida pela gcloud ou acessível via gcloud app browse.

Endpoints da API

A API está disponível no URL base fornecido pelo App Engine, seguido do prefixo /rest.

Exemplos:

Registo: POST {URL_BASE}/rest/register/

Login: POST {URL_BASE}/rest/login/

Logout: POST {URL_BASE}/rest/login/logout

Listar Utilizadores: POST {URL_BASE}/rest/user/listusers

Mudar Estado: POST {URL_BASE}/rest/user/changestate

Remover Utilizador: DELETE {URL_BASE}/rest/user/{username}

Criar/Atualizar Folha Obra: POST {URL_BASE}/rest/worksheet/

Atualizar Estado Obra: POST {URL_BASE}/rest/worksheet/updatestate

(Outros endpoints de utilizador em /rest/user/...)

Nota: Todas as operações, exceto Registo e Login, requerem um Header Authorization com o valor Bearer {TOKEN_ID}, onde {TOKEN_ID} é o UUID obtido da resposta do Login.

Notas Adicionais

O utilizador root com role ADMIN e password password123 (verificar AppInitListener.java) deve ser criado automaticamente no primeiro deploy. Caso contrário, criar manualmente no Datastore.

A validação de tokens é feita verificando a existência e expiração do token no Datastore.



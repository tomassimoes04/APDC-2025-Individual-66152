# ADC-PEI 2024/2025, 2º Semestre
## Sessões Google Datastore + Cloud Storage
## Projeto exemplificativo das sessões de formação ADC-PEI 24/25 (Projeto 1 - Projeto Inicial)
Códificado por Tomás Simões N 66152
# APDC 2024/2025 - Exercício de Avaliação Individual

**Aluno:** Tomás Simões
**Número:** 66152

## Descrição do Projeto

Este repositório contém a implementação do Exercício de Avaliação Individual para a unidade curricular de Arquitetura e Desenho de Computadores (ADC) na edição 2024/2025.

O projeto consiste numa API REST desenvolvida em Java, utilizando o framework JAX-RS (Jersey), e persistência de dados no Google Cloud Datastore. A aplicação corre no ambiente Google App Engine Standard (Java 21).

Foram implementadas as 10 operações obrigatórias (OP1 a OP10) descritas no enunciado, incluindo:
*   Registo e Login de Utilizadores (com hashing de password e tokens de autenticação)
*   Gestão de Contas de Utilizador (alteração de role, estado, atributos e password) com base em permissões por role (ADMIN, BACKOFFICE, ENDClUSER, PARTNER)
*   Listagem de Utilizadores com visibilidade variável consoante o role
*   Remoção de Contas e Logout (aro! Aqui tens um exemplo de README.md que podes usar como baseinvalidação de token)
*   Criação/Modificação de Fol para o teu repositório GitHub. Adapta-o conforme necessário.

```markdown
# APDC 2024/2025 - Exerchas de Obra (WorkSheets) com gestão de estado e adjudicação.

## Tecnologias Utilizadas

*   **Linguagem:** Java 21
*   **ício Individual

Este repositório contém a implementação do Exercício de Avaliação Individual daFramework Web/REST:** JAX-RS (Jersey 3.1.x)
*   **Persistência:** Google Cloud Datastore (via biblioteca unidade curricular de Aplicações para a Cloud (ADC), Edição 2024/2025.

**Aluno:** Tomás Simões
**Número:** 66152

 `google-cloud-datastore`)
*   **Plataforma Cloud## Descrição do Projeto

O projeto consiste numa API REST desenvolvida em Java com:** Google App Engine Standard (Java 21 Runtime)
*   **Build Tool Maven, utilizando o Google App Engine (Standard Environment - Java 21) e o Google:** Apache Maven
*   **Bibliotecas Adicionais:**
    *   Gson (para manipulação JSON)
    *   Commons Codec (para hashing SHA-512 de passwords)
    *   Jakarta EE Web API (Serv Cloud Datastore como base de dados. A API implementa um sistema de gestão de utilizadores (lets, etc.)
    *   Google Cloud Client Libraries

## Como Compcom diferentes roles e estados), autenticação baseada em tokens, e gestão de folhas de obra,ilar e Fazer Deploy

**Pré-requisitos:**
*   JDK conforme os requisitos especificados no enunciado do exercício.

## Funcionalidades Implementadas ( 21 instalado
*   Apache Maven instalado
*   Google Cloud SDK (`gcloud`) instalado e autenticado (`gcloud auth login`, `gcloud auth application-default loginOperações Obrigatórias)

*   **OP1:** Registo de novas contas de utilizador (`/rest/register/`).
*   **OP2:**`)

**1. Clonar o Repositório:**
   ```bash
   git clone https://github.com/TEU-USERNAME-GITHUB/APDC-2025-Individual-66152.git
   cd APDC-2025-Individual-66152

# Projeto de Chat em Java (Servidor e Cliente)

Este projeto consiste no desenvolvimento de um sistema de chat em Java, composto por um servidor e um cliente. O servidor é baseado no modelo multiplex, utilizando NIO para gerir múltiplas conexões de clientes de forma eficiente. O cliente, por sua vez, implementa uma interface gráfica simples e utiliza múltiplas threads para garantir uma experiência de utilizador fluida, permitindo o envio e receção de mensagens simultaneamente.

## Funcionalidades

### Servidor (`ChatServer.java`)

O servidor de chat é responsável por:

*   **Gestão de Conexões:** Aceita múltiplas conexões de clientes usando `ServerSocketChannel` e `Selector` em modo não bloqueante.
*   **Gestão de Nicks:** Permite que os utilizadores definam um `nick` (nome de utilizador) único através do comando `/nick`.
*   **Gestão de Salas:** Suporta a criação e entrada em salas de chat através do comando `/join`.
*   **Mensagens Públicas:** Encaminha mensagens de um cliente para todos os outros clientes na mesma sala.
*   **Mensagens Privadas:** Permite o envio de mensagens privadas entre utilizadores através do comando `/priv`.
*   **Saída de Salas/Servidor:** Permite que os utilizadores saiam de uma sala (`/leave`) ou se desconectem do servidor (`/bye`).
*   **Tratamento de Erros:** Envia mensagens de erro (`ERROR`) para comandos inválidos ou ações não permitidas.

### Cliente (`ChatClient.java`)

O cliente de chat é responsável por:

*   **Interface Gráfica:** Apresenta uma interface gráfica simples (`JFrame`, `JTextField`, `JTextArea`) para interação com o utilizador.
*   **Conexão ao Servidor:** Estabelece conexão com o servidor de chat.
*   **Envio de Mensagens/Comandos:** Permite ao utilizador enviar mensagens e comandos para o servidor.
*   **Receção de Mensagens:** Recebe e exibe mensagens do servidor numa área de texto, formatando-as para uma leitura mais amigável.
*   **Multithreading:** Utiliza uma thread dedicada para a receção de mensagens do servidor, evitando o bloqueio da interface de utilizador.
*   **Escape de Comandos:** Implementa um mecanismo de escape para mensagens que começam com `/` mas não são comandos, prefixando-as com um `/` adicional.

## Protocolo de Comunicação

O protocolo de comunicação é orientado à linha de texto, onde cada mensagem termina com uma mudança de linha (`\n`). O servidor é responsável por lidar com mensagens parciais ou múltiplas recebidas numa única operação de leitura da socket.

### Comandos Suportados (Cliente para Servidor)

Os comandos são prefixados por `/` e podem incluir argumentos. As mensagens simples são enviadas diretamente.

*   `/nick <nome_de_utilizador>`: Define ou altera o nome de utilizador.
*   `/join <nome_da_sala>`: Entra numa sala de chat existente ou cria uma nova.
*   `/leave`: Sai da sala de chat atual.
*   `/bye`: Desconecta-se do servidor.
*   `/priv <nome_do_recetor> <mensagem>`: Envia uma mensagem privada a um utilizador específico.

### Mensagens do Servidor para o Cliente

O servidor envia mensagens com um formato específico, que o cliente interpreta e formata para exibição:

*   `OK`: Confirmação de uma operação bem-sucedida.
*   `ERROR`: Indica um erro (comando inválido, nick já em uso, etc.).
*   `BYE`: Confirmação de desconexão.
*   `MESSAGE <emissor> <mensagem>`: Mensagem pública enviada por um utilizador na sala.
*   `PRIVATE <emissor> <mensagem>`: Mensagem privada recebida de um utilizador.
*   `JOINED <nome_de_utilizador>`: Notificação de que um utilizador entrou na sala.
*   `LEFT <nome_de_utilizador>`: Notificação de que um utilizador saiu da sala.
*   `NEWNICK <nome_antigo> <nome_novo>`: Notificação de que um utilizador mudou o seu nome.

## Como Compilar e Executar

Para compilar e executar o projeto, siga os passos abaixo:

### Pré-requisitos

*   Java Development Kit (JDK) 8 ou superior.

### Compilação

Navegue até ao diretório `project_files` e compile os ficheiros Java:

```bash
javac ChatServer.java ChatClient.java
```

### Execução do Servidor

Para iniciar o servidor, especifique o número da porta TCP:

```bash
java ChatServer <porta_tcp>
```

Exemplo:

```bash
java ChatServer 8000
```

### Execução do Cliente

Para iniciar o cliente, especifique o nome DNS ou endereço IP do servidor e o número da porta TCP:

```bash
java ChatClient <endereco_servidor> <porta_tcp>
```

Exemplo:

```bash
java ChatClient localhost 8000
```

## Autores

*   202306412 Paulo Diogo Lopes Pinto
*   202307196 Pedro Maria Neves Cameira de Sousa Machado


# Chat Application

## Descripción

Este proyecto es una **aplicación de mensajería en consola** que permite la comunicación en tiempo real mediante **mensajes de texto y audio** entre usuarios. Además, proporciona la funcionalidad para crear grupos, unirse a ellos, gestionar chats privados y realizar llamadas de audio.

### Funcionalidades principales:
- **Mensajería de texto y audio** a usuarios individuales o grupos.
- **Historial de mensajes** y audios accesibles en cualquier momento.
- **Gestión de grupos de chat** para conversaciones múltiples.
- **Llamadas de audio** entre usuarios, con opción para iniciar y colgar.

### Requisitos:
- **Java 8 o superior**.
- Conexión a internet para la interacción cliente-servidor.

## Estructura del Proyecto

1. **Cliente** (`src/main/java/co/com/icesi/Cliente.java`): Maneja la interfaz de usuario en consola, incluyendo los comandos para enviar mensajes y audios, crear grupos y realizar llamadas.
2. **Servidor** (`src/main/java/co/com/icesi/Server.java`): Procesa las solicitudes de los clientes, gestiona los usuarios conectados, las conversaciones grupales, las llamadas de audio y almacena el historial.
3. **Constantes** (`src/main/java/co/com/icesi/Constants/Constants.java`): Contiene las constantes del proyecto, como mensajes de error, direcciones de red, etc.

## Uso del Cliente

El cliente se controla a través de **comandos de texto** ingresados en la consola. A continuación, se detallan los comandos disponibles y su uso específico:

### Comandos Principales

- **/creargrupo `<nombre_grupo>`**:
    - Crea un nuevo grupo de chat con el nombre proporcionado.
    - **Ejemplo**: `/creargrupo amigos`.

- **/unirse `<nombre_grupo>`**:
    - Únete a un grupo existente para participar en la conversación.
    - **Ejemplo**: `/unirse amigos`.

- **/privado `<nombre_usuario>`**:
    - Inicia un chat privado con un usuario específico.
    - **Ejemplo**: `/privado juan`.

- **/mensaje `<usuario/grupo>` `<mensaje>`**:
    - Envía un mensaje de texto a un usuario o grupo.
    - **Ejemplo**: `/mensaje juan Hola, ¿cómo estás?`.

- **/enviaraudio `<usuario/grupo>` `<archivo_audio>`**:
    - Envía un archivo de audio a un usuario o grupo. El archivo debe estar disponible en el sistema de archivos local.
    - **Ejemplo**: `/enviaraudio amigos saludo.wav`.

- **/escucharaudio `<nombre_audio>`**:
    - Escucha un archivo de audio que te fue enviado previamente.
    - **Ejemplo**: `/escucharaudio saludo.wav`.

### Comandos de Llamadas de Audio

- **/llamar**:
    - Inicia una llamada de audio con el usuario o grupo con el que estás chateando en ese momento.
    - **Nota**: La llamada utiliza sockets para la transmisión de audio en tiempo real.

- **/colgar**:
    - Termina la llamada de audio en curso.

### Otros Comandos Útiles

- **/usuarios**:
    - Muestra una lista de todos los usuarios actualmente conectados al servidor.

- **/historial `<grupo/usuario>`**:
    - Visualiza el historial de mensajes con un usuario o grupo.
    - **Ejemplo**: `/historial amigos` muestra el historial de mensajes del grupo "amigos".

- **/help**:
    - Muestra una lista completa de los comandos disponibles.

## Ejecución del Proyecto

### 1. Ejecutar el Servidor
El servidor debe ejecutarse primero, ya que es el encargado de gestionar la comunicación entre los clientes.

```bash
cd src/main/java
javac co/com/icesi/Server.java
java co.com.icesi.Server

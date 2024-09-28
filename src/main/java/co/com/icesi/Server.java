package co.com.icesi;

import java.io.*;
import java.net.*;
import java.util.*;

public class Server {
    private static final Map<String, ClientHandler> usuariosConectados = new HashMap<>(); // Usuarios conectados
    private static final Map<String, List<ClientHandler>> grupos = new HashMap<>(); // Grupos y sus miembros
    private static final Map<String, List<String>> historial = new HashMap<>(); // Historial de mensajes

    // Colores ANSI
    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_YELLOW = "\u001B[33m";
    public static final String ANSI_BLUE = "\u001B[34m";
    public static final String ANSI_PURPLE = "\u001B[35m";
    public static final String ANSI_CYAN = "\u001B[36m";

    public static void main(String[] args) {
        System.out.println("Servidor iniciado...");
        try (ServerSocket serverSocket = new ServerSocket(12345)) {
            while (true) {
                new ClientHandler(serverSocket.accept()).start();
            }
        } catch (IOException e) {
            System.err.println("Error en el servidor: " + e.getMessage());
        }
    }

    // Clase interna para manejar la conexión de cada cliente
    private static class ClientHandler extends Thread {
        private final Socket socket;
        private PrintWriter out;
        private BufferedReader in;
        private String nombreUsuario;
        private String grupoActual = null; // Variable para almacenar el grupo actual
        private String chatPrivadoActual = null; // Variable para almacenar el chat privado actual

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);

                // Solicitar el nombre del usuario
                out.println("Ingresa tu nombre de usuario:");
                nombreUsuario = in.readLine();

                synchronized (usuariosConectados) {
                    usuariosConectados.put(nombreUsuario, this);
                }

                System.out.println(nombreUsuario + " se ha conectado.");
                enviarATodos(nombreUsuario + " se ha unido al chat.");

                out.println("Comandos disponibles:");
                mostrarInstrucciones();

                // Procesar comandos del cliente
                String input;
                while ((input = in.readLine()) != null) {
                    // Si el usuario está en un grupo o chat privado, enviará mensajes automáticamente
                    if (grupoActual != null && !input.startsWith("/")) {
                        enviarMensaje(grupoActual, input); // Mensaje al grupo actual
                    } else if (chatPrivadoActual != null && !input.startsWith("/")) {
                        enviarMensaje(chatPrivadoActual, input); // Mensaje al chat privado actual
                    } else {
                        procesarComando(input);
                    }
                }
            } catch (IOException e) {
                System.err.println("Error en la conexión con el cliente: " + e.getMessage());
            } finally {
                try {
                    synchronized (usuariosConectados) {
                        usuariosConectados.remove(nombreUsuario);
                    }
                    socket.close();
                    enviarATodos(nombreUsuario + " ha salido del chat.");
                } catch (IOException e) {
                    System.err.println("Error al cerrar el socket: " + e.getMessage());
                }
            }
        }

        private void mostrarInstrucciones() {
            out.println(ANSI_CYAN + "===== SELECCIONA UNA OPCION =====" + ANSI_RESET);
            out.println(ANSI_YELLOW + "1. Comandos de Grupo" + ANSI_RESET);
            out.println(ANSI_YELLOW + "2. Comandos de Chat Privado" + ANSI_RESET);
            out.println(ANSI_YELLOW + "3. Comandos Generales" + ANSI_RESET);
            out.println(ANSI_CYAN + "=================================" + ANSI_RESET);


            try {
                String opcion = in.readLine();
                switch (opcion) {
                    case "1":
                        mostrarComandosGrupo();
                        break;
                    case "2":
                        mostrarComandosChatPrivado();
                        break;
                    case "3":
                        mostrarComandosGenerales();
                        break;
                    default:
                        out.println(ANSI_RED + "Opción no válida. Por favor, selecciona 1, 2 o 3." + ANSI_RESET);
                }
            } catch (IOException e) {
                out.println(ANSI_RED + "Error al leer la opción: " + e.getMessage() + ANSI_RESET);
            }
        }

        private void mostrarComandosGrupo() {
            out.println(ANSI_GREEN + "===== COMANDOS DE GRUPO =====" + ANSI_RESET);
            out.println(ANSI_GREEN + "/creargrupo <nombre_grupo>" + ANSI_RESET + "          - Crear un nuevo grupo.");
            out.println(ANSI_GREEN + "/unirse <nombre_grupo>" + ANSI_RESET + "              - Unirse a un grupo existente.");
            out.println(ANSI_GREEN + "/ingresar <nombre_grupo>" + ANSI_RESET + "            - Volver a ingresar a un grupo existente.");
            out.println(ANSI_GREEN + "/salirgrupo" + ANSI_RESET + "                         - Salir completamente del grupo actual.");
            out.println(ANSI_GREEN + "/grupos" + ANSI_RESET + "                             - Mostrar grupos disponibles.\n");
        }

        private void mostrarComandosChatPrivado() {
            out.println(ANSI_PURPLE + "===== COMANDOS DE CHAT PRIVADO =====" + ANSI_RESET);
            out.println(ANSI_PURPLE + "/privado <nombre_usuario>" + ANSI_RESET + "           - Iniciar un chat privado con un usuario.");
            out.println(ANSI_PURPLE + "/salir" + ANSI_RESET + "                              - Salir del grupo o chat privado actual.\n");
        }

        private void mostrarComandosGenerales() {
            out.println(ANSI_BLUE + "===== COMANDOS GENERALES =====" + ANSI_RESET);
            out.println(ANSI_BLUE + "/mensaje <usuario/grupo> <mensaje>" + ANSI_RESET + "  - Enviar un mensaje a un usuario o grupo.");
            out.println(ANSI_BLUE + "/usuarios" + ANSI_RESET + "                           - Mostrar usuarios conectados.");
            out.println(ANSI_BLUE + "/historial <usuario/grupo>" + ANSI_RESET + "          - Ver el historial de mensajes de un grupo o usuario.");
            out.println(ANSI_BLUE + "/help" + ANSI_RESET + "                               - Mostrar comandos.\n");
        }


        // Procesar comandos como crear grupo, enviar mensajes, etc.
        private void procesarComando(String input) {
            String[] partes = input.split(" ", 3);
            String comando = partes[0];
            out.println("\n");
            switch (comando.toLowerCase()) {
                case "/creargrupo":
                    if (partes.length >= 2) {
                        crearGrupo(partes[1]);
                    } else {
                        out.println("Uso: /creargrupo <nombre_grupo>");
                    }
                    break;
                case "/unirse":
                    if (partes.length >= 2) {
                        unirseAGrupo(partes[1]);
                    } else {
                        out.println("Uso: /unirse <nombre_grupo>");
                    }
                    break;
                case "/ingresar":
                    if (partes.length >= 2) {
                        ingresarAGrupo(partes[1]);
                    } else {
                        out.println("Uso: /ingresar <nombre_grupo>");
                    }
                    break;
                case "/privado":
                    if (partes.length >= 2) {
                        iniciarChatPrivado(partes[1]);
                    } else {
                        out.println("Uso: /privado <nombre_usuario>");
                    }
                    break;
                case "/mensaje":
                    if (partes.length >= 3) {
                        enviarMensaje(partes[1], partes[2]);
                    } else {
                        out.println("Uso: /mensaje <usuario/grupo> <mensaje>");
                    }
                    break;
                case "/salir":
                    salirDeChatOGrupo();
                    break;
                case "/salirgrupo":
                    salirCompletamenteDelGrupo();
                    break;
                case "/usuarios":
                    mostrarUsuariosConectados();
                    break;
                case "/grupos":
                    mostrarGruposDisponibles();
                    break;
                case "/historial":
                    if (partes.length >= 2) {
                        mostrarHistorial(partes[1]);
                    } else {
                        out.println("Uso: /historial <usuario/grupo>");
                    }
                    break;
                case "/help":
                    mostrarInstrucciones();
                    break;
                default:
                    out.println("Comando no reconocido.");
            }
        }

        // Método para crear un grupo de chat
        private void crearGrupo(String nombreGrupo) {
            if (!grupos.containsKey(nombreGrupo)) {
                grupos.put(nombreGrupo, new ArrayList<>());
                grupos.get(nombreGrupo).add(this); // Agregar al creador del grupo
                historial.put(nombreGrupo, new ArrayList<>());
                grupoActual = nombreGrupo; // Asignar el grupo actual
                chatPrivadoActual = null; // Salir de cualquier chat privado
                out.println(ANSI_GREEN + "Grupo " + nombreGrupo + " creado y te has unido." + ANSI_RESET);
            } else {
                out.println(ANSI_RED + "El grupo ya existe." + ANSI_RESET);
            }
        }

        // Método para unirse a un grupo
        private void unirseAGrupo(String nombreGrupo) {
            if (grupos.containsKey(nombreGrupo)) {
                List<ClientHandler> miembros = grupos.get(nombreGrupo);
                if (!miembros.contains(this)) {
                    miembros.add(this); // Agregar al usuario al grupo
                    grupoActual = nombreGrupo; // Asignar el grupo actual
                    chatPrivadoActual = null; // Salir de cualquier chat privado
                    out.println(ANSI_GREEN + "Te has unido al grupo " + nombreGrupo + ANSI_RESET);
                } else {
                    out.println(ANSI_YELLOW + "Ya eres miembro del grupo " + nombreGrupo + ANSI_RESET);
                }
            } else {
                out.println(ANSI_RED + "El grupo " + nombreGrupo + " no existe." + ANSI_RESET);
            }
        }

        // Método para volver a ingresar a un grupo
        private void ingresarAGrupo(String nombreGrupo) {
            if (grupos.containsKey(nombreGrupo)) {
                List<ClientHandler> miembros = grupos.get(nombreGrupo);
                if (miembros.contains(this)) {
                    grupoActual = nombreGrupo; // Asignar el grupo actual
                    chatPrivadoActual = null; // Salir de cualquier chat privado
                    out.println(ANSI_GREEN + "Te has unido al grupo " + nombreGrupo + ANSI_RESET);
                } else {
                    out.println(ANSI_YELLOW + "No eres miembro del grupo " + nombreGrupo + ANSI_RESET);
                }
            } else {
                out.println(ANSI_RED + "El grupo " + nombreGrupo + " no existe." + ANSI_RESET);
            }
        }

        // Método para iniciar un chat privado con un usuario
        private void iniciarChatPrivado(String nombreUsuarioDestino) {
            if (usuariosConectados.containsKey(nombreUsuarioDestino)) {
                chatPrivadoActual = nombreUsuarioDestino; // Asignar el chat privado actual
                grupoActual = null; // Salir de cualquier grupo
                out.println(ANSI_GREEN + "Estás en un chat privado con " + nombreUsuarioDestino + ANSI_RESET);
            } else {
                out.println(ANSI_RED + "El usuario " + nombreUsuarioDestino + " no está conectado." + ANSI_RESET);
            }
        }

        // Método para salir del grupo o chat privado actual
        private void salirDeChatOGrupo() {
            if (grupoActual != null) {
                out.println(ANSI_GREEN + "Has salido del grupo " + grupoActual + ANSI_RESET);
                grupoActual = null;
            } else if (chatPrivadoActual != null) {
                out.println(ANSI_GREEN + "Has salido del chat privado con " + chatPrivadoActual + ANSI_RESET);
                chatPrivadoActual = null;
            } else {
                out.println(ANSI_YELLOW + "No estás en ningún grupo o chat privado." + ANSI_RESET);
            }
        }

        // Método para salir completamente del grupo actual
        private void salirCompletamenteDelGrupo() {
            if (grupoActual != null) {
                List<ClientHandler> miembros = grupos.get(grupoActual);
                if (miembros != null) {
                    miembros.remove(this); // Remover al usuario del grupo
                    out.println(ANSI_GREEN + "Has salido completamente del grupo " + grupoActual + ANSI_RESET);
                    grupoActual = null; // Limpiar la variable de grupo actual
                }
            } else {
                out.println(ANSI_YELLOW + "No estás en ningún grupo." + ANSI_RESET);
            }
        }

        // Método para enviar un mensaje a un grupo o usuario
        private void enviarMensaje(String destino, String mensaje) {
            if (grupos.containsKey(destino)) {
                List<ClientHandler> miembros = grupos.get(destino);
                synchronized (historial) {
                    historial.get(destino).add(nombreUsuario + ": " + mensaje); // Guardar en el historial
                }
                for (ClientHandler miembro : miembros) {
                    miembro.out.println(ANSI_PURPLE + "[Grupo " + destino + "] " + nombreUsuario + ": " + mensaje + ANSI_RESET);
                }
            } else if (usuariosConectados.containsKey(destino)) {
                ClientHandler receptor = usuariosConectados.get(destino);
                synchronized (historial) {
                    historial.get(destino).add(nombreUsuario + ": " + mensaje); // Guardar en el historial
                }
                receptor.out.println(ANSI_BLUE + "[Privado] " + nombreUsuario + ": " + mensaje + ANSI_RESET);
            } else {
                out.println(ANSI_RED + "El grupo o usuario " + destino + " no existe." + ANSI_RESET);
            }
        }

        // Método para mostrar usuarios conectados
        private void mostrarUsuariosConectados() {
            out.println(ANSI_CYAN + "===== USUARIOS CONECTADOS =====" + ANSI_RESET);
            synchronized (usuariosConectados) {
                for (String usuario : usuariosConectados.keySet()) {
                    out.println(ANSI_GREEN + usuario + ANSI_RESET);
                }
            }
            out.println(ANSI_CYAN + "=============================" + ANSI_RESET);
        }

        // Método para mostrar grupos disponibles
        private void mostrarGruposDisponibles() {
            out.println(ANSI_CYAN + "===== GRUPOS DISPONIBLES =====" + ANSI_RESET);
            synchronized (grupos) {
                for (String grupo : grupos.keySet()) {
                    out.println(ANSI_GREEN + grupo + ANSI_RESET);
                }
            }
            out.println(ANSI_CYAN + "=============================" + ANSI_RESET);
        }

        // Método para mostrar el historial de un grupo o usuario
        private void mostrarHistorial(String destino) {
            if (historial.containsKey(destino)) {
                out.println(ANSI_CYAN + "===== HISTORIAL DE " + destino + " =====" + ANSI_RESET);
                List<String> mensajes = historial.get(destino);
                for (String mensaje : mensajes) {
                    out.println(ANSI_GREEN + mensaje + ANSI_RESET);
                }
                out.println(ANSI_CYAN + "===============================" + ANSI_RESET);
            } else {
                out.println(ANSI_RED + "No hay historial disponible para " + destino + ANSI_RESET);
            }
        }

        // Método para enviar un mensaje a todos los usuarios
        private void enviarATodos(String mensaje) {
            synchronized (usuariosConectados) {
                for (ClientHandler usuario : usuariosConectados.values()) {
                    usuario.out.println(ANSI_YELLOW + "[Todos] " + mensaje + ANSI_RESET);
                }
            }
        }
    }
}

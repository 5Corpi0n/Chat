package co.com.icesi;

import java.io.*;
import java.net.*;
import java.util.*;
import java.awt.Desktop;
import java.io.File;
import java.io.IOException;

import co.com.icesi.Constants.Constants;
import co.com.icesi.database.GuardarHistorial;

import javax.sound.sampled.*;

public class Server {

    private static final Map<String, ClientHandler> usuariosConectados = new HashMap<>(); // Usuarios conectados
    private static final Map<String, List<ClientHandler>> grupos = new HashMap<>(); // Grupos y sus miembros
    private static final Map<String, List<String>> historial = new HashMap<>(); // Historial de mensajes
    private static final Map<String, List<String>> audios = new HashMap<>(); // Audios enviados

    public static void main(String[] args) {
        System.out.println("Servidor iniciado...");
        try (ServerSocket serverSocket = new ServerSocket(12345)) {
            while (true) {
                new ClientHandler(serverSocket.accept()).start();
            }
        } catch (IOException | UnsupportedAudioFileException e) {
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
        private boolean flag = true;
        private static final int BUFFER_SIZE = 2048;


        private boolean enLlamada = false;

        // Variables para la grabación de audio
        private TargetDataLine lineaGrabacion;
        private AudioFormat formato;
        private volatile boolean grabando = false;  // Variable para controlar el estado de grabación

        //streams
        private final OutputStream outputStream;
        private final InputStream inputStream;


        public ClientHandler(Socket socket) throws UnsupportedAudioFileException, IOException {
            this.socket = socket;

            this.inputStream = socket.getInputStream();
            this.outputStream = socket.getOutputStream();

        }

        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);
                boolean temp = true;

                solicitarNombreUsuario();

                synchronized (usuariosConectados) {
                    usuariosConectados.put(nombreUsuario, this);
                }

                System.out.println(nombreUsuario + " se ha conectado.");
                enviarATodos(nombreUsuario + " se ha unido al chat.");

                out.println("Comandos disponibles:");
                mostrarInstrucciones();

                // Procesar comandos del cliente
                String input;
                while (flag) {
                    input = in.readLine();

                    if (input == null) {
                        out.println("Cliente desconectado.");
                        break;
                    }

                    procesarInput(input);
                }
            } catch (IOException e) {
                System.err.println("Error en la conexión con el cliente: " + e.getMessage());
            } finally {
                cerrarConexion();
            }
        }

        private void solicitarNombreUsuario() throws IOException {
            boolean temp = true;
            do {
                out.println("Ingresa tu nombre de usuario:");
                nombreUsuario = in.readLine();
                if (!nombreUsuario.contains("-")) temp = false;
                else out.println("El nombre de usuario no puede contener el carácter '-'.");
            } while (temp);
        }

        private void cerrarConexion() {
            try {
                synchronized (usuariosConectados) {
                    usuariosConectados.remove(nombreUsuario);
                }
                if (outputStream != null) outputStream.close();
                if (inputStream != null) inputStream.close();
                if (socket != null) socket.close();

                enviarATodos(nombreUsuario + " ha salido del chat.");
            } catch (IOException e) {
                System.err.println("Error al cerrar el socket: " + e.getMessage());
            }
        }

        private void procesarInput(String input) {
            try {
                if (grupoActual != null && !input.startsWith("/"))
                    enviarMensaje(grupoActual, input);
                else if (chatPrivadoActual != null && !input.startsWith("/"))
                    enviarMensaje(chatPrivadoActual, input);
                else
                    procesarComando(input);
            } catch (Exception e) {
                System.out.println(Constants.ANSI_RED + "Error al procesar el comando: " + e.getMessage() + Constants.ANSI_RESET);
            }
        }


        private void mostrarInstrucciones() {
            out.println(Constants.MENU_PRINCIPAL);
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
                    case "4":
                        flag = false;
                        break;
                    default:
                        procesarInput(opcion);
                }
            } catch (IOException e) {
                out.println(Constants.ANSI_RED + "Error al leer la opción: " + e.getMessage() + Constants.ANSI_RESET);
            }
        }

        private void mostrarComandosGrupo() {
            out.println(Constants.COMANDOS_GRUPO);
        }

        private void mostrarComandosChatPrivado() {
            out.println(Constants.COMANDOS_CHAT_PRIVADO);
        }

        private void mostrarComandosGenerales() {
            out.println(Constants.COMANDOS_GENERALES);
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
                case "/enviaraudio":
                    if (partes.length >= 2) {
                        String ruta = Constants.RUTA_DATABASE;
                        if(grupoActual != null) {
                            ruta += grupoActual + "/audios/" + partes[1];
                            enviarAudio(ruta, partes[1]);
                        } else if(chatPrivadoActual != null) {
                            ruta += generarClaveChatPrivado(nombreUsuario,chatPrivadoActual) + "/audios/" + partes[1];
                            enviarAudio(ruta, partes[1]);
                        } else
                            System.out.println("No estas dentro de un chat. No se puede enviar un audio.");
                    } else {
                        out.println("Uso: /enviaraudio <nombre_archivo_audio>");
                    }
                    break;
                case "/escucharaudio":
                    if (partes.length >= 2) {
                        escucharAudio(partes);
                    } else {
                        out.println("Uso: /escucharaudio <nombre_archivo>");
                    }
                    break;
                case "/deteneraudio":
                    detenerGrabacion();  // Detener la grabación de audio
                    break;
                case "/salir":
                    salirDeChatOGrupo();
                    break;
                case "/salirgrupo":
                    salirCompletamenteDelGrupo();
                    break;
                case "/llamar":
                    iniciarLlamada();
                    break;

                case "/terminarllamada":
                    terminarLlamada();
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
                case "/ayuda":
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
                out.println(Constants.ANSI_GREEN + "Grupo " + nombreGrupo + " creado y te has unido." + Constants.ANSI_RESET);
            } else {
                out.println(Constants.ANSI_RED + "El grupo ya existe." + Constants.ANSI_RESET);
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
                    out.println(Constants.ANSI_GREEN + "Has ingresado al grupo " + nombreGrupo + Constants.ANSI_RESET);
                } else {
                    out.println(Constants.ANSI_YELLOW + "Ya eres miembro del grupo " + nombreGrupo + Constants.ANSI_RESET);
                }
            } else {
                out.println(Constants.ANSI_RED + "El grupo " + nombreGrupo + " no existe." + Constants.ANSI_RESET);
            }
        }

        // Método para volver a ingresar a un grupo
        private void ingresarAGrupo(String nombreGrupo) {
            if (grupos.containsKey(nombreGrupo)) {
                List<ClientHandler> miembros = grupos.get(nombreGrupo);
                if (miembros.contains(this)) {
                    grupoActual = nombreGrupo; // Asignar el grupo actual
                    chatPrivadoActual = null; // Salir de cualquier chat privado
                    out.println(Constants.ANSI_GREEN + "Te has unido al grupo " + nombreGrupo + Constants.ANSI_RESET);
                } else {
                    out.println(Constants.ANSI_YELLOW + "No eres miembro del grupo " + nombreGrupo + Constants.ANSI_RESET);
                }
            } else {
                out.println(Constants.ANSI_RED + "El grupo " + nombreGrupo + " no existe." + Constants.ANSI_RESET);
            }
        }

        // Método para iniciar un chat privado con un usuario
        private void iniciarChatPrivado(String nombreUsuarioDestino) {
            if (usuariosConectados.containsKey(nombreUsuarioDestino)) {
                chatPrivadoActual = nombreUsuarioDestino; // Asignar el chat privado actual
                grupoActual = null; // Salir de cualquier grupo
                out.println(Constants.ANSI_GREEN + "Estás en un chat privado con " + nombreUsuarioDestino + Constants.ANSI_RESET);
            } else {
                out.println(Constants.ANSI_RED + "El usuario " + nombreUsuarioDestino + " no está conectado." + Constants.ANSI_RESET);
            }
        }

        // Método para salir del grupo o chat privado actual
        private void salirDeChatOGrupo() {
            if (grupoActual != null) {
                out.println(Constants.ANSI_GREEN + "Has salido del grupo " + grupoActual + Constants.ANSI_RESET);
                grupoActual = null;
            } else if (chatPrivadoActual != null) {
                out.println(Constants.ANSI_GREEN + "Has salido del chat privado con " + chatPrivadoActual + Constants.ANSI_RESET);
                chatPrivadoActual = null;
            } else {
                out.println(Constants.ANSI_YELLOW + "No estás en ningún grupo o chat privado." + Constants.ANSI_RESET);
            }
        }

        // Método para salir completamente del grupo actual
        private void salirCompletamenteDelGrupo() {
            if (grupoActual != null) {
                List<ClientHandler> miembros = grupos.get(grupoActual);
                if (miembros != null) {
                    miembros.remove(this); // Remover al usuario del grupo
                    out.println(Constants.ANSI_GREEN + "Has salido completamente del grupo " + grupoActual + Constants.ANSI_RESET);
                    grupoActual = null; // Limpiar la variable de grupo actual
                }
            } else {
                out.println(Constants.ANSI_YELLOW + "No estás en ningún grupo." + Constants.ANSI_RESET);
            }
        }

        // Método para enviar un mensaje a un grupo o usuario
        private void enviarMensaje(String destino, String mensaje) {

            // Verificar si el destino es un grupo
            if (grupos.containsKey(destino)) {
                List<ClientHandler> miembros = grupos.get(destino);
                synchronized (historial) {
                    historial.get(destino).add("[Grupo " + destino + "] " + nombreUsuario + ": " + mensaje); // Guardar en el historial del grupo
                }
                GuardarHistorial.guardarHistorial(destino, "[Grupo " + destino + "] " + nombreUsuario + ": " + mensaje);
                for (ClientHandler miembro : miembros) {
                    miembro.out.println(Constants.ANSI_PURPLE + "[Grupo " + destino + "] " + nombreUsuario + ": " + mensaje + Constants.ANSI_RESET);
                }
                return; // Salir de la función después de enviar el mensaje al grupo
            }

            // Verificar si el destino es un usuario privado
            if (usuariosConectados.containsKey(destino)) {
                ClientHandler receptor = usuariosConectados.get(destino);
                // Crear clave única para el historial de chat privado
                String claveChatPrivado = generarClaveChatPrivado(nombreUsuario, destino);
                historial.putIfAbsent(claveChatPrivado, new ArrayList<>());
                GuardarHistorial.guardarHistorial(claveChatPrivado, "[Privado] " + nombreUsuario + ": " + mensaje);
                synchronized (historial) {
                    historial.get(claveChatPrivado).add("[Privado] " + nombreUsuario + ": " + mensaje); // Guardar en el historial del chat privado
                }
                this.out.println(Constants.ANSI_PURPLE + "[Privado] " + nombreUsuario + ": " + mensaje + Constants.ANSI_RESET);
                receptor.out.println(Constants.ANSI_BLUE + "[Privado] " + nombreUsuario + ": " + mensaje + Constants.ANSI_RESET);
            } else {
                out.println(Constants.ANSI_RED + "El usuario o grupo " + destino + " no existe." + Constants.ANSI_RESET);
            }
        }

        // Método para mostrar usuarios conectados
        private void mostrarUsuariosConectados() {
            out.println(Constants.ANSI_CYAN + "===== USUARIOS CONECTADOS =====" + Constants.ANSI_RESET);
            synchronized (usuariosConectados) {
                for (String usuario : usuariosConectados.keySet()) {
                    out.println(Constants.ANSI_GREEN + usuario + Constants.ANSI_RESET);
                }
            }
            out.println(Constants.ANSI_CYAN + "=============================" + Constants.ANSI_RESET);
        }

        // Método para mostrar grupos disponibles
        private void mostrarGruposDisponibles() {
            out.println(Constants.ANSI_CYAN + "===== GRUPOS DISPONIBLES =====" + Constants.ANSI_RESET);
            synchronized (grupos) {
                for (String grupo : grupos.keySet()) {
                    out.println(Constants.ANSI_GREEN + grupo + Constants.ANSI_RESET);
                }
            }
            out.println(Constants.ANSI_CYAN + "=============================" + Constants.ANSI_RESET);
        }

        // Método para mostrar el historial de un grupo o usuario
        private void mostrarHistorial(String destino) {
            if (historial.containsKey(verificateWithSwapUserName(destino))) {
                out.println(Constants.ANSI_CYAN + "===== HISTORIAL DE " + destino + " =====" + Constants.ANSI_RESET);
                List<String> mensajes = historial.get(destino);
                for (String mensaje : mensajes) {
                    out.println(Constants.ANSI_GREEN + mensaje + Constants.ANSI_RESET);
                }
                out.println(Constants.ANSI_CYAN + "===============================" + Constants.ANSI_RESET);
            } else {
                out.println(Constants.ANSI_RED + "No hay historial disponible para " + destino + Constants.ANSI_RESET);
            }
        }

        // Método para enviar un mensaje a todos los usuarios
        private void enviarATodos(String mensaje) {
            synchronized (usuariosConectados) {
                for (ClientHandler usuario : usuariosConectados.values()) {
                    usuario.out.println(Constants.ANSI_YELLOW + "[Todos] " + mensaje + Constants.ANSI_RESET);
                }
            }
        }

        // Método para generar una clave única para el historial de chat privado
        private String generarClaveChatPrivado(String usuario1, String usuario2) {
            // Ordenar los nombres alfabéticamente para tener una clave consistente
            if (usuario1.compareTo(usuario2) < 0) {
                return usuario1 + "-" + usuario2;
            } else {
                return usuario2 + "-" + usuario1;
            }
        }

        private String verificateWithSwapUserName(String chatName){
            if(historial.containsKey(chatName))
                return chatName;
            else {
                String[] users = chatName.split("-");
                return users[1] + "-" + users[0];
            }
        }

        // Configurar el formato de audio que usaremos para la grabación
        private AudioFormat obtenerFormatoAudio() {
            float sampleRate = 16000;
            int sampleSizeInBits = 16;
            int channels = 1;
            boolean signed = true;
            boolean bigEndian = false;
            return new AudioFormat(sampleRate, sampleSizeInBits, channels, signed, bigEndian);
        }

        // Método para escuchar audio
        private void escucharAudio(String[] partes) {
            String ruta= "";
            if(grupoActual != null) {
                ruta = grupoActual + "/audios/" + partes[1] + ".wav";
                List<ClientHandler> miembros = grupos.get(grupoActual);
                for (ClientHandler miembro : miembros) {
                    miembro.out.println(ruta);
                }
            }else if(chatPrivadoActual != null) {
                ruta = generarClaveChatPrivado(nombreUsuario, chatPrivadoActual) + "/audios/" + partes[1] + ".wav";
                ClientHandler receptor = usuariosConectados.get(nombreUsuario);
                receptor.out.println(ruta);
            }else
                System.out.println("No estas dentro de un chat. No se puede escuchar un audio.");
        }

        // Método para enviar un archivo de audio a un usuario o grupo
        private void enviarAudio(String ruta, String nombreArchivo) {
            try {
                if (audios.containsKey(grupoActual)) {
                    List<String> audiosEnviados = audios.get(grupoActual);
                    audiosEnviados.add(nombreArchivo);
                    audios.put(grupoActual, audiosEnviados);
                } else if (audios.containsKey(chatPrivadoActual)) {
                    List<String> audiosEnviados = audios.get(chatPrivadoActual);
                    audiosEnviados.add(nombreArchivo);
                    audios.put(chatPrivadoActual, audiosEnviados);
                } else {
                    List<String> audiosEnviados = new ArrayList<>();
                    audiosEnviados.add(nombreArchivo);
                    audios.put(grupoActual, audiosEnviados);
                }

                iniciarGrabacion(ruta);

                Thread leerConsola = new Thread(() -> {
                    try {
                        String input;
                        BufferedReader inp = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                        while ((input = inp.readLine()) != null) {
                            procesarInput(input);
                            if(!grabando) {
                                if (grupos.containsKey(grupoActual))
                                    enviarAudioAGrupo(ruta, grupoActual, nombreArchivo);
                                else if (usuariosConectados.containsKey(chatPrivadoActual))
                                    enviarAudioAChatPrivado(ruta, chatPrivadoActual, nombreArchivo);
                                break;
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });

                leerConsola.start();

                leerConsola.join();
                //out.println("Audio enviado como: " + nombreArchivo);
            } catch (Exception e) {
                System.out.println("Error al enviar el archivo de audio: " + e.getMessage());
            }
        }

        private void enviarAudioAGrupo(String rutaArchivo, String nombreGrupo, String nombreArchivo) {
            if (grupos.containsKey(nombreGrupo)) {
                List<ClientHandler> miembros = grupos.get(nombreGrupo);
                for (ClientHandler miembro : miembros) {
                    enviarAudioACliente(rutaArchivo, miembro);
                    miembro.out.println("Nuevo archivo de audio recibido: " + nombreArchivo);
                }
            } else {
                if (!socket.isClosed())
                    out.println("El grupo " + nombreGrupo + " no existe.");
            }
        }

        private void enviarAudioAChatPrivado(String rutaArchivo, String nombreUsuario, String nombreArchivo) {
            if (usuariosConectados.containsKey(nombreUsuario)) {
                ClientHandler receptor = usuariosConectados.get(nombreUsuario);
                enviarAudioACliente(rutaArchivo, receptor);
                receptor.out.println("Nuevo archivo de audio recibido: " + nombreArchivo);
            } else {
                if (!socket.isClosed())
                    out.println("El usuario " + nombreUsuario + " no está conectado.");
            }
        }

        private void enviarAudioACliente(String ruta, ClientHandler receptor) {
            // Ruta donde se guardará el archivo en el servidor
            ruta = ruta.replace("*", "Cliente") + ".wav";
            receptor.out.println(ruta.replace(Constants.RUTA_DATABASE, ""));
            try (FileInputStream fos = new FileInputStream(ruta);
                 OutputStream is = receptor.socket.getOutputStream()) {

                byte[] buffer = new byte[4096];
                int bytesLeidos;

                // Recibir datos del archivo desde el cliente
                while ((bytesLeidos = fos.read(buffer)) != -1) {
                    is.write(buffer, 0, bytesLeidos);
                }

                is.flush();

                System.out.println("Archivo recibido correctamente.");

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // Iniciar grabación de audio
        private void iniciarGrabacion(String ruta) {

            formato = obtenerFormatoAudio();
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, formato);

            try {
                if (!AudioSystem.isLineSupported(info)) {
                    out.println("Línea de audio no soportada.");
                    return;
                }

                // Inicializar la línea de grabación
                lineaGrabacion = (TargetDataLine) AudioSystem.getLine(info);
                lineaGrabacion.open(formato);
                lineaGrabacion.start();  // Comenzar a grabar

                grabando = true;
                Thread hiloGrabacion = new Thread(() -> grabarAudio(ruta));

                hiloGrabacion.start();

                out.println("Grabación de audio iniciada. Use /deteneraudio para finalizar.");

            } catch (LineUnavailableException e) {
                if (!socket.isClosed())
                    out.println("Error al iniciar la grabación: " + e.getMessage());
            }
        }

        // Método para escribir el audio en un archivo
        private void grabarAudio(String ruta) {
            File archivo = new File(ruta.replace("*", "Cliente") + ".wav");
            if (!archivo.getParentFile().exists()) {
                archivo.getParentFile().mkdirs();
            }

            try (AudioInputStream ais = new AudioInputStream(lineaGrabacion)) {
                AudioSystem.write(ais, AudioFileFormat.Type.WAVE, archivo);
            } catch (IOException e) {
                if (!socket.isClosed())
                    out.println("Error al guardar el archivo de audio: " + e.getMessage());
            }
        }

        // Detener la grabación de audio
        private void detenerGrabacion() {
            if (grabando) {
                grabando = false;
                lineaGrabacion.stop();
                lineaGrabacion.close();
                if (!socket.isClosed()) {
                    out.println("Grabación de audio detenida y guardada.");
                }
            } else {
                if (!socket.isClosed()) {
                    out.println("No hay ninguna grabación en curso.");
                }
            }
        }



        private void iniciarLlamada() {
            new Thread(() -> {
                try {
                    procesarInput(in.readLine());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }).start();

            if (grupoActual != null) {
                manejarLlamadaGrupo();
            } else if (chatPrivadoActual != null) {
                manejarLlamadaPrivada();
            }else{
                out.println("No estas en un chat o grupo.");
            }

        }

        private void manejarLlamadaGrupo() {
            List<ClientHandler> miembrosGrupo = grupos.get(grupoActual);
            if (miembrosGrupo != null) {
                enLlamada = true;
                for (ClientHandler miembro : miembrosGrupo) {
                    miembro.enLlamada = true;
                    miembro.out.println("Llamada aceptada.");
                    // Iniciar hilos para que todos escuchen
                    new Thread(() -> manejarRecepcionAudio(miembro)).start();
                }
                // Iniciar hilo de recepción de audio para el propio iniciador
                new Thread(() -> manejarRecepcionAudio(this)).start();
                // Iniciar envío de audio en el grupo
                manejarEnvioAudioGrupo(miembrosGrupo);
            }
        }

        private void manejarLlamadaPrivada() {
            ClientHandler receptor = usuariosConectados.get(chatPrivadoActual);
            if (receptor != null) {
                enLlamada = true;
                receptor.enLlamada = true;
                out.println("Llamada aceptada.");
                receptor.out.println("Llamada aceptada.");
                // Iniciar hilo de recepción de audio para ambos
                new Thread(() -> manejarRecepcionAudio(receptor)).start();
                new Thread(() -> manejarRecepcionAudio(this)).start();
                // Envío de audio
                manejarEnvioAudioPrivado(receptor);
            }
        }

        private void manejarRecepcionAudio(ClientHandler receptor) {
            try {
                byte[] buffer = new byte[BUFFER_SIZE];
                int bytesRead;
                while (receptor.enLlamada && (bytesRead = receptor.inputStream.read(buffer)) != -1) {
                    // Reproducir el audio recibido (simulado aquí con la impresión de bytes)
                    outputStream.write(buffer, 0, bytesRead);
                    outputStream.flush();
                }
            } catch (IOException e) {
                System.err.println("Error recibiendo audio: " + e.getMessage());
            }
        }


        private void manejarEnvioAudio() {
            if (enLlamada) {
                try {
                    byte[] buffer = new byte[BUFFER_SIZE];
                    int bytesRead;
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        if (grupoActual != null) {
                            List<ClientHandler> miembrosGrupo = grupos.get(grupoActual);
                            broadcastToGrupo(buffer, bytesRead, miembrosGrupo);
                        } else if (chatPrivadoActual != null) {
                            ClientHandler receptor = usuariosConectados.get(chatPrivadoActual);
                            broadcastToPrivado(buffer, bytesRead, receptor);
                        }
                    }
                } catch (IOException e) {
                    System.err.println("Error enviando audio: " + e.getMessage());
                }
            }
        }

        private void manejarEnvioAudioGrupo(List<ClientHandler> miembrosGrupo) {
            try {
                byte[] buffer = new byte[BUFFER_SIZE];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    broadcastToGrupo(buffer, bytesRead, miembrosGrupo);
                }
            } catch (IOException e) {
                System.err.println("Error enviando audio en grupo: " + e.getMessage());
            }
        }

        private void manejarEnvioAudioPrivado(ClientHandler receptor) {
            try {
                byte[] buffer = new byte[BUFFER_SIZE];
                int bytesRead;
                String input = in.readLine();
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    procesarInput(input);
                    broadcastToPrivado(buffer, bytesRead, receptor);
                }

            } catch (IOException e) {
                System.err.println("Error enviando audio en chat privado: " + e.getMessage());
            }

        }

        // Enviar audio a los miembros de un grupo
        private void broadcastToGrupo(byte[] buffer, int bytesRead, List<ClientHandler> miembrosGrupo) throws IOException {
            synchronized (grupos) {
                for (ClientHandler client : miembrosGrupo) {
                    if (client != this) {
                        client.outputStream.write(buffer, 0, bytesRead);
                        client.outputStream.flush();
                    }
                }
            }
        }

        // Enviar audio a un usuario en un chat privado
        private void broadcastToPrivado(byte[] buffer, int bytesRead, ClientHandler receptor) throws IOException {
            if (receptor != null && receptor != this) {
                try {
                    receptor.outputStream.write(buffer, 0, bytesRead);
                    receptor.outputStream.flush();

                } catch (IOException e) {
                    System.err.println("Error enviando audio a cliente: " + e.getMessage());
                    usuariosConectados.remove(receptor);
                    try {
                        receptor.socket.close();
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                }
            }
        }

        private void terminarLlamada(){
            if (grupoActual != null ||chatPrivadoActual != null) {
                this.out.println("colgar");
                start();

            }else{
                out.println("No estas en una llamada.");
            }

        }



    }
}

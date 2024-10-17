package co.com.icesi.Constants;

public class Constants {

    //Ruta database
    public static final String RUTA_DATABASE = System.getProperty("user.dir") + "/database/";


    // Colores ANSI
    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_YELLOW = "\u001B[33m";
    public static final String ANSI_BLUE = "\u001B[34m";
    public static final String ANSI_PURPLE = "\u001B[35m";
    public static final String ANSI_CYAN = "\u001B[36m";



    // Menú principal
    public static final String MENU_PRINCIPAL = ANSI_CYAN + "===== SELECCIONA UNA OPCION =====" + ANSI_RESET +
            "\n" + ANSI_YELLOW + "1. Comandos de Grupo" + ANSI_RESET +
            "\n" + ANSI_YELLOW + "2. Comandos de Chat Privado" + ANSI_RESET +
            "\n" + ANSI_YELLOW + "3. Comandos Generales" + ANSI_RESET +
            "\n" + ANSI_YELLOW + "4. Abandonar Servidor" + ANSI_RESET +
            "\n" + ANSI_YELLOW + "/ayuda " + ANSI_RESET + "- Utilizalo para volver al menú" +
            "\n" + ANSI_CYAN + "=================================" + ANSI_RESET;

    // Comandos de grupo
    public static final String COMANDOS_GRUPO = ANSI_GREEN + "===== COMANDOS DE GRUPO =====" + ANSI_RESET +
            "\n" + ANSI_GREEN + "/creargrupo <nombre_grupo>" + ANSI_RESET + "          - Crear un nuevo grupo." +
            "\n" + ANSI_GREEN + "/unirse <nombre_grupo>" + ANSI_RESET + "              - Unirse a un grupo existente." +
            "\n" + ANSI_GREEN + "/ingresar <nombre_grupo>" + ANSI_RESET + "            - Volver a ingresar a un grupo existente." +
            "\n" + ANSI_GREEN + "/abandonargrupo" + ANSI_RESET + "                     - Salir completamente del grupo actual." +
            "\n" + ANSI_PURPLE + "/salir" + ANSI_RESET + "                             - Salir del grupo o chat privado actual.\n" +
            "\n" + ANSI_GREEN + "/grupos" + ANSI_RESET + "                             - Mostrar grupos disponibles.\n" +
            "\n" + ANSI_BLUE + "/historial <usuario/grupo>" + ANSI_RESET + "           - Ver el historial de mensajes de un grupo o usuario.";

    // Comandos de chat privado
    public static final String COMANDOS_CHAT_PRIVADO = ANSI_PURPLE + "===== COMANDOS DE CHAT PRIVADO =====" + ANSI_RESET +
            "\n" + ANSI_PURPLE + "/privado <nombre_usuario>" + ANSI_RESET + "           - Iniciar un chat privado con un usuario." +
            "\n" + ANSI_BLUE + "/historial <tuUsuario-usuarioDestino>" + ANSI_RESET + " - Ver el historial de mensajes de un grupo o usuario." +
            "\n" + ANSI_PURPLE + "/salir" + ANSI_RESET + "                              - Salir del grupo o chat privado actual.\n";

    // Comandos generales
    public static final String COMANDOS_GENERALES = ANSI_BLUE + "===== COMANDOS GENERALES =====" + ANSI_RESET +
            "\n" + ANSI_BLUE + "/mensaje <usuario/grupo> <mensaje>" + ANSI_RESET + "  - Enviar un mensaje a un usuario o grupo." +
            "\n" + ANSI_BLUE + "/enviaraudio <usuario/grupo> <archivo_audio>" + ANSI_RESET + "  - Enviar un archivo de audio a un usuario o grupo." +
            "\n" + ANSI_BLUE + "/escucharaudio <nombre_audio>" + ANSI_RESET + " - Escuchar un archivo de audio enviado previamente." +
            "\n" + ANSI_BLUE + "/usuarios" + ANSI_RESET + "                           - Mostrar usuarios conectados." +
            "\n" + ANSI_BLUE + "/historial <grupo>" + ANSI_RESET + "          - Ver el historial de mensajes de un grupo o usuario." +
            "\n" + ANSI_BLUE + "/llamar + ANSI_RESET" + ANSI_RESET + "llamar" +
            "\n" + ANSI_BLUE + "/terminarllamada + ANSI_RESET" + ANSI_RESET + "colgar" +

            "\n" + ANSI_BLUE + "/help" + ANSI_RESET + "                               - Mostrar comandos.\n";
}

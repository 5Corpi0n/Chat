package co.com.icesi;

import co.com.icesi.Constants.Constants;

import java.awt.*;
import java.io.*;
import java.net.*;

public class Cliente {

    private static boolean onCall = false;

    public static void main(String[] args) {
        String serverAddress = "localhost";
        int port = 12345;

        try (Socket socket = new Socket(serverAddress, port);
             BufferedReader input = new BufferedReader(new InputStreamReader(System.in));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            AudioClient audioClient = new AudioClient(socket);

            Thread messageReceiver = new Thread(() -> {
                try {
                    run(socket, in, audioClient);
                } catch (IOException e) {
                    System.err.println("Error al leer mensajes del servidor: " + e.getMessage());
                }
            });
            messageReceiver.start();

            //new Thread(() -> recibirAudio(socket)).start();

            String clientInput;
            while ((clientInput = input.readLine()) != null) {
                out.println(clientInput);
            }
        } catch (IOException e) {
            System.err.println("Error en la conexión con el servidor: " + e.getMessage());
        }
    }

    private static void run(Socket socket, BufferedReader in, AudioClient audioClient) throws IOException {
        String serverMessage;
        while ((serverMessage = in.readLine()) != null) {
            try {
                if (serverMessage.contains(".wav")) {
                    String ruta = Constants.RUTA_DATABASE + serverMessage;
                    recibirYGuardarAudio(socket, ruta);
                } else if (serverMessage.contains("audios")) {
                    escucharAudio(Constants.RUTA_DATABASE + serverMessage);
                } else if (serverMessage.contains("Llamada aceptada")) {
                    onCall = true; // Iniciar la llamada de audio
                    audioClient.startSendingAudio(); // Hilo para enviar audio
                    audioClient.startReceivingAudio(); // Hilo para recibir audio
                } else if (serverMessage.contains("colgar")) {
                    onCall = false; // Terminar la llamada de audio
                    audioClient.stopAudio(); // Detener el envío y la recepción de audio
                    System.out.println("Llamada terminada.");
                }
                else
                    System.out.println(serverMessage);
            } catch (Exception e) {
                System.err.println("Error al procesar el mensaje del servidor: " + e.getMessage());
            }
        }
    }

    private static void recibirYGuardarAudio(Socket socket, String rutaDestino) {
        File archivoAudio = new File(rutaDestino);
        if (!archivoAudio.getParentFile().exists()) {
            archivoAudio.getParentFile().mkdirs();
        }

        try (InputStream is = socket.getInputStream();
             FileOutputStream fos = new FileOutputStream(archivoAudio)) {
            byte[] buffer = new byte[4096];
            int bytesLeidos;

            while ((bytesLeidos = is.read(buffer)) != -1) {
                fos.write(buffer, 0, bytesLeidos);
            }

        } catch (IOException e) {
            System.err.println("Error al recibir el archivo de audio: " + e.getMessage());
        }
    }

    private static void escucharAudio(String ruta) {
        String[] temp = ruta.split("/");
        String nombreAudio = temp[temp.length - 1].replace(".wav", "");

        File audioFile = new File(ruta);
        if (audioFile.exists()) {
            System.out.println(Constants.ANSI_GREEN + "Reproduciendo audio: " + nombreAudio + Constants.ANSI_RESET);

            try {
                if (Desktop.isDesktopSupported()) {
                    Desktop desktop = Desktop.getDesktop();
                    if (desktop.isSupported(Desktop.Action.OPEN)) {
                        desktop.open(audioFile);
                    } else {
                        System.out.println(Constants.ANSI_RED + "No se puede abrir el archivo de audio en este sistema." + Constants.ANSI_RESET);
                    }
                } else {
                    System.out.println(Constants.ANSI_RED + "La clase Desktop no es soportada en este sistema." + Constants.ANSI_RESET);
                }
            } catch (IOException e) {
                System.out.println(Constants.ANSI_RED + "Error al intentar abrir el archivo de audio: " + e.getMessage() + Constants.ANSI_RESET);
            }
        } else {
            System.out.println(Constants.ANSI_RED + "El archivo de audio no se encontró: " + nombreAudio + Constants.ANSI_RESET);
        }
    }

}
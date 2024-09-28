package co.com.icesi;

import java.io.*;
import java.net.*;

public class Cliente {
    public static void main(String[] args) {
        String serverAddress = "localhost"; // Cambiar a la IP del servidor si no es localhost
        int port = 12345;

        try (Socket socket = new Socket(serverAddress, port);
             BufferedReader input = new BufferedReader(new InputStreamReader(System.in));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            // Hilo para leer mensajes desde el servidor
            Thread messageReceiver = new Thread(() -> {
                try {
                    String serverMessage;
                    while ((serverMessage = in.readLine()) != null) {
                        System.out.println(serverMessage);
                    }
                } catch (IOException e) {
                    System.err.println("Error al leer mensajes del servidor: " + e.getMessage());
                }
            });
            messageReceiver.start();

            // Enviar comandos y mensajes al servidor
            String clientInput;
            while ((clientInput = input.readLine()) != null) {
                out.println(clientInput);
            }
        } catch (IOException e) {
            System.err.println("Error en la conexión con el servidor: " + e.getMessage());
        }
    }
}
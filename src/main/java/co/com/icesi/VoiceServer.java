package co.com.icesi;

import java.net.*;

public class VoiceServer {
    public static void main(String[] args) throws Exception {
        DatagramSocket serverSocket = new DatagramSocket(9876);
        byte[] receiveData = new byte[1024];
        System.out.println("Servidor UDP para llamadas/notas de voz iniciado...");

        while (true) {
            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
            serverSocket.receive(receivePacket);

            String sentence = new String(receivePacket.getData(), 0, receivePacket.getLength());
            System.out.println("Mensaje recibido: " + sentence);

            // Transmitir la nota de voz o llamada a otros usuarios del grupo
            InetAddress IPAddress = receivePacket.getAddress();
            int port = receivePacket.getPort();
            DatagramPacket sendPacket = new DatagramPacket(receiveData, receiveData.length, IPAddress, port);
            serverSocket.send(sendPacket);
        }
    }
}


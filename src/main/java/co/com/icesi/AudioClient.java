package co.com.icesi;


import javax.sound.sampled.*;
import java.io.*;
import java.net.*;

public class AudioClient {
    private static final int BUFFER_SIZE = 4096;
    private final Socket socket;
    private volatile boolean running;  // Indicador para controlar la transmisión de audio

    public AudioClient(Socket socket) {
        this.socket = socket;
        this.running = true; // Inicialmente está en funcionamiento
    }

    public void startReceivingAudio() {
        this.running = true;
        new Thread(this::receiveAudioFromServer).start();
    }

    public void startSendingAudio() {
        this.running = true;
        new Thread(this::sendAudioToServer).start();
    }

    public void stopAudio() {
        running = false; // Detener el envío y la recepción de audio
    }

    private void sendAudioToServer() {
        try {
            AudioFormat format = getAudioFormat();
            TargetDataLine microphone = AudioSystem.getTargetDataLine(format);
            microphone.open(format);
            microphone.start();

            OutputStream outputStream = socket.getOutputStream();
            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead;

            while (running) {  // Continuar solo si 'running' es true
                bytesRead = microphone.read(buffer, 0, buffer.length);
                if (bytesRead > 0) {
                    outputStream.write(buffer, 0, bytesRead);
                    outputStream.flush();
                }
            }

            // Parar y cerrar el micrófono al detener la transmisión
            microphone.stop();
            microphone.close();

        } catch (LineUnavailableException e) {
            System.err.println("Línea de audio no disponible: " + e.getMessage());
        } catch (IOException e) {
            System.err.println("Error de E/S: " + e.getMessage());
        }
    }

    private void receiveAudioFromServer() {
        try {
            AudioFormat format = getAudioFormat();
            SourceDataLine speakers = AudioSystem.getSourceDataLine(format);
            speakers.open(format);
            speakers.start();

            InputStream inputStream = socket.getInputStream();
            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead;

            // Tamaño de un frame de audio (en bytes)
            int frameSize = format.getFrameSize();

            while (running) {  // Continuar solo si 'running' es true
                bytesRead = inputStream.read(buffer);
                if (bytesRead > 0) {
                    // Asegurarse de escribir un número entero de frames
                    int framesToWrite = (bytesRead / frameSize) * frameSize;
                    if (framesToWrite > 0) {
                        speakers.write(buffer, 0, framesToWrite);
                    }
                }
            }

            // Parar y cerrar los altavoces al detener la transmisión
            speakers.stop();
            speakers.close();

        } catch (IOException e) {
            System.err.println("Error de E/S: " + e.getMessage());
        } catch (LineUnavailableException e) {
            System.err.println("Línea de audio no disponible: " + e.getMessage());
        }
    }

    private AudioFormat getAudioFormat() {
        float sampleRate = 44100; // Frecuencia de muestreo estándar
        int sampleSizeInBits = 16; // Tamaño de muestra
        int channels = 1; // Mono
        boolean signed = true; // PCM
        boolean bigEndian = false; // Little-endian
        return new AudioFormat(sampleRate, sampleSizeInBits, channels, signed, bigEndian);
    }
}

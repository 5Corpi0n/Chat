package co.com.icesi.database;

import co.com.icesi.Constants.Constants;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.UUID;

public class GuardarHistorial {

    public static void guardarHistorial(String nombreArchivo, String contenido) {
        // Especificar la ruta del archivo txt
        String rutaArchivo = Constants.RUTA_DATABASE + nombreArchivo + "/" + nombreArchivo + ".txt";

        File archivoHistorial = new File(rutaArchivo);

        // Crear la carpeta si no existe
        File carpeta = archivoHistorial.getParentFile();
        if (!carpeta.exists()) {
            carpeta.mkdirs(); // Crear todas las carpetas necesarias
        }

        // Abrir el archivo en modo de "apéndice" para agregar nuevas líneas
        try (FileWriter writer = new FileWriter(archivoHistorial, true)) {
            writer.write(contenido + System.lineSeparator()); // Agregar contenido al final
            System.out.println("Historial actualizado en: " + archivoHistorial.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace(); // Manejar errores de IO
        }
    }

    public static void guardarAudio(String nombreArchivo, byte[] audioData) {
        // Generar un nombre único para el archivo de audio
        String nombreAudio = "audio_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString();

        // Especificar la ruta del archivo de audio (ejemplo: .wav)
        String rutaArchivoAudio = Constants.RUTA_DATABASE + nombreArchivo + "/" + nombreAudio + ".wav";

        File archivoAudio = new File(rutaArchivoAudio);

        // Crear la carpeta si no existe
        File carpeta = archivoAudio.getParentFile();
        if (!carpeta.exists()) {
            carpeta.mkdirs(); // Crear todas las carpetas necesarias
        }

        // Guardar el archivo de audio
        try (FileOutputStream fos = new FileOutputStream(archivoAudio)) {
            fos.write(audioData); // Escribir los datos del archivo de audio
        } catch (IOException e) {
            e.printStackTrace(); // Manejar errores de IO
        }
    }

}


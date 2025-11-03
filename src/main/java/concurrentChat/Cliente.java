package concurrentChat;

import Handlers.WriteHandler;
import Handlers.ReadHandler;
import java.io.IOException;
import java.net.Socket;
import java.util.Scanner;

public class Cliente {

    private static int PORT = 8080;
    private static String ADDRESS = "localhost";

    public static Socket conection = null;

    public static void main(String[] args) throws IOException {

        Scanner scanner = new Scanner(System.in);

        System.out.println("Escribe 'start-conection' para iniciar:");
        String commands = scanner.nextLine();

        switch (commands) {
            case "start-conection":
                StartConection(PORT, ADDRESS);
                break;
            default:
                System.err.println("Comando desconocido. Terminando.");
                return;
        }

        // Si la conexión falló, 'conection' será null
        if (conection == null) {
            System.err.println("Error: No se pudo establecer la conexión. El programa terminará.");
            return;
        }

        System.out.println("¡Conexión exitosa! Bienvenido al chat.");
        System.out.println("Comandos disponibles: /nick <nombre>, /msg <usuario> <mensaje>, /global <mensaje>, /list, /help, /exit");

        WriteHandler writer = new WriteHandler(conection);
        ReadHandler reader = new ReadHandler(conection);

        Thread writeThread = new Thread(writer);
        Thread readThread = new Thread(reader);

        writeThread.start();
        readThread.start();

    }

    // IMPLEMTAR: ip y port, sean dinamicos.
    // solicitar ip y port.
    // Mejora: cambiar coneccion.
    public static void StartConection(int port, String address) {

        Scanner setupScanner = new Scanner(System.in);

        // Variables finales para la conexión
        String finalAddress = address; // Valor por defecto
        int finalPort = port;        // Valor por defecto

        // Solicitar IP
        System.out.println("Introduce la IP del servidor (default: " + finalAddress + "):");
        String inputIp = setupScanner.nextLine();

        if (!inputIp.isEmpty()) {
            finalAddress = inputIp;
        }

        // Solicitar Puerto
        System.out.println("Introduce el Puerto (default: " + finalPort + "):");
        String inputPort = setupScanner.nextLine();

        try {
            if (!inputPort.isEmpty()) {
                finalPort = Integer.parseInt(inputPort);
            }
        } catch (NumberFormatException e) {
            System.err.println("Puerto inválido, usando el puerto por defecto: " + finalPort);
        }

        // Intenta la conexión con los datos finales
        try {
            System.out.println("Iniciando conexión a " + finalAddress + ":" + finalPort + "...");
            conection = new Socket(finalAddress, finalPort);

            // Actualizamos las variables estáticas por consistencia
            ADDRESS = finalAddress;
            PORT = finalPort;

        } catch (IOException ex) {
            System.err.println("Error: No se pudo conectar al servidor en " + finalAddress + ":" + finalPort);
            // System.getLogger(Cliente.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
        }
    }

}

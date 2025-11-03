package Handlers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Set;

public class ClientHandler implements Runnable {

    private final Socket socket;
    private final Set<ClientHandler> clients;
    private BufferedReader in;
    private PrintWriter out;

    String username = "";

    public ClientHandler(Socket clientSocket, Set<ClientHandler> clients) {
        this.socket = clientSocket;
        this.clients = clients;
    }

    @Override
    public void run() {

        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);

            Thread.sleep(500);

            out.println("Se ha establecido la conexión");
            out.println("Introduce tu nombre de usuario:");

            // Establecer nombre de usuario
            String inputName = in.readLine();
            if (inputName == null || inputName.trim().isEmpty() || inputName.trim().contains(" ")) {
                this.username = "invitado_" + socket.getPort();
                out.println("servidor: Nombre inválido. Se te asigna: " + this.username);
            } else {
                this.username = inputName.trim();
            }

            // Imprime en el servidor.
            System.err.println(username + " se ha unido al chat");
            // Notificar a todos
            globalMessage("servidor", this.username + " se ha unido al chat.", true);

            // Bucle principal para procesar mensajes y comandos
            String inputMessage;
            while ((inputMessage = in.readLine()) != null) {

                if (inputMessage.equalsIgnoreCase("/exit")) {
                    break;
                }

                // Manejar comandos
                if (inputMessage.startsWith("/")) {
                    handleCommand(inputMessage);
                } else {
                    // Si no es un comando, es un mensaje global por defecto
                    globalMessage(this.username, inputMessage, false);
                }
            }

        } catch (IOException ex) {
            System.err.println("Cliente " + username + " desconectado abruptamente.");
        } catch (InterruptedException ex) {
            System.getLogger(ClientHandler.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
        } finally {
            // Lógica de desconexión
            clients.remove(this);
            System.err.println(username + " ha abandonado el chat.");
            // Notificar a todos
            globalMessage("servidor", username + " ha abandonado el chat.", true);
            try {
                socket.close();
            } catch (IOException e) {

            }
        }
    }


    // Helper para procesar comandos del cliente.
    private void handleCommand(String message) {

        String[] parts = message.split(" ", 3);
        String command = parts[0].toLowerCase();

        switch (command) {
            case "/nick":
            case "/change-username":
                if (parts.length >= 2) {
                    ChangeUserName(parts[1]);
                } else {
                    out.println("servidor: Uso: /nick <nuevo_nombre>");
                }
                break;
            case "/msg":
            case "/send-msg":
                if (parts.length >= 3) {
                    privateMessage(parts[1], parts[2]);
                } else {
                    out.println("servidor: Uso: /msg <usuario_destino> <mensaje>");
                }
                break;
            case "/global":
            case "/global-msg":
                if (parts.length >= 2) {
                    String globalMsg = message.substring(parts[0].length() + 1);
                    globalMessage(this.username, globalMsg, false);
                } else {
                    out.println("servidor: Uso: /global <mensaje>");
                }
                break;
            case "/list":
                listUsers();
                break;
            case "/help":
                showHelp();
                break;
            default:
                out.println("servidor: Comando desconocido. Escribe /help para ver la lista de comandos.");
        }
    }


    // IMPLEMENTAR: Cambiar el nombre de usuario del cliente.
    // Mejora: notificar el cambio de nombre. 0.25
    public void ChangeUserName(String newName) {
        if (newName == null || newName.trim().isEmpty() || newName.contains(" ")) {
            out.println("servidor: Nombre no válido. No puede estar vacío.");
            return;
        }

        String oldName = this.username;
        this.username = newName.trim();

        out.println("servidor: Tu nombre ha sido cambiado a: " + this.username);

        // Mejora: Notificar a todos los demás
        String notification = oldName + " cambió su nombre a " + this.username;
        System.err.println("CAMBIO DE NOMBRE: " + notification); // Log en servidor
        globalMessage("servidor", notification, true);
    }


    // IMPLEMENTAR: Mandar mensaje privado a un usuario en la red.
    // Mejora: mandar una alerta si el usuario no esta en linea o no existe. 0.25
    public void privateMessage(String targetUsername, String message) {
        boolean userFound = false;
        for (ClientHandler client : clients) {
            if (client.username.equalsIgnoreCase(targetUsername)) {
                // No enviarse a uno mismo
                if (client.equals(this)) {
                    out.println("servidor: No puedes enviarte un mensaje privado a ti mismo.");
                    userFound = true;
                    break;
                }

                // Enviar mensaje al destinatario
                client.out.println("(Privado) " + this.username + ": " + message);

                // Confirmar al emisor que se envió
                this.out.println("servidor: Mensaje enviado a " + targetUsername + ": " + message);
                userFound = true;
                break;
            }
        }

        // Mejora: Alerta si no se encuentra
        if (!userFound) {
            this.out.println("servidor: Usuario '" + targetUsername + "' no encontrado o no está en línea.");
        }
    }

    /**
     * IMPLEMENTAR: Mandar mensaje global a todos los usuarios.
     * Notificar no. de usuarios que recibieron el mensaje. 0.25
     *
     * @param sender El nombre del remitente (o "SERVER")
     * @param message El mensaje a enviar
     * @param sendToSelf Si es true, se envía a todos (incluso al remitente),
     */

    public void globalMessage(String sender, String message, boolean sendToSelf) {
        int count = 0;
        String formattedMessage = sender + ": " + message;

        for (ClientHandler client : clients) {
            // Si sendToSelf es falso, el remitente no recibe su propio mensaje
            if (sendToSelf || !client.equals(this)) {
                client.out.println(formattedMessage);
                count++;
            }
        }

        // Mejora: Notificar al remitente cuántos lo recibieron
        if (!sendToSelf) {
            out.println("servidor: Mensaje global enviado a " + count + " usuarios");
        }

        if (!sender.equals("servidor")) {
            System.err.println("(Global) " + formattedMessage);
        }
    }

    // Funcion adicional: Lista de usuarios (/list).
    // Manda al cliente que lo solicitó una lista de todos los usuarios actualmente conectados al chat.

    public void listUsers() {
        out.println("--- Usuarios Conectados (" + clients.size() + ") ---");
        for (ClientHandler client : clients) {
            String tag = client.equals(this) ? " (Tú)" : "";
            out.println("- " + client.username + tag);
        }
        out.println("---------------------------");
    }

    // Funcion adicional: Ayuda (/help)
    // Muestra al cliente los comandos disponibles.
    private void showHelp() {
        out.println("--- Lista de Comandos ---");
        out.println("/nick <nuevo_nombre>   : Cambia tu nombre de usuario.");
        out.println("/msg <usuario> <msg>   : Envía un mensaje privado a un usuario.");
        out.println("/global <msg>          : Envía un mensaje a todos.");
        out.println("/list                  : Muestra la lista de usuarios conectados.");
        out.println("/help                  : Muestra esta ayuda.");
        out.println("/exit                  : Te desconecta del chat.");
        out.println("-------------------------");
    }
}



package Handlers;

import Handlers.Handler;
import java.io.IOException;
import java.net.Socket;
import java.util.Scanner;

public class WriteHandler extends Handler implements Runnable {

    private String messageTx = "_";

    public WriteHandler(Socket socket) {
        super(socket);
    }

    @Override
    public void run() {
        Scanner messageScanner = new Scanner(System.in);

        while (out!= null && !messageTx.equalsIgnoreCase("/exit")) {

            messageTx = messageScanner.nextLine();
            out.print(messageTx + "\r\n");
            System.out.println( "yo: " + messageTx );
            out.flush();
        }
        System.out.println("com.uacam.p03_chat.WriteHandler.run()");
        dismiss();

    }
}
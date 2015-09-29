/** Based off of www.tutorialspoint.com/java/java_networking.htm */

package socs.network.net;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;

public class Server extends Thread {
    private ServerSocket serverSocket;

    public Server(short port) throws IOException {
        serverSocket = new ServerSocket(port);
    }

    public void run() {
        while (true) {
            try {
                System.out.println("Waiting for client on port " + serverSocket.getLocalPort() + "...");
                Socket server = serverSocket.accept();
                System.out.println("Connected to " + server.getRemoteSocketAddress());
                DataInputStream in = new DataInputStream(server.getInputStream());
                System.out.println(in.readUTF());
                DataOutputStream out = new DataOutputStream(server.getOutputStream());
                out.writeUTF("Hello from " + server.getLocalSocketAddress());
            } catch (SocketTimeoutException timeout) {
                System.out.println("Socket timed out :(");
                break;
            } catch (IOException e) {
                e.printStackTrace();
                break;
            }
        }
    }
}

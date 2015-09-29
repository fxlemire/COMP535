/** Based off of www.tutorialspoint.com/java/java_networking.htm */

package socs.network.net;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class Client extends Thread {
    private Socket clientSocket;
    private String remoteIp;
    private short remotePort;

    public Client(String serverName, short port) {
        remoteIp = serverName;
        remotePort = port;
    }

    @Override
    public void start() {
        try {
            System.out.println("Connecting to " + remoteIp + " on port " + remotePort);
            clientSocket = new Socket(remoteIp, remotePort);
            System.out.println("Just connected to " + clientSocket.getRemoteSocketAddress());
            super.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void run() {
        try {
            DataOutputStream out = new DataOutputStream(clientSocket.getOutputStream());
            out.writeUTF("Hello from " + clientSocket.getLocalSocketAddress());
            DataInputStream in = new DataInputStream(clientSocket.getInputStream());
            System.out.println("Server says " + in.readUTF());
            clientSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

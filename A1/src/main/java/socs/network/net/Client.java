/** Based off of www.tutorialspoint.com/java/java_networking.htm */

package socs.network.net;

import socs.network.message.SOSPFPacket;
import socs.network.node.RouterDescription;
import socs.network.node.RouterStatus;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class Client extends Thread {
    private Socket _clientSocket;
    private RouterDescription _remoteRouterDescription;
    private RouterDescription _rd;
    private String remoteRouterIP;

    public Client(RouterDescription remoteRouter, RouterDescription rd) {
        _remoteRouterDescription = remoteRouter;
        _rd = rd;
    }

    @Override
    public void start() {
        try {
            short remotePort = _remoteRouterDescription.getProcessPortNumber();
            remoteRouterIP = _remoteRouterDescription.getSimulatedIPAddress();

            System.out.println("Connecting to " + remoteRouterIP);
            _clientSocket = new Socket(_remoteRouterDescription.getProcessIPAddress(), remotePort);
            System.out.println("Just connected to " + _clientSocket.getRemoteSocketAddress());
            super.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void run() {
        connect();
    }

    private void connect() {
        sendHello();
        Util.receiveMessage(_clientSocket);
        _remoteRouterDescription.setStatus(RouterStatus.TWO_WAY);
        sendHello();
    }

    private void sendHello() {
        try {
            System.out.println("Sending HELLO message to " + remoteRouterIP + "...");
            SOSPFPacket message = Util.makeMessage(_rd, _remoteRouterDescription, (short) 0);
            ObjectOutputStream out = new ObjectOutputStream(_clientSocket.getOutputStream());
            out.writeObject(message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

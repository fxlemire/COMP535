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
    private String _remoteRouterIP;

    public Client(RouterDescription remoteRouter, RouterDescription rd) {
        _remoteRouterDescription = remoteRouter;
        _rd = rd;
        _remoteRouterIP = _remoteRouterDescription.getSimulatedIPAddress();

        try {
            System.out.println("Connecting to " + _remoteRouterIP);
            _clientSocket = new Socket(_remoteRouterDescription.getProcessIPAddress(), _remoteRouterDescription.getProcessPortNumber());
            System.out.println("Just connected to " + _clientSocket.getRemoteSocketAddress());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void run() {
        sendHello();
        Util.receiveMessage(_clientSocket);
        _remoteRouterDescription.setStatus(RouterStatus.TWO_WAY);
        sendHello();
    }

    private void sendHello() {
        try {
            System.out.println("Sending HELLO message to " + _remoteRouterIP + "...");
            SOSPFPacket message = Util.makeMessage(_rd, _remoteRouterDescription, (short) 0);
            ObjectOutputStream out = new ObjectOutputStream(_clientSocket.getOutputStream());
            out.writeObject(message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

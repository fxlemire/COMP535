/** Based off of www.tutorialspoint.com/java/java_networking.htm */

package socs.network.net;

import socs.network.message.SOSPFPacket;
import socs.network.node.Router;
import socs.network.node.RouterDescription;
import socs.network.node.RouterStatus;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class Client implements Runnable {
    private ObjectInputStream _inputStream = null;
    private ObjectOutputStream _outputStream = null;
    private Router _router;
    private RouterDescription _remoteRouterDescription;
    private RouterDescription _rd;
    private Socket _clientSocket;
    private String _remoteRouterIP;

    public Client(RouterDescription remoteRouter, Router router) {
        _remoteRouterDescription = remoteRouter;
        _router = router;
        _rd = router.getRd();
        _remoteRouterIP = _remoteRouterDescription.getSimulatedIPAddress();

        try {
            System.out.println("Connecting to " + _remoteRouterIP + "...");
            _clientSocket = new Socket(_remoteRouterDescription.getProcessIPAddress(), _remoteRouterDescription.getProcessPortNumber());
            System.out.println("Just connected to " + _remoteRouterIP + "...");
        } catch (IOException e) {
            e.printStackTrace();
        }

        Thread runner = new Thread(this);
        runner.start();
    }

    public void run() {
        try {
            _outputStream = new ObjectOutputStream(_clientSocket.getOutputStream());
            sendHello();
            _inputStream = new ObjectInputStream(_clientSocket.getInputStream());
            SOSPFPacket message = Util.receiveMessage(_inputStream);
            if (message.sospfType == SOSPFPacket.OVER_BURDENED) {
                System.out.println("Removing link with router " + message.srcIP + "...");
                _router.removeLink(_remoteRouterIP);
                return;
            }
            _remoteRouterDescription.setStatus(RouterStatus.TWO_WAY);
            sendHello();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendHello() {
        try {
            //System.out.println("Sending HELLO message to " + _remoteRouterIP + "...");
            SOSPFPacket message = Util.makeMessage(_rd, _remoteRouterDescription, SOSPFPacket.HELLO);
            _outputStream.writeObject(message);
            _outputStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

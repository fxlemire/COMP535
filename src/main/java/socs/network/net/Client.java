/** Based off of www.tutorialspoint.com/java/java_networking.htm */

package socs.network.net;

import socs.network.message.SOSPFPacket;
import socs.network.node.Link;
import socs.network.node.Router;
import socs.network.node.RouterDescription;
import socs.network.node.RouterStatus;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class Client implements Runnable {
    private Link _link;
    private ObjectInputStream _inputStream = null;
    private ObjectOutputStream _outputStream = null;
    private Router _router;
    private RouterDescription _remoteRouterDescription;
    private RouterDescription _rd;
    private Socket _clientSocket;
    private String _remoteRouterIP;
    private Thread _runner;

    public Thread getRunner() { return _runner; }

    public RouterDescription getRemoteRouterDescription() {
        return _remoteRouterDescription;
    }

    private Client(RouterDescription remoteRouter, Router router, Link link) {
        _link = link;
        _remoteRouterDescription = remoteRouter;
        _router = router;
        _rd = router.getRd();
        _remoteRouterIP = _remoteRouterDescription.getSimulatedIPAddress();

        try {
            System.out.println("Connecting to " + _remoteRouterIP + "...");
            _clientSocket = new Socket(_remoteRouterDescription.getProcessIPAddress(), _remoteRouterDescription.getProcessPortNumber());
            System.out.println("Just connected to " + _remoteRouterIP + "...");
        } catch (Exception e) {
            //e.printStackTrace();
        }

        _runner = new Thread(this);
    }

    public static Client runNonBlocking(RouterDescription remoteRouter, Router router, Link link) {
        Client client = new Client(remoteRouter, router, link);
        client.getRunner().start();
        return client;
    }

    public void run() {
        try {
            _outputStream = new ObjectOutputStream(_clientSocket.getOutputStream());
            sendMessage(SOSPFPacket.HELLO);
            _inputStream = new ObjectInputStream(_clientSocket.getInputStream());
            SOSPFPacket message = Util.receiveMessage(_inputStream);

            if (message.sospfType == SOSPFPacket.OVER_BURDENED) {
                System.out.println("Removing link with router " + message.srcIP + "...");
                _router.removeLink(_remoteRouterIP);
                return;
            }

            _remoteRouterDescription.setStatus(RouterStatus.TWO_WAY);
            _router.addLinkDescriptionToDatabase(_remoteRouterDescription, _link.getWeight());
            sendMessage(SOSPFPacket.HELLO);

            while (true) {
                message = Util.receiveMessage(_inputStream);

                if (message.sospfType == SOSPFPacket.LSU) {
                    Util.synchronizeAndPropagate(message, _router);
                }
            }
        } catch (Exception e) {
            //e.printStackTrace();
        } finally {
            try {
                _inputStream.close();
                _outputStream.close();
                _clientSocket.close();
                //System.out.println("...Stopped");
            } catch (Exception e) {
                //e.printStackTrace();
            }
        }
    }

    private void sendMessage(short messageType) {
        //System.out.println("Sending HELLO message to " + _remoteRouterIP + "...");
        SOSPFPacket message = Util.makeMessage(_rd, _remoteRouterDescription, messageType, _router);
        Util.sendMessage(message, _outputStream);
    }

    public void propagateSynchronization(String initiator, short synchronizationType, String disconnectInitiator, String disconnectVictim) {
        SOSPFPacket message;

        if (synchronizationType == SOSPFPacket.DISCONNECT) {
            message = Util.makeMessage(_rd, _remoteRouterDescription, synchronizationType, _router, disconnectInitiator, disconnectVictim);
        } else {
            message = Util.makeMessage(_rd, _remoteRouterDescription, synchronizationType, _router);
            message.lsaInitiator = initiator;
        }

        Util.sendMessage(message, _outputStream);
    }

    public boolean isFor(String remoteIp) {
        return _remoteRouterDescription.getSimulatedIPAddress().equals(remoteIp);
    }
}

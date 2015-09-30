/**
 * Based off of www.tutorialspoint.com/java/java_networking.htm
 * and
 * http://www.mysamplecode.com/2011/12/java-multithreaded-socket-server.html
 */

package socs.network.net;

import socs.network.message.SOSPFPacket;
import socs.network.node.Link;
import socs.network.node.Router;
import socs.network.node.RouterDescription;
import socs.network.node.RouterStatus;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class Server implements Runnable {
    private Router _router;
    private ServerSocket _serverSocket;

    public Server(Router router) {
        _router = router;
        Thread runner = new Thread(this);
        runner.start();
    }

    public void run() {
        try {
            _serverSocket = new ServerSocket(_router.getRd().getProcessPortNumber());
        } catch (IOException e) {
            e.printStackTrace();
        }

        while (true) {
            try {
                System.out.println("Waiting for client on port " + _serverSocket.getLocalPort() + "...");
                Socket clientSocket = _serverSocket.accept();
                new ClientServiceThread(clientSocket);
            } catch (IOException e) {
                e.printStackTrace();
                break;
            }
        }

        try {
            _serverSocket.close();
            System.out.println("Server stopped.");
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }

    private class ClientServiceThread implements Runnable {
        Socket _clientSocket;

        ClientServiceThread(Socket s) {
            _clientSocket = s;
            Thread runner = new Thread(this);
            runner.start();
        }

        public void run() {
            ObjectInputStream inputStream = null;
            ObjectOutputStream outputStream = null;

            try {
                inputStream = new ObjectInputStream(_clientSocket.getInputStream());
                outputStream = new ObjectOutputStream(_clientSocket.getOutputStream());

                while (true) {
                    try {
                        SOSPFPacket receivedMessage = Util.receiveMessage(inputStream);

                        RouterDescription routerAttachedDescription = new RouterDescription(
                                receivedMessage.srcProcessIP,
                                receivedMessage.srcProcessPort,
                                receivedMessage.srcIP);

                        updateLink(routerAttachedDescription);

                        RouterStatus routerAttachedStatus = routerAttachedDescription.getStatus();

                        if (routerAttachedStatus == RouterStatus.INIT || routerAttachedStatus == RouterStatus.OVER_BURDENED) {
                            final short messageType = routerAttachedStatus == RouterStatus.INIT ? SOSPFPacket.HELLO : SOSPFPacket.OVER_BURDENED;
                            SOSPFPacket outgoingMessage = Util.makeMessage(_router.getRd(), routerAttachedDescription, messageType);
                            outputStream.writeObject(outgoingMessage);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        break;
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    inputStream.close();
                    outputStream.close();
                    _clientSocket.close();
                    System.out.println("...Stopped");
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        private void updateLink(RouterDescription routerAttachedDescription) {
            routerAttachedDescription = updateWithNeighbourStatus(routerAttachedDescription);

            if (routerAttachedDescription.getStatus() == RouterStatus.INIT) {
                boolean isAdded = addRouterLink(routerAttachedDescription);

                if (!isAdded) {
                    routerAttachedDescription.setStatus(RouterStatus.OVER_BURDENED);
                }
            }
        }

        private RouterDescription updateWithNeighbourStatus(RouterDescription neighbourDescription) {
            Link[] ports = _router.getPorts();

            for (Link link : ports) {
                if (link == null) {
                    continue; //empty port
                }

                String neighbourIP = neighbourDescription.getSimulatedIPAddress();

                if (link.getRouter1().getSimulatedIPAddress().equals(neighbourIP)) {
                    neighbourDescription = link.getRouter1();
                } else if (link.getRouter2().getSimulatedIPAddress().equals(neighbourIP)) {
                    neighbourDescription = link.getRouter2();
                } else {
                    continue;
                }

                // since the neighbour has been found in the links, it had necessarily an INIT status, so set to TWO_WAY
                neighbourDescription.setStatus(RouterStatus.TWO_WAY);
                return neighbourDescription;
            }

            if (neighbourDescription.getStatus() == null) {
                neighbourDescription.setStatus(RouterStatus.INIT);
            }

            return neighbourDescription;
        }

        private boolean addRouterLink(RouterDescription routerAttachedDescription) {
            Link link = new Link(_router.getRd(), routerAttachedDescription);

            boolean isLinkAdded = _router.addLink(link);
            if (!isLinkAdded) {
                System.out.println("[ERROR] ROUTER ALREADY CONNECTED TO 4 OTHER ROUTERS. COULD NOT ADD LINK.");
                return false;
            }

            return true;
        }
    }
}

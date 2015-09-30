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

                        routerAttachedDescription = updateLink(routerAttachedDescription);

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

        private RouterDescription updateLink(RouterDescription routerAttachedDescription) {
            routerAttachedDescription = updateWithNeighbourStatus(routerAttachedDescription);

            if (routerAttachedDescription.getStatus() == RouterStatus.INIT) {
                boolean isAdded = _router.isLinkExisting(routerAttachedDescription.getProcessPortNumber(), routerAttachedDescription.getSimulatedIPAddress()) ||
                        addRouterLink(routerAttachedDescription);

                if (!isAdded) {
                    routerAttachedDescription.setStatus(RouterStatus.OVER_BURDENED);
                }
            }

            return routerAttachedDescription;
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

                RouterStatus neighbourStatus = neighbourDescription.getStatus();

                if (neighbourDescription.getStatus() == null) {
                    // case where "attach" is run on both routers before start. user should not do that, but it is handled
                    neighbourDescription.setStatus(RouterStatus.INIT);
                } else {
                    switch (neighbourStatus) {
                        case INIT:
                            neighbourDescription.setStatus(RouterStatus.TWO_WAY);
                            break;
                        case TWO_WAY:
                            System.out.println("Router already has a TWO_WAY status.");
                            break;
                        case OVER_BURDENED:
                            // status over_burdened should not be stored in the links
                            System.out.println("Something wrong happened, as a router with OVER_BURDENED status should not be linked.");
                            break;
                        default:
                            System.out.println("This should never happen...");
                    }
                }

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

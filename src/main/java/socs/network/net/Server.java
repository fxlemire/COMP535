/**
 * Based off of www.tutorialspoint.com/java/java_networking.htm
 * and
 * http://www.mysamplecode.com/2011/12/java-multithreaded-socket-server.html
 */

package socs.network.net;

import socs.network.message.LSA;
import socs.network.message.LinkDescription;
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
import java.util.Iterator;
import java.util.Optional;
import java.util.Vector;

public class Server implements Runnable {
    private Router _router;
    private ServerSocket _serverSocket;
    private Thread _runner;

    public Thread getRunner() { return _runner; }

    private Server(Router router) {
        _router = router;
        _runner = new Thread(this);
    }

    public static Server runNonBlocking(Router router) {
        Server server = new Server(router);
        server.getRunner().start();
        return server;
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
                ClientServiceThread cst = new ClientServiceThread(clientSocket);
                cst.getRunner().start();
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
        Thread _runner;

        ClientServiceThread(Socket s) {
            _clientSocket = s;
            _runner = new Thread(this);
        }

        public Thread getRunner() { return _runner; }

        public void run() {
            ObjectInputStream inputStream = null;
            ObjectOutputStream outputStream = null;

            try {
                inputStream = new ObjectInputStream(_clientSocket.getInputStream());
                outputStream = new ObjectOutputStream(_clientSocket.getOutputStream());

                while (true) {
                    try {
                        SOSPFPacket receivedMessage = Util.receiveMessage(inputStream);

                        switch (receivedMessage.sospfType) {
                            case SOSPFPacket.HELLO: {
                                RouterDescription routerAttachedDescription = new RouterDescription(
                                        receivedMessage.srcProcessIP,
                                        receivedMessage.srcProcessPort,
                                        receivedMessage.srcIP);

                                short weight = getLinkWeight(receivedMessage);

                                routerAttachedDescription = updateLink(routerAttachedDescription, weight, receivedMessage.lsaArray);

                                RouterStatus routerAttachedStatus = routerAttachedDescription.getStatus();

                                if (routerAttachedStatus == RouterStatus.INIT || routerAttachedStatus == RouterStatus.OVER_BURDENED) {
                                    final short messageType = routerAttachedStatus == RouterStatus.INIT ? SOSPFPacket.HELLO : SOSPFPacket.OVER_BURDENED;
                                    SOSPFPacket outgoingMessage = Util.makeMessage(_router.getRd(), routerAttachedDescription, messageType, _router);
                                    outputStream.writeObject(outgoingMessage);
                                }

                                if (routerAttachedStatus == RouterStatus.TWO_WAY) {
                                    SOSPFPacket outgoingMessage = Util.makeMessage(_router.getRd(), routerAttachedDescription, SOSPFPacket.LSU, _router);
                                    outputStream.writeObject(outgoingMessage);
                                    _router.propagateSynchronization(outgoingMessage.lsaInitiator, receivedMessage.srcIP);
                                }
                                break;
                            }
                            case SOSPFPacket.LSU: {
                                //get lsa
                                _router.synchronize(receivedMessage.lsaArray);

                                //propagate to neighbors
                                _router.propagateSynchronization(receivedMessage.lsaInitiator, receivedMessage.srcIP);
                                break;
                            }
                            case SOSPFPacket.OVER_BURDENED: {
                                //OVER_BURDENED should not be received by the router
                                //escalate
                            }
                            default: {
                                System.out.println("Invalid Message Type");
                            }
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

        private RouterDescription updateLink(RouterDescription routerAttachedDescription, short weight, Vector<LSA> lsaArray) {
            routerAttachedDescription = updateStatus(routerAttachedDescription, weight, lsaArray);

            if (routerAttachedDescription.getStatus() == RouterStatus.INIT) {
                boolean isAdded = !_router.isLinkExisting(routerAttachedDescription.getProcessPortNumber(), routerAttachedDescription.getSimulatedIPAddress()) &&
                        addRouterLink(routerAttachedDescription, weight);

                if (!isAdded) {
                    routerAttachedDescription.setStatus(RouterStatus.OVER_BURDENED);
                }
            }

            return routerAttachedDescription;
        }

        private RouterDescription updateStatus(RouterDescription neighbourDescription, short weight, Vector<LSA> lsaArray) {
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
                            _router.addLinkDescriptionToDatabase(neighbourDescription, weight);
                            _router.synchronize(lsaArray);
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

        private boolean addRouterLink(RouterDescription routerAttachedDescription, short weight) {
            Link link = new Link(_router.getRd(), routerAttachedDescription, weight);

            boolean isLinkAdded = _router.addLink(link);
            if (!isLinkAdded) {
                System.out.println("[ERROR] ROUTER ALREADY CONNECTED TO 4 OTHER ROUTERS. COULD NOT ADD LINK.");
                return false;
            }

            return true;
        }

        private short getLinkWeight(SOSPFPacket message) {
            short weight = 0;

            final Optional<LSA> lsaOption = message.lsaArray.stream().filter(l -> l.linkStateID.equals(message.srcIP)).findFirst();
            if (lsaOption.isPresent()) {
                Optional<LinkDescription> linkDescription = lsaOption.get().links.stream().findFirst().filter(ld -> ld.linkID.equals(_router.getRd().getSimulatedIPAddress()));
                if (linkDescription.isPresent()) {
                    weight = (short) linkDescription.get().tosMetrics;
                }
            }

            return weight;
        }
    }
}

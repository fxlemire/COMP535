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
import java.net.Socket;
import java.util.Optional;
import java.util.Vector;

public class ClientServiceThread implements Runnable {
    ObjectInputStream _inputStream = null;
    ObjectOutputStream _outputStream = null;
    Router _router;
    RouterDescription _remoteRouterDescription;
    Socket _clientSocket;
    Thread _runner;

    ClientServiceThread(Router r, Socket s) {
        _router = r;
        _clientSocket = s;
        _runner = new Thread(this);
    }

    public Thread getRunner() { return _runner; }

    public void run() {
        try {
            _inputStream = new ObjectInputStream(_clientSocket.getInputStream());
            _outputStream = new ObjectOutputStream(_clientSocket.getOutputStream());

            while (true) {
                SOSPFPacket receivedMessage = Util.receiveMessage(_inputStream);

                switch (receivedMessage.sospfType) {
                    case SOSPFPacket.HELLO: {
                        _remoteRouterDescription = new RouterDescription(
                                receivedMessage.srcProcessIP,
                                receivedMessage.srcProcessPort,
                                receivedMessage.srcIP);

                        short weight = getLinkWeight(receivedMessage);

                        _remoteRouterDescription = updateLink(_remoteRouterDescription, weight, receivedMessage.lsaArray);

                        RouterStatus routerAttachedStatus = _remoteRouterDescription.getStatus();

                        if (routerAttachedStatus == RouterStatus.INIT || routerAttachedStatus == RouterStatus.OVER_BURDENED) {
                            final short messageType = routerAttachedStatus == RouterStatus.INIT ? SOSPFPacket.HELLO : SOSPFPacket.OVER_BURDENED;
                            SOSPFPacket outgoingMessage = Util.makeMessage(_router.getRd(), _remoteRouterDescription, messageType, _router);
                            Util.sendMessage(outgoingMessage, _outputStream);
                        }

                        if (routerAttachedStatus == RouterStatus.TWO_WAY) {
                            SOSPFPacket outgoingMessage = Util.makeMessage(_router.getRd(), _remoteRouterDescription, SOSPFPacket.LSU, _router);
                            Util.sendMessage(outgoingMessage, _outputStream);
                            _router.propagateSynchronization(outgoingMessage.lsaInitiator, receivedMessage.srcIP);
                        }
                        break;
                    }
                    case SOSPFPacket.LSU: {
                        Util.synchronizeAndPropagate(receivedMessage, _router);
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
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                _inputStream.close();
                _outputStream.close();
                _clientSocket.close();
                System.out.println("...Stopped");
            } catch (IOException|NullPointerException e) {
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
                        _router.synchronize(lsaArray);
                        _router.addLinkDescriptionToDatabase(neighbourDescription, weight);
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
            Optional<LinkDescription> linkDescription = lsaOption.get().links.stream().filter(ld -> ld.linkID.equals(_router.getRd().getSimulatedIPAddress())).findFirst();
            if (linkDescription.isPresent()) {
                weight = (short) linkDescription.get().tosMetrics;
            }
        }

        return weight;
    }

    public void propagateSynchronization(String initiator) {
        SOSPFPacket message = Util.makeMessage(_router.getRd(), _remoteRouterDescription, SOSPFPacket.LSU, _router);
        message.lsaInitiator = initiator;
        Util.sendMessage(message, _outputStream);
    }

    public boolean isFor(String remoteIp) {
        return _remoteRouterDescription.getSimulatedIPAddress().equals(remoteIp);
    }
}
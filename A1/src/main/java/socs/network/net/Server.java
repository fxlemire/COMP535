/** Based off of www.tutorialspoint.com/java/java_networking.htm */

package socs.network.net;

import socs.network.message.SOSPFPacket;
import socs.network.node.Link;
import socs.network.node.Router;
import socs.network.node.RouterDescription;
import socs.network.node.RouterStatus;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;

public class Server extends Thread {
    private Router _router;
    private ServerSocket _serverSocket;

    public Server(Router router) throws IOException {
        _router = router;
        _serverSocket = new ServerSocket(_router.getRd().getProcessPortNumber());
    }

    public void run() {
        while (true) {
            try {
                System.out.println("Waiting for client on port " + _serverSocket.getLocalPort() + "...");

                Socket server = _serverSocket.accept();

                SOSPFPacket receivedMessage = Util.receiveMessage(server);

                RouterDescription routerAttachedDescription = new RouterDescription(
                        receivedMessage.srcProcessIP,
                        receivedMessage.srcProcessPort,
                        receivedMessage.srcIP);

                updateLink(routerAttachedDescription);

                if (routerAttachedDescription.getStatus() == RouterStatus.INIT) {
                    SOSPFPacket outgoingMessage = Util.makeMessage(_router.getRd(), routerAttachedDescription, (short) 0);
                    ObjectOutputStream out = new ObjectOutputStream(server.getOutputStream());
                    out.writeObject(outgoingMessage);
                }
            } catch (SocketTimeoutException timeout) {
                System.out.println("Socket timed out :(");
                break;
            } catch (IOException e) {
                e.printStackTrace();
                break;
            }
        }
    }

    private void updateLink(RouterDescription routerAttachedDescription) {
        routerAttachedDescription = updateWithNeighbourStatus(routerAttachedDescription);

        if (routerAttachedDescription.getStatus() == RouterStatus.INIT) {
            addRouterLink(routerAttachedDescription);
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

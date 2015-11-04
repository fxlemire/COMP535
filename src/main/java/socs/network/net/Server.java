/**
 * Based off of www.tutorialspoint.com/java/java_networking.htm
 * and
 * http://www.mysamplecode.com/2011/12/java-multithreaded-socket-server.html
 */

package socs.network.net;

import socs.network.node.Router;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Server implements Runnable {
    private ClientServiceThread[] _clientServicers = new ClientServiceThread[4];
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

    public ClientServiceThread[] getClientServicers() {
        return _clientServicers;
    }

    public void run() {
        try {
            _serverSocket = new ServerSocket(_router.getRd().getProcessPortNumber());
        } catch (IOException e) {
            e.printStackTrace();
        }

        while (true) {
            try {
                int port = getFreeServerPort();

                if (port != -1) {
                    System.out.println("Waiting for client on port " + _serverSocket.getLocalPort() + "...");
                    Socket clientSocket = _serverSocket.accept();
                    ClientServiceThread cst = new ClientServiceThread(_router, clientSocket);
                    _clientServicers[port] = cst;
                    cst.getRunner().start();
                }
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

    private int getFreeServerPort() {
        int port = -1;
        for (int i = 0; i < _clientServicers.length; ++i) {
            if (_clientServicers[i] == null) {
                port = i;
                break;
            }
        }
        return port;
    }
}

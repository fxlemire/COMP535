package socs.network.node;

import socs.network.message.LSA;
import socs.network.message.LinkDescription;
import socs.network.message.SOSPFPacket;
import socs.network.net.Client;
import socs.network.net.ClientServiceThread;
import socs.network.net.Server;
import socs.network.util.Configuration;
import socs.network.util.Util;
import socs.network.util.path.Dijkstra;
import socs.network.util.path.Edge;
import socs.network.util.path.Graph;
import socs.network.util.path.Vertex;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.*;


public class Router {

  protected LinkStateDatabase _lsd;
  RouterDescription _rd;
  final Object _lsdLock = new Object();

  //assuming that all routers are with 4 _ports
  Link[] _ports = new Link[4];
  Client[] _clients = new Client[4];
  ArrayList<ClientServiceThread> _clientServers = new ArrayList<>();
  HashMap<String, Integer> _initiators = new HashMap<String, Integer>();
  Server _server;
  boolean _isStarted = false;

  public Router(Configuration config) {
    _rd = new RouterDescription(Configuration.PROCESS_IP, config.getShort("socs.network.router.port"), config.getString("socs.network.router.ip"));
    _lsd = new LinkStateDatabase(_rd);
  }

  public void addClientServer(ClientServiceThread clientServer) {
    _clientServers.add(clientServer);
  }

  public Client[] getClients() {
    return _clients;
  }

  public LinkStateDatabase getLsd() {
    return _lsd;
  }

  public RouterDescription getRd() {
    return _rd;
  }

  public Link[] getPorts() {
    return _ports;
  }

  /**
   * output the shortest path to the given destination ip
   * <p/>
   * format: source ip address  -> ip address -> ... -> destination ip
   *
   * @param destinationIP the ip address of the destination simulated router
   */
  private void processDetect(String destinationIP) {
    ArrayList<Vertex> nodes = new ArrayList<>();
    ArrayList<Edge> edges = new ArrayList<>();

    _lsd._store.forEach((routerIp, lsa) -> nodes.add(new Vertex(routerIp, routerIp)));

    _lsd._store.forEach((routerIp, lsa) -> {
      Vertex source = Util.findVertex(nodes, routerIp).get();
      lsa.links.forEach(ld -> {
        if (!ld.linkID.equals(routerIp)) {
          Vertex destination = Util.findVertex(nodes, ld.linkID).get();
          edges.add(new Edge(routerIp + " -> " + ld.linkID, source, destination, ld.tosMetrics));
        }
      });
    });

    Graph graph = new Graph(nodes, edges);
    Dijkstra dijkstra = new Dijkstra(graph);
    Vertex source = Util.findVertex(nodes, _rd.getSimulatedIPAddress()).get();
    dijkstra.execute(source);
    Optional<Vertex> destination = Util.findVertex(nodes, destinationIP);

    if (destination.isPresent()) {
      LinkedList<Vertex> path = dijkstra.getPath(destination.get());

      for (int i = 0; i < path.size(); ++i) {
        Vertex v = path.get(i);
        System.out.print(v);
        if (i < path.size() - 1) {
          System.out.print(" ->(" + Util.getLinkWeight(v.getName(), path.get(i+1).getName(), this) + ") ");
        }
      }

      System.out.println();
    } else {
      System.out.println("[ERROR] The destination " + destinationIP + " is not part of the network.");
    }
  }

  /**
   * disconnect with the router identified by the given destination ip address
   * Notice: this command should trigger the synchronization of database
   *
   * @param portNumber the port number which the link attaches at
   */
  private void processDisconnect(short portNumber) {
    --portNumber;
    boolean isDisconnected = false;

    if (portNumber < 0) {
      System.out.println("[ERROR] port number cannot be smaller than 1");
      return;
    }

    Link link = _ports[portNumber];
    if (link == null) {
      System.out.println("[ERROR] No router is connected to this port.");
      return;
    }

    RouterDescription neighborDescription = link.router1.simulatedIPAddress.equals(_rd.simulatedIPAddress) ? link.router2 : link.router1;
    String neighborIp = neighborDescription.getSimulatedIPAddress();

    for (int i = 0; i < _clients.length; ++i) {
      if (_clients[i] != null) {
        _clients[i].propagateSynchronization(_rd.getSimulatedIPAddress(), SOSPFPacket.DISCONNECT, _rd.getSimulatedIPAddress(), neighborIp);
      }
    }

    ClientServiceThread[] clientServicers = _server.getClientServicers();
    for (int i = 0; i < clientServicers.length; ++i) {
      if (clientServicers[i] != null) {
        clientServicers[i].propagateSynchronization(_rd.getSimulatedIPAddress(), SOSPFPacket.DISCONNECT, _rd.getSimulatedIPAddress(), neighborIp);
      }
    }

    removeLink(neighborIp);
  }

  /**
   * attach the link to the remote router, which is identified by the given simulated ip;
   * to establish the connection via socket, you need to identify the process IP and process Port;
   * additionally, weight is the cost to transmitting data through the link
   * <p/>
   * NOTE: this command should not trigger link database synchronization
   */
  private void processAttach(String processIP, short processPort, String simulatedIP, short weight) {
    attach(processIP, processPort, simulatedIP, weight, -1);
  }

  /**
   * broadcast Hello to neighbors
   */
  private void processStart() {
    for (int i = 0; i < _ports.length; ++i) {
      boolean isAlreadyConnected = isConnected(i);

        if (!isAlreadyConnected) {
          initiateConnection(i);
        }
      }
    _isStarted = true;
  }

  /**
   * attach the link to the remote router, which is identified by the given simulated ip;
   * to establish the connection via socket, you need to identify the process IP and process Port;
   * additionally, weight is the cost to transmitting data through the link
   * <p/>
   * This command does trigger the link database synchronization
   */
  private void processConnect(String processIP, short processPort, String simulatedIP, short weight) {
    if (!_isStarted) {
      System.out.println("[ERROR] Please first start the router.");
      return;
    }

    for (int i = 0; i < _ports.length; ++i) {
      if (_ports[i] != null) {
        String ip = getRemoteRouterSimulatedIp(i);
        if (ip.equals(simulatedIP)) {
          if (isConnected(i)) {
            //possible problem: the router has only been attached to the ports but `start` was not run. This use case is not legit though.
            System.out.println("[ERROR] This router is already connected.");
            return;
          } else {
            initiateConnection(i);
            break;
          }
        }
      } else {
        if (attach(processIP, processPort, simulatedIP, weight, i)) {
          initiateConnection(i);
        } else {
          System.out.println("[ERROR] Could not attach router.");
        }
        break;
      }
    }
  }

  /**
   * output the neighbors of the routers
   */
  private void processNeighbors() {
    int i = 1;

    for (Link link : _ports) {
      if (link == null) {
        continue; //empty port
      }

      RouterDescription neighbourDescription = link.router1.simulatedIPAddress.equals(_rd.simulatedIPAddress) ? link.router2 : link.router1;

      String status = "ABOUT TO CONNECT";
      RouterStatus rStatus = neighbourDescription.getStatus();

      if (rStatus != null) {
        switch (rStatus) {
          case INIT:
            status = "INIT";
            break;
          case TWO_WAY:
            status = "TWO_WAY";
            break;
          case OVER_BURDENED:
            status = "OVER_BURDENED";
            break;
          default:
            break;
        }
      }

      System.out.println("neighbour " + i++ + ": " + neighbourDescription.getSimulatedIPAddress() + " [" + status + "]");
    }
  }

  /**
   * disconnect with all neighbors and quit the program
   */
  private void processQuit() {
    System.exit(1);
  }

  public void terminal() {
    _server = Server.runNonBlocking(this);

    try {
      InputStreamReader isReader = new InputStreamReader(System.in);
      BufferedReader br = new BufferedReader(isReader);
      System.out.print(">> ");
      String command = br.readLine();
      while (true) {
        if (command.startsWith("detect ")) {
          String[] cmdLine = command.split(" ");
          processDetect(cmdLine[1]);
        } else if (command.startsWith("disconnect ")) {
          String[] cmdLine = command.split(" ");
          processDisconnect(Short.parseShort(cmdLine[1]));
        } else if (command.startsWith("quit")) {
          processQuit();
          break;
        } else if (command.startsWith("attach ")) {
          String[] cmdLine = command.split(" ");
          processAttach(cmdLine[1], Short.parseShort(cmdLine[2]),
                  cmdLine[3], Short.parseShort(cmdLine[4]));
        } else if (command.equals("start")) {
          processStart();
        } else if (command.startsWith("connect ")) {
          String[] cmdLine = command.split(" ");
          processConnect(cmdLine[1], Short.parseShort(cmdLine[2]),
                  cmdLine[3], Short.parseShort(cmdLine[4]));
        } else if (command.equals("neighbors")) {
          //output neighbors
          processNeighbors();
        } else if (command.equals("lsd")) {
          //print LSD
          System.out.print(_lsd.toString());
        } else {
          //invalid command
          System.out.println("[ERROR] Invalid command.");
        }
        System.out.print(">> ");
        command = br.readLine();
      }
      isReader.close();
      br.close();
    } catch (Exception e) {
      //e.printStackTrace();
    }
  }

  private boolean isValidAttach(short processPort, String simulatedIP) {
    boolean isValid = false;

    if (simulatedIP.equals(_rd.simulatedIPAddress) && processPort == _rd.processPortNumber) {
      System.out.println("[ERROR] The two routers share the same IP address and port number. Please choose a different IP address or port number.");
    } else if (isLinkExisting(processPort, simulatedIP)) {
      System.out.println("[ERROR] The two routers are already linked.");
    } else if (!Configuration.getPortUser(processPort).equals(simulatedIP)) {
      System.out.println("[ERROR] Port " + processPort + " already in use by " + Configuration.getPortUser(processPort) + ".");
    } else {
      isValid = true;
    }

    return isValid;
  }

  public boolean addLink(Link link) {
    for (int i = 0; i < _ports.length; ++i) {
      if (addLink(link, i)) {
        return true;
      }
    }

    return false;
  }

  public boolean addLink(Link link, int i) {
    if (_ports[i] == null) {
      _ports[i] = link;
      return true;
    }

    if (i == 3) {
      System.out.println("[ERROR] Current router is already attached to 4 routers. Disconnect a router and retry.");
    }

    return false;
  }

  public void removeLink(String ip) {
    for (int i = 0; i < _ports.length; ++i) {
      if (_ports[i] == null) {
        continue;
      }

      boolean isLinkToDelete = _ports[i].router1.simulatedIPAddress.equals(ip) || _ports[i].router2.simulatedIPAddress.equals(ip);

      if (isLinkToDelete) {
        _ports[i] = null;
        _clients[i] = null;
        for (int j = 0; j < _clientServers.size(); ++j) {
          ClientServiceThread cst = _clientServers.get(j);
          if (cst.getRemoteRouterDescription().getSimulatedIPAddress().equals(ip)) {
            _clientServers.remove(j);
            break;
          }
        }

        _server.remove(ip);

        _lsd.remove(_rd.getSimulatedIPAddress(), ip);

        break;
      }
    }
  }

  public boolean isLinkExisting(short processPort, String simulatedIp) {
    for (Link link: _ports) {
      if (link != null &&
              (link.router1.processPortNumber == processPort && link.router1.simulatedIPAddress.equals(simulatedIp) ||
              link.router2.processPortNumber == processPort && link.router2.simulatedIPAddress.equals(simulatedIp))) {
        return true;
      }
    }

    return false;
  }

  public void addLinkDescriptionToDatabase(RouterDescription remoteRd, short weight) {
    LinkDescription linkDescription = new LinkDescription(remoteRd.getSimulatedIPAddress(), remoteRd.getProcessPortNumber(), weight);
    LSA lsa = _lsd._store.get(_rd.simulatedIPAddress);
    boolean isLinkFound = false;

    // check if link description already exists. If so, override.
    for (int i = 0; i < lsa.links.size(); ++i) {
      LinkDescription ld = lsa.links.get(i);

      if (ld.linkID.equals(remoteRd.getSimulatedIPAddress())) {
        ld = linkDescription;
        isLinkFound = true;
      }
    }

    if (!isLinkFound) {
      lsa.links.add(linkDescription);
      ++lsa.lsaSeqNumber;
    }
  }

  public void synchronize(Vector<LSA> lsaVector) {
    synchronized(_lsdLock) {
      Iterator<LSA> lsaIterator = lsaVector.iterator();

      while (lsaIterator.hasNext()) {
        LSA lsa = lsaIterator.next();
        if (isLsaOutdated(lsa)) {
          _lsd._store.put(lsa.linkStateID, lsa);
        }
      }
    }
  }

  private boolean isLsaOutdated(LSA newLsa) {
    boolean isOutdated = true;
    if (_lsd._store.containsKey(newLsa.linkStateID) && _lsd._store.get(newLsa.linkStateID).lsaSeqNumber >= newLsa.lsaSeqNumber) {
      isOutdated = false;
    }
    return isOutdated;
  }

  public void propagateSynchronization(String initiator, String ipToExclude, short sospfType) {
    for (int i = 0; i < _clients.length; ++i) {
      if (_clients[i] != null && !_clients[i].isFor(initiator) && !_clients[i].isFor(ipToExclude)) {
        _clients[i].propagateSynchronization(initiator, sospfType, null, null);
      }
    }

    ClientServiceThread[] clientServicers = _server.getClientServicers();
    for (int i = 0; i < clientServicers.length; ++i) {
      if (clientServicers[i] != null && !clientServicers[i].isFor(initiator) && !clientServicers[i].isFor(ipToExclude)) {
        clientServicers[i].propagateSynchronization(initiator, sospfType, null, null);
      }
    }
  }

  private boolean initiateConnection(int i) {
    if (_clients[i] != null || _ports[i] == null) {
      return false; //empty port
    }

    Link link = _ports[i];
    RouterDescription neighbourDescription = link.router1.simulatedIPAddress.equals(_rd.simulatedIPAddress) ? link.router2 : link.router1;
    Client client = Client.runNonBlocking(neighbourDescription, this, link);
    _clients[i] = client;

    return true;
  }

  public int getInitiatorLatestVersion(String initiator) {
    return _initiators.containsKey(initiator) ? _initiators.get(initiator) : -1;
  }

  public void setInitiatorLatestVersion(String initiator, int version) {
    _initiators.put(initiator, version);
  }

  private boolean isConnected(int i) {
    boolean isConnected = false;

    if (_ports[i] != null) {
      String ip = getRemoteRouterSimulatedIp(i);

      for (int j = 0; j < _clientServers.size(); ++j) {
        ClientServiceThread cst = _clientServers.get(j);

        if (cst.getRemoteRouterDescription().getSimulatedIPAddress().equals(ip)) {
          isConnected = true;
          break;
        }
      }
    }

    return isConnected;
  }

  private String getRemoteRouterSimulatedIp(int i) {
    String ip = _ports[i].getRouter1().getSimulatedIPAddress();

    if (ip.equals(_rd.getSimulatedIPAddress())) {
      ip = _ports[i].getRouter2().getSimulatedIPAddress();
    }

    return ip;
  }

  private boolean attach(String processIP, short processPort, String simulatedIP, short weight, int portNumber) {
    if (!isValidAttach(processPort, simulatedIP)) {
      return false;
    }

    RouterDescription routerAttachedDescription = new RouterDescription(processIP, processPort, simulatedIP);
    Link link = new Link(_rd, routerAttachedDescription, weight);
    return portNumber == -1 ? addLink(link) : addLink(link, portNumber);
  }
}

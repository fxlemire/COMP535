package socs.network.node;

import socs.network.message.LSA;
import socs.network.message.LinkDescription;
import socs.network.net.Client;
import socs.network.net.Server;
import socs.network.util.Configuration;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.Vector;


public class Router {

  protected LinkStateDatabase _lsd;

  RouterDescription _rd;

  //assuming that all routers are with 4 _ports
  Link[] _ports = new Link[4];
  Client[] _clients = new Client[4];

  public Router(Configuration config) {
    _rd = new RouterDescription(Configuration.PROCESS_IP, config.getShort("socs.network.router.port"), config.getString("socs.network.router.ip"));
    _lsd = new LinkStateDatabase(_rd);
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

  }

  /**
   * disconnect with the router identified by the given destination ip address
   * Notice: this command should trigger the synchronization of database
   *
   * @param portNumber the port number which the link attaches at
   */
  private void processDisconnect(short portNumber) {

  }

  /**
   * attach the link to the remote router, which is identified by the given simulated ip;
   * to establish the connection via socket, you need to identify the process IP and process Port;
   * additionally, weight is the cost to transmitting data through the link
   * <p/>
   * NOTE: this command should not trigger link database synchronization
   */
  private void processAttach(String processIP, short processPort, String simulatedIP, short weight) {
    if (!isValidAttach(processPort, simulatedIP)) {
      return;
    }

    RouterDescription routerAttachedDescription = new RouterDescription(processIP, processPort, simulatedIP);
    Link link = new Link(_rd, routerAttachedDescription, weight);
    addLink(link);
  }

  /**
   * broadcast Hello to neighbors
   */
  private void processStart() {
    for (int i = 0; i < _ports.length; ++i) {
      initiateConnection(i);
    }
  }

  /**
   * attach the link to the remote router, which is identified by the given simulated ip;
   * to establish the connection via socket, you need to identify the process IP and process Port;
   * additionally, weight is the cost to transmitting data through the link
   * <p/>
   * This command does trigger the link database synchronization
   */
  private void processConnect(String processIP, short processPort,
                              String simulatedIP, short weight) {

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

  }

  public void terminal() {
    Server.runNonBlocking(this);

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
        } else if (command.startsWith("attach ")) {
          String[] cmdLine = command.split(" ");
          processAttach(cmdLine[1], Short.parseShort(cmdLine[2]),
                  cmdLine[3], Short.parseShort(cmdLine[4]));
        } else if (command.equals("start")) {
          processStart();
        } else if (command.equals("connect ")) {
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
          break;
        }
        System.out.print(">> ");
        command = br.readLine();
      }
      isReader.close();
      br.close();
    } catch (Exception e) {
      e.printStackTrace();
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
      if (_ports[i] == null) {
        _ports[i] = link;
        break;
      }

      if (i == 3) {
        System.out.println("[ERROR] Current router is already attached to 4 routers. Disconnect a router and retry.");
        return false;
      }
    }

    return true;
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
    Iterator<LSA> lsaIterator = lsaVector.iterator();

    while (lsaIterator.hasNext()) {
      LSA lsa = lsaIterator.next();
      if (isLsaOutdated(lsa)) {
        _lsd._store.put(lsa.linkStateID, lsa);
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

  public void propagateSynchronization(String initiator, String ipToExclude) {
    for (int i = 0; i < _ports.length; ++i) {
      if ((_clients[i] != null || initiateConnection(i)) && !_clients[i].getRemoteRouterDescription().getSimulatedIPAddress().equals(ipToExclude)) {
        _clients[i].propagateSynchronization(initiator);
      }
    }
  }

  private boolean initiateConnection(int i) {
    if (_ports[i] == null) {
      return false; //empty port
    }

    Link link = _ports[i];
    RouterDescription neighbourDescription = link.router1.simulatedIPAddress.equals(_rd.simulatedIPAddress) ? link.router2 : link.router1;
    Client client = Client.runNonBlocking(neighbourDescription, this, link);
    _clients[i] = client;

    return true;
  }

}

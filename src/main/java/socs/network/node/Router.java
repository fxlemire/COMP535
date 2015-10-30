package socs.network.node;

import socs.network.message.LSA;
import socs.network.message.LinkDescription;
import socs.network.net.Client;
import socs.network.net.Server;
import socs.network.util.Configuration;

import java.io.BufferedReader;
import java.io.InputStreamReader;


public class Router {

  protected LinkStateDatabase lsd;

  RouterDescription rd;

  //assuming that all routers are with 4 ports
  Link[] ports = new Link[4];

  public Router(Configuration config) {
    rd = new RouterDescription(Configuration.PROCESS_IP, config.getShort("socs.network.router.port"), config.getString("socs.network.router.ip"));
    lsd = new LinkStateDatabase(rd);
  }

  public RouterDescription getRd() {
    return rd;
  }

  public Link[] getPorts() {
    return ports;
  }

  /**
   * output the shortest path to the given destination ip
   * <p/>
   * format: source ip address  -> ip address -> ... -> destination ip
   *
   * @param destinationIP the ip adderss of the destination simulated router
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
   * to establish the connection via socket, you need to indentify the process IP and process Port;
   * additionally, weight is the cost to transmitting data through the link
   * <p/>
   * NOTE: this command should not trigger link database synchronization
   */
  private void processAttach(String processIP, short processPort, String simulatedIP, short weight) {
    if (!isValidAttach(processPort, simulatedIP)) {
      return;
    }

    RouterDescription routerAttachedDescription = new RouterDescription(processIP, processPort, simulatedIP);
    Link link = new Link(rd, routerAttachedDescription);
    boolean isLinkAdded = addLink(link);
    if (!isLinkAdded) {
      return;
    }

    addLinkDescriptionToDatabase(processPort, simulatedIP, weight);
  }

  /**
   * broadcast Hello to neighbors
   */
  private void processStart() {
    for (Link link : ports) {
      if (link == null) {
        continue; //empty port
      }

      RouterDescription neighbourDescription = link.router1.simulatedIPAddress.equals(rd.simulatedIPAddress) ? link.router2 : link.router1;

      RouterStatus neighbourStatus = neighbourDescription.getStatus();

      if (neighbourStatus != RouterStatus.TWO_WAY) {
        Client.runNonBlocking(neighbourDescription, this);
      }
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

    for (Link link : ports) {
      if (link == null) {
        continue; //empty port
      }

      RouterDescription neighbourDescription = link.router1.simulatedIPAddress.equals(rd.simulatedIPAddress) ? link.router2 : link.router1;

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
    if (simulatedIP.equals(rd.simulatedIPAddress) && processPort == rd.processPortNumber) {
      System.out.println("[ERROR] The two routers share the same IP address and port number. Please choose a different IP address or port number.");
      return false;
    } else if (isLinkExisting(processPort, simulatedIP)) {
      System.out.println("[ERROR] The two routers are already linked.");
      return false;
    }

    return true;
  }

  public boolean addLink(Link link) {
    for (int i = 0; i < ports.length; ++i) {
      if (ports[i] == null) {
        ports[i] = link;
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
    for (int i = 0; i < ports.length; ++i) {
      if (ports[i] == null) {
        continue;
      }

      boolean isLinkToDelete = ports[i].router1.simulatedIPAddress.equals(ip) || ports[i].router2.simulatedIPAddress.equals(ip);

      if (isLinkToDelete) {
        ports[i] = null;
      }
    }
  }

  public boolean isLinkExisting(short processPort, String simulatedIp) {
    for (Link link: ports) {
      if (link != null &&
              (link.router1.processPortNumber == processPort && link.router1.simulatedIPAddress.equals(simulatedIp) ||
              link.router2.processPortNumber == processPort && link.router2.simulatedIPAddress.equals(simulatedIp))) {
        return true;
      }
    }

    return false;
  }

  private void addLinkDescriptionToDatabase(short processPort, String simulatedIP, short weight) {
    LinkDescription linkDescription = new LinkDescription(simulatedIP, processPort, weight);
    LSA lsa = lsd._store.get(rd.simulatedIPAddress);
    boolean isLinkFound = false;

    // check if link description already exists. If so, override.
    for (int i = 0; i < lsa.links.size(); ++i) {
      LinkDescription ld = lsa.links.get(i);

      if (ld.linkID.equals(simulatedIP)) {
        ld = linkDescription;
        isLinkFound = true;
      }
    }

    if (!isLinkFound) {
      lsa.links.add(linkDescription);
    }
  }

  private void debugPrintLSD() {
    System.out.println(lsd.toString());
  }

}
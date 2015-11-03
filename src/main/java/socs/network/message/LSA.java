package socs.network.message;

import java.io.Serializable;
import java.util.LinkedList;

public class LSA implements Serializable {
  public static int sequenceNumber = 0;

  //IP address of the router originate this LSA
  public String linkStateID;
  public int lsaSeqNumber = sequenceNumber++;

  public LinkedList<LinkDescription> links = new LinkedList<LinkDescription>();

  public LSA() {}

  public LSA(LSA lsa) {
    linkStateID = lsa.linkStateID;
    lsaSeqNumber = lsa.lsaSeqNumber;
    links = new LinkedList<>();
    for (LinkDescription ld : lsa.links) {
      LinkDescription linkD = new LinkDescription();
      linkD.linkID = ld.linkID;
      linkD.portNum = ld.portNum;
      linkD.tosMetrics = ld.tosMetrics;
      links.add(linkD);
    }
  }

  @Override
  public String toString() {
    StringBuffer sb = new StringBuffer();
    sb.append(linkStateID + ":").append(lsaSeqNumber + "\n");
    for (LinkDescription ld : links) {
      sb.append(ld);
    }
    sb.append("\n");
    return sb.toString();
  }
}

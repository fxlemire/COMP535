package socs.network.util;

import socs.network.message.LSA;
import socs.network.message.LinkDescription;
import socs.network.node.Router;
import socs.network.util.path.Vertex;

import java.util.ArrayList;
import java.util.Optional;

public class Util {
    public static Optional<Vertex> findVertex(ArrayList<Vertex> list, String id) {
        return list.stream().filter(v -> v.getId().equals(id)).findFirst();
    }

    public static short getLinkWeight(String sourceIp, String destIp, Router rd) {
        short weight = -1;

        Optional<LinkDescription> linkOption = rd.getLsd().getStore().get(sourceIp).links.stream().filter(ld -> ld.linkID.equals(destIp)).findFirst();

        if (linkOption.isPresent()) {
            weight = (short) linkOption.get().tosMetrics;
        }

        return weight;
    }
}

package socs.network.net;

import socs.network.message.SOSPFPacket;
import socs.network.node.RouterDescription;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.Socket;

public class Util {
    public static SOSPFPacket makeMessage(RouterDescription local, RouterDescription external, short messageType) {
        SOSPFPacket message = new SOSPFPacket();
        message.srcProcessIP = local.getProcessIPAddress();
        message.srcProcessPort = local.getProcessPortNumber();
        message.srcIP = local.getSimulatedIPAddress();
        message.dstIP = external.getSimulatedIPAddress();
        message.sospfType = messageType; //HELLO
        message.routerID = local.getSimulatedIPAddress();
        message.neighborID = external.getSimulatedIPAddress();

        return message;
    }

    public static SOSPFPacket receiveMessage(Socket server) {
        SOSPFPacket receivedMessage = null;

        try {
            ObjectInputStream in = new ObjectInputStream(server.getInputStream());
            receivedMessage = (SOSPFPacket) in.readObject();
            String messageType = receivedMessage.sospfType == 0 ? "HELLO" : "LINKSTASTEUPDATE";
            System.out.println("Received " + messageType + " from " + receivedMessage.srcIP);
        } catch (ClassNotFoundException e) {
            System.out.println("No message received.");
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return receivedMessage;
    }
}

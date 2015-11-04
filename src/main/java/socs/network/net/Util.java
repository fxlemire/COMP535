package socs.network.net;

import socs.network.message.LSA;
import socs.network.message.SOSPFPacket;
import socs.network.node.Router;
import socs.network.node.RouterDescription;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class Util {
    public static SOSPFPacket makeMessage(RouterDescription local, RouterDescription external, short messageType, Router rd) {
        SOSPFPacket message = new SOSPFPacket();
        message.srcProcessIP = local.getProcessIPAddress();
        message.srcProcessPort = local.getProcessPortNumber();
        message.srcIP = local.getSimulatedIPAddress();
        message.dstIP = external.getSimulatedIPAddress();
        message.sospfType = messageType;
        message.routerID = local.getSimulatedIPAddress();
        message.neighborID = external.getSimulatedIPAddress();
        rd.getLsd().getStore().forEach((k, v) -> message.lsaArray.add(v));
        message.lsaInitiator = messageType == SOSPFPacket.LSU ? message.srcIP : null;

        return message;
    }

    public static SOSPFPacket receiveMessage(ObjectInputStream inputStream) {
        SOSPFPacket receivedMessage = null;

        try {
            receivedMessage = (SOSPFPacket) inputStream.readObject();

            String messageType;

            switch (receivedMessage.sospfType) {
                case SOSPFPacket.HELLO:
                    messageType = "HELLO";
                    break;
                case SOSPFPacket.LSU:
                    messageType = "LINKSTATEUPDATE";
                    break;
                case SOSPFPacket.OVER_BURDENED:
                    messageType = "OVER_BURDENED";
                    break;
                default:
                    messageType = "UNKNOWN_STATE";
                    break;
            }

            System.out.println("received " + messageType + " from " + receivedMessage.srcIP + ";");
        } catch (ClassNotFoundException e) {
            System.out.println("No message received.");
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return receivedMessage;
    }

    public static void sendMessage(SOSPFPacket message, ObjectOutputStream outputStream) {
        try {
            outputStream.writeObject(message);
            outputStream.flush();
            outputStream.reset();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

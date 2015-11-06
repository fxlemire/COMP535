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
        } catch (Exception e) {
            //System.out.println("No message received.");
            //e.printStackTrace();
        }

        return receivedMessage;
    }

    public static void sendMessage(SOSPFPacket message, ObjectOutputStream outputStream) {
        try {
            outputStream.writeObject(message);
            outputStream.reset();
            outputStream.flush();
        } catch (Exception e) {
            //e.printStackTrace();
        }
    }

    public static boolean synchronizeAndPropagate(SOSPFPacket message, Router router) {
        String initiator = message.lsaInitiator;
        int version = message.messageId;

        if (router.getInitiatorLatestVersion(initiator) < version) {
            router.setInitiatorLatestVersion(initiator, version);
            router.synchronize(message.lsaArray);
            router.propagateSynchronization(initiator, message.srcIP);
            return true;
        }

        return false;
    }
}

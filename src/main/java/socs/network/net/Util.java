package socs.network.net;

import socs.network.message.LSA;
import socs.network.message.SOSPFPacket;
import socs.network.node.Router;
import socs.network.node.RouterDescription;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class Util {
    private static int SAFETYLOOPCHECKER_COUNT = 0;
    private static String SAFETYLOOPCHECKER_INITIATOR = "";

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

    public static SOSPFPacket makeMessage(RouterDescription local, RouterDescription external, short messageType, Router rd, String ip1, String ip2) {
        SOSPFPacket message = makeMessage(local, external, messageType, rd);
        message.lsaInitiator = ip1;
        message.disconnectInitiator = ip1;
        message.disconnectVictim = ip2;
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
                case SOSPFPacket.DISCONNECT:
                    messageType = "DISCONNECT";
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
        boolean canProceed = true;

        if (SAFETYLOOPCHECKER_INITIATOR.equals(initiator)) {
            if (SAFETYLOOPCHECKER_COUNT == 5) {
                for (LSA lsa : message.lsaArray) {
                    String id = lsa.linkStateID;
                    LSA localLsa = router.getLsd().getStore().get(id);
                    if (localLsa.lsaSeqNumber == lsa.lsaSeqNumber) {
                        canProceed = false;
                    } else {
                        SAFETYLOOPCHECKER_COUNT = 0;
                    }
                }
            }
        } else {
            SAFETYLOOPCHECKER_INITIATOR = initiator;
            SAFETYLOOPCHECKER_COUNT = 0;
        }

        if (canProceed && router.getInitiatorLatestVersion(initiator) < version) {
            router.setInitiatorLatestVersion(initiator, version);
            if (message.sospfType == SOSPFPacket.LSU) {
                router.synchronize(message.lsaArray);
            }
            router.propagateSynchronization(initiator, message.srcIP, message.sospfType);
            ++SAFETYLOOPCHECKER_COUNT;
            return true;
        }

        return false;
    }
}

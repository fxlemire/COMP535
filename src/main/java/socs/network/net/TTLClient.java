package socs.network.net;

import socs.network.message.SOSPFPacket;

import java.util.Timer;
import java.util.TimerTask;

public class TTLClient extends TTL {
    private Client _client;

    public TTLClient(Client client) {
        _client = client;
        _timer = new Timer();
    }

    public void start() {
        _timer.schedule(new RemindTask(), _delay * 1000);
    }

    public void restart() {
        _timer.cancel();
        _timer = new Timer();
        _timer.schedule(new RemindTask(), _delay * 1000);
    }

    class RemindTask extends TimerTask {
        @Override
        public void run() {
            _timer.cancel();
            String remoteIp = _client.getRemoteRouterDescription().getSimulatedIPAddress();
            System.out.println("No heartbeat from " + remoteIp + "... Disconnecting...");
            _client.getRouter().disconnect(remoteIp, SOSPFPacket.ANNIHILATE);
            this.cancel();
        }
    }
}

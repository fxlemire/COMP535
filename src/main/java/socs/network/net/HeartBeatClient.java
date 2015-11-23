package socs.network.net;

import socs.network.message.SOSPFPacket;

import java.util.Timer;
import java.util.TimerTask;

public class HeartBeatClient extends HeartBeat {
    private Client _client;

    public HeartBeatClient(Client client) {
        _client = client;
        _timer = new Timer();
    }

    public void start() {
        _timer.scheduleAtFixedRate(new HeartBeat(), _heartbeatDelay * 1000, _heartbeatDelay * 1000);
    }

    class HeartBeat extends TimerTask {
        public void run() {
            _client.sendMessage(SOSPFPacket.HELLO);
        }
    }
}

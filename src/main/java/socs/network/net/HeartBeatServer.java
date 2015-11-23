package socs.network.net;

import socs.network.message.SOSPFPacket;

import java.util.Timer;
import java.util.TimerTask;

public class HeartBeatServer extends HeartBeat {
    private ClientServiceThread _cst;

    public HeartBeatServer(ClientServiceThread cst) {
        _cst = cst;
        _timer = new Timer();
        _timer.scheduleAtFixedRate(new HeartBeat(), _heartbeatDelay * 1000, _heartbeatDelay * 1000);
    }

    public void start() {
        _timer.scheduleAtFixedRate(new HeartBeat(), _heartbeatDelay * 1000, _heartbeatDelay * 1000);
    }

    class HeartBeat extends TimerTask {
        public void run() {
            _cst.sendMessage(SOSPFPacket.HELLO);
        }
    }
}

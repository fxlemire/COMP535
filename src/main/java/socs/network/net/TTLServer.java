package socs.network.net;

import socs.network.message.SOSPFPacket;

import java.util.Timer;
import java.util.TimerTask;

public class TTLServer extends TTL {
    private ClientServiceThread _cst;

    public TTLServer(ClientServiceThread cst) {
        _cst = cst;
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
            _cst.getRouter().disconnect(_cst.getRemoteRouterDescription().getSimulatedIPAddress(), SOSPFPacket.ANNIHILATE);
//            String myIp = _cst.getRouterDescription().getSimulatedIPAddress();
//            _cst.propagateSynchronization(myIp, SOSPFPacket.ANNIHILATE, myIp, _cst.getRemoteRouterDescription().getSimulatedIPAddress());
            this.cancel();
        }
    }
}

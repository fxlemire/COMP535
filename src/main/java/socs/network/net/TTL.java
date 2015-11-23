package socs.network.net;

import java.util.Timer;

public abstract class TTL {
    protected Timer _timer;
    protected int _delay = 10;

    public void kill() {
        _timer.cancel();
    }
}
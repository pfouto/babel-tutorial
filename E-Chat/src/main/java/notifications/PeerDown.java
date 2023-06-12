package notifications;

import pt.unl.fct.di.novasys.babel.generic.ProtoNotification;
import pt.unl.fct.di.novasys.network.data.Host;

public class PeerDown extends ProtoNotification {

    public static final short NOTIFICATION_ID = 101;

    private final Host peer;

    public PeerDown(Host peer) {
        super(NOTIFICATION_ID);
        this.peer = peer;
    }


    public Host getPeer() {
        return peer;
    }
}

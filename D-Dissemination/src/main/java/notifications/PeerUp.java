package notifications;

import pt.unl.fct.di.novasys.babel.generic.ProtoNotification;
import pt.unl.fct.di.novasys.network.data.Host;

public class PeerUp extends ProtoNotification {

    public static final short NOTIFICATION_ID = 102;

    private final Host peer;

    public PeerUp(Host peer) {
        super(NOTIFICATION_ID);
        this.peer = peer;
    }


    public Host getPeer() {
        return peer;
    }
}

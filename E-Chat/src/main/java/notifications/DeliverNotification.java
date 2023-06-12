package notifications;

import pt.unl.fct.di.novasys.babel.generic.ProtoNotification;
import pt.unl.fct.di.novasys.network.data.Host;

public class DeliverNotification extends ProtoNotification {

    public static final short NOTIFICATION_ID = 201;

    private final String msg;
    private final Host via;
    private final int nHops;

    public DeliverNotification(String msg, Host via, int nHops) {
        super(NOTIFICATION_ID);
        this.msg = msg;
        this.nHops = nHops;
        this.via = via;
    }

    public String getMsg() {
        return msg;
    }

    public Host getVia() {
        return via;
    }

    public int getnHops() {
        return nHops;
    }
}

package pingpong.requests;

import pt.unl.fct.di.novasys.babel.generic.ProtoReply;
import pt.unl.fct.di.novasys.network.data.Host;

public class PongReply extends ProtoReply {

    public static final short REPLY_ID = 2;
    private final String message;
    private final Host destination;
    private final long rtt;

    public PongReply(String message, Host destination, long rtt) {
        super(REPLY_ID);
        this.message = message;
        this.destination = destination;
        this.rtt = rtt;
    }
    public String getMessage() {
        return message;
    }

    public Host getDestination() {
        return destination;
    }

    public long getRTT() {
        return rtt;
    }

}

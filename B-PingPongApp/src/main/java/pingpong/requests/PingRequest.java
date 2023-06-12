package pingpong.requests;

import pt.unl.fct.di.novasys.babel.generic.ProtoRequest;
import pt.unl.fct.di.novasys.network.data.Host;

public class PingRequest extends ProtoRequest {

    public static final short REQUEST_ID = 1;
    private final String message;
    private final Host destination;
    private final int nPings;

    public PingRequest(String message, Host destination, int nPings) {
        super(REQUEST_ID);
        this.message = message;
        this.destination = destination;
        this.nPings = nPings;

    }

    public String getMessage() {
        return message;
    }

    public Host getDestination() {
        return destination;
    }

    public PongReply produceReply(long rtt) {
        return new PongReply(message, destination, rtt);
    }

    public int getNPings() {
        return nPings;
    }
}

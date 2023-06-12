package pingpong;

import pingpong.requests.PingRequest;

public class PingState {

    private final PingRequest request;
    private final short requestSource;
    private int receivedPongs;
    public PingState(PingRequest request, short requestSource) {
        this.request = request;
        this.requestSource = requestSource;
        this.receivedPongs = 0;
    }

    public PingRequest getRequest() {
        return request;
    }

    public int getReceivedPongs() {
        return receivedPongs;
    }

    public void incrementReceivedPongs() {
        receivedPongs++;
    }

    public boolean isDone() {
        return receivedPongs == request.getNPings();
    }

    public short getRequestSource() {
        return requestSource;
    }
}

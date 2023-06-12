package pingpong.timers;

import pt.unl.fct.di.novasys.babel.generic.ProtoTimer;

public class NextPingTimer extends ProtoTimer {

    public static final short TIMER_ID = 101;

    private int sentPings;
    public NextPingTimer() {
        super(TIMER_ID);
        sentPings = 0;
    }

    public int getSentPings() {
        return sentPings;
    }

    public void incrementSentPings() {
        sentPings++;
    }

    @Override
    public ProtoTimer clone() {
        return this;
    }

}

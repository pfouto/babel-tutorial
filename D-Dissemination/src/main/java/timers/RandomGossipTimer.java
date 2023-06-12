package timers;

import pt.unl.fct.di.novasys.babel.generic.ProtoTimer;

public class RandomGossipTimer extends ProtoTimer {

    public static final short TIMER_ID = 102;

    public RandomGossipTimer() {
        super(TIMER_ID);
    }

    @Override
    public ProtoTimer clone() {
        return this;
    }
}

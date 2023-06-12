package notifications;

import pt.unl.fct.di.novasys.babel.generic.ProtoNotification;
import pt.unl.fct.di.novasys.network.data.Host;

public class ChannelNotification extends ProtoNotification {
    public static final short NOTIFICATION_ID = 103;

    private final Host myself;
    private final int channelId;
    public ChannelNotification(Host myself, int channelId) {
        super(NOTIFICATION_ID);
        this.channelId = channelId;
        this.myself = myself;
    }

    public int getChannelId() {
        return channelId;
    }

    public Host getMyself() {
        return myself;
    }
}

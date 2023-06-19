import messages.GossipMessage;
import notifications.ChannelNotification;
import notifications.DeliverNotification;
import notifications.PeerDown;
import notifications.PeerUp;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pt.unl.fct.di.novasys.babel.core.GenericProtocol;
import pt.unl.fct.di.novasys.babel.exceptions.HandlerRegistrationException;
import pt.unl.fct.di.novasys.network.data.Host;
import requests.BroadcastRequest;

import java.util.*;

public class FloodGossip extends GenericProtocol {

    private static final Logger logger = LogManager.getLogger(FloodGossip.class);

    public static final String PROTO_NAME = "FloodGossip";
    public static final short PROTO_ID = 201;

    private Host myself;
    private final Set<Host> peers = new HashSet<>();
    private final Set<UUID> received;

    private int gossipSize;

    public FloodGossip() {
        super(PROTO_NAME, PROTO_ID);

        this.received = new HashSet<>();
    }

    @Override
    public void init(Properties props) throws HandlerRegistrationException {
        this.gossipSize = Integer.parseInt(props.getProperty("gossip_size", "2"));

        /*--------------------- Register Notification Handlers ------------------------ */
        subscribeNotification(PeerUp.NOTIFICATION_ID, this::uponPeerUp);
        subscribeNotification(PeerDown.NOTIFICATION_ID, this::uponPeerDown);
        subscribeNotification(ChannelNotification.NOTIFICATION_ID, this::onChannelNotification);
        /*--------------------- Register Request Handlers ----------------------------- */
        registerRequestHandler(BroadcastRequest.REQUEST_ID, this::uponBroadcast);

    }

    // We wait until we receive a ChannelNotification from the membership protocol to know which networkChannel to use
    private void onChannelNotification(ChannelNotification not, short sourceProto) {
        logger.debug("I am {}", not.getMyself());
        logger.debug("Using channel {} for communication", not.getChannelId());
        myself = not.getMyself();
        try {
            registerSharedChannel(not.getChannelId());
            /*---------------------- Register Message Serializers ---------------------- */
            registerMessageSerializer(not.getChannelId(), GossipMessage.MSG_ID, GossipMessage.serializer);
            /*---------------------- Register Message Handlers -------------------------- */
            registerMessageHandler(not.getChannelId(), GossipMessage.MSG_ID, this::uponReceiveGossip);
        } catch (HandlerRegistrationException e) {
            throw new RuntimeException(e);
        }
    }

    /*--------------------------------- Requests ---------------------------------------- */

    //When the gossip timer triggers, we disseminate a random message (using the same logic as when we receive a message)
    private void uponBroadcast(BroadcastRequest request, short sourceProto) {
        UUID mid = UUID.randomUUID();
        GossipMessage msg = new GossipMessage(mid, 0, request.getMsg());
        uponReceiveGossip(msg, myself, PROTO_ID, -1);
    }

    /*--------------------------------- Messages ---------------------------------------- */
    //Upon receiving a gossip message, we check if we have already received it, and if not, we deliver it and send it to
    //3 random peers (excluding the sender)
    private void uponReceiveGossip(GossipMessage msg, Host from, short sourceProto, int channelId) {
        logger.trace("Received {} from {}", msg, from);
        if (received.add(msg.getMid())) {
            triggerNotification(new DeliverNotification(msg.getContent(), from, msg.getRound()));
            msg.setRound(msg.getRound() + 1);
            List<Host> randomPeers = new LinkedList<>(peers);
            randomPeers.remove(from);
            Collections.shuffle(randomPeers);
            randomPeers.subList(0, Math.min(gossipSize, randomPeers.size())).forEach(host -> {
                if (!host.equals(from)) {
                    sendMessage(channelId, msg, host);
                    logger.trace("Sent {} to {}", msg, host);
                }
            });
        }
    }

    /*--------------------------------- Notifications ---------------------------------------- */
    //When receiving a PeerUp notification, we add the peer to our list of peers
    private void uponPeerUp(PeerUp not, short sourceProto) {
        peers.add(not.getPeer());
        logger.info("New peer {}, curr view({}): {}", not.getPeer(), peers.size(), peers);
    }

    //When receiving a PeerDown notification, we remove the peer from our list of peers
    private void uponPeerDown(PeerDown not, short sourceProto) {
        peers.remove(not.getPeer());
        logger.info("Bad peer {}, curr view({}): {}", not.getPeer(), peers.size(), peers);
    }

}

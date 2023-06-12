import messages.ShuffleMessage;
import messages.ShuffleReplyMessage;
import notifications.ChannelNotification;
import notifications.PeerDown;
import notifications.PeerUp;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pt.unl.fct.di.novasys.babel.core.GenericProtocol;
import pt.unl.fct.di.novasys.babel.exceptions.HandlerRegistrationException;
import pt.unl.fct.di.novasys.babel.generic.ProtoMessage;
import pt.unl.fct.di.novasys.channel.tcp.TCPChannel;
import pt.unl.fct.di.novasys.channel.tcp.events.*;
import pt.unl.fct.di.novasys.network.data.Host;
import timers.ShuffleTimer;

import java.io.IOException;
import java.net.InetAddress;
import java.util.*;

public class FullMembership extends GenericProtocol {

    private static final Logger logger = LogManager.getLogger(FullMembership.class);

    public static final int DEFAULT_PORT = 8000; // default port to listen on

    //Protocol information, to register in babel
    public final static short PROTOCOL_ID = 102;
    public final static String PROTOCOL_NAME = "FullMembership";

    private Host self;     //My own address/port
    private final Set<Host> membership; //Peers I am connected to
    private final Set<Host> pending; //Peers I am trying to connect to

    private int subsetSize; //param: maximum size of sample;

    private final Random rnd;
    protected int channelId;

    public FullMembership() {
        super(PROTOCOL_NAME, PROTOCOL_ID);

        this.membership = new HashSet<>();
        this.pending = new HashSet<>();

        this.rnd = new Random();
    }

    @Override
    public void init(Properties properties) throws IOException, HandlerRegistrationException {
        this.subsetSize = Integer.parseInt(properties.getProperty("sample_size"));
        int shuffleTimer = Integer.parseInt(properties.getProperty("shuffle_time"));

        Properties channelProps = new Properties();


        if (properties.containsKey("interface"))
            //if defined interface, get interface address
            channelProps.setProperty(TCPChannel.ADDRESS_KEY, Main.getAddress(properties.getProperty("interface")));
        else if (properties.containsKey("address"))
            // else use defined interface
            channelProps.setProperty(TCPChannel.ADDRESS_KEY, properties.getProperty("address"));
        else {
            // channel will throw exception upon creation
        }

        // set network port to listen on
        if (properties.containsKey("port")) {
            // if port is defined, used defined port
            channelProps.setProperty(TCPChannel.PORT_KEY, properties.getProperty("port"));
        } else
            // else use default value
            channelProps.setProperty(TCPChannel.PORT_KEY, DEFAULT_PORT + "");

        self = new Host(InetAddress.getByName(channelProps.getProperty(TCPChannel.ADDRESS_KEY)),
                Short.parseShort(channelProps.getProperty(TCPChannel.PORT_KEY)));
        logger.info("I am {}", self);
        this.channelId = createChannel(TCPChannel.NAME, channelProps);

        /*---------------------- Register Message Serializers ---------------------- */
        registerMessageSerializer(channelId, ShuffleMessage.MSG_ID, ShuffleMessage.serializer);
        registerMessageSerializer(channelId, ShuffleReplyMessage.MSG_ID, ShuffleReplyMessage.serializer);

        /*---------------------- Register Message Handlers -------------------------- */
        registerMessageHandler(channelId, ShuffleMessage.MSG_ID, this::uponShuffle, this::uponMsgFail);
        registerMessageHandler(channelId, ShuffleReplyMessage.MSG_ID, this::uponShuffleReply, this::uponMsgFail);

        /*--------------------- Register Timer Handlers ----------------------------- */
        registerTimerHandler(ShuffleTimer.TIMER_ID, this::uponShuffleTimer);

        /*-------------------- Register Channel Events ------------------------------- */
        registerChannelEventHandler(channelId, OutConnectionDown.EVENT_ID, this::uponOutConnectionDown);
        registerChannelEventHandler(channelId, OutConnectionFailed.EVENT_ID, this::uponOutConnectionFailed);
        registerChannelEventHandler(channelId, OutConnectionUp.EVENT_ID, this::uponOutConnectionUp);
        registerChannelEventHandler(channelId, InConnectionUp.EVENT_ID, this::uponInConnectionUp);
        registerChannelEventHandler(channelId, InConnectionDown.EVENT_ID, this::uponInConnectionDown);

        // If we define a contact node, we start by connecting to it
        // (and eventually exchanging membership through shuffle messages)
        if (properties.containsKey("contact")) {
            try {
                String contact = properties.getProperty("contact");
                String[] hostElems = contact.split(":");
                Host contactHost = new Host(InetAddress.getByName(hostElems[0]), Short.parseShort(hostElems[1]));
                pending.add(contactHost);
                openConnection(contactHost);
            } catch (Exception e) {
                logger.error("Invalid contact on configuration: '" + properties.getProperty("contact"));
                e.printStackTrace();
                System.exit(-1);
            }
        }

        // Set up a periodic timer to send shuffle messages
        setupPeriodicTimer(new ShuffleTimer(), shuffleTimer, shuffleTimer);

        triggerNotification(new ChannelNotification(self, channelId));
    }

    /*--------------------------------- Timers ---------------------------------------- */
    // When the shuffle timer is triggered, we grab a subset of the membership (including ourselves) and send it to a
    // random connected peer
    private void uponShuffleTimer(ShuffleTimer timer, long timerId) {

        StringBuilder sb = new StringBuilder();
        sb.append("[").append(membership.size()).append("]").append(" { ");
        membership.forEach(h -> sb.append(h.getAddress().getHostAddress()).append(":").append(h.getPort()).append(" "));
        sb.append("}");

        logger.debug("Shuffle: " + sb);

        if (membership.size() > 0) {
            Host target = getRandom(membership);
            Set<Host> subset = getRandomSubsetExcluding(membership, subsetSize, target);
            subset.add(self);
            sendMessage(new ShuffleMessage(subset), target);
            logger.debug("Sent SampleMessage {}", target);
        }
    }

    /*--------------------------------- Messages ---------------------------------------- */
    //When receiving a shuffle message, we reply with a subset of our membership.
    // Additionally, we try to connect to the peers in the sample that we are not connected to yet, adding them to the
    // pending set
    private void uponShuffle(ShuffleMessage msg, Host from, short sourceProto, int channelId) {
        logger.debug("Received {} from {}", msg, from);

        Set<Host> subset = getRandomSubsetExcluding(membership, subsetSize, from);
        subset.add(self);
        sendMessage(new ShuffleReplyMessage(subset), from, TCPChannel.CONNECTION_IN);
        for (Host h : msg.getSample()) {
            if (!h.equals(self) && !membership.contains(h) && !pending.contains(h)) {
                pending.add(h);
                openConnection(h);
            }
        }
    }

    // Upon receiving a shuffle reply, we try to connect to the peers in the sample that we are not connected to yet,
    // adding them to the pending set
    private void uponShuffleReply(ShuffleReplyMessage msg, Host from, short sourceProto, int channelId) {
        logger.debug("Received {} from {}", msg, from);
        for (Host h : msg.getSample()) {
            if (!h.equals(self) && !membership.contains(h) && !pending.contains(h)) {
                pending.add(h);
                openConnection(h);
            }
        }
    }

    // Auxiliary function to get a random element from the membership set
    private Host getRandom(Set<Host> hostSet) {
        int idx = rnd.nextInt(hostSet.size());
        int i = 0;
        for (Host h : hostSet) {
            if (i == idx)
                return h;
            i++;
        }
        return null;
    }

    // Auxiliary function to get a random subset of the membership set, excluding a given host
    private static Set<Host> getRandomSubsetExcluding(Set<Host> hostSet, int sampleSize, Host exclude) {
        List<Host> list = new LinkedList<>(hostSet);
        list.remove(exclude);
        Collections.shuffle(list);
        return new HashSet<>(list.subList(0, Math.min(sampleSize, list.size())));
    }

    /* --------------------------------- TCPChannel Events ---------------------------- */

    //When we establish a connection to a peer, we add it to the membership
    private void uponOutConnectionUp(OutConnectionUp event, int channelId) {
        Host peer = event.getNode();
        logger.debug("Connection to {} is up", peer);
        pending.remove(peer);
        if (membership.add(peer)) {
            logger.info("Added {} to membership", peer);
            triggerNotification(new PeerUp(peer));
        }
    }

    //When a connection to a peer goes down, we remove it from the membership
    private void uponOutConnectionDown(OutConnectionDown event, int channelId) {
        Host peer = event.getNode();
        logger.debug("Connection to {} is down cause {}", peer, event.getCause());
        membership.remove(event.getNode());
        logger.info("Removed {} from membership", peer);
        triggerNotification(new PeerDown(event.getNode()));

    }

    //When a connection to a peer fails, we remove it from the pending set
    private void uponOutConnectionFailed(OutConnectionFailed<ProtoMessage> event, int channelId) {
        logger.debug("Connection to {} failed cause: {}", event.getNode(), event.getCause());
        pending.remove(event.getNode());
    }

    private void uponInConnectionUp(InConnectionUp event, int channelId) {
        logger.trace("Connection from {} is up", event.getNode());
    }

    private void uponInConnectionDown(InConnectionDown event, int channelId) {
        logger.trace("Connection from {} is down, cause: {}", event.getNode(), event.getCause());
    }

    private void uponMsgFail(ProtoMessage msg, Host host, short destProto, Throwable throwable, int channelId) {
        logger.error("Message {} to {} failed, reason: {}", msg, host, throwable);
    }

}

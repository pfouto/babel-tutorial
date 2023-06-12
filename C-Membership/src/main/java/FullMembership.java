
import pingpong.messages.ShuffleMessage;
import pingpong.messages.ShuffleReplyMessage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pt.unl.fct.di.novasys.babel.core.GenericProtocol;
import pt.unl.fct.di.novasys.babel.exceptions.HandlerRegistrationException;
import pt.unl.fct.di.novasys.babel.generic.ProtoMessage;
import pt.unl.fct.di.novasys.channel.tcp.TCPChannel;
import pt.unl.fct.di.novasys.channel.tcp.events.*;
import pt.unl.fct.di.novasys.network.data.Host;
import pingpong.timers.ShuffleTimer;

import java.io.IOException;
import java.net.InetAddress;
import java.util.*;

public class FullMembership extends GenericProtocol {

    private static final Logger logger = LogManager.getLogger(FullMembership.class);

    //Protocol information, to register in babel
    public final static short PROTOCOL_ID = 102;
    public final static String PROTOCOL_NAME = "FullMembership";

    private final Host self;     //My own address/port
    private final Set<Host> membership; //Peers I am connected to
    private final Set<Host> pending; //Peers I am trying to connect to

    private int shuffleTimer; //param: timeout for samples
    private int subsetSize; //param: maximum size of sample;

    private final Random rnd;
    protected int channelId;

    private int shuffleIdCounter;

    public FullMembership(Properties properties, Host self) throws IOException, HandlerRegistrationException {
        super(PROTOCOL_NAME, PROTOCOL_ID);

        this.self = self;
        this.membership = new HashSet<>();
        this.pending = new HashSet<>();

        this.rnd = new Random();

        this.shuffleIdCounter = 0;

        String address = properties.getProperty("address");
        String port = properties.getProperty("port");
        Properties channelProps = new Properties();
        channelProps.setProperty(TCPChannel.ADDRESS_KEY, address); //The address to bind to
        channelProps.setProperty(TCPChannel.PORT_KEY, port); //The port to bind to
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
    }

    @Override
    public void init(Properties props) {
        this.subsetSize = Integer.parseInt(props.getProperty("sample_size"));
        this.shuffleTimer = Integer.parseInt(props.getProperty("shuffle_time"));

        if (props.containsKey("contact")) {
            try {
                String contact = props.getProperty("contact");
                String[] hostElems = contact.split(":");
                Host contactHost = new Host(InetAddress.getByName(hostElems[0]), Short.parseShort(hostElems[1]));
                pending.add(contactHost);
                openConnection(contactHost);
            } catch (Exception e) {
                logger.error("Invalid contact on configuration: '" + props.getProperty("contact"));
                e.printStackTrace();
                System.exit(-1);
            }
        }
        setupPeriodicTimer(new ShuffleTimer(), this.shuffleTimer, this.shuffleTimer);
    }

    /*--------------------------------- Messages ---------------------------------------- */
    private void uponShuffle(ShuffleMessage msg, Host from, short sourceProto, int channelId) {
        logger.debug("Received {} from {}", msg, from);

        Set<Host> subset = getRandomSubsetExcluding(membership, subsetSize, from);
        subset.add(self);
        sendMessage(new ShuffleReplyMessage(msg.getShuffleId(), subset), from, TCPChannel.CONNECTION_IN);
        for (Host h : msg.getSample()) {
            if (!h.equals(self) && !membership.contains(h) && !pending.contains(h)) {
                pending.add(h);
                openConnection(h);
            }
        }
    }

    private void uponShuffleReply(ShuffleReplyMessage msg, Host from, short sourceProto, int channelId) {
        logger.debug("Received {} from {}", msg, from);
        for (Host h : msg.getSample()) {
            if (!h.equals(self) && !membership.contains(h) && !pending.contains(h)) {
                pending.add(h);
                openConnection(h);
            }
        }
    }

    /*--------------------------------- Timers ---------------------------------------- */
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
            int shuffleId = shuffleIdCounter++;
            sendMessage(new ShuffleMessage(subset, shuffleId), target);
            logger.debug("Sent SampleMessage {}", target);
        }
    }

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

    private static Set<Host> getRandomSubsetExcluding(Set<Host> hostSet, int sampleSize, Host exclude) {
        List<Host> list = new LinkedList<>(hostSet);
        list.remove(exclude);
        Collections.shuffle(list);
        return new HashSet<>(list.subList(0, Math.min(sampleSize, list.size())));
    }

    /* --------------------------------- TCPChannel Events ---------------------------- */

    private void uponOutConnectionUp(OutConnectionUp event, int channelId) {
        Host peer = event.getNode();
        logger.debug("Connection to {} is up", peer);
        pending.remove(peer);
        if (membership.add(peer)) {
            logger.info("Added {} to membership", peer);
        }
    }

    private void uponOutConnectionDown(OutConnectionDown event, int channelId) {
        Host peer = event.getNode();
        logger.debug("Connection to {} is down cause {}", peer, event.getCause());
        membership.remove(event.getNode());
        logger.info("Removed {} from membership", peer);
    }

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

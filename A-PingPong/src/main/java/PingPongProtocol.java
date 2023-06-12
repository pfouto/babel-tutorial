import messages.PingMessage;
import messages.PongMessage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pt.unl.fct.di.novasys.babel.core.GenericProtocol;
import pt.unl.fct.di.novasys.babel.exceptions.HandlerRegistrationException;
import pt.unl.fct.di.novasys.babel.generic.ProtoMessage;
import pt.unl.fct.di.novasys.channel.tcp.TCPChannel;
import pt.unl.fct.di.novasys.channel.tcp.events.*;
import pt.unl.fct.di.novasys.network.data.Host;
import timers.NextPingTimer;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.util.Properties;

public class PingPongProtocol extends GenericProtocol {

    public static final int DEFAULT_PORT = 9000;
    private static final Logger logger = LogManager.getLogger(PingPongProtocol.class);
    public static final short PROTO_ID = 1;


    private int channelId;

    private int nextPingId = 0;

    private Host pingTarget;
    private int nPings;
    private String message;
    private int pingIntervalMillis;
    public PingPongProtocol() {
        // The super constructor receives the protocol name and the (unique) protocol ID
        super("PingPong", PROTO_ID);

    }

    public void init(Properties props) throws IOException, HandlerRegistrationException {

        if(props.containsKey("n_pings")){
            nPings = Integer.parseInt(props.getProperty("n_pings"));
        } else
            nPings = 0;

        if(nPings > 0) {
            message = props.getProperty("message");
            pingIntervalMillis = Integer.parseInt(props.getProperty("ping_interval"));

            InetAddress pingTargetAddr = Inet4Address.getByName(props.getProperty("target_address"));
            int pingTargetPort;
            if (props.containsKey("target_port"))
                pingTargetPort = Integer.parseInt(props.getProperty("target_port"));
            else
                pingTargetPort = DEFAULT_PORT;

            pingTarget = new Host(pingTargetAddr, pingTargetPort);
        }
        Properties channelProps = new Properties();
        // configuration of the network channel

        // set network address to listen on
        if (props.containsKey("interface"))
            //if defined interface, get interface address
            channelProps.setProperty(TCPChannel.ADDRESS_KEY, Main.getAddress(props.getProperty("interface")));
        else if (props.containsKey("address"))
            // else use defined interface
            channelProps.setProperty(TCPChannel.ADDRESS_KEY, props.getProperty("address"));
        else {
            // channel will throw exception upon creation
        }

        // set network port to listen on
        if (props.containsKey("port")) {
            // if port is defined, used defined port
            channelProps.setProperty(TCPChannel.PORT_KEY, props.getProperty("port"));
        }else
            // else use default value
            channelProps.setProperty(TCPChannel.PORT_KEY, DEFAULT_PORT + "");

        // Receive a notification that the message was sent to the network
        channelProps.setProperty(TCPChannel.TRIGGER_SENT_KEY, "true");

        // create the channel with the provided properties
        channelId = createChannel(TCPChannel.NAME, channelProps);

        // register channel event handlers
        registerChannelEventHandler(channelId, InConnectionDown.EVENT_ID, this::uponInConnectionDown);
        registerChannelEventHandler(channelId, InConnectionUp.EVENT_ID, this::uponInConnectionUp);
        registerChannelEventHandler(channelId, OutConnectionDown.EVENT_ID, this::uponOutConnectionDown);
        registerChannelEventHandler(channelId, OutConnectionUp.EVENT_ID, this::uponOutConnectionUp);
        registerChannelEventHandler(channelId, OutConnectionFailed.EVENT_ID, this::uponOutConnectionFailed);

        // register message serializers
        registerMessageSerializer(channelId, PingMessage.MSG_ID, PingMessage.serializer);
        registerMessageSerializer(channelId, PongMessage.MSG_ID, PongMessage.serializer);

        // register protocol handlers
        // register message handlers
        registerMessageHandler(channelId, PingMessage.MSG_ID, this::uponReceivePingMessage, this::uponMessageFailed);
        registerMessageHandler(channelId, PongMessage.MSG_ID, this::uponReceivePongMessage, this::uponMessageFailed);

        // register timer handlers
        registerTimerHandler(NextPingTimer.TIMER_ID, this::uponNextPing);

        if(nPings > 0) {
            logger.info("Opening a TCP connection to {}", pingTarget);
            openConnection(pingTarget, channelId);
        }
    }

    private void uponNextPing(NextPingTimer timer, long timerId) {
        sendPingMessage(pingTarget, message);
        timer.incrementSentPings();
        if (timer.getSentPings() >= nPings) {
            logger.info("Sent {} pings. Closing connection", timer.getSentPings());
            cancelTimer(timerId);
            closeConnection(pingTarget, channelId);
            System.exit(0);
        }
    }

    private void uponOutConnectionUp(OutConnectionUp event, int channel) {
        logger.info("Connection to {} is now up", event.getNode());
        // start the timer
        setupPeriodicTimer(new NextPingTimer(), 0, pingIntervalMillis);
    }

    private void uponOutConnectionFailed(OutConnectionFailed<ProtoMessage> event, int channel) {
        logger.debug(event);
        System.exit(1);
    }


    /**
     * Send Ping message to Host destination with the given string message
     * @param destination Host destination
     * @param message String message
     */
    public void sendPingMessage(Host destination, String message) {
        logger.debug("Sending Ping Message to {} with message {}", destination, message);
        sendMessage(new PingMessage(++nextPingId, message), destination);
    }

    /**
     * Handle a newly received Ping Message
     * Reply to the Source Host with a Pong Message
     * @param msg PingMessage
     * @param from Source Host
     * @param sourceProto Source protocol ID
     * @param channelId Source channel ID (from which channel was the message was received)
     */
    public void uponReceivePingMessage(PingMessage msg, Host from, short sourceProto, int channelId) {
        logger.info("Received PingMessage with id: {} and message: {}", msg.getPingId(), msg.getMessage());
        // use connection created by client (TCPChannel.CONNECTION_IN) to reply with pong message
        sendMessage(new PongMessage(msg.getPingId(), msg.getMessage()), from, TCPChannel.CONNECTION_IN);
    }

    /**
     * Handle a newly received Pong Message
     * Print received pingId and message
     * @param msg PongMessage
     * @param from Source Host
     * @param sourceProto Source protocol ID
     * @param channelId Source channel ID (from which channel was the message was received)
     */
    public void uponReceivePongMessage(PongMessage msg, Host from, short sourceProto, int channelId) {
        logger.info("Received PongMessage with id: {} and message: {}", msg.getPingId(), msg.getMessage());
    }

    /**
     * Handle the case when a message fails to be (confirmed to be) delivered to the destination
     * @param msg the message that failed delivery
     * @param host the destination host
     * @param i
     * @param throwable
     * @param i1
     */
    private void uponMessageFailed(ProtoMessage msg, Host host, short i, Throwable throwable, int i1) {
        logger.warn("Failed: " + msg + ", to: " + host + ", reason: " + throwable.getMessage());
    }

    private void uponInConnectionUp(InConnectionUp event, int channel) {
        logger.debug(event);
    }

    private void uponInConnectionDown(InConnectionDown event, int channel) {
        logger.info(event);
    }

    private void uponOutConnectionDown(OutConnectionDown event, int channel) {
        logger.warn(event);
    }

}

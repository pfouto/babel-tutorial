package pingpong;

import pingpong.messages.PingMessage;
import pingpong.messages.PongMessage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pt.unl.fct.di.novasys.babel.core.GenericProtocol;
import pt.unl.fct.di.novasys.babel.exceptions.HandlerRegistrationException;
import pt.unl.fct.di.novasys.babel.generic.ProtoMessage;
import pt.unl.fct.di.novasys.channel.tcp.TCPChannel;
import pt.unl.fct.di.novasys.channel.tcp.events.*;
import pt.unl.fct.di.novasys.network.data.Host;
import pingpong.requests.PingRequest;
import pingpong.requests.PongReply;
import pingpong.timers.NextPingTimer;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class PingPongProtocol extends GenericProtocol {


    public static final int DEFAULT_PORT = 9000; // default port to listen on
    private static final Logger logger = LogManager.getLogger(PingPongProtocol.class); // logger for the protocol
    public static final short PROTO_ID = 1; // unique protocol id


    private int channelId; // id of the channel used by the protocol

    private int nextPingId = 0; // id of the next ping message to send
    private int pingIntervalMillis; // interval between pings

    private int nextPendingRequest = 0; // id of the next pending request

    private PingRequest ongoingRequest = null; // ongoing request (if any)
    private short ongoingRequestSource = -1; // source of the ongoing request (if any)

    private Map<Integer, Long> ongoingPings = new HashMap<>(); // map of ongoing pings (id -> send time)

    public PingPongProtocol() {
        // The super constructor receives the protocol name and the (unique) protocol ID
        super("PingPong", PROTO_ID);

    }

    public void init(Properties props) throws IOException, HandlerRegistrationException {


        Properties channelProps = new Properties();
        // configuration of the network channel

        // set network address to listen on
        if (props.containsKey("interface"))
            //if defined interface, get interface address
            channelProps.setProperty(TCPChannel.ADDRESS_KEY, Utils.getAddress(props.getProperty("interface")));
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

        // register request handlers
        registerRequestHandler(PingRequest.REQUEST_ID, this::uponReceivePingRequest);

    }

    private void uponReceivePingRequest(PingRequest pingRequest, short sourceProtocol) {
        ongoingRequest = pingRequest;
        logger.info("Received ping request to {}. Sending {} pings.",
                pingRequest.getDestination(), pingRequest.getNPings());
        openConnection(pingRequest.getDestination(), channelId);
    }

    /**
     * Handle a Timer event
     * Send a Ping message to the pingTarget
     * @param timer NextPingTimer
     * @param timerId Timer ID
     */
    private void uponNextPing(NextPingTimer timer, long timerId) {
        // send a ping message to target
        sendPingMessage(ongoingRequest.getDestination(),
                ongoingRequest.getMessage());

        // increment the number of sent pings in the timer
        timer.incrementSentPings();

        if (timer.getSentPings() >= ongoingRequest.getNPings()) { // if we have sent all pings
            logger.info("Sent {} pings. Closing connection", timer.getSentPings());
            cancelTimer(timerId); // cancel the timer
            closeConnection(ongoingRequest.getDestination(), channelId); // close the connection
            System.exit(0); // exit
        }
    }

    /**
     * Handle when an open connection operation succeeded
     * Start the periodic timer to send Ping pingpong.messages
     * @param event OutConnectionUp event
     * @param channel Channel ID
     */
    private void uponOutConnectionUp(OutConnectionUp event, int channel) {
        logger.info("Connection to {} is now up", event.getNode());
        // start the timer
        setupPeriodicTimer(new NextPingTimer(), 0, pingIntervalMillis);
    }

    /**
     * Handle when an open connection operation has failed
     * Print error message and exit
     * @param event OutConnectionFailed event
     * @param channel Channel ID
     */
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
        ongoingPings.put(++nextPingId, System.currentTimeMillis());
        sendMessage(new PingMessage(nextPingId, message), destination);
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
        PongReply reply = ongoingRequest.produceReply(System.currentTimeMillis() - ongoingPings.get(msg.getPingId()));
        sendReply(reply, ongoingRequestSource);
    }

    /**
     * Handle the case when a message fails to be (confirmed to be) delivered to the destination
     * Print the error
     * @param msg the message that failed delivery
     * @param host the destination host
     * @param destProto the destination protocol ID
     * @param error the error that caused the failure
     * @param channelId the channel ID (from which channel was the message was sent)
     */
    private void uponMessageFailed(ProtoMessage msg, Host host, short destProto, Throwable error, int channelId) {
        logger.warn("Failed message: {} to host: {} with error: {}", msg, host, error.getMessage());
    }

    /**
     * Handle the case when someone opened a connection to this node
     * Print the event
     * @param event the event containing the connection information
     * @param channel the channel ID (from which channel the event was received)
     */
    private void uponInConnectionUp(InConnectionUp event, int channel) {
        logger.debug(event);
    }

    /**
     * Handle the case when someone closed a connection to this node
     * Print the event
     * @param event the event containing the connection information
     * @param channel the channel ID (from which channel the event was received)
     */
    private void uponInConnectionDown(InConnectionDown event, int channel) {
        logger.info(event);
    }

    /**
     * Handle the case when a connection to a remote node went down or was closed
     * Print the event
     * @param event the event containing the connection information
     * @param channel the channel ID (from which channel the event was received)
     */
    private void uponOutConnectionDown(OutConnectionDown event, int channel) {
        logger.warn(event);
    }

}

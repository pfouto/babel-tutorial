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
import pingpong.timers.NextPingTimer;
import utils.NetworkingUtilities;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.util.Properties;

public class PingPongProtocol extends GenericProtocol {

    public static final int DEFAULT_PORT = 9000; // default port to listen on
    private static final Logger logger = LogManager.getLogger(PingPongProtocol.class); // logger for the protocol
    public static final short PROTO_ID = 1; // unique protocol id


    private int channelId; // id of the channel used by the protocol

    private int nextPingId = 0; // id of the next ping message to send

    private Host pingTarget; // target to send pings to
    private int nPings; // number of pings to send
    private String message; // message to send in the ping
    private int pingIntervalMillis; // interval between pings

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
            pingIntervalMillis = Integer.parseInt(props.getProperty("ping_interval", "100"));

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
            channelProps.setProperty(TCPChannel.ADDRESS_KEY, NetworkingUtilities.getAddress(props.getProperty("interface")));
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

        logger.info("PingPongProtocol initialized, running on " + channelProps.getProperty(TCPChannel.ADDRESS_KEY) + ":" + channelProps.getProperty(TCPChannel.PORT_KEY));

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

        if(nPings > 0) { // if we have to send pings
            logger.info("Opening a TCP connection to ping target {}", pingTarget);
            openConnection(pingTarget, channelId); // open connection to target
        } // else wait for pings only
    }

    /**
     * Handle a Timer event
     * Send a Ping message to the pingTarget
     * @param timer NextPingTimer
     * @param timerId Timer ID
     */
    private void uponNextPing(NextPingTimer timer, long timerId) {
        if (timer.getSentPings() >= nPings) { // if we have sent all pings
            logger.info("Sent {} pings. Closing connection", timer.getSentPings());
            cancelTimer(timerId); // cancel the timer
            closeConnection(pingTarget, channelId); // close the connection
            System.exit(0); // exit
        }

        // send a ping message to target
        sendPingMessage(pingTarget, message);

        // increment the number of sent pings in the timer
        timer.incrementSentPings();


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

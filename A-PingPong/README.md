# Step A - Simple PingPong

In this step we build a very simple ping pong protocol with the following specification:

## Protocol Specification

The protocol has two messages:
- ``PingMessage``: sent by the initiator and carries a monotonically increasing ``counter`` and a string ``message``
- ``PongMessage``: sent by the responder and carries a monotonically increasing ``counter`` and a string ``message``

The protocol has one timer:
- ``NextPingTimer``: used to trigger the next ping message


The protocol handles may process the following channel events:
- ``OutConnectionUp``: when an outgoing connection is established
- ``OutConnectionFailed``: when an outgoing connection fails
- ``OutConnectionDown``: when an outgoing connection is closed
- ``InConnectionUp``: when an incoming connection is established
- ``InConnectionDown``: when an incoming connection is closed


The protocol has the following parameters:
- ``n_pings``: the number of ping messages to send, if not specified or is set to ``0``, the protocol will only wait for ``PingMessage`` messages
- ``ping_interval``: the period between ping messages
- ``target_address``: the address of the target node
- ``target_port``: the port of the target node


The protocol works as follows:
- The protocol will check if ``n_pings`` is set to higher than ``0``. If it is, the protocol will open a connection to the target host.
- When the ``OutConnectionUp`` event is received, the protocol will start the ``NextPingTimer``.
- When the ``NextPingTimer`` triggers, the protocol will send a ``PingMessage`` to the target host.
- When the protocol receives a ``PingMessage`` it will send a ``PongMessage`` to the host that sent the ``PingMessage``.
- When the protocol receives a ``PongMessage`` it will print the message to the console.
- The protocol will stop sending ``PingMessage`` messages when it has sent ``n_pings`` messages.
- When the protocol has sent ``n_pings`` messages, it will close the connection to the target host.


Use the following abstractions to implement the protocol:

## Networking and Timer Abstractions

### Networking Abstractions

```java
import pt.unl.fct.di.novasys.babel.generic.ProtoMessage;

public class MyProtocolMessage extends ProtoMessage {

    public static final short MSG_CODE = 1; // unique code of the message
    
    // protocol message fields
    
    public MyProtocolMessage() {
        super(MSG_CODE);
    }
    
    // protocol message logic
    
    // protocol message serialization/deserialization
    public static ISerializer<? extends ProtoMessage> serializer = new ISerializer<MyProtocolMessage>() {
        public void serialize(MyProtocolMessage msg, ByteBuf out) {
            // serialize the message
            // out is a Netty ByteBuf where the message should be serialized
        }

        public PingMessage deserialize(ByteBuf in) {
            // deserialize the message
            // in is a Netty ByteBuf from where the message should be deserialized
        }
    };
}
```

### Timer Abstractions

```java
import pt.unl.fct.di.novasys.babel.generic.ProtoTimer;

public class MyProtocolTimer extends ProtoTimer {
    
    public static final short TIMER_CODE = 1; // unique code of the timer

    // protocol timer fields
    
    public MyProtocolTimer() {
        super(TIMER_CODE);
    }
    
    // protocol timer logic
    
    @Override
    public ProtoTimer clone() {
        return this;
    }
}
```

## Exercise

Implement the PingPong protocol.

```java
import pingpong.timers.NextPingTimer;
import pt.unl.fct.di.novasys.babel.core.GenericProtocol;
import pt.unl.fct.di.novasys.channel.tcp.events.OutConnectionUp;

public class PingPongProtocol extends GenericProtocol {

    public PingPongProtocol() {
        super("PingPong", (short) 1);
    }

    public void init(Properties props) {
        // implement the initialization of the protocol
        // need to register handlers for events, timers and messages
    }

    // implement protocol handlers
    private void uponNextPing(NextPingTimer timer, long timerId) {
        // what to do when the timer "NextPingTimer" triggers
    }

    private void uponReceivePingMessage(PingMessage msg, Host from, short sourceProto, int channelId) {
        // what to do when a "PingMessage" is received
    }

    private void uponReceivePongMessage(PongMessage msg, Host from, short sourceProto, int channelId) {
        // what to do when a "PongMessage" is received
    }

    // channel events
    private void uponOutConnectionUp(OutConnectionUp event, int channelId) {
        // what to do when an "OutConnectionUp" event is received
    }
    
    private void uponOutConnectionFailed(OutConnectionFailed event, int channelId) {
        // what to do when an "OutConnectionFailed" event is received
    }
    
    private void uponOutConnectionDown(OutConnectionDown event, int channelId) {
        // what to do when an "OutConnectionDown" event is received
    }
    
    private void uponInConnectionUp(InConnectionUp event, int channelId) {
        // what to do when an "InConnectionUp" event is received
    }
    
    private void uponInConnectionDown(InConnectionDown event, int channelId) {
        // what to do when an "InConnectionDown" event is received
    }
}

```

The main class should look like this:

```java
import ...

public class Main {

    private static final Logger logger = LogManager.getLogger(Main.class); // log4j logger

    public static void main(String[] args) throws Exception {

        //Creates a new instance of babel
        Babel babel = Babel.getInstance();

        //Reads arguments from the command line and loads them into a Properties object
        Properties props = Babel.loadConfig(args, null);

        //Creates a new instance of the PingPongProtocol
        PingPongProtocol pingPong = new PingPongProtocol();

        //Registers the protocol in babel
        babel.registerProtocol(pingPong);

        //Initializes the protocol
        pingPong.init(props);

        //Starts babel
        babel.start();
    }

}
```

## How to compile

``mvn clean package``

``docker build  -t babel-tutorial/a-pingpong .``

## How to run

### Args

``docker run --rm -h server -it babel-tutorial/a-pingpong interface=eth0 port=9000``

``docker run --rm -h client -it babel-tutorial/a-pingpong interface=eth0 port=9001 target_addres=server target_port=9000``

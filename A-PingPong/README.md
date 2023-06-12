# Step A - Simple PingPong

In this step we build a very simple ping pong protocol.

## Networking and Timer Abstractions

### Networking Abstractions

### Timer Abstractions


## Exercise

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
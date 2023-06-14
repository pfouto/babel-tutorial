# Step D - Dissemination Protocol

In this step, we will build a dissemination protocol that leverages on the membership protocol from the previous step.
Upon being requested to propagate a message, the dissemination protocol will pick a number of nodes from its known
membership and propagate it to them. Upon receiving a (non-duplicated) message, the protocol will simply re-transmit it
forward to other nodes.

## Exercise

Before implementing the dissemination protocol, we will make two changes to the membership protocol.

First, we need to change the logic of the membership protocol to trigger a
notification each time a node is added or removed from the membership. For this we create two notifications: `PeerDown`
and `PeerUp`.

Example of `PeerDown` notification:

```java
public class PeerDown extends ProtoNotification {

    public static final short NOTIFICATION_ID = 101;

    private final Host peer;

    public PeerDown(Host peer) {
        super(NOTIFICATION_ID);
        this.peer = peer;
    }


    public Host getPeer() {
        return peer;
    }
}
```

Then, using the `triggernotification` method, we trigger a `PeerDown` notification when a node is removed from the
membership and a `PeerUp` notification when a node is added to the membership.

Second, we will create a `ChannelNotification` which will be used to send information to the dissemination protocol
about the channel that it should use to send messages to a given peer (thus avoiding the creation of multiple network
channels and multiple connection between the same pair of peers). This notification will also contain the address of the
local node:

```java
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
```

We trigger this notification at the end of the `init` step.

After having made these changes to the membership protocol, we can now implement the dissemination protocol.

## Dissemination Protocol Specification

The protocol has one timer:

- `RandomGossipTimer`: used to trigger the dissemination of a new message

The protocol a single message:

- `GossipMessage`: this message contains the message to be disseminated, along with a random UUID (to detect duplicates)
  and a round number to keep track of the number of hops that the message took before reaching each node.

The protocol has the following parameters:

- ``gossip_time``: the period between gossip steps (in milliseconds)
- ``gossip_size``: the number of known peers to which each message should be disseminated

The protocol works as follows:

- Upon receiving the `ChannelNotification`, the protocol stores the channel id and the address of the local node.
  Additionally, it calls the method `registerSharedChannel` (with the receiving channel id as parameter) to inform the
  babel engine that the channel is shared between
  the membership and the dissemination protocols, and then registers the handlers and serializers for
  the `GossipMessage`.

- A timer is started with the period ``gossip_time``. When the timer expires, the node generates a GossipMessage
  containing a random UUID, a round number of 0 and a random message to be disseminated. It then adds the message to a
  set of messages that it has already disseminated (to prevent disseminating duplicated messages) and sends the message
  to ``gossip_size`` random peers from its known membership.
- Upon receiving a GossipMessage, the node checks if it has already received the message (by checking the UUID). If it
  has not, it increments the round number and re-transmits the message to ``gossip_size`` random peers from its known
  membership.
- Upon receiving a PeerUp or PeerDown notification from the membership protocol, the node updates its known membership.

## Exercise

Implement the Dissemination protocol.
The protocol skeleton should look like this:

```java
public class FloodGossip extends GenericProtocol {

  public FloodGossip() {
    super(PROTO_NAME, PROTO_ID);
  }

  @Override
  public void init(Properties props) {
  }

  private void onChannelNotification(ChannelNotification not, short sourceProto) {
  }

  private void uponRandomGossipTimer(RandomGossipTimer timer, long timerId) {
  }

  private void uponReceiveGossip(GossipMessage msg, Host from, short sourceProto, int channelId) {
  }

  private void uponPeerUp(PeerUp not, short sourceProto) {
  }

  private void uponPeerDown(PeerDown not, short sourceProto) {
  }
}
```

## How to compile

``mvn clean package``

``docker build -t babel-tutorial/d-dissemination .``

## How to run

``docker network create babel-tutorial-net``

- Run a contact node:

``docker run --network babel-tutorial-net --rm -h node-1 --name node-1 -it babel-tutorial/d-dissemination sample_size=2``

- Then run any number of nodes, specifying the contact node:

``docker run --network babel-tutorial-net --rm -h node-2 --name node-2 -it babel-tutorial/d-dissemination sample_size=2 contact=node-1:8000``

``docker run --network babel-tutorial-net --rm -h node-3 --name node-3 -it babel-tutorial/d-dissemination sample_size=2 contact=node-1:8000``

``docker run --network babel-tutorial-net --rm -h node-4 --name node-4 -it babel-tutorial/d-dissemination sample_size=2 contact=node-1:8000``

...


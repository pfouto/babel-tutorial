# Step C - Membership Protocol

In this step we will build a very simple membership protocol, in which each protocol periodically exchanges a sample of
its known membership with a random peer - we call this process "shuffle".

## Membership Protocol Specification

The protocol has one timer:

- `ShuffleTimer`: used to trigger the next shuffle step

The protocol has two messages:

- `ShuffleMessage`: the initiator sends this message to a random peer. It contains a sample of the initiator's
  membership.
- `ShuffleReplyMessage`: the responder sends this message to the initiator. It contains a sample of the responder's
  membership.

The protocol has the following parameters:

- ``sample_size``: the size of the sample to exchange
- ``shuffle_time``: the period between shuffle steps (in milliseconds)
- ``contact``: the ip address and port of a node in the network (in the format `ip:port`)

The protocol works as follows:

- If the node has a contact, it starts with that contact in its know membership.

- A timer is started with the period ``shuffle_time``. When the timer expires, the node selects a random peer in its
  know membership and sends a `ShuffleMessage` to it. This message will contain a sample (with size ``sample_size``) of
  the node's membership.
- Upon receiving a `ShuffleMessage`, the node replies with a `ShuffleReplyMessage` containing a sample of its own
  membership.
- Additionally, the node adds all nodes in the sample that it did not know before to a list of ``pending`` nodes and
  attempts to create a connection to them.
- If the node is able to connect to a node in the ``pending`` list, it adds that node to its known membership and
  removes it from the ``pending`` list.
- If the node is not able to connect to a node in the ``pending`` list, it simply removes it from the list.
- If the connection to a node in the known membership is lost, that node is removed from the membership.

## Exercise

Implement the FullMembership protocol.
The protocol skeleton should look like this:

```java
public class FullMembership extends GenericProtocol {

    public FullMembership() {
        super(PROTOCOL_NAME, PROTOCOL_ID);
    }

    @Override
    public void init(Properties properties) {
        // implement the initialization of the protocol: reading parameters, connecting to the contact node, etc.
        // need to register handlers for events, timers and messages
    }

    private void uponShuffleTimer(ShuffleTimer timer, long timerId) {
        //When a shuffle timer expires, we select a random peer from the membership and send a ShuffleMessage to it
    }

    private void uponShuffle(ShuffleMessage msg, Host from, short sourceProto, int channelId) {
        //Upon receiving a shuffle message, we reply with a ShuffleReplyMessage containing a sample of our membership
        // Additionally, we add all peers in the sample that we are not connected to yet to the pending set and attempt to connect to them
    }

    private void uponShuffleReply(ShuffleReplyMessage msg, Host from, short sourceProto, int channelId) {
      // Upon receiving a shuffle reply, we try to connect to the peers in the sample that we are not connected to yet,
      // adding them to the pending set
    }
    
    private void uponOutConnectionUp(OutConnectionUp event, int channelId) {
      //When we successfully establish a connection to a peer, we add it to the membership
    }

    private void uponOutConnectionDown(OutConnectionDown event, int channelId) {
      //When a connection to a peer goes down, we remove it from the membership
    }

    private void uponOutConnectionFailed(OutConnectionFailed<ProtoMessage> event, int channelId) {
      //When a connection to a peer fails, we remove it from the pending set
    }

    private void uponInConnectionUp(InConnectionUp event, int channelId) {
        //In our example, we do nothing when a connection is established to us
    }

    private void uponInConnectionDown(InConnectionDown event, int channelId) {
        //In our example, we do nothing when a connection to us goes down
    }
}
```

## How to compile

To compile your code, run the following commands:
- ``mvn clean package``
- ``docker build -t babel-tutorial/c-membership .``

## How to run

### Setup
You need to create a docker network for the tutorial:
``docker network create babel-tutorial-net``

To remove the network:
``docker network rm babel-tutorial-net``

- Run a contact node:

``docker run --network babel-tutorial-net --rm -h node-1 --name node-1 -it babel-tutorial/c-membership sample_size=2``

- Then run any number of nodes, specifying the contact node:

``docker run --network babel-tutorial-net --rm -h node-2 --name node-2 -it babel-tutorial/c-membership sample_size=2 contact=node-1:8000``

``docker run --network babel-tutorial-net --rm -h node-3 --name node-3 -it babel-tutorial/c-membership sample_size=2 contact=node-1:8000``

``docker run --network babel-tutorial-net --rm -h node-4 --name node-4 -it babel-tutorial/c-membership sample_size=2 contact=node-1:8000``

...


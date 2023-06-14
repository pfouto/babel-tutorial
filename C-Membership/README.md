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

## How to compile

``mvn clean package``

``docker build  -t babel-tutorial/c-membership .``


``docker network create babel-tutorial-net``
``docker network rm babel-tutorial-net``

## How to run

### Args

``docker run --network babel-tutorial-net --rm -h node-1 --name node-1 -it babel-tutorial/c-membership interface=eth0``

``docker run --network babel-tutorial-net --rm -h node-2 --name node-1 -it babel-tutorial/c-membership interface=eth0``



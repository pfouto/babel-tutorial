# Step E - Chat Application

In this step, we will create a very simple chat application that leverages on the dissemination and membership protocols
from the previous steps.

## Exercise

First, we will alter the dissemination protocol to receive requests to send messages (instead of relying on a timer)
and to send a notification every time a message is received. To do this we need to:
- Create a `BroadcastRequest` that will be sent by the chat application and received by the dissemination protocol.
- Create a `DeliverNotification` that will be triggered by the dissemination protocol when a message is received.
- Remove the timer from the dissemination protocol, and instead, use the `BroadcastRequest` handler to trigger the dissemination
  of new messages.
- Trigger the `DeliverNotification` when a message is received.

## Chat protocol specification:

Our chat application will be implemented as a Babel protocol. However, as we are building an interactive application,
our protocol will be a bit different from the ones we have seen so far. In particular, we will create a new thread
that will be responsible for reading user input and sending the requests to the dissemination protocol.

Other than this, all the protocol needs is to subscribe to the `DeliverNotification` and print the received messages.

You can check the source code of step B as an example.

## How to compile

To compile your code, run the following commands:
- ``mvn clean package``
- ``docker build  -t babel-tutorial/e-chat .``

## How to run

### Setup
You need to create a docker network for the tutorial:
``docker network create babel-tutorial-net``

To remove the network:
``docker network rm babel-tutorial-net``

### Args

### Run

``docker run --network babel-tutorial-net --rm -h node-1 --name node-1 -it babel-tutorial/e-chat``

``docker run --network babel-tutorial-net --rm -h node-2 --name node-2 -it babel-tutorial/e-chat contact=node-1:8000``

``docker run --network babel-tutorial-net --rm -h node-3 --name node-3 -it babel-tutorial/e-chat contact=node-1:8000``

``docker run --network babel-tutorial-net --rm -h node-4 --name node-4 -it babel-tutorial/e-chat contact=node-1:8000``
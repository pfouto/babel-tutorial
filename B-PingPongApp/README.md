# Step B - PingPing Application

In this step we extend the PingPong protocol to implement a simple interactive application that sends a ping message and waits for a pong reply.
To do this, the developer needs to use InterProcess Communication (IPC) abstractions, to enable the application to coordinate with the PingPong protocol.

## InterProcess Communication Abstractions

Babel offers simple and generic IPC abstractions that allow the developer to easily implement complex applications that use multiple protocols.

Babel offers two types of IPC abstractions:
- **Request-Reply**: used to send a direct request to a protocol. The requested protocol can produce an asynchronous reply to the requester protocol. 
- **Notification**: used to notify a protocol/application of an event that occurred in another protocol/application. A single notification can go to multiple protocols/applications.

### Request-Reply Abstractions

To use the Request-Reply abstractions, the developer needs to extend the `ProtoRequest` and `ProtoReply` classes.
These classes are used to define the request and reply message between protocols that are in the same Babel processes.

```java

import pt.unl.fct.di.novasys.babel.generic.ProtoRequest;

public class MyProtocolRequest extends ProtoRequest {
    
    public static final short REQUEST_ID = 1;
    
    public MyProtocolRequest() {
        super(MyProtocol.REQUEST_ID);
    }
}
```

```java
import pt.unl.fct.di.novasys.babel.generic.ProtoReply;

public class MyProtocolReply extends ProtoReply {

    public static final short REPLY_ID = 1;

    public MyProtocolReply() {
        super(MyProtocol.REPLY_ID);
    }
}

```

Babel offers API calls that are defined in the GenericProtocol class to send requests and replies to the protocol with the
corresponding numerical unique identifier.

```java
sendRequest(new MyProtocolRequest(), SomeOtherProtocol.PROTO_ID);
```

```java
private void uponMyProtocolRequest(MyProtocolRequest request, short requesterProto) {
    // handle the request
    sendReply(new MyProtocolReply(), requesterProto);
}
```

### Notification Abstractions

To use the Notification abstractions, the developer needs to extend the `ProtoNotification` class.
This class is used to define notification messages between protocols that are in the same Babel processes.

```java

public class MyProtocolNotification extends ProtoNotification {
    
    public static final short NOTIFICATION_ID = 1;
    
    public MyProtocolNotification() {
        super(MyProtocol.NOTIFICATION_ID);
    }
}
```

Babel offers API calls that are defined in the GenericProtocol class to register interest in receiving notifications (subscribe) and to trigger notifications.

```java
subscribeNotification(MyProtocolNotification.NOTIFICATION_ID, notificationHandler);
```

```java
triggerNotification(new MyProtocolNotification());
```

## Exercise

Modify the PingPong protocol in [Step A](../A-PingPong) to implement the PingPing application.
Create a new App protocol that reads from the console the ping command and performs a request to the ``PingPongProtocol``.

The App protocol send a ``PingRequest`` request to the ``PingPongProtocol`` and waits for a ``PongReply`` reply.
When the reply is received, the App protocol prints the reply to the console.

The App protocol should be able to handle a single command in the format:

    ``ping <target_addr:target_port> <message> [n_pings]``
    where ``<target_addr:target_port>`` is the address and port of the target node, ``<message>`` is the message to be sent in the ``PingRequest`` and ``[n_pings]`` is the number of pings to send (default is 1).


The App protocol should look like this:

```java

import pt.unl.fct.di.novasys.babel.core.GenericProtocol;

public class App extends GenericProtocol {
    
    public App() {
        super("App", (short) 2);
    }
    
    public void init(Properties props) {
        // implement the initialization of the protocol
        // need to register handlers for events, timers and messages
    }
    
    private void uponPongReply(PongReply reply, short sourceProto) {
        // what to do when a "PongReply" is received
    }
    
    private void readSystemIn() {
        // read from the console
    }
}

```

## How to compile

To compile your code, run the following commands:
- ``mvn clean package``
- ``docker build  -t babel-tutorial/b-pingpongapp .``

## How to run

### Setup
You need to create a docker network for the tutorial:

``docker network create babel-tutorial-net``

To remove the network:

``docker network rm babel-tutorial-net``

### Args

The applications need to be run with the following arguments:
- ``interface=eth0``


### Run
To run the protocol, you need to run the following commands:

This will run the application in server mode:

``docker run --network babel-tutorial-net --rm -h ping-server --name ping-server -it babel-tutorial/b-pingpongapp interface=eth0``

This will run the protocol in client mode:

``docker run --network babel-tutorial-net --rm -h ping-client --name ping-client -it babel-tutorial/b-pingpongapp interface=eth0``
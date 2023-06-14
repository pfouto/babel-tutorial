# Step B - PingPing Application

## InterProcess Communication Abstractions

### Request-Reply Abstractions

### Notification Abstractions


## Exercise

Modify the PingPong protocol in [Step A](../A-PingPong) to implement the PingPing application.
Create a new App protocol that reads from the console the ping command and performs a request to the ``PingPongProtocol``.

The App protocol send a ``PingRequest`` request to the ``PingPongProtocol`` and waits for a ``PongReply`` reply.
When the reply is received, the App protocol prints the reply to the console.


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

``mvn clean package``

``docker build  -t babel-tutorial/b-pingpongapp .``


``docker network create babel-tutorial-net``
``docker network rm babel-tutorial-net``

## How to run

### Args

``docker run --network babel-tutorial-net --rm -h ping-server --name ping-server -it babel-tutorial/b-pingpongapp interface=eth0``

``docker run --network babel-tutorial-net --rm -h ping-client --name ping-client -it babel-tutorial/b-pingpongapp interface=eth0``
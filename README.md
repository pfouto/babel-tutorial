# Babel Tutorial

This tutorial will guide you through the process of developing protocols and applications using the Babel framework.

[Babel](https://github.com/pfouto/babel-core) is a framework for developing distributed systems. It provides a set of abstractions that allow the developer to focus on the application logic, while the framework handles the underlying networking and concurrency issues.

In Babel, the developer implements protocols by extending a Java abstract class (GenericProtocol) that contains Babel API
used to interact with other protocols in the same process and in different processes.

Protocols in Babel are implemented as event-driven state machines. The developer implements the protocol logic by defining
the actions that are executed when the protocol receives an event. The events are defined by the developer and can be
triggered by the protocol itself, by other protocols in the same process or by other protocols in different processes.

Babel defines 4 types of events that characterize the interaction between protocols:
- **Request**: used to send a direct request to a protocol in the same Babel process. The requested protocol can produce an asynchronous reply to the requester protocol.
- **Notification**: used to notify a protocol/application of an event that occurred in another protocol/application in the same Babel process. A single notification can go to multiple protocols/applications.
- **Timer**: used to notify a protocol that a timer has expired (to handle timeouts or periodic actions).
- **Network**: used to notify a protocol that a message has arrived from a different Babel process.

If you use Babel, please cite the [paper](https://ieeexplore.ieee.org/abstract/document/9996836):

```
P. Fouto, P. Á. Costa, N. Preguiça and J. Leitão,
"Babel: A Framework for Developing Performant and Dependable Distributed Protocols"
2022 41st International Symposium on Reliable Distributed Systems (SRDS), Vienna, Austria, 2022, pp. 146-155,
doi: 10.1109/SRDS55811.2022.00022.
```

In this tutorial, we will guide you through the process of developing a simple PingPong protocol and a simple Chat application
using Babel.



## Table of Contents
- [Step A](A-PingPong) - Simple PingPong Protocol and Networking and Timer abstractions.
- [Step B](B-PingPongApp) - PingPong Application and InterProcess Communication abstractions.
- [Step C](C-Membership) - Simple Membership Protocol.
- [Step D](D-Dissemination) - Dissemination Protocol.
- [Step E](E-Chat) - Chat Application.

## Prerequisites

- Java 8
- Maven
- Docker




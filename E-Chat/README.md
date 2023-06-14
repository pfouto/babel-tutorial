# Step E - Chat Application

## Putting everything together


## How to compile

To compile your code, run the following commands:
- ``mvn clean package``
- ``docker build  -t babel-tutorial/e-chat .``



## How to run

## How to run

### Setup
You need to create a docker network for the tutorial:
``docker network create babel-tutorial-net``

To remove the network:
``docker network rm babel-tutorial-net``

### Args

### Run

``docker run --network babel-tutorial-net --rm -h node-1 --name node-1 -it babel-tutorial/e-chat interface=eth0``

``docker run --network babel-tutorial-net --rm -h node-2 --name node-2 -it babel-tutorial/e-chat interface=eth0``
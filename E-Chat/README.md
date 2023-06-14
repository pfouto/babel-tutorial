# Step E - Chat Application

## Putting everything together


## How to compile

``mvn clean package``

``docker build  -t babel-tutorial/e-chat .``


``docker network create babel-tutorial-net``
``docker network rm babel-tutorial-net``

## How to run

### Args

``docker run --network babel-tutorial-net --rm -h node-1 --name node-1 -it babel-tutorial/e-chat interface=eth0``

``docker run --network babel-tutorial-net --rm -h node-2 --name node-2 -it babel-tutorial/e-chat interface=eth0``
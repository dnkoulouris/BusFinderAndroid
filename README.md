# Bus Finder 🚌
*Distributed Systems University Project*

# Using Pub/Sub Model
## Broker class
* Give a number id for each Broker. Broker 1 is the one that every other broker will connect to in order to have a common point of reference. **IMPORANT** After all brokers are up and running there is no difference between Broker 1 and Broker x, in other words Broker 1 is not a master Broker.
* After every Broker have connected to Broker 1 the user has to inform any of the Brokers that there are no more Brokers to add and so the topic sharing process can begin.
* Now all Brokers are waiting for the Publisher’s and Subscriber's input.

### Topic Sharing
* Each Broker gets the topics whose hashes (MD5) are less than the hash of its IP + port.
* Topics are *LineId* from the file *ΒusLines.txt*

## Publisher class
* For testing purposes each publisher needs to know the total number of publishers, say n, in order to divide the file *ΒusLines.txt* and bind the 1/n of topics.
* In addition before the publisher can start sending topics to the respective Broker, need to know (prompts the user) the IP and port of any Broker in our network.
* Once all the information have been provided the publisher will begin sending topics to the respective Broker every 0.2 seconds, reading from the file *BusPositions.txt*.


## Subscriber class
* Same as the publisher before it begins it need to know the IP and the port of any of the Brokers.
* Once the information is provider it prompts the user for the line they want to get information about. It then contacts the Broker provided above. Should this Broker not have the information the user needs it will then return a list of all other Broker and the Subscriber will have to ask all of them until it finds the one, which is responsible for the topic.
* The Broker registers this Subscriber to its list and every time it finds that the information the Publisher give it is about a topic that registered Subscribers have asked about, it send it to the respective Subscribers (even if they are more than one subscribed at the same topic).
* Since this is a command line, based application there is currently no way to change the topic a Subscriber asks for.

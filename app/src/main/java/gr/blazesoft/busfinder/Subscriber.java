package gr.blazesoft.busfinder;


import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;



public class Subscriber implements  Serializable {
    public String subscriberID, topic;
    public ObjectOutputStream brokerOut;
    public volatile Broker currentBroker;
}

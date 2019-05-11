package gr.blazesoft.busfinder;

import java.io.ObjectOutputStream;

public class Subscriber extends Node
{
    public String subscriberID, topic;
    public ObjectOutputStream brokerOut;
    public volatile Broker currentBroker;
}

package gr.blazesoft.busfinder;

import java.io.Serializable;
import java.util.ArrayList;

public class Node extends Thread implements Serializable
{
    public volatile static ArrayList<Broker> brokers;

    public Node()
    {
        this.brokers = new ArrayList<>();
    }

    public Node(boolean init)
    {

    }
}

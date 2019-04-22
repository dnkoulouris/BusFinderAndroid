package gr.blazesoft.busfinder;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Scanner;

public class Broker extends Node
{
    public String IP;
    public int ipHash, id, port;
    public volatile static ArrayList<Subscriber> registeredSubs;
    public ArrayList<Publisher> registeredPubs;
    public volatile ArrayList<BusLine> myTopics;

    private boolean gotTopics = false;

    //Normal Constructor
    public Broker(String IP, int port, String id)
    {
        ipHash = Utils.getMd5(IP + port);
        this.IP = IP;
        this.port = port;
        this.id =Integer.parseInt(id);
        registeredSubs = new ArrayList<>();
        myTopics = new ArrayList<>();

        Scanner in = new Scanner(System.in);
        brokers.add(this);
        new Broker(this).start();

        if (this.id == 1)
        {
            System.out.println("This Broker 1.");
            System.out.println("Use this ip to help other brokers connect: " + IP);
        }
        else
        {
            System.out.print("Enter Broker 1 IP\n> ");
            String broker1_ip = in.nextLine();
            Utils.sendPacket(this, broker1_ip, this.port, "add_me");
        }
        String ans = "n";
        do
        {
            if(!gotTopics)
            {
                System.out.println("When all Brokers are set press (y) to start sharing topics");
                ans = in.nextLine();
            }
        }while(!ans.toLowerCase().equals("y"));
        gotTopics = false;
        if(ans.toLowerCase().equals("y"))
        {
            brokers = Utils.getTopicList(brokers);
            System.out.println("Topics Calculated. Sending...");
            for (Broker bl : brokers)
            {
                Utils.sendPacket(bl.myTopics, bl.IP, bl.port, "add_topics_list");
                Utils.sendPacket(brokers, bl.IP, bl.port, "add_list");
            }
        }
    }

    //Copy Constructor
    public Broker(Broker b)
    {
        super(true);
        this.ipHash = b.ipHash;
        this.IP = b.IP;
        this.port = b.port;
        this.id = b.id;
        this.registeredPubs = b.registeredPubs;
    }

    public BusLine containsTopic(String topic)
    {
        if (myTopics.size() == 0) return null;
        for (BusLine top : myTopics) {
            if (top.lineID.equals(topic)) return top;
        }
        return null;
    }

    public boolean containsLineCode(String code)
    {
        if (myTopics.size() == 0) return false;
        for (BusLine top : myTopics) {
            if (top.lineCode.equals(code)) return true;
        }
        return false;
    }

    public boolean containsSubId(String subID) {
        for (Subscriber s : registeredSubs) {
            if (s.subscriberID.equals(subID)){
                return true;
            }
        }
        return false;
    }

    public static void main(String[] args)
    {
        new Broker(Utils.getSystemIP(), 8080, args[0]);
    }

    //Parallel Server
    public void run()
    {
        ServerSocket providerSocket = null;
        try
        {
            providerSocket = new ServerSocket(port);
            while (true)
            {
                Socket requestSocket = providerSocket.accept();
                ObjectInputStream in = new ObjectInputStream(requestSocket.getInputStream());
                ObjectOutputStream out = new ObjectOutputStream(requestSocket.getOutputStream());

                String sendtext = in.readUTF();
                if (sendtext.equals("add_me"))
                {

                    Broker b2 = (Broker) in.readObject();
                    System.out.println("Broker " + b2.id + " with IP " + b2.IP + " is trying to connect.");
                    if(!contains_broker(b2))
                    {
                        int i;
                        for(i = 0; i < brokers.size(); i++)
                        {
                            if(b2.ipHash < brokers.get(i).ipHash)
                            {
                                brokers.add(i, b2);
                                break;
                            }
                            else if(i == brokers.size()-1)
                            {
                                brokers.add(b2);
                                break;
                            }
                        }
                        System.out.println("It was placed at position " + i + " (Hash: " + b2.ipHash + ")");
                        System.out.println("Our Broker's IP list: ");
                        for(Broker test: brokers)
                        {
                            System.out.println(test.IP);
                        }
                        System.out.println("Updating other Broker's list");
                        for (Broker b : brokers)
                        {
                            if (b.IP != this.IP)
                            {
                                Utils.sendPacket(brokers, b.IP, b.port, "add_list");
                            }
                        }
                    }
                    else {
                        System.err.println("Broker already exists");
                    }
                }
                else if (sendtext.equals("add_list"))
                {
                    System.out.println("Got a request to update my Broker list");
                    ArrayList<Broker> bl = (ArrayList<Broker>) in.readObject();
                    brokers = bl;
                    System.out.println("My new Broker list is:");
                    for (Broker b : brokers)
                    {
                        System.out.println(b.IP);
                    }
                }
                else if (sendtext.equals("i_want_bus"))
                {
                    Subscriber s = (Subscriber) in.readObject();
                    BusLine top = containsTopic(s.topic);

                    if (top != null)
                    {
                        if(registeredSubs.isEmpty() || !containsSubId(s.subscriberID))
                        {
                            s.brokerOut = out;
                            registeredSubs.add(s);
                            System.out.println("Subscriber " + s.subscriberID + " registered");
                        }
                        out.reset();
                        out.writeUTF("bus_is_here");
                        out.flush();



                        out.reset();
                        out.writeUnshared(this);
                        out.flush();

                        push(top);
                    }
                    else
                    {
                        out.reset();
                        out.writeUTF("bus_not_here");
                        out.flush();

                        out.reset();
                        out.writeUnshared(brokers);
                        out.flush();
                    }
                }
                else if(sendtext.equals("add_topics_list"))
                {
                    System.out.println("Got Topics\nMy new topics are:");
                    gotTopics = true;
                    myTopics = (ArrayList<BusLine>) in.readObject();
                    for(BusLine s : myTopics)
                    {
                        System.out.println(s.lineID);
                    }
                }
                else if(sendtext.equals("give_me_brokers_list"))
                {
                    out.reset();
                    out.writeUnshared(brokers);
                    out.flush();
                    out.flush();
                }
                else if(sendtext.equals("update_times"))
                {
                    Bus leoforeio = (Bus) in.readObject();
                    BusLine top = null;
                    for (BusLine busL : myTopics)
                    {
                        if (busL.lineCode.equals(leoforeio.LineCode))
                        {
                            top = busL;
                            busL.updateBus(leoforeio);
                        }
                    }
                    push(top);
                }
                else if(sendtext.equals("broker_down"))
                {
                    Broker b = (Broker) in.readObject();
                    System.out.println("Publisher " + requestSocket.getLocalSocketAddress() + " just informed me that Broker " + b.id + " is down");

                    for (int i = 0; i < brokers.size(); i++)
                    {
                        if (brokers.get(i).id == b.id)
                        {
                            System.out.println("I didn't know that! Removing..");
                            brokers.remove(i);

                            System.out.println("Recalculating topics and updating the other Brokers...");
                            brokers = Utils.getTopicList(brokers);
                            for(Broker br: brokers)
                            {
                                if(!br.IP.equals(IP))
                                {
                                    Utils.sendPacket(brokers, br.IP, br.port, "add_list");
                                    Utils.sendPacket(br.myTopics, br.IP, br.port, "add_topics_list");
                                }
                                else
                                {
                                    System.out.println("Updating my self");
                                    myTopics = br.myTopics;
                                    for(BusLine bsl: myTopics)
                                    {
                                        System.out.println(bsl.lineID);
                                    }
                                }
                            }
                            break;
                        }
                    }
                    out.reset();
                    out.writeUnshared(brokers);
                    out.flush();
                }
                else if(sendtext.equals("remove_me"))
                {
                    Subscriber s = (Subscriber) in.readObject();
                    for (int i=0; i<registeredSubs.size(); i++) {
                        if (s.subscriberID.equals(registeredSubs.get(i).subscriberID)) {
                            registeredSubs.get(i).brokerOut.writeUnshared("proceed");
                            try {

                                registeredSubs.get(i).brokerOut.close();
                            } catch (IOException ioException){
                                ioException.printStackTrace();
                            }
                            registeredSubs.remove(i);
                        }
                    }
                }
            }

        } catch (IOException ioException) {
            ioException.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } finally {
            try {
                providerSocket.close();
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        }
    }

    public void removeFromRegistered(Subscriber s) {

        for (int i=0; i<registeredSubs.size(); i++) {
            if (registeredSubs.get(i).subscriberID.equals(s.subscriberID)) {
                try {

                    registeredSubs.get(i).brokerOut.close();
                } catch (IOException ioException){
                    System.out.println("Subscriber " + registeredSubs.get(i).subscriberID + " connection closed");
                }
                registeredSubs.remove(i);
                System.out.println("Subscriber " + s.subscriberID + " unregistered");
            }
        }

    }

    public void push(BusLine top)
    {
        for(Subscriber s: registeredSubs)
        {
            if (top.lineID.equals(s.topic)) {
                try {


                    s.brokerOut.reset();
                    s.brokerOut.writeUnshared(top.runningBuses);
                    s.brokerOut.flush();
                    System.out.println("Sent " + top.lineID + " to Subcriber " + s.subscriberID);
                } catch (IOException e) {
                    //e.printStackTrace();

                    removeFromRegistered(s);
                    return;
                }
            }
        }
    }


    public boolean contains_broker(Broker bin)
    {
        for(Broker b:brokers)
        {
            if(b.IP.equals(bin.IP))
            {
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return "Broker{" +
                "IP='" + IP + '\'' +
                ", id=" + id +
                ", port=" + port +
                ", ipHash=" + ipHash +
                '}';
    }
}

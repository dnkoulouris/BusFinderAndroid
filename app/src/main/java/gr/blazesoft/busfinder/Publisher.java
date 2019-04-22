package gr.blazesoft.busfinder;


import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Publisher extends Node
{
    int id, broker1_port;
    private List<BusLine> lines;
    String broker1_ip;
    Scanner in = new Scanner(System.in);


    public Publisher(int id)
    {
        this.id = id;

        System.out.print("Enter a Broker's IP\n>");
        broker1_ip = in.nextLine();
        System.out.print("Enter Broker port\n>");
        broker1_port = in.nextInt();
        in.nextLine();

        brokers = getBrokersList(broker1_ip,broker1_port);
        System.out.println("Got Broker list");

        sendLines();
        sendTimes();
    }

    public static void main(String[] args)
    {
        new Publisher(Integer.parseInt(args[0]));
    }

    public void sendLines()
    {
        ArrayList<BusLine> busLines = Utils.readLinesList("busLinesNew.txt", brokers);

        System.out.print("Enter number of publishers\n> ");
        int c = in.nextInt();
        in.nextLine();

        int step = busLines.size() / c;
        int idNorm = id - 1;
        if (idNorm == c - 1)
        {
            lines = busLines.subList(idNorm * step, busLines.size());
        }
        else
        {
            lines = busLines.subList(idNorm * step, (idNorm + 1) * step);
        }
        System.out.println("Just calculated my topic list:");
        for(BusLine l: lines)
        {
            System.out.println(l.lineID);
        }
    }

    public Broker hashTopic(String busLID)
    {
        if (brokers.size() ==0) return null;
        for (BusLine busL: lines)
        {
            if (busLID.equals(busL.lineCode))
            {
                for (Broker b : brokers)
                {
                    if (b.containsLineCode(busLID))
                    {
                        return b;
                    }
                }
            }
        }

        return null;
    }

    public void push(Bus leoforeio, Broker b)
    {
        Utils.sendPacket(leoforeio, b.IP, b.port, "update_times");
    }

    public void sendTimes() {
        try (BufferedReader br = new BufferedReader(new FileReader("busPositionsNew.txt"))){
            String line;
            System.out.println("Start sending topics to respective Brokers...");
            while ((line = br.readLine()) != null)
            {
                String[] values = line.split(",");
                Broker b = hashTopic(values[0]);
                try
                {
                    if (b != null)
                    {
                        System.out.println(b.IP);
                        Bus tempLine = new Bus(values[0], values[1], values[2], Double.parseDouble(values[3]), Double.parseDouble(values[4]), values[5]);
                        System.out.println(tempLine.toString()+ " sent to " +b.toString());
                        push(tempLine, b);
                        sleep(200);
                    }
                }catch (NullPointerException ex)
                {
                    System.out.println("CANNOT CONNECT TO BROKER " + b.id + "\nRemoving it from my list..");
                    for (int i = 0; i < brokers.size(); i++)
                    {
                        if (brokers.get(i).id == b.id)
                        {
                            brokers.remove(i);
                            break;
                        }
                    }
                    System.out.println("Informing broker " + brokers.get(0).id + " that this broker is down and requesting an updated version of Broker list...");
                    brokers = (ArrayList<Broker>) Utils.sendPacketWithAnswer(b, brokers.get(0).IP, brokers.get(0).port, "broker_down");
                }
            }
        }catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public ArrayList<Broker> getBrokersList(String ip, int port)
    {
        Socket requestSocket = null;
        ObjectOutputStream out = null;
        ObjectInputStream in = null;
        try
        {
            requestSocket = new Socket(InetAddress.getByName(ip), port);
            out = new ObjectOutputStream(requestSocket.getOutputStream());
            in = new ObjectInputStream(requestSocket.getInputStream());

            out.writeUTF("give_me_brokers_list");
            out.flush();

            System.out.println("Connection with Broker established");

            return (ArrayList<Broker>) in.readObject();

        } catch (UnknownHostException unknownHost) {
            System.err.println("You are trying to connect to an unknown host!");
        } catch (IOException ioException) {
            ioException.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } finally {
            try {
                in.close();
                out.close();
                requestSocket.close();
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        }
        return null;
    }

}
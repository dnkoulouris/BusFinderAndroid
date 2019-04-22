package gr.blazesoft.busfinder;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;


import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;


import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;


public class MainActivity extends FragmentActivity implements OnMapReadyCallback, View.OnClickListener, Serializable {



    private volatile GoogleMap mMap;
    private Button mButton;
    private EditText TextBoxBusline;

    private String brokerIP;

    public volatile Broker currentBroker;
    public static volatile boolean stopDownload = false;



    private static Socket requestSocket = null;
    private static ObjectOutputStream out = null;
    private static ObjectInputStream in = null;



    private static Subscriber sub;
    private static Client c = null;



    //first startup of app
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        //subscriber seperated from executable
        sub = new Subscriber();


        setContentView(R.layout.activity_maps);


        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);


        mButton = findViewById(R.id.button);
        mButton.setOnClickListener(this);

        TextBoxBusline = findViewById(R.id.busLineID);

        //popup to enter broker IP
        {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Enter Broker IP");

            final EditText input = new EditText(this);
            input.setInputType(InputType.TYPE_CLASS_TEXT);
            builder.setView(input);
            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    brokerIP = input.getText().toString();
                }
            });
            builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                }
            });
            builder.show();
        }


    }



    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;


    }

    public void disconnect (String ip, int port)
    {
        Utils.sendPacket(this,ip,port,"remove_me");
    }




    //button click event handler
    @Override
    public void onClick(View view)
    {


        //add the ID1 to this sub
        //TODO make this work for more subs

            sub.subscriberID = "1";
            sub.brokerOut = out;
            sub.topic = TextBoxBusline.getText().toString();


            //create the client (subscriber) thread
            c = new Client(sub);

            //run the thread
            c.execute();


    }





    //client socket thread
    class Client extends AsyncTask<Void,Void,Void> implements Serializable
    {

        Subscriber s;

        public Client(Subscriber s) {
            this.s = s;
        }

        @Override
        protected Void doInBackground(Void... voids) {

            register(brokerIP,8080,TextBoxBusline.getText().toString());

            return null;
        }

        public void visualizeData(final ArrayList<Bus> ret, final boolean first)
        {
            Handler handler = new Handler(Looper.getMainLooper());
            handler.post(new Runnable(){
                             @Override
                             public void run() {
                                 mMap.clear();
                                 LatLng bus = null;
                                 for (Bus b : ret)
                                 {
                                     bus = new LatLng(b.lat,b.lon);
                                     mMap.addMarker(new MarkerOptions().position(bus).title(b.LineCode + " " + b.vehicleId));
                                 }


                                 //zoom
                                 if(first) {

                                     mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(bus, 15));
                                     // Zoom in, animating the camera.
                                     mMap.animateCamera(CameraUpdateFactory.zoomIn());
                                     // Zoom out to zoom level 10, animating with a duration of 2 seconds.
                                     mMap.animateCamera(CameraUpdateFactory.zoomTo(15), 2000, null);

                                 }


                             }
                             // your UI code here
                         });


        }


        //register as before
        private void register(String ip, int port, String topic) {
            try
            {

                requestSocket = new Socket(ip,port);
                out = new ObjectOutputStream(requestSocket.getOutputStream());
                in = new ObjectInputStream(requestSocket.getInputStream());

                out.writeUTF("i_want_bus");
                out.flush();
                out.reset();
                out.writeUnshared(this.s);
                out.flush();

                String returned = in.readUTF();
                if (returned.equals("bus_is_here"))
                {
                    currentBroker = (Broker) in.readObject();
                    ArrayList<Bus> ret = (ArrayList<Bus>) in.readObject();
                    visualizeData(ret, true);


                    while (!stopDownload) {
                        ret = (ArrayList<Bus>) in.readObject();
                        visualizeData(ret, false);
                        if(isCancelled())
                            break;
                    }

                }
                else if (returned.equals("bus_not_here"))
                {
                    System.out.println("Line not found in broker 1");
                    ArrayList<Broker> brokers = (ArrayList<Broker>) in.readObject();

                    for(Broker b: brokers)
                    {
                        if( b.containsTopic(topic) != null )
                        {
                            System.out.println(b.IP + " has your data.");
                            register(b.IP, b.port,topic);
                        }
                    }
                }
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
        }

    }
}

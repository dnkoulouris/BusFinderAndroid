package gr.blazesoft.busfinder;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.FragmentActivity;
import android.view.View;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;

public class MainActivity extends FragmentActivity implements OnMapReadyCallback, View.OnClickListener, Serializable
{
    private GoogleMap mMap;

    private Socket requestSocket = null;
    private ObjectOutputStream out = null;
    private ObjectInputStream in = null;

    private Subscriber sub;
    private String brokerIP;
    private Client c;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        sub = new Subscriber();
        sub.topic = BusSelector.topic;
        sub.subscriberID = BusSelector.id;
        brokerIP = BusSelector.brokerIP;

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        findViewById(R.id.button).setOnClickListener(this);
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        c = new Client(sub);
        c.execute();
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
    public void onMapReady(GoogleMap googleMap)
    {
        mMap = googleMap;
    }

    @Override
    public void onClick(View view)
    {
        c.cancel(true);
        new Remover(sub).execute();

        Intent intent = new Intent(this, BusSelector.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        startActivityIfNeeded(intent, 0);
    }

    //client socket thread
    class Client extends AsyncTask<Void, Void, Void> implements Serializable
    {
        private Subscriber s;

        public Client(Subscriber s)
        {
            this.s = s;
        }

        @Override
        protected Void doInBackground(Void... voids)
        {
            register(brokerIP, 49000, s.topic);
            return null;
        }

        private void register(String ip, int port, String topic)
        {
            try
            {
                requestSocket = new Socket(ip, port);
                out = new ObjectOutputStream(requestSocket.getOutputStream());
                in = new ObjectInputStream(requestSocket.getInputStream());

                out.reset();
                out.writeUTF("i_want_bus");
                out.flush();

                out.reset();
                out.writeUnshared(sub);
                out.flush();

                String returned = in.readUTF();
                if (returned.equals("bus_is_here"))
                {
                    sub.currentBroker = (Broker) in.readObject();
                    ArrayList<Bus> ret = (ArrayList<Bus>) in.readObject();
                    visualizeData(ret, true);

                    while (!isCancelled())
                    {
                        ret = (ArrayList<Bus>) in.readObject();
                        visualizeData(ret, false);
                    }
                } else if (returned.equals("bus_not_here"))
                {
                    ArrayList<Broker> brokers = (ArrayList<Broker>) in.readObject();

                    for (Broker b : brokers)
                    {
                        if (b.containsTopic(topic) != null)
                        {
                            System.out.println(b.IP + " has your data.");
                            register(b.IP, b.port, topic);
                        }
                    }
                }
            } catch (UnknownHostException unknownHost)
            {
                System.err.println("You are trying to connect to an unknown host!");
            } catch (Exception ioException)
            {
                ioException.printStackTrace();
            } finally
            {
                try
                {
                    in.close();
                    out.close();
                    requestSocket.close();
                } catch (IOException ioException)
                {
                    ioException.printStackTrace();
                }
            }
        }

        public void visualizeData(final ArrayList<Bus> ret, final boolean first)
        {
            Handler handler = new Handler(Looper.getMainLooper());
            handler.post(new Runnable()
            {
                @Override
                public void run()
                {
                    mMap.clear();
                    LatLng bus = null;
                    for (Bus b : ret)
                    {
                        bus = new LatLng(b.lat, b.lon);
                        mMap.addMarker(new MarkerOptions().position(bus).title(b.LineCode + " " + b.vehicleId).icon(BitmapDescriptorFactory.fromResource(R.drawable.icon)));
                    }
                    //zoom
                    if (first)
                    {
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
    }

    class Remover extends AsyncTask<Void, Void, Void>
    {
        Subscriber s;

        public Remover(Subscriber s)
        {
            this.s = s;
        }

        @Override
        protected Void doInBackground(Void... voids)
        {
            Utils.sendPacket(s, s.currentBroker.IP, 49000, "remove_me");
            return null;
        }
    }
}

package gr.blazesoft.busfinder;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.InputType;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;

public class BusSelector extends AppCompatActivity implements ListView.OnItemClickListener
{
    public static String brokerIP, topic, id;

    private ListView mList;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bus_selector);

        mList = findViewById(R.id.busList);
        mList.setOnItemClickListener(this);

        //popup to enter broker IP
        {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Enter Broker IP");

            final EditText input = new EditText(this);
            input.setInputType(InputType.TYPE_CLASS_TEXT);
            builder.setView(input);
            builder.setPositiveButton("OK", new DialogInterface.OnClickListener()
            {
                @Override
                public void onClick(DialogInterface dialog, int which)
                {
                    brokerIP = input.getText().toString();
                }
            });
            builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener()
            {
                @Override
                public void onClick(DialogInterface dialog, int which)
                {
                    dialog.cancel();
                }
            });
            builder.show();
        }
        //popup to enter sub ID
        {
            AlertDialog.Builder builder2 = new AlertDialog.Builder(this);
            builder2.setTitle("Enter Subscriber ID");

            final EditText subID = new EditText(this);
            subID.setInputType(InputType.TYPE_CLASS_TEXT);
            builder2.setView(subID);
            builder2.setPositiveButton("OK", new DialogInterface.OnClickListener()
            {
                @Override
                public void onClick(DialogInterface dialog, int which)
                {
                    id = subID.getText().toString();
                }
            });
            builder2.setNegativeButton("Cancel", new DialogInterface.OnClickListener()
            {
                @Override
                public void onClick(DialogInterface dialog, int which)
                {
                    dialog.cancel();
                }
            });
            builder2.show();
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id)
    {
        topic = mList.getItemAtPosition(position).toString();

        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }
}

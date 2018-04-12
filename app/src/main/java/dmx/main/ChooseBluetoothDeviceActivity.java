package dmx.main;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

public class ChooseBluetoothDeviceActivity extends AppCompatActivity {

    private BluetoothAdapter BA;

    //android gui component which shows list of all bluetooth paired devices
    private ListView devices_list_view;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_choose_bluetooth_device);

        devices_list_view = (ListView) findViewById(R.id.bluetooth_devices_listview);

        //add usage to update_devices_button
        Button update_button = (Button) findViewById(R.id.update_devices_button);
        update_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                updatePairedDevicesList();
            }
        });

        Button test_button = (Button) findViewById(R.id.test_button);
        test_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(ChooseBluetoothDeviceActivity.this, DMXConsoleActivity.class);
                intent.putExtra("DEVICE NAME", "IGNORE");
                startActivity(intent);
            }
        });

        //get bluetooth device instance
        BA = BluetoothAdapter.getDefaultAdapter();

        //first enable bluetooth if there is need for it
        if (!BA.isEnabled()) {
            Intent turn_on_intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(turn_on_intent, 0);

            Toast.makeText(getApplicationContext(), "Activating bluetooth...", Toast.LENGTH_SHORT).show();
        }

        //get all paired devices
        updatePairedDevicesList();

        //make listener for item list touching event
        devices_list_view.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                TextView device_name = (TextView) view.findViewById(R.id.device_name);

                Toast.makeText(getApplicationContext(), "Selected device: " + device_name.getText(), Toast.LENGTH_LONG).show();

                Intent intent = new Intent(ChooseBluetoothDeviceActivity.this, DMXConsoleActivity.class);
                intent.putExtra("DEVICE NAME", device_name.getText());
                startActivity(intent);
            }
        });
    }

    /**
     * Update list of paired devices
     */
    private void updatePairedDevicesList() {
        ArrayList<String> devices_names = new ArrayList<String>();

        for(BluetoothDevice device : BA.getBondedDevices()) {
            devices_names.add(device.getName());
        }

        ArrayAdapter adapter = new ArrayAdapter(ChooseBluetoothDeviceActivity.this, R.layout.bluetooth_device_list_item, devices_names);
        devices_list_view.setAdapter(adapter);
    }
}

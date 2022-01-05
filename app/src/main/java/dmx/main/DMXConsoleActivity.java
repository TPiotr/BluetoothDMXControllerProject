package dmx.main;

import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.NumberPicker;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.UUID;

import yuku.ambilwarna.AmbilWarnaDialog;

public class DMXConsoleActivity extends AppCompatActivity {

    public static final int MAX_VALUE_PER_CHANNEL = 255;
    public static final byte CHECK_BYTE_VALUE = 99;

    //bluetooth communication variables
    private BluetoothAdapter bluetooth_adapter;
    private BluetoothSocket socket;
    private BluetoothDevice device;

    private OutputStream output_stream;
    private InputStream input_stream;

    //preferences where saved channel values are stored
    private SharedPreferences preferences;

    //hash map that contains all sliders
    private HashMap<SeekBar, Integer> seek_bars;

    //color picker variables
    private int r_channel = 1, g_channel = 1, b_channel = 1;
    private int last_selected_color;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.console_layout);

        //grab preferences instance from system
        preferences = PreferenceManager.getDefaultSharedPreferences(this);

        //grab selected bluetooth device
        bluetooth_adapter = BluetoothAdapter.getDefaultAdapter();

        //get selected device name
        Intent intent = getIntent();
        String device_name = intent.getStringExtra("DEVICE NAME");

        if(!device_name.equals("IGNORE")) {
            //get instance of that device
            for (BluetoothDevice d : bluetooth_adapter.getBondedDevices()) {
                if (d.getName().equals(device_name)) {
                    device = d;

                    //print out device we are trying to connect name
                    Toast.makeText(getApplicationContext(), "Selected device: " + device.getName(), Toast.LENGTH_SHORT).show();
                    break;
                }
            }

            //when we have bluetooth device open connection with it
            try {
                openBT();
            } catch (IOException e) {
                Toast.makeText(getApplicationContext(), "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                e.printStackTrace();

                //return back to paired devices selection if some error happen
                Intent i = new Intent(this, ChooseBluetoothDeviceActivity.class);
                startActivity(i);
            }
        }

        //setup gui
        last_selected_color = Color.BLUE;

        seek_bars = new HashMap<SeekBar, Integer>();

        seek_bars.put((SeekBar) findViewById(R.id.channel1), 1);
        seek_bars.put((SeekBar) findViewById(R.id.channel2), 2);
        seek_bars.put((SeekBar) findViewById(R.id.channel3), 3);
        seek_bars.put((SeekBar) findViewById(R.id.channel4), 4);
        seek_bars.put((SeekBar) findViewById(R.id.channel5), 5);
        seek_bars.put((SeekBar) findViewById(R.id.channel6), 6);
        seek_bars.put((SeekBar) findViewById(R.id.channel7), 7);
        seek_bars.put((SeekBar) findViewById(R.id.channel8), 8);

        final TextView current_channel_value_text_view = (TextView) findViewById(R.id.current_chanel_value_textview);
        current_channel_value_text_view.setText("");

        //make listener for all seekbars
        SeekBar.OnSeekBarChangeListener listener = new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                float val = (float) i / (float) seekBar.getMax();

                int channel = seek_bars.get(seekBar);
                int dmx_value = (int) (val * DMXConsoleActivity.MAX_VALUE_PER_CHANNEL);

                writeDMXValue(channel, dmx_value);

                current_channel_value_text_view.setText("CH: " + channel + "\nVAL: " + dmx_value);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        };

        //assign listener to all seek bars
        for(SeekBar bar : seek_bars.keySet())
            bar.setOnSeekBarChangeListener(listener);


        //save, load, reset, color part
        Button save_button = (Button) findViewById(R.id.save_button);
        Button load_button = (Button) findViewById(R.id.load_button);
        Button reset_button = (Button) findViewById(R.id.reset_button);
        Button color_button = (Button) findViewById(R.id.color_button);

        save_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //save all current values to preferences
                int i = 0;
                SharedPreferences.Editor editor = getPreferences().edit();
                for(SeekBar bar : seek_bars.keySet()) {
                    editor.putInt("channel" + (i++), bar.getProgress());
                }
                editor.apply();
            }
        });

        load_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //load saved values from preferences
                int i = 0;
                for(SeekBar bar : seek_bars.keySet()) {
                    int value = getPreferences().getInt("channel" + (i++), bar.getMax() / 2);
                    bar.setProgress(value);
                }
            }
        });

        reset_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                for(SeekBar bar : seek_bars.keySet()) {
                    bar.setProgress(0);
                }
            }
        });

        color_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showColorSelectionDialogs();
            }
        });
    }

    /**
     * Show dialog for selecting first channels (R, G, B channel) then color picker and finally send new color to dmx controller
     */
    private void showColorSelectionDialogs() {
        final Dialog d = new Dialog(DMXConsoleActivity.this);
        d.setTitle("Color channels");
        d.setContentView(R.layout.color_channels_dialog_layout);

        Button ok_button = (Button) d.findViewById(R.id.color_dialog_ok_button);
        Button cancel_button = (Button) d.findViewById(R.id.color_dialog_cancel_button);

        final NumberPicker r_picker = d.findViewById(R.id.r_channel);
        r_picker.setMaxValue(100);
        r_picker.setMinValue(1);
        r_picker.setValue(r_channel);
        r_picker.setWrapSelectorWheel(false);

        final NumberPicker g_picker = d.findViewById(R.id.g_channel);
        g_picker.setMaxValue(100);
        g_picker.setMinValue(1);
        g_picker.setValue(g_channel);
        g_picker.setWrapSelectorWheel(false);

        final NumberPicker b_picker = d.findViewById(R.id.b_channel);
        b_picker.setMaxValue(100);
        b_picker.setMinValue(1);
        b_picker.setValue(b_channel);
        b_picker.setWrapSelectorWheel(false);

        ok_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                r_channel = r_picker.getValue();
                g_channel = g_picker.getValue();
                b_channel = b_picker.getValue();

                d.dismiss();

                //show color picker dialog after selecting channels
                AmbilWarnaDialog dialog = new AmbilWarnaDialog(DMXConsoleActivity.this, last_selected_color, new AmbilWarnaDialog.OnAmbilWarnaListener() {
                    @Override
                    public void onCancel(AmbilWarnaDialog dialog) {}

                    @Override
                    public void onOk(AmbilWarnaDialog dialog, int color) {
                        last_selected_color = color;

                        int red = Color.red(color);
                        int green = Color.green(color);
                        int blue = Color.blue(color);

                        SeekBar red_seekbar = getSeekBarByChannel(r_channel);
                        red_seekbar.setProgress(getSeekBarValueFromChannelValue(red_seekbar, red));

                        SeekBar green_seekbar = getSeekBarByChannel(g_channel);
                        green_seekbar.setProgress(getSeekBarValueFromChannelValue(green_seekbar, green));

                        SeekBar blue_seekbar = getSeekBarByChannel(b_channel);
                        blue_seekbar.setProgress(getSeekBarValueFromChannelValue(blue_seekbar, blue));

                        System.out.println("R: " + red + " G: " + green + " B: " + blue);
                    }
                });
                dialog.show();
            }
        });

        cancel_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                d.dismiss();
            }
        });

        d.show();
    }

    /**
     * Get seek bar instance by channel id
     * @param channel channel
     * @return seek bar instance
     */
    public SeekBar getSeekBarByChannel(int channel) {
        for(SeekBar bar : seek_bars.keySet()) {
            int val = seek_bars.get(bar);

            if(val == channel)
                return bar;
        }
        return null;
    }

    /**
     * Transform 0 - 255 channel value to 0 - bar.getMax(), so it can be properly applied onto seek bars
     * @param bar bar from which MIN, MAX will be taken
     * @param channel_value channel value (so value in range 0  - MAX_VALUE_PER_CHANNEL)
     * @return value in range between 0 - bar.getMax()
     */
    public int getSeekBarValueFromChannelValue(SeekBar bar, int channel_value) {
        float ch_val_01_range = (float) channel_value / (float) MAX_VALUE_PER_CHANNEL;
        return (int) (ch_val_01_range * bar.getMax());
    }

    /**
     * Close bluetooth communication when app is being killed
     */
    @Override
    protected void onDestroy() {
        //close bluetooth connection because we are leaving from this activity/app
        try {
            socket.close();
        } catch (IOException e) {
            Toast.makeText(getApplicationContext(), "Closing app error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }

        super.onDestroy();
    }

    /**
     * Try to open communication between phone and arduino
     * @throws IOException
     */
    private void openBT() throws IOException {
        UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); //Standard SerialPortService ID from android documentation
        socket = device.createInsecureRfcommSocketToServiceRecord(uuid); //use insecure option because our bluetooth device don't support encrypted connection
        socket.connect();

        //grab streams instances (they are used to send bytes over bluetooth connection)
        output_stream = socket.getOutputStream();
        input_stream = socket.getInputStream();

        Toast.makeText(getApplicationContext(), "Connection opened", Toast.LENGTH_SHORT).show();
    }

    /**
     * Send information about channel value (so arduino will parse it and properly handle it)
     * @param channel channel
     * @param value value of channel
     */
    public void writeDMXValue(int channel, int value) {
        int value_to_send = (channel * (MAX_VALUE_PER_CHANNEL + 1)) + value;

        System.out.println("Sending value: " + value_to_send + " (ch: " + channel + " val: " + value + ")");

        if(output_stream == null) {
            Toast.makeText(getApplicationContext(), "Failed to send data because stream is null!", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            //send checkbyte first
            output_stream.write(CHECK_BYTE_VALUE);

            //now just send value stored in 2 bytes (so just as short because its length is bytes)
            output_stream.write(ByteBuffer.allocate(Short.SIZE / 12).putShort((short) value_to_send).array());

        } catch (IOException e) {
            Toast.makeText(getApplicationContext(), "Error while sending: " + e.getMessage(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }

    /**
     * Get preferences instance
     * @return preferences instance
     */
    public SharedPreferences getPreferences() {
        return preferences;
    }
}

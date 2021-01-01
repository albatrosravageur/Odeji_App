package com.mcuhq.simplebluetooth;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private final String TAG = MainActivity.class.getSimpleName();

    private static final UUID BT_MODULE_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); // "random" unique identifier

    // #defines for identifying shared types between calling functions
    private final static int REQUEST_ENABLE_BT = 1; // used to identify adding bluetooth names
    public final static int MESSAGE_READ = 2; // used in bluetooth handler to identify message update
    private final static int CONNECTING_STATUS = 3; // used in bluetooth handler to identify message status

    private final Boolean mConnectMeetingButtonState = false;
    private TextView mReadBuffer;
    private Button mScanBtn;
    private Button mOffBtn;
    private Button mListPairedDevicesBtn;
    private Button mDiscoverBtn;
    private ListView mDevicesListView;

    private BluetoothAdapter mBTAdapter;
    private Set<BluetoothDevice> mPairedDevices;
    private ArrayAdapter<String> mBTArrayAdapter;

    private Handler mHandler; // Our main handler that will receive callback notifications
    private ConnectedThread mConnectedThread; // bluetooth background worker thread to send and receive data
    private BluetoothSocket mBTSocket = null; // bi-directional client-to-client data path
    // Bluetooth connection part
    private TextView mBluetoothStatus;
    private final AdapterView.OnItemClickListener mDeviceClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

            if (!mBTAdapter.isEnabled()) {
                Toast.makeText(getBaseContext(), "Bluetooth not on", Toast.LENGTH_SHORT).show();
                return;
            }

            mBluetoothStatus.setText("Connecting...");
            // Get the device MAC address, which is the last 17 chars in the View
            String info = ((TextView) view).getText().toString();
            final String address = info.substring(info.length() - 17);
            final String name = info.substring(0, info.length() - 17);

            // Spawn a new thread to avoid blocking the GUI one
            new Thread() {
                @Override
                public void run() {
                    boolean fail = false;

                    BluetoothDevice device = mBTAdapter.getRemoteDevice(address);

                    try {
                        mBTSocket = createBluetoothSocket(device);
                    } catch (IOException e) {
                        fail = true;
                        Toast.makeText(getBaseContext(), "Socket creation failed", Toast.LENGTH_SHORT).show();
                    }
                    // Establish the Bluetooth socket connection.
                    try {
                        mBTSocket.connect();
                    } catch (IOException e) {
                        try {
                            fail = true;
                            mBTSocket.close();
                            mHandler.obtainMessage(CONNECTING_STATUS, -1, -1)
                                    .sendToTarget();
                        } catch (IOException e2) {
                            //insert code to deal with this
                            Toast.makeText(getBaseContext(), "Socket creation failed", Toast.LENGTH_SHORT).show();
                        }
                    }
                    if (!fail) {
                        mConnectedThread = new ConnectedThread(mBTSocket, mHandler);
                        mConnectedThread.start();

                        mHandler.obtainMessage(CONNECTING_STATUS, 1, -1, name)
                                .sendToTarget();
                        //
                        // Get informations when you connect to the device
                        if (mConnectedThread != null) //First check to make sure thread created
                            mConnectedThread.write("F");
                    }
                }
            }.start();
        }
    };
    // Show parameters part
    private TextView mAboutBluetoothState;
    private TextView mAboutWifiState;
    private TextView mAboutIDState;
    private TextView mAboutWifiConnectionState;
    private TextView mAboutMeetingConnectionState;
    private Button mGetDeviceStateButton;
    // Set parameters part
    private EditText mSetWifiSSIDText;
    private EditText mSetWifiPASSText;
    private EditText mSetIDText;
    private Button mSetWifiSSIDButton;
    private Button mSetWifiPASSButton;
    private Button mSetIDButton;
    // Start meeting part
    private Button mConnectWIFIButton;
    private Boolean mConnectWIFIButtonState = false;
    private Button mConnectMeetingButton;
    private ImageButton mbutton_playpause;
    private ImageButton mbutton_next;
    private ImageButton mbutton_off;
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
            } else if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {
                mSetWifiSSIDButton.setEnabled(true);
                mSetIDButton.setEnabled(true);
                mSetWifiPASSButton.setEnabled(true);
                mConnectWIFIButton.setEnabled(true);
                mbutton_off.setEnabled(true);
                mGetDeviceStateButton.setEnabled(true);

            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
            } else if (BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED.equals(action)) {
            } else if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
                //Device has disconnected

                resetAll();
                Toast.makeText(getApplicationContext(), "Device disconnected", Toast.LENGTH_SHORT).show();
            }
        }
    };
    // Start fun
    private ImageButton mLogoButton;

    private void bluetoothOn() {
        if (!mBTAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            mBluetoothStatus.setText("Bluetooth enabled");
            Toast.makeText(getApplicationContext(), "Bluetooth turned on", Toast.LENGTH_SHORT).show();

        } else {
            Toast.makeText(getApplicationContext(), "Bluetooth is already on", Toast.LENGTH_SHORT).show();
        }
    }

    // Enter here after user selects "yes" or "no" to enabling radio
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent Data) {
        // Check which request we're responding to
        if (requestCode == REQUEST_ENABLE_BT) {
            // Make sure the request was successful
            if (resultCode == RESULT_OK) {
                // The user picked a contact.
                // The Intent's data Uri identifies which contact was selected.
                mBluetoothStatus.setText("Enabled");
            } else
                mBluetoothStatus.setText("Disabled");
        }
    }

    private void bluetoothOff() {
        mBTAdapter.disable(); // turn off
        mBluetoothStatus.setText("Bluetooth disabled");
        Toast.makeText(getApplicationContext(), "Bluetooth turned Off", Toast.LENGTH_SHORT).show();
    }

    private void discover() {
        // Check if the device is already discovering
        if (mBTAdapter.isDiscovering()) {
            mBTAdapter.cancelDiscovery();
            Toast.makeText(getApplicationContext(), "Discovery stopped", Toast.LENGTH_SHORT).show();
        } else {
            if (mBTAdapter.isEnabled()) {
                mBTArrayAdapter.clear(); // clear items
                mBTAdapter.startDiscovery();
                Toast.makeText(getApplicationContext(), "Discovery started", Toast.LENGTH_SHORT).show();
                registerReceiver(blReceiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));
            } else {
                Toast.makeText(getApplicationContext(), "Bluetooth not on", Toast.LENGTH_SHORT).show();
            }
        }
    }

    final BroadcastReceiver blReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // add the name to the list
                mBTArrayAdapter.add(device.getName() + "\n" + device.getAddress());
                mBTArrayAdapter.notifyDataSetChanged();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Here is a part to trigger disconnection
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED);
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        this.registerReceiver(mReceiver, filter);

        // Bluetooth connection part
        mBluetoothStatus = (TextView) findViewById(R.id.bluetooth_status);
        mReadBuffer = (TextView) findViewById(R.id.read_buffer);
        mScanBtn = (Button) findViewById(R.id.scan);
        mOffBtn = (Button) findViewById(R.id.off);
        mDiscoverBtn = (Button) findViewById(R.id.discover);
        mListPairedDevicesBtn = (Button) findViewById(R.id.paired_btn);

        mBTArrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        mBTAdapter = BluetoothAdapter.getDefaultAdapter(); // get a handle on the bluetooth radio

        mDevicesListView = (ListView) findViewById(R.id.devices_list_view);
        mDevicesListView.setAdapter(mBTArrayAdapter); // assign model to view
        mDevicesListView.setOnItemClickListener(mDeviceClickListener);

        // Show parameters part
        mAboutBluetoothState = (TextView) findViewById(R.id.aboutBluetoothState);
        mAboutWifiState = (TextView) findViewById(R.id.aboutWifiState);
        mAboutIDState = (TextView) findViewById(R.id.aboutIDState);
        mAboutWifiConnectionState = (TextView) findViewById(R.id.aboutWifiConnectionState);
        mAboutMeetingConnectionState = (TextView) findViewById(R.id.aboutMeetingConnectionState);

        mGetDeviceStateButton = (Button) findViewById(R.id.aboutGet);

        // Set parameters part
        mSetWifiSSIDText = (EditText) findViewById(R.id.SetWifiSSIDText);
        mSetWifiPASSText = (EditText) findViewById(R.id.SetWifiPASSText);
        mSetIDText = (EditText) findViewById(R.id.SetIDText);

        mSetWifiSSIDButton = (Button) findViewById(R.id.SetWifiSSIDButton);
        mSetWifiPASSButton = (Button) findViewById(R.id.SetWifiPASSButton);
        mSetIDButton = (Button) findViewById(R.id.SetIDButton);

        // Start meeting part
        mConnectWIFIButton = (Button) findViewById(R.id.ConnectWIFIButton);
        mConnectMeetingButton = (Button) findViewById(R.id.ConnectMeetingButton);

        mbutton_playpause = (ImageButton) findViewById(R.id.button_playpause);
        mbutton_next = (ImageButton) findViewById(R.id.button_next);
        mbutton_off = (ImageButton) findViewById(R.id.button_off);

        // Funny part
        mLogoButton = (ImageButton) findViewById(R.id.logoButton);

        // HERE ARE SET THE MESSAGES FROM APP TO DEVICE
        // Give callbacks to these items

        mSetWifiSSIDButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (mConnectedThread != null) //First check to make sure thread created
                    mConnectedThread.write("0" + mSetWifiSSIDText.getText().toString());
            }
        });
        mSetWifiSSIDButton.setEnabled(false);
        mSetWifiPASSButton.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("ResourceType")
            @Override
            public void onClick(View v) {
                if (mConnectedThread != null) //First check to make sure thread created
                    mConnectedThread.write("2" + mSetWifiPASSText.getText().toString());
            }
        });
        mSetWifiPASSButton.setEnabled(false);
        mSetIDButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mConnectedThread != null) //First check to make sure thread created
                    mConnectedThread.write("4" + mSetIDText.getText().toString());
            }
        });
        mSetIDButton.setEnabled(false);
        mGetDeviceStateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mConnectedThread != null) //First check to make sure thread created
                    if ((mConnectedThread.isAlive())) {
                        mConnectedThread.write("F");
                    }
            }
        });
        mGetDeviceStateButton.setEnabled(false);
        mbutton_playpause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mConnectedThread != null) //First check to make sure thread created
                    mConnectedThread.write("C");
            }
        });
        mbutton_playpause.setEnabled(false);
        mbutton_next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mConnectedThread != null) //First check to make sure thread created
                    mConnectedThread.write("D");
            }
        });
        mbutton_next.setEnabled(false);
        mbutton_off.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mConnectedThread != null) //First check to make sure thread created
                    mConnectedThread.write("E");
            }
        });
        mbutton_off.setEnabled(false);
        mConnectMeetingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mConnectedThread != null) //First check to make sure thread created
                {
                    mConnectedThread.write("6");
                }
            }
        });
        mConnectMeetingButton.setEnabled(false);

        mConnectWIFIButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mConnectedThread != null) //First check to make sure thread created
                {
                    mConnectWIFIButtonState = !mConnectWIFIButtonState;
                    if (mConnectWIFIButtonState) {
                        mConnectedThread.write("8");
                        mConnectWIFIButton.setText("Stop Wifi research");
                    } else {
                        mConnectedThread.write("9");
                        mConnectWIFIButton.setText("Connect to Wifi");
                    }
                }
            }
        });
        mConnectWIFIButton.setEnabled(false);
        mLogoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mConnectedThread != null) //First check to make sure thread created
                    mConnectedThread.write("A");
            }
        });
        // HERE I HANDLE THE INCOMING MESSAGES FROM THE DEVICE
        mHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                String readMessage = null;
                if (msg.what == MESSAGE_READ) {
                    readMessage = null;
                    readMessage = new String((byte[]) msg.obj, StandardCharsets.UTF_8);
                    mReadBuffer.setText(readMessage);
                }

                if (msg.what == CONNECTING_STATUS) {
                    if (msg.arg1 == 1)
                        mBluetoothStatus.setText("Connected to Device: " + msg.obj);
                    else {
                        mBluetoothStatus.setText("Connection Failed");
                        resetAll();
                    }
                }

                if (readMessage != null) {
                    if (readMessage.charAt(0) == 'F') {
                        Toast.makeText(getApplicationContext(), "Refreshed about your device", Toast.LENGTH_SHORT).show();

                        // Get device name
                        mAboutBluetoothState.setText(mBTSocket.getRemoteDevice().getName());

                        // Get wifi State
                        String subMessage = "";
                        int i = 1;
                        while (readMessage.charAt(i) != '\n' && i < 9999) {
                            subMessage = subMessage + readMessage.charAt(i++);
                        }
                        mAboutWifiState.setText(subMessage);

                        // Get Meeting ID
                        subMessage = "";
                        i++;
                        while (readMessage.charAt(i) != '\n' && i < 9999) {
                            subMessage = subMessage + readMessage.charAt(i++);
                        }
                        mAboutIDState.setText(subMessage);

                        // Get wifi Connection
                        subMessage = "";
                        i++;
                        while (readMessage.charAt(i) != '\n' && i < 9999) {
                            subMessage = subMessage + readMessage.charAt(i++);
                        }
                        mAboutWifiConnectionState.setText(subMessage);

                        // Get meeting connection
                        subMessage = "";
                        i++;
                        while (readMessage.charAt(i) != '\n' && i < 9999) {
                            subMessage = subMessage + readMessage.charAt(i++);
                        }
                        mAboutMeetingConnectionState.setText(subMessage);
                    }

                    if (readMessage.charAt(0) == '0') {
                        Toast.makeText(getApplicationContext(), "Wifi SSID changed successfully", Toast.LENGTH_SHORT).show();
                        mAboutWifiState.setText(mSetWifiSSIDText.getText());
                        mSetWifiSSIDText.setText("");
                    }
                    if (readMessage.charAt(0) == '2') {
                        Toast.makeText(getApplicationContext(), "Wifi Password changed successfully", Toast.LENGTH_SHORT).show();
                        mSetWifiPASSText.setText("");

                    }
                    if (readMessage.charAt(0) == '4') {
                        Toast.makeText(getApplicationContext(), "Meeting ID changed successfully", Toast.LENGTH_SHORT).show();
                        mAboutIDState.setText(mSetIDText.getText());
                        mSetIDText.setText("");
                    }
                    if (readMessage.charAt(0) == '6') {
                        if (readMessage.charAt(1) == '1') {
                            Toast.makeText(getApplicationContext(), "Connected to meeting successfully", Toast.LENGTH_SHORT).show();

                            mAboutMeetingConnectionState.setText("Connected to meeting");
                            mConnectMeetingButton.setText("Disconnect meeting");
                            mbutton_playpause.setEnabled(true);
                            mbutton_next.setEnabled(true);
                            mConnectMeetingButton.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    if (mConnectedThread != null) //First check to make sure thread created
                                    {
                                        mConnectedThread.write("7");
                                    }
                                }
                            });
                        } else {
                            Toast.makeText(getApplicationContext(), "Unable to connect, check meeting ID", Toast.LENGTH_SHORT).show();
                            mAboutMeetingConnectionState.setText("Coudn't connect to meeting");
                        }
                    }
                    if (readMessage.charAt(0) == '7') {
                        Toast.makeText(getApplicationContext(), "Disconnected from meeting", Toast.LENGTH_SHORT).show();
                        mConnectMeetingButton.setText("Connect to meeting");
                        mAboutMeetingConnectionState.setText("Coudn't connect to meeting");
                        mbutton_playpause.setEnabled(false);
                        mbutton_next.setEnabled(false);
                        mConnectMeetingButton.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                if (mConnectedThread != null) //First check to make sure thread created
                                {
                                    mConnectedThread.write("6");
                                }
                            }
                        });
                    }
                    if (readMessage.charAt(0) == '8') {
                        Toast.makeText(getApplicationContext(), "Connected to Wifi successfully", Toast.LENGTH_SHORT).show();
                        mAboutWifiConnectionState.setText("WiFi connected");
                        mConnectWIFIButton.setText("Wifi Connected");
                        mConnectMeetingButton.setEnabled(true);
                        mConnectWIFIButton.setEnabled(false);
                    }
                    if (readMessage.charAt(0) == 'E') {
                        Toast.makeText(getApplicationContext(), "Device turns off successfully", Toast.LENGTH_SHORT).show();
                    }
                    if (readMessage.charAt(0) == 'C') {
                        Toast.makeText(getApplicationContext(), "Agenda paused successfully", Toast.LENGTH_SHORT).show();
                    }
                    if (readMessage.charAt(0) == 'D') {
                        Toast.makeText(getApplicationContext(), "Agenda went to next point successfully", Toast.LENGTH_SHORT).show();
                    }
                    if (readMessage.charAt(0) == 'G') {
                        Toast.makeText(getApplicationContext(), "Disconnected from meeting successfully", Toast.LENGTH_SHORT).show();
                    }

                }
            }
        };

        if (mBTArrayAdapter == null) {
            // Device does not support Bluetooth
            mBluetoothStatus.setText("Status: Bluetooth not found");
            Toast.makeText(getApplicationContext(),"Bluetooth device not found!",Toast.LENGTH_SHORT).show();
        }
        else {

            mScanBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    bluetoothOn();
                }
            });

            mOffBtn.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View v){
                    bluetoothOff();
                }
            });

            mListPairedDevicesBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v){
                    listPairedDevices();
                }
            });

            mDiscoverBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    discover();
                }
            });

            // Show the researches from the beginning to avoid blank space
            if (mBTAdapter.isEnabled()) {
                listPairedDevices();
            }
        }
    }

    private void listPairedDevices(){
        mBTArrayAdapter.clear();
        mPairedDevices = mBTAdapter.getBondedDevices();
        if(mBTAdapter.isEnabled()) {
            // put it's one to the adapter
            for (BluetoothDevice device : mPairedDevices)
                if (device.getName().contains("Odeji")) {
                    mBTArrayAdapter.add(device.getName() + "\n" + device.getAddress());
                }

            Toast.makeText(getApplicationContext(), "Show Paired Devices", Toast.LENGTH_SHORT).show();
        }
        else
            Toast.makeText(getApplicationContext(), "Bluetooth not on", Toast.LENGTH_SHORT).show();
    }

    private BluetoothSocket createBluetoothSocket(BluetoothDevice device) throws IOException {
        try {
            final Method m = device.getClass().getMethod("createInsecureRfcommSocketToServiceRecord", UUID.class);
            return (BluetoothSocket) m.invoke(device, BT_MODULE_UUID);
        } catch (Exception e) {
            Log.e(TAG, "Could not create Insecure RFComm Connection", e);
        }
        return device.createRfcommSocketToServiceRecord(BT_MODULE_UUID);
    }

    private void resetAll() {
        mAboutBluetoothState.setText("Device not connected");
        mBluetoothStatus.setText("Device not connected");
        mAboutWifiState.setText("");
        mAboutIDState.setText("");
        mAboutMeetingConnectionState.setText("");
        mAboutWifiConnectionState.setText("");
        mConnectWIFIButtonState = false;
        mConnectWIFIButtonState = false;
        mConnectWIFIButton.setText("Connect to Wifi");
        mConnectMeetingButton.setEnabled(false);
        mbutton_playpause.setEnabled(false);
        mbutton_next.setEnabled(false);
        mGetDeviceStateButton.setEnabled(false);
        mSetWifiSSIDButton.setEnabled(false);
        mSetIDButton.setEnabled(false);
        mSetWifiPASSButton.setEnabled(false);
        mConnectWIFIButton.setEnabled(false);
        mbutton_off.setEnabled(false);
        mConnectMeetingButton.setText("Connect to meeting");
        mConnectMeetingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mConnectedThread != null) //First check to make sure thread created
                {
                    mConnectedThread.write("6");
                }
            }
        });
        mReadBuffer.setText("");
    }
}

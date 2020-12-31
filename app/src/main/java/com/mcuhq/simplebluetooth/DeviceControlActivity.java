package com.mcuhq.simplebluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;

import java.util.Set;

public class DeviceControlActivity extends AppCompatActivity {

    private Button SetWifiSSIDButton;

    private Handler mHandler; // Our main handler that will receive callback notifications
    private ConnectedThread mConnectedThread; // bluetooth background worker thread to send and receive data
    private BluetoothSocket mBTSocket = null; // bi-directional client-to-client data path


    private BluetoothAdapter mBTAdapter;
  //  private Set<BluetoothDevice> mPairedDevices;
  //  private ArrayAdapter<String> mBTArrayAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_control);
    /*    SetWifiSSIDButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mConnectedThread != null) //First check to make sure thread created
                    mConnectedThread.write("A");
            }
        });*/

       // mBTArrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
       // mBTAdapter = BluetoothAdapter.getDefaultAdapter(); // get a handle on the bluetooth radio
    }


}

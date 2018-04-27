package com.example.tony.clawcopteruicontroller;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebView;
import android.widget.Button;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Set;

/**
 * Created by Tony on 2/7/2018.
 */

public class ClawActivity extends Activity{

    private BluetoothAdapter mBluetoothAdapter;
    private boolean sendClawCommands;
    private View ClawControl;
    private int closeAngle;

    WebView vidStream;
    String videoAddr = "http://172.24.1.1:9000";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //remove title
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_claw);

        //Changing view
        Button changeView = findViewById(R.id.ModeSwitch);
        changeView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ClawActivity.this, FlightActivity.class);
                startActivity(intent);
            }
        });
        //Register for broadcasts when a device is discovered
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(mBluetoothReceiver, filter);


        ClawControl = findViewById(R.id.ClawSlider);

        ClawControl.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                    case MotionEvent.ACTION_MOVE:
                        float height = v.getHeight();
                        closeAngle =179;
                        Log.i("ClawControl", "Send" + closeAngle);

                        if (event.getY() < height / 3) {
                            closeAngle = 179;
                            Log.i("ClawControl", "Send" + closeAngle);

                        } else if (event.getY() < 2 * height / 3 && event.getY() > height / 3) {
                            closeAngle = 89;
                            Log.i("ClawControl", "Send" + closeAngle);
                        } else if (event.getY() > 2 * height / 3) {
                            closeAngle = 0;
                            Log.i("ClawControl", "Send" + closeAngle);

                        }
                        return true;
                    case MotionEvent.ACTION_UP:
                        //Claw shall remain at the same position
                  }

                return false;

            }
        });

        vidStream = findViewById(R.id.clawCam);
        vidStream.loadUrl(videoAddr);


    }
    private final BroadcastReceiver mBluetoothReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                //Discovery found a device
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                String deviceName = device.getName();
                String deviceHardwareAddress = device.getAddress(); //MAC Address
            }
        }
    };

    @Override
    protected void onResume()
    {
        super.onResume();
        //Connects to bluetooth.
        //Bluetooth Connectivity
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            Log.d("Device: ", "Doesn't support Bluetooth");
        }
        //Enable Bluetooth
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, 433);
        }

        //Query paired devices
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        BluetoothDevice Copter = null;
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                String deviceName = device.getName();
                String deviceHardwareAddress = device.getAddress(); // MAC Address
                Log.i("Device: ", "Found Device " + deviceName);
                if (deviceName.equals("HC-05")) {
                    Copter = device;
                }
            }
        }
        if (Copter != null) {
            sendClawCommands = true;
            new ClawActivity.Commander(Copter).start();
        }
    }
    //Writing to the socket-------------------------------------------------------------------------
    private class Commander extends Thread {
        private BluetoothDevice Copter;

        public Commander(BluetoothDevice Copter) {
            this.Copter = Copter;
        }

        @Override
        public void run() {
            BluetoothSocket BtSocket = null;
            try {
                OutputStream commands;

                Log.i("Copter", "In Bluetooth Commands Copter = " + Copter.getName());
                Log.i("Copter", "Device UUID = " + Copter.getUuids());
                BtSocket = Copter.createRfcommSocketToServiceRecord(Copter.getUuids()[0].getUuid());
                BtSocket.connect();
                if (!BtSocket.isConnected()) {
                    BtSocket.connect();
                    Log.i("Bluetooth Connection: ", "Connected");
                }

                commands = BtSocket.getOutputStream();

                while (sendClawCommands) {
                    commands.write(closeAngle);
                    Log.i("Copter", "Sent Command " + closeAngle);
                    Thread.sleep(15);

                }
                BtSocket.close();
            }catch (Exception e) {
                e.printStackTrace();
                if (BtSocket != null) {
                    try {
                        BtSocket.close();
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                }
            }
        }
    }
}

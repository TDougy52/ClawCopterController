package com.example.tony.clawcopteruicontroller;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pDevice;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.RadioButton;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;




public class FlightActivity extends Activity implements SensorEventListener {

    private static final String TAG = "TAG";
    private SensorManager mSensorManager;
    private final float[] mAccelerometerReading = new float[3];
    private final float[] mMagnetometerReading = new float[3];

    private final float[] mRotationMatrix = new float[9];
    private final float[] mOrientationAngles = new float[3];

    private Sensor accelerometer;
    private Sensor magnetometer;

    private RadioButton PHigh;
    private RadioButton PLow;
    private RadioButton RHigh;
    private RadioButton RLow;
    private RadioButton YHigh;
    private RadioButton YLow;
    private RadioButton THigh;
    private RadioButton TLow;
    private View ThrottleSlider;

    //Booleans to be sent over bluetooth holding commands (Original idea)
    //Used to show orientation of the phone and turn on and off corresponding Radio buttons
    private boolean PitchHigh;
    private boolean PitchLow;
    private boolean RollHigh;
    private boolean RollLow;
    private boolean YawHigh;
    private boolean YawLow;
    private boolean ThrottleHigh;
    private boolean ThrottleLow;


    private boolean isWifiP2pEnabled;

    WebView vidStream;
    String videoAddr = "http://172.24.1.1:9000";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //remove title
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_flight);

        //Changing view using the button
        Button changeView = findViewById(R.id.ModeSwitch);
        changeView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(FlightActivity.this, ClawActivity.class);
                startActivity(intent);
            }
        });
        WifiManager wifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        Log.i("Wifi", "Wifi Enabled = " + wifi.isWifiEnabled());

        //Pitch, Roll, Yaw, Throttle sensors
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        //Pitch
        PHigh = findViewById(R.id.PitchHigh);
        PLow = findViewById(R.id.PitchLow);
        //Roll
        RHigh = findViewById(R.id.RollHigh);
        RLow = findViewById(R.id.RollLow);
        //Yaw
        YHigh = findViewById(R.id.YawHigh);
        YLow = findViewById(R.id.YawLow);
        //Throttle
        ThrottleSlider = findViewById(R.id.Throttle);

        ThrottleSlider.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                    case MotionEvent.ACTION_MOVE:

                        float height = v.getHeight();

                        if (event.getY() < height / 3) {
                            THigh.setChecked(true);
                            TLow.setChecked(false);
                            //booleans to be sent over bluetooth
                            ThrottleHigh = true;
                            ThrottleLow = false;
                        } else if (event.getY() < 2 * height / 3 && event.getY() > height / 3) {
                            THigh.setChecked(true);
                            TLow.setChecked(true);
                            //booleans to be sent over bluetooth
                            ThrottleHigh = true;
                            ThrottleLow = true;
                        } else if (event.getY() > 2 * height / 3) {
                            THigh.setChecked(false);
                            TLow.setChecked(true);
                            //booleans to be sent over bluetooth
                            ThrottleHigh = false;
                            ThrottleLow = true;
                        }

                        Log.d("Throttle", "" + event.getY());

                        return true;

                    case MotionEvent.ACTION_UP:
                        THigh.setChecked(true);
                        TLow.setChecked(true);
                        //booleans to be sent over bluetooth
                        ThrottleHigh = true;
                        ThrottleLow = true;
                }

                return false;

            }
        });

        vidStream = findViewById(R.id.surroundCam);
        vidStream.loadUrl(videoAddr);


        THigh = findViewById(R.id.ThrottleHigh);
        TLow = findViewById(R.id.ThrottleLow);

        updateOrientationAngles();

    }

    // ----------Sensor code------------------------------------------------------------------------
    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            System.arraycopy(event.values, 0, mAccelerometerReading,
                    0, mAccelerometerReading.length);
            updateOrientationAngles();

        } else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            System.arraycopy(event.values, 0, mMagnetometerReading,
                    0, mMagnetometerReading.length);
            updateOrientationAngles();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Do something here if sensor accuracy changes.
        // You must implement this callback in your code.
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Get updates from the accelerometer and magnetometer at a constant rate.
        // To make batch operations more efficient and reduce power consumption,
        // provide support for delaying updates to the application.
        //
        // In this example, the sensor reporting delay is small enough such that
        // the application receives an update before the system checks the sensor
        // readings again.
        mSensorManager.registerListener(this, accelerometer,
                SensorManager.SENSOR_DELAY_NORMAL, SensorManager.SENSOR_DELAY_UI);
        mSensorManager.registerListener(this, magnetometer,
                SensorManager.SENSOR_DELAY_NORMAL, SensorManager.SENSOR_DELAY_UI);
    }

    @Override
    protected void onPause() {
        super.onPause();

        // Don't receive any more updates from either sensor.
        mSensorManager.unregisterListener(this);
    }

    // Compute the three orientation angles based on the most recent readings from
    // the device's accelerometer and magnetometer.
    public void updateOrientationAngles() {


        Log.d("Copter", "Updating");

        // Update rotation matrix, which is needed to update orientation angles.
        mSensorManager.getRotationMatrix(mRotationMatrix, null,
                mAccelerometerReading, mMagnetometerReading);

        // "mRotationMatrix" now has up-to-date information.

        mSensorManager.getOrientation(mRotationMatrix, mOrientationAngles);

        // "mOrientationAngles" now has up-to-date information.

        //Yaw in degrees
        final float Yaw = mOrientationAngles[0];
        if (Yaw < .26 && Yaw > -.26) {
            YHigh.setChecked(true);
            YLow.setChecked(true);
            //booleans to be sent over bluetooth
            YawHigh = true;
            YawLow = true;
        } else if (Yaw > .26) {
            YHigh.setChecked(true);
            YLow.setChecked(false);
            //booleans to be sent over bluetooth
            YawHigh = true;
            YawLow = false;
        } else if (Yaw < -.26) {
            YHigh.setChecked(false);
            YLow.setChecked(true);
            //booleans to be sent over bluetooth
            YawHigh = false;
            YawLow = true;
        }

        Log.d("Yaw: ", "" + Yaw);

        //Roll in degrees
        final float Roll = mOrientationAngles[1];
        if (Roll < .26 && Roll > -.26) {
            RHigh.setChecked(true);
            RLow.setChecked(true);
            //booleans to be sent over bluetooth
            RollHigh = true;
            RollLow = true;
        } else if (Roll > .26) {
            RHigh.setChecked(true);
            RLow.setChecked(false);
            //booleans to be sent over bluetooth
            RollHigh = true;
            RollLow = false;
        } else if (Roll < -.26) {
            RHigh.setChecked(false);
            RLow.setChecked(true);
            //booleans to be sent over bluetooth
            RollHigh = false;
            RollLow = true;
        }

        Log.d("Roll: ", "" + Roll);

        //Pitch in degrees
        final float Pitch = mOrientationAngles[2];
        if (Pitch < .26 && Pitch > -.26) {
            PHigh.setChecked(true);
            PLow.setChecked(true);
            //booleans to be sent over bluetooth
            PitchHigh = true;
            PitchLow = true;
        } else if (Pitch > .26) {
            PHigh.setChecked(true);
            PLow.setChecked(false);
            //booleans to be sent over bluetooth
            PitchHigh = true;
            PitchLow = false;
        } else if (Pitch < -.26) {
            PHigh.setChecked(false);
            PLow.setChecked(true);
            //booleans to be sent over bluetooth
            PitchHigh = false;
            PitchLow = true;
        }

        Log.d("Pitch: ", "" + Pitch);
    }

    //WiFi Socket communication to receive video--------------------------------------------------
    private class Commander extends Thread {
        private WifiP2pDevice WifiCopter;

        public Commander(WifiP2pDevice peers) {
            peers = WifiCopter;
        }

        @Override
        public void run() {
            Socket WiFiSocket = null;

            try {
                OutputStream commands;
                InputStream video;
                //create the socket
                ServerSocket serverSocket = new ServerSocket(8888);
                WiFiSocket = serverSocket.accept();
                //write to the socket
                commands = WiFiSocket.getOutputStream();
                //read from the socket
                video = WiFiSocket.getInputStream();

            } catch (Exception e) {
                e.printStackTrace();
                if (WiFiSocket != null) {
                    try {
                        WiFiSocket.close();
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                }
            }
        }
    }
}
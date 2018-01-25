package com.example.qnb14161.usbtestapp;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.felhr.usbserial.UsbSerialDevice;
import com.felhr.usbserial.UsbSerialInterface;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends Activity implements View.OnClickListener{

    public final String ACTION_USB_PERMISSION = "com.example.qnb14161.usbtestapp.USB_PERMISSION";
    //Declare widgets variables
    Button mStartBtn,mStopBtn,mClearBtn;
    TextView mInfoTxt,mDataTxt;
    //Declare USB elements
    UsbDevice mDevice;
    UsbManager mUsbManager;
    UsbSerialDevice mSerialPort;
    UsbDeviceConnection mConnection;

    //A callback to read serial data from the USB asynchronously
    UsbSerialInterface.UsbReadCallback mCallback = new UsbSerialInterface.UsbReadCallback() { //Defining a Callback which triggers whenever data is read.
        @Override
        public void onReceivedData(byte[] arg0) {
            String data = null;
            try {
                data = new String(arg0, "UTF-8");
                data.concat("\n");
                //Constantly update the text contents of the data text view
                appendText(mDataTxt,data);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }


        }
    };

    //The BroadcastReceiver to receive the broadcast to ask for user permission and start/stop connection automatically
    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getAction().equals(ACTION_USB_PERMISSION)){
                //Get the user permission
                boolean permitted = intent.getExtras().getBoolean(UsbManager.EXTRA_PERMISSION_GRANTED);
                if(permitted){
                    //Open a connection and create a serial device
                    mConnection = mUsbManager.openDevice(mDevice);
                    mSerialPort = UsbSerialDevice.createUsbSerialDevice(mDevice,mConnection);
                    if(mSerialPort != null){
                        if(mSerialPort.open()){
                            //Set up the USB communication
                            setUiEnabled(true);
                            mSerialPort.setBaudRate(9600);
                            mSerialPort.setDataBits(UsbSerialInterface.DATA_BITS_8);
                            mSerialPort.setStopBits(UsbSerialInterface.STOP_BITS_1);
                            mSerialPort.setParity(UsbSerialInterface.PARITY_NONE);
                            mSerialPort.setFlowControl(UsbSerialInterface.FLOW_CONTROL_OFF);
                            //Read the Serial data with the callback function
                            mSerialPort.read(mCallback);
                            appendText(mInfoTxt,"Serial Port Opened\n");
                        }else{
                            Log.d("SERIAL","PORT COULDNT OPEN");
                        }

                    }else{
                        Log.d("SERIAL","PORT IS NULL");
                    }

                }else{
                    Log.d("SERIAL","ACCESS DENIED");
                }
            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mStartBtn = (Button) findViewById(R.id.beginBtn);
        mStopBtn = (Button) findViewById(R.id.stopBtn);
        mClearBtn = (Button) findViewById(R.id.clearBtn);
        mInfoTxt = (TextView) findViewById(R.id.infoTxt);
        mDataTxt = (TextView) findViewById(R.id.dataTxt);
        mDataTxt.setMovementMethod(new ScrollingMovementMethod());
        mStartBtn.setOnClickListener(this);
        mStopBtn.setOnClickListener(this);
        mClearBtn.setOnClickListener(this);
        mUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        setUiEnabled(false);
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_USB_PERMISSION);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        registerReceiver(broadcastReceiver, filter);


    }

    @Override
    public void onClick(View view) {
        if(view.getId() == mStartBtn.getId()){
            beginConnection();
        }
        if(view.getId() == mStopBtn.getId()){
            stopConnection();
        }
        if(view.getId() == mClearBtn.getId()){
            clearDataText();
        }
    }

    private void beginConnection(){
        HashMap<String,UsbDevice> devicesList = mUsbManager.getDeviceList();
        if(!devicesList.isEmpty()){
            boolean keep = true;
            for (Map.Entry<String, UsbDevice> entry : devicesList.entrySet()) {
                mDevice = entry.getValue();
                int deviceVID = mDevice.getVendorId();
                if (deviceVID == 1027){
                    PendingIntent pi = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
                    mUsbManager.requestPermission(mDevice, pi);
                    keep = false;
                } else {
                    mConnection = null;
                    mDevice = null;
                }

                if (!keep)
                    break;
            }
        }else{
            mInfoTxt.setText("No device attached");
            Log.d("beginConnection()", " DEVICE LIST IS EMPTY");
        }
    }

    private void stopConnection(){
        setUiEnabled(false);
        mSerialPort.close();
        appendText(mInfoTxt,"\nSerial Connection Closed! \n");
    }

    private void clearDataText(){
        mDataTxt.setText("");
    }

    private void setUiEnabled(boolean status){
        mStartBtn.setEnabled(!status);
        mStopBtn.setEnabled(status);
        mInfoTxt.setEnabled(status);
    }

    private void appendText(TextView textView, final CharSequence text){
        final TextView fTextView = textView;
        final CharSequence fText = text;

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                fTextView.setText(fText);
            }
        });
    }


}

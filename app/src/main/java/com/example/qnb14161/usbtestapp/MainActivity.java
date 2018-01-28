package com.example.qnb14161.usbtestapp;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.*;
import android.hardware.usb.*;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.*;

import com.felhr.usbserial.*;

import java.io.UnsupportedEncodingException;
import java.util.*;

public class MainActivity extends Activity implements View.OnClickListener{

    public final String ACTION_USB_PERMISSION = "com.example.qnb14161.usbtestapp.USB_PERMISSION";
    //Declare widgets variables
    private Button mStartBtn,mStopBtn,mClearBtn;
    private TextView mInfoTxt,mDataTxt;
    //Declare USB elements
    private UsbDevice mDevice;
    private UsbManager mUsbManager;
    private UsbSerialDevice mSerialPort;
    private UsbDeviceConnection mConnection;

    /**
     * A callback function, used to read data received from the serial port.
     * onReceivedData is invoked whenever new data is available on the port.
     * Since the data is received in raw byte format, it is first encoded to UTF-8
     * and then used accordingly
     */
    UsbSerialInterface.UsbReadCallback mCallback = new UsbSerialInterface.UsbReadCallback() {
        @Override
        public void onReceivedData(byte[] arg0) {
            String data = null;
            try {
                data = new String(arg0, "UTF-8");
                data.concat("\n");
                //Constantly update the text contents of the data text view
                updateText(mDataTxt,data);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }


        }
    };

    /**
     * This broadcast receiver is used to ask for a user permission to accept the found USB device.
     * If permission is granted, a new serial port connection is opened and set up.
     * Finally, the incoming data is read with the use of a callback function passed as an argument.
     */
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
                            updateText(mInfoTxt,"Serial Port Opened\n");
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
        initialize();
        setUiEnabled(false);
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_USB_PERMISSION);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        registerReceiver(broadcastReceiver, filter);


    }

    @Override
    public void onClick(View view) {
        //Check which button is clicked
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

    /**
     * When called, this method looks for any connected devices
     * If such are found, it compares the vendorID of each of them to that of the TinyDuino (1024),
     * and if the two are matching, it requests user permission to use the device
     */
    private void beginConnection(){
        int tinyDuinoVendorId = 1027;
        HashMap<String,UsbDevice> devicesList = mUsbManager.getDeviceList();
        if(!devicesList.isEmpty()){
            boolean keep = true;
            for (Map.Entry<String, UsbDevice> entry : devicesList.entrySet()) {
                mDevice = entry.getValue();
                int deviceVID = mDevice.getVendorId();
                if (deviceVID == tinyDuinoVendorId){
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
            updateText(mInfoTxt,"No Device Attached");
            Log.d("beginConnection()", " DEVICE LIST IS EMPTY");
        }
    }

    /**
     * The method closes the serial port and disables the UI elements
     */
    private void stopConnection(){
        setUiEnabled(false);
        mSerialPort.close();
        updateText(mInfoTxt,"Connection Stopped");
    }

    /**
     * This method is used to clear the sensor data shown on the screen
     */
    private void clearDataText(){
        updateText(mDataTxt,"");
    }

    /**
     * This method enables/disables the buttons showing on the screen.
     *
     * @param status - the status variable to which the buttons are set
     */
    private void setUiEnabled(boolean status){
        mStartBtn.setEnabled(!status);
        mStopBtn.setEnabled(status);
        mInfoTxt.setEnabled(status);
    }

    /**
     * This method updates the text of a text view. Since the serial data reading is done in the
     * background, not affecting the main UI thread, the runOnUiThread method is called, so that
     * whenever new data is received, the text view is updated.
     * @param textView - the particular textView element to be updated
     * @param text - the character sequence to be attached on the textView
     */
    private void updateText(TextView textView, final CharSequence text){
        final TextView fTextView = textView;
        final CharSequence fText = text;

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                fTextView.setText(fText);
            }
        });
    }

    /**
     * A helper function to initialize the widgets and other variables.
     * Keeps the onCreate method short.
     */
    private void initialize(){
        mStartBtn = (Button) findViewById(R.id.beginBtn);
        mStopBtn = (Button) findViewById(R.id.stopBtn);
        mClearBtn = (Button) findViewById(R.id.clearBtn);
        mInfoTxt = (TextView) findViewById(R.id.infoTxt);
        mDataTxt = (TextView) findViewById(R.id.dataTxt);
        mStartBtn.setOnClickListener(this);
        mStopBtn.setOnClickListener(this);
        mClearBtn.setOnClickListener(this);
        mUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
    }


}

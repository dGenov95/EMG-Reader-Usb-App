package com.example.dimitar.usbtestapp;

import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import com.felhr.usbserial.UsbSerialDevice;
import com.felhr.usbserial.UsbSerialInterface;

import java.util.HashMap;
import java.util.Map;

/**
 * The UsbService class is used to perform all the USB actions in the background, while the user
 * interacts with the UI.
 * It is used as a bound service, because the Activity calling it should be able to communicate with
 * it, based on the user interaction.
 * It listens to USB specific Broadcast events (usb attached/detached etc.) and sends the appropriate
 * feedback to the UI to notify the user.
 * Furthermore, when the user grants a permission for a USB communication, the Service looks for a
 * TinyDuino device. If such is found, it tries to open a connection and read the data coming from it.
 * Whenever new data is received, it is passed to a Handler object which allows the Activity
 * that is binding to the UsbService to update its UI.
 */

public class UsbService extends Service {

    //Declare variables for the different usb related actions, used to send intents to the MainActivity
    public static final String ACTION_USB_READY = "com.felhr.connectivityservices.USB_READY";
    public static final String ACTION_USB_ATTACHED = "android.hardware.usb.action.USB_DEVICE_ATTACHED";
    public static final String ACTION_USB_DETACHED = "android.hardware.usb.action.USB_DEVICE_DETACHED";
    public static final String ACTION_USB_NOT_SUPPORTED = "com.felhr.usbservice.USB_NOT_SUPPORTED";
    public static final String ACTION_NO_USB = "com.felhr.usbservice.NO_USB";
    public static final String ACTION_USB_PERMISSION_GRANTED = "com.felhr.usbservice.USB_PERMISSION_GRANTED";
    public static final String ACTION_USB_PERMISSION_NOT_GRANTED = "com.felhr.usbservice.USB_PERMISSION_NOT_GRANTED";
    public static final String ACTION_USB_DISCONNECTED = "com.felhr.usbservice.USB_DISCONNECTED";
    private static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";
    private static final int BAUD_RATE = 9600; // BaudRate of the connection
    public static final int MESSAGE_FROM_SERIAL_PORT = 0;
    private static final int TINY_DUINO_VID = 1027; //The particular Vendor ID of the TinyDuino

    //The IBinder object used for binding the service
    private IBinder binder = new UsbBinder();

    //The variables needed for the USB connection, its data passing to the Activity and
    //writing to a separate file
    private Context context;
    private Handler mHandler;
    private UsbManager usbManager;
    private UsbDevice device;
    private UsbDeviceConnection connection;
    private UsbSerialDevice serialPort;
    private SensorDataWriter mDataWriter;
    //A variable to track if the connection is opened
    private boolean serialPortOpened;

    /**
     *  Data received from the serial port will be received here. The
     *  byte stream is converted to String and send to UI thread to be treated there.
     */
    private UsbSerialInterface.UsbReadCallback mCallback = new UsbSerialInterface.UsbReadCallback() {
        @Override
        public void onReceivedData(byte[] arg0) {
            String data = new String(arg0);
//            String[]separatedData = data.split("\\r?\\n");

                if(mDataWriter != null) {
                    mDataWriter.writeToFile(data);
                }

                if (mHandler != null) {
                    mHandler.obtainMessage(MESSAGE_FROM_SERIAL_PORT, data).sendToTarget();
                }
            }

    };


    /**
     * This broadcast receiver will listen for system notifications. In particular it will listen
     * for USB events - USB Permission,USB attached/detached etc. It will deal with each of them
     * accordingly.
     */
    private final BroadcastReceiver usbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context ctx, Intent intent) {
            if (intent.getAction().equals(ACTION_USB_PERMISSION)) {
                boolean granted = intent.getExtras().getBoolean(UsbManager.EXTRA_PERMISSION_GRANTED);
                if (granted){ // User accepted the USB connection - try to open the device as a serial port
                    //Tell the MainActivity that permission was granted
                    Intent acceptedIntent = new Intent(ACTION_USB_PERMISSION_GRANTED);
                    ctx.sendBroadcast(acceptedIntent);
                    //Open the device as device connection
                    connection = usbManager.openDevice(device);
                    //Run the thread responsible for opening the serial port
                    new ConnectionThread().start();
                } else { // User not accepted the USB connection - Send an Intent to the Main Activity
                    Intent rejectedIntent = new Intent(ACTION_USB_PERMISSION_NOT_GRANTED);
                    ctx.sendBroadcast(rejectedIntent);
                }
            } else if (intent.getAction().equals(ACTION_USB_ATTACHED)) { //The USB has been attached
                //Check whether a connection is opened or not
                if (!serialPortOpened)
                    //If it is not
                    findDevice(); // try to open the USB as a Serial port
            } else if (intent.getAction().equals(ACTION_USB_DETACHED)) {
                // Usb device was disconnected - send an intent to the MainActivity
                Intent disconnectedIntent = new Intent(ACTION_USB_DISCONNECTED);
                ctx.sendBroadcast(disconnectedIntent);
                //Close the connection
                closeUsbConnection();
            }
        }
    };

    /**
     * onCreate will be executed when the service is started. It runs once and
     * initializes the variables. An IntentFilter to listen for incoming Intents
     * (USB ATTACHED, USB DETACHED...) is also configured. Finally it tries to open a serial port
     * if a particular USB device is attached.
     */
    @Override
    public void onCreate() {
        this.context = this;
        //This will change once the port is connected successfully
        serialPortOpened = false;
        //Set the filters
        setIntentFilters();
        usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        //Search for the attached TinyDuino device
        findDevice();
    }

    /**
     * This method is called when another component calls the bindService method. It passes an intent
     * as a parameter, which can be used to get extra information passed from the component. It executes
     * every time the bindService method is called.
     * @param intent - the Intent passed from the component binding to the service
     * @return - an IBinder implementation.
     */
    @Override
    public IBinder onBind(Intent intent) {
        //Whenever the service is bound, get the file name specified by user
        if(intent.hasExtra("file_name")){
            String fileName = intent.getStringExtra("file_name");
            //Create a new data writer object that will store the data whenever it is received
            mDataWriter = new SensorDataWriter(fileName);
        }
        return binder;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d("Service", "onDestroy");
        //Before the service is destroyed, make sure the connection with the usb is closed.
        closeUsbConnection();
        //And unregister the broadcast receiver
        unregisterReceiver(usbReceiver);
    }

    /**
     * This method is a setter for the data Handler, used to transfer data to the Activity thread.
     * @param mHandler - the handler passed from the Activity thread
     */
    public void setHandler(Handler mHandler) {
        this.mHandler = mHandler;
    }

    /**
     * This method gets all the devices connected to the device and checks the vendor ID of each one.
     * It is looking for the particular Tiny Duino vendor ID, and once it finds it, it asks the user
     * for permission to use the device. If no such device was found, an intent indicating that is
     * sent.
     */
    private void findDevice() {
        //This snippet will try to open the TinyDuino device connected
        //Get all the attached devices list
        HashMap<String, UsbDevice> usbDevices = usbManager.getDeviceList();

        if (!usbDevices.isEmpty()) { //Checks if there are any devices attached at all
            //A variable to indicate whether to keep looking for TinyDuino or not
            boolean keep = true;
            //Go through each one of the attached devices until a TinyDuino device is found
            for (Map.Entry<String, UsbDevice> entry : usbDevices.entrySet()) {
                //Get the device object of the current entry
                device = entry.getValue();
                //Get its vendor ID for comparison
                int deviceVID = device.getVendorId();
                if (deviceVID == TINY_DUINO_VID) {
                    // There is a TinyDuino connected to our Android device. Ask the user for
                    // permission to open it as a serial port.
                    requestUserPermission();
                    //Also indicate that it was found
                    keep = false;
                } else {
                    //The device is not a TinyDuino, set the connection and device variables to null
                    connection = null;
                    device = null;
                }
                //Tell the loop to stop looking for devices if the TinyDuino was found
                if (!keep)
                    break;
            }
            if (!keep) {
                // There is no USB devices connected (but usb host were listed). Send an intent to MainActivity.
                Intent intent = new Intent(ACTION_NO_USB);
                sendBroadcast(intent);
            }
        } else {
            // There is no USB devices connected. Send an intent to MainActivity
            Intent intent = new Intent(ACTION_NO_USB);
            sendBroadcast(intent);
        }
    }

    /**
     * A helper method that closes the serial port connection if it was opened previously.
     */
    private void closeUsbConnection(){
        if(serialPortOpened) serialPort.close();
        serialPortOpened = false;
    }

    /**
     * This method registers an intent filter to listen to certain ,USB related, intents.
     */
    private void setIntentFilters() {
        IntentFilter filter = new IntentFilter();
        //The receiver should listen for these events
        filter.addAction(ACTION_USB_PERMISSION); //when user permission has to be asked
        filter.addAction(ACTION_USB_DETACHED); //when the usb is detached
        filter.addAction(ACTION_USB_ATTACHED); //when to usb is attached
        registerReceiver(usbReceiver, filter);
    }

    /**
     * A helper method to ask the user if it is okay to access the usb device.
     * The response will be received in the broadcast receiver and handled appropriately.
     */
    private void requestUserPermission() {
        PendingIntent mPendingIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
        usbManager.requestPermission(device, mPendingIntent);
    }

    /**
     * A binder class to get the UsbService - needed for the activity to bind to the service
     */
    class UsbBinder extends Binder {
        UsbService getUsbService() {
            return UsbService.this;
        }
    }

    /**
     * A Thread used to connect to the usb serial port - running separately from the main UI thread.
     */
    private class ConnectionThread extends Thread {
        @Override
        public void run() {
            serialPort = UsbSerialDevice.createUsbSerialDevice(device, connection);
            if (serialPort != null) {
                //Try to open the port
                if (serialPort.open()) {
                    serialPortOpened = true;
                    //Set the connection properties to match those of the TinyDuino
                    serialPort.setBaudRate(BAUD_RATE);
                    serialPort.setDataBits(UsbSerialInterface.DATA_BITS_8);
                    serialPort.setStopBits(UsbSerialInterface.STOP_BITS_1);
                    serialPort.setParity(UsbSerialInterface.PARITY_NONE);
                    serialPort.setFlowControl(UsbSerialInterface.FLOW_CONTROL_OFF);
                    serialPort.read(mCallback);
                    // Everything went as expected. Send an intent to MainActivity
                    Intent readyIntent = new Intent(ACTION_USB_READY);
                    context.sendBroadcast(readyIntent);
                }

            } else {
                // No driver for current device - could not be loaded
                Intent notSupportedIntent = new Intent(ACTION_USB_NOT_SUPPORTED);
                context.sendBroadcast(notSupportedIntent);
            }
        }
    }
}
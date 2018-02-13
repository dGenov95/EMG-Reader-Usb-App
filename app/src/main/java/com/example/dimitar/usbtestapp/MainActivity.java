package com.example.dimitar.usbtestapp;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.ref.WeakReference;
import java.util.Set;

public class MainActivity extends AppCompatActivity {


    private UsbService usbService;
    private TextView display;
    private EditText fileNameText;
    private MyHandler mHandler;

    /**
     * Receive the UsbService notifications and display a Toast message to inform the current
     * status of the Usb
     */
    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case UsbService.ACTION_USB_PERMISSION_GRANTED: // USB PERMISSION GRANTED
                    Toast.makeText(context, "USB Ready", Toast.LENGTH_SHORT).show();
                    break;
                case UsbService.ACTION_USB_PERMISSION_NOT_GRANTED: // USB PERMISSION NOT GRANTED
                    Toast.makeText(context, "USB Permission not granted", Toast.LENGTH_SHORT).show();
                    break;
                case UsbService.ACTION_NO_USB: // NO USB CONNECTED
                    Toast.makeText(context, "No USB connected", Toast.LENGTH_SHORT).show();
                    break;
                case UsbService.ACTION_USB_DISCONNECTED: // USB DISCONNECTED
                    Toast.makeText(context, "USB disconnected", Toast.LENGTH_SHORT).show();
                    break;
                case UsbService.ACTION_USB_NOT_SUPPORTED: // USB NOT SUPPORTED
                    Toast.makeText(context, "USB device not supported", Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };

    /**
     * The service connection object will form the connection between the activity and the UsbService
     * It will call the onBind method and receives the IBinder object returned from it as arg1.
     * Then the usbService reference is cast from the Binder class's getUsbService method.
     */
    private final ServiceConnection usbConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName arg0, IBinder arg1) {
            usbService = ((UsbService.UsbBinder) arg1).getUsbService();
            usbService.setHandler(mHandler);
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            usbService = null;
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mHandler = new MyHandler(this);
        //Initialize the widgets
        fileNameText = (EditText) findViewById(R.id.fileNameView);
        display = (TextView) findViewById(R.id.data);

    }

    @Override
    public void onResume() {
        super.onResume();
        setFilters();  // Start listening notifications from UsbService
    }

    @Override
    public void onPause() {
        super.onPause();

        if(UsbService.SERVICE_CONNECTED){
            unregisterReceiver(mUsbReceiver);
            unbindService(usbConnection);
        }
    }

    /**
     * This method is called when the start button is pressed. It checks whether the EditText
     * has any text inside it. If it does, it adds a .txt extension to it, adds it to a Bundle
     * and starts the service, with this bundle passed as an argument.
     */
    public void onClickStart(View view){
        String fileName = fileNameText.getText().toString();
        if(fileName.isEmpty() || fileName.equals("")){
            AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).create();
            alertDialog.setTitle("Oops");
            alertDialog.setMessage("Please eneter a name for the file to save sensor data to.");
            alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
            alertDialog.show();
        }else{
            fileName.trim();
            fileName += ".txt";
            Bundle fileNameExtra = new Bundle();
            fileNameExtra.putString("file_name",fileName);
            Log.d("onClick bundle",fileNameExtra.getString("file_name"));
            startService(UsbService.class, usbConnection, fileNameExtra); // Start UsbService(if it was not started before) and Bind it
        }
    }


    /**
     * This method is intended to start the service
     * @param service - the .class of the service to be started
     * @param serviceConnection - the service connection object
     * @param extras - any extras that the service might need
     */
    private void startService(Class<?> service, ServiceConnection serviceConnection, Bundle extras) {
        if (!UsbService.SERVICE_CONNECTED) {
            Intent startService = new Intent(this, service);
            if (extras != null && !extras.isEmpty()) {
                Set<String> keys = extras.keySet();
                for (String key : keys) {
                    String extra = extras.getString(key);
                    startService.putExtra(key, extra);
                }
            }
            startService(startService);
        }
        Intent bindingIntent = new Intent(this, service);
        bindService(bindingIntent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    /**
     * Sets the intent filters for the broadcast receiver
     */
    private void setFilters() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(UsbService.ACTION_USB_PERMISSION_GRANTED);
        filter.addAction(UsbService.ACTION_NO_USB);
        filter.addAction(UsbService.ACTION_USB_DISCONNECTED);
        filter.addAction(UsbService.ACTION_USB_NOT_SUPPORTED);
        filter.addAction(UsbService.ACTION_USB_PERMISSION_NOT_GRANTED);
        registerReceiver(mUsbReceiver, filter);
    }


    /**----------------------------------------------------------------------------------------*/

    /**
     * This handler will be passed to UsbService. Data received from serial port is displayed through this handler
     */
    private static class MyHandler extends Handler {
        private final WeakReference<MainActivity> mActivity;

        public MyHandler(MainActivity activity) {
            mActivity = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            if (msg.what ==UsbService.MESSAGE_FROM_SERIAL_PORT ) {
                String data = (String) msg.obj;
                Log.d("handleMessage",data);
                mActivity.get().display.setText(data);

            }
        }
    }
}

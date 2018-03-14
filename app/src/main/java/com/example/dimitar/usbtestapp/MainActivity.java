package com.example.dimitar.usbtestapp;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Color;
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

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.ValueDependentColor;
import com.jjoe64.graphview.series.BarGraphSeries;
import com.jjoe64.graphview.series.DataPoint;

import java.lang.ref.WeakReference;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Set;

public class MainActivity extends AppCompatActivity {


    private UsbService usbService;
    private TextView display;
    private EditText fileNameText;
    private MyHandler mHandler;
    private GraphView graph;
    private BarGraphSeries<DataPoint> dataSeries;

    /**
     * Receive the UsbService notifications and display a Toast message to inform the current
     * status of the Usb
     */
    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case UsbService.ACTION_USB_PERMISSION_GRANTED: // USB PERMISSION GRANTED
                    displayToast(context, "USB Ready");
                    break;
                case UsbService.ACTION_USB_PERMISSION_NOT_GRANTED: // USB PERMISSION NOT GRANTED
                    displayToast(context, "USB Permission not granted");
                    break;
                case UsbService.ACTION_NO_USB: // NO USB CONNECTED
                    displayToast(context, "No USB connected");
                    break;
                case UsbService.ACTION_USB_DISCONNECTED: // USB DISCONNECTED
                    displayToast(context, "USB disconnected");
                    break;
                case UsbService.ACTION_USB_NOT_SUPPORTED: // USB NOT SUPPORTED
                    displayToast(context, "USB device not supported");
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
        graph = (GraphView) findViewById(R.id.graph);
        dataSeries = new BarGraphSeries<>();
        setGraphOptions();
        graph.addSeries(dataSeries);



    }

    @Override
    public void onResume() {
        super.onResume();
        setFilters();  // Start listening for notifications from UsbService
        startService(UsbService.class, usbConnection,null);

    }

    @Override
    public void onPause() {
        super.onPause();
        unregisterReceiver(mUsbReceiver);
        unbindService(usbConnection);

    }

    /**
     * A method that sets the graph properties. It also configres the graph
     * so that its color changes, based on the value appended to it
     */
    private void setGraphOptions(){
        // set manual X bounds
        graph.getViewport().setYAxisBoundsManual(true);
        graph.getViewport().setMinY(0);
        graph.getViewport().setMaxY(1024);

        graph.getViewport().setXAxisBoundsManual(true);
        graph.getViewport().setMinX(0);
        graph.getViewport().setMaxX(2);

        // enable scaling and scrolling
        graph.getViewport().setScalable(true);
        graph.getViewport().setScalableY(true);
        graph.getViewport().setScrollableY(true); // enables vertical scrolling

        dataSeries.setValueDependentColor(new ValueDependentColor<DataPoint>() {
            @Override
            public int get(DataPoint dataPoint) {
                if(dataPoint.getY() >= 800)
                    return Color.GREEN;
                else if(dataPoint.getY() < 800 && dataPoint.getY() >= 500)
                    return Color.YELLOW;
                else if(dataPoint.getY() <500 && dataPoint.getY() > 200)
                    return Color.rgb(255,69,0);
                else{
                    return Color.RED;
                }
            }
        });
    }

    /**
     * This method is called when the start button is pressed. It checks whether the EditText
     * has any text inside it. If it does, it adds a .txt extension to it, adds it to a Bundle
     * and starts the service, with this bundle passed as an argument.
     */
    public void onClickStart(View view){
        String fileName = fileNameText.getText().toString();
        if(fileName.isEmpty() || fileName.equals("")){
            displayToast(this,"Please provide a file name to save the sensor data");
        }else{
            fileName.trim();
            fileName += ".txt";
            Bundle fileNameExtra = new Bundle();
            fileNameExtra.putString("file_name",fileName);
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

    private void displayToast(Context context,String message){
        Toast.makeText(context,message,Toast.LENGTH_SHORT).show();
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
                String newData = (String) msg.obj;
                try {
                    int numData = NumberFormat.getInstance().parse(newData).intValue();
                    DataPoint dp = new DataPoint(2, numData);
                    Log.d("Data point X", String.valueOf(dp.getX()));
                    Log.d("Data point Y", String.valueOf(dp.getY()));
                    mActivity.get().dataSeries.appendData(dp,true,40);
                    Log.d("Appending to graph", newData.toString());

                }catch (ParseException e){
                    Log.d("Exception in service", e.getMessage());
                }

            }
        }
    }
}

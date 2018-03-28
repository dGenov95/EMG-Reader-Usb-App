package com.example.dimitar.usbtestapp;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.*;
import android.graphics.Color;
import android.os.*;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GridLabelRenderer;
import com.jjoe64.graphview.LegendRenderer;
import com.jjoe64.graphview.Viewport;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.lang.ref.WeakReference;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Set;

/**
 * The MainActivity class is the one responsible for the main screen shown to the user. It receives
 * user events, responds to them accordingly and shows the sensor data on the screen.
 * It contains an inner class, responsible for handling the data coming from the UsbService
 * and has all the methods that correspond to a certain user action (pressing buttons etc.)
 * Reference code:
 * https://github.com/felHR85/UsbSerial/blob/master/examplesync/src/main/java/com/felhr/serialportexamplesync/MainActivity.java
 */
public class MainActivity extends Activity {

    private UsbService usbService;
    private EditText fileNameText;
    private UsbHandler mHandler;
    private GraphView graph;
    private Button startButton,stopButton;
    private LineGraphSeries<DataPoint> dataSeries;
    private boolean canUnbind;
    //A variable to keep track of the last X point in the graph
    private double graphX;
    /**
     * Receive the UsbService notifications and display a Toast message to inform the current
     * status of the Usb
     * Reference code:
     * https://github.com/felHR85/UsbSerial/blob/master/examplesync/src/main/java/com/felhr/serialportexamplesync/MainActivity.java
     */
    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getAction() != null) {
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
        }
    };
    /**
     * The service connection object will form the connection between the activity and the UsbService
     * It will call the onBind method and receives the IBinder object returned from it as arg1.
     * Then the usbService reference is cast from the Binder class's getUsbService method.
     * Reference code:
     * https://github.com/felHR85/UsbSerial/blob/master/examplesync/src/main/java/com/felhr/serialportexamplesync/MainActivity.java
     */
    private final ServiceConnection usbConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName arg0, IBinder arg1) {
            Log.d("MainActivity", "onServiceConnected");
            usbService = ((UsbService.UsbBinder) arg1).getUsbService();
            usbService.setHandler(mHandler);
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            Log.d("MainActivity", "onServiceDisconnected");
            usbService = null;

        }
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mHandler = new UsbHandler(this);
        //Initialize the widgets
        setupWidgets();
        //Customize the graph
        setGraphOptions();

    }
    @Override
    public void onResume() {
        super.onResume();
        setFilters();  // Start listening for notifications from UsbService
    }
    @Override
    public void onStop() {
        super.onStop();
        //Stop receiving the sensor data
        unregisterReceiver(mUsbReceiver);
        unbindService();

    }
    private void setupWidgets(){
        //Initialize the widgets
        fileNameText = findViewById(R.id.fileNameView);
        graph = findViewById(R.id.graph);
        startButton = findViewById(R.id.startButton);
        stopButton = findViewById(R.id.stopButton);
        //Initialize the Graph Series and add them to the graph
        dataSeries = new LineGraphSeries<>();
        graph.addSeries(dataSeries);
        //Disable the stop button when the app is started
        stopButton.setEnabled(false);
    }
    /**
     * A method that sets the graph properties. It also configures the graph
     * so that its color changes, based on the value appended to it
     */
    private void setGraphOptions(){
        Viewport graphViewPort = graph.getViewport();
        LegendRenderer graphLegend = graph.getLegendRenderer();
        GridLabelRenderer graphGrid = graph.getGridLabelRenderer();
        //Set the graph's X axis
        graphX = System.currentTimeMillis()/ 1000.0;
        //Name the graph and the series
        graph.setTitle("Muscle Activity Graph");
        dataSeries.setTitle("Muscle Activity Level");
        //Set the view port options
        // Scaling and scrolling
        graphViewPort.setScalable(true);
        //X & Y axis options
        graphViewPort.setMaxY(0);
        graphViewPort.setMaxY(1024);
        graphViewPort.setYAxisBoundsManual(true);
        //Set background to black
        graphViewPort.setBackgroundColor(Color.BLACK);
        //Set the legend options
        //Show a legend at the top right
        graphLegend.setVisible(true);
        graphLegend.setAlign(LegendRenderer.LegendAlign.TOP);
        graphLegend.setTextColor(Color.WHITE);
        //Show the graph data in green
        dataSeries.setColor(Color.rgb(136, 255, 77));
        dataSeries.setDrawBackground(true);
        //Set the grid options
        graphGrid.setNumVerticalLabels(5);
        //graphGrid.setHorizontalLabelsVisible(false);
        graphGrid.setGridStyle(GridLabelRenderer.GridStyle.HORIZONTAL);
        graphGrid.setGridColor(Color.WHITE);


    }
    /**
     * This method is called when the start button is pressed. It checks whether the EditText
     * has any text inside it. If it does, it adds a .txt extension to it, adds it to a Bundle
     * and starts the service, with this bundle passed as an argument. Furthermore, it records the
     * current time of the system as seconds, to be the start point of the graph's X axis.
     */
    public void onButtonStartClick(View view){
        String fileName = fileNameText.getText().toString();
        if(fileName.isEmpty() || fileName.equals("")){
            displayToast(this,"Please provide a file name to save the sensor data");
        }else{
            fileName = fileName.trim();
            fileName += ".txt";
            Bundle fileNameExtra = new Bundle();
            fileNameExtra.putString("file_name",fileName);
            bindService(fileNameExtra); // Start UsbService(if it was not started before) and Bind it
        }
    }
    /**
     * When the Stop button is pressed, stop recording from the sensor
     * @param view - the button view reference
     */
    public void onButtonStopClick(View view){
        unbindService();
    }
    /**
     * A pop up window that shows the terms and conditions of the application. Activates when the user
     * clicks the Terms and Conditions text shown on the screen. Meanwhile, the applications continues
     * running in the background.
     * @param view - the corresponding view object of the TextView
     */
    public void showTsAndCs(View view) {
        //Check the Android version to determine what the dialog would look like
        AlertDialog.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder = new AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert);
        } else {
            builder = new AlertDialog.Builder(this);
        }
        //Set the UI options of the dialog
        builder.setTitle("Terms & Conditions")
                .setMessage("Text to be added...")
                .setPositiveButton("Proceed", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //Nothing happens when clicked
                    }
                })
                .setIcon(android.R.drawable.ic_dialog_info)
                .show();
    }
    /**
     * This method is intended to start the service. It checks if any extras are passed. If so, it
     * adds all of them to the binding intent and tries to bind to the service. If this is successful
     * a toast message is shown to the user to indicate that, the stop button is enabled to use and the
     * file name text disappears
     * @param extras - any extras that the service might need
     */
    private void bindService(Bundle extras) {

        Intent bindingIntent = new Intent(this, UsbService.class);
        if (extras != null && !extras.isEmpty()) {
            Set<String> keys = extras.keySet();
            for (String key : keys) {
                String extra = extras.getString(key);
                bindingIntent.putExtra(key, extra);
            }
        }
        if(bindService(bindingIntent, usbConnection, Context.BIND_AUTO_CREATE)){
            //Indicate that the service can now unbind
            canUnbind = true;
            //Set the widgets status
            if(!stopButton.isEnabled()){
                stopButton.setEnabled(true);
            }
            startButton.setEnabled(false);
            fileNameText.setVisibility(View.GONE);

            displayToast(this,"Starting Recording");
        }
    }
    /**
     * The method is used to safely unbind from an already bound service. It checks whether it is
     * already bound. If so, it unbinds from it and displays a toast message to the user. Otherwise
     * a toast message to indicate that the service is already unbound is shown.
     * Furthermore, the start button is disabled until the service is stopped with the stop button
     * and the text field for the file name appears.
     */
    private void unbindService(){
        if(canUnbind){
            //Indicate that the service is already unbound
            canUnbind = false;
            //Set the widgets status
            if(!startButton.isEnabled()){
                startButton.setEnabled(true);
            }
            fileNameText.setVisibility(View.VISIBLE);
            stopButton.setEnabled(false);
            unbindService(usbConnection);
            displayToast(this,"Stopping recording");
        }
    }
    /**
     * Sets the intent filters for the broadcast receiver
     * Reference code:
     * https://github.com/felHR85/UsbSerial/blob/master/examplesync/src/main/java/com/felhr/serialportexamplesync/MainActivity.java
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
    /**
     * A helper function to display a toast message.
     * @param context - the context on which to display it
     * @param message - the message to be displayed
     */
    private void displayToast(Context context,String message){
        Toast.makeText(context,message,Toast.LENGTH_SHORT).show();
    }
    /**
     * This handler will be passed to UsbService. Data received from serial port is displayed through
     * this handler.
     * Reference Code:
     * https://github.com/felHR85/UsbSerial/blob/master/examplesync/src/main/java/com/felhr/serialportexamplesync/MainActivity.java
     */
    private static class UsbHandler extends Handler {
        private final WeakReference<MainActivity> mActivity;
        /**
         * The constructor of the Handler object.
         * @param activity - the reference to the MainActivity class
         */
        UsbHandler(MainActivity activity) {
            mActivity = new WeakReference<>(activity);
        }
        /**
         * This method executes every time new data is passed to the handler from the UsbService
         * @param msg - the message sent from the UsbService
         */
        @Override
        public void handleMessage(Message msg) {
            //Get the new X axis to be added to the graph - subtract the current time in seconds
            // from the initial X time
            double newGraphX = System.currentTimeMillis()/ 1000.0 - mActivity.get().graphX;
            //Check if the message is the one from the serial port
            if (msg.what == UsbService.MESSAGE_FROM_SERIAL_PORT ) {
                //Get the string object of it
                String newData = (String) msg.obj;
                try {
                    //Try to parse it as an Integer
                    int graphY = NumberFormat.getInstance().parse(newData).intValue();
                    //Create a new graph data point with the X axis set to the new X
                    DataPoint dp = new DataPoint(newGraphX, graphY);
                    Log.d("Data point X", String.valueOf(dp.getX()));
                    Log.d("Data point Y", String.valueOf(dp.getY()));
                    //Get the series reference from the activity and append the data to it
                    mActivity.get().dataSeries.appendData(dp,true,40);
                    Log.d("Appending to graph", newData);

                }catch (ParseException e){
                    Log.d("Exception in service", e.getMessage());
                }

            }
        }
    }
}

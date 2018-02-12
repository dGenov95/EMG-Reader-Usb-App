package com.example.dimitar.usbtestapp;

import android.os.Environment;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class SensorDataWriter {

    private File mFile;

    public SensorDataWriter(String mFileName){
        mFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),mFileName);
    }


    public  void writeToFile(String mData){
        if(isExternalStorageWritable()){

            try{
                BufferedWriter buf = new BufferedWriter(new FileWriter(mFile,true));

                Log.d("Writing data to file", mData);
                buf.append(mData);
                buf.close();
            }
            catch (IOException e){
                Log.d("EXCEPTION WHEN WRITING", e.getMessage());
                e.printStackTrace();
            }
        }
    }

    /* Checks if external storage is available for read and write */
    private boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }
}

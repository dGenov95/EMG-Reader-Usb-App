package com.example.dimitar.usbtestapp;

import android.os.Environment;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * This is a helper class, used to write data to a new file. The destination folder is the Downloads
 * folder of the external device memory.
 * The class is instantiated when given a file name. It creates a new File object within the above
 * specified directory, and when its method to write to the contents of that file is called it appends
 * the passed data to it.
 */
public class SensorDataWriter {

    private File mFile;

    public SensorDataWriter(String mFileName){
        mFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),mFileName);
    }
    /**
     * This method writes data to the File object. It uses a BufferedWriter and appends the passed
     * data to the previous content written in the File.
     * @param mData - the data passed for writing
     */
    public  void writeToFile(String mData){
        if(isExternalStorageWritable()){
            try{
                //A BufferedWriter object is created. A FileWriter of the File that is going to be
                // written is passed to its constructor, specifying that the data is going to be
                // appended and not overwritten.
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
    /**
     *  Checks if external storage is available for read and write
     */
    private boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }
}

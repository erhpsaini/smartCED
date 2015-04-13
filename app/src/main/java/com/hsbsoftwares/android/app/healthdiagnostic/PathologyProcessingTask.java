package com.hsbsoftwares.android.app.healthdiagnostic;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.hsbsoftwares.android.app.healthdiagnostic.nativepack.NativeMethods;

import java.util.ArrayList;

/**
 * AsyncTask for doing processing in the background and give feedback to the UI thread
 */
public class PathologyProcessingTask extends AsyncTask<ArrayList<Integer>, Void, Integer> {

    private static final String TAG = "PathologyProcessingTask";

    private static Context mContext;

    //For emergency alarm
    private IEmergencyAlarmListener mEmergencyAlarmListener;

    //Used to call native method for processing
    private NativeMethods mNativeMethods;

    //Constructor
    public PathologyProcessingTask(Context context) {
        this.mContext = context;
        mNativeMethods = new NativeMethods();
    }

    //Processing in the background
    @Override
    protected Integer doInBackground(ArrayList<Integer>... params) {
        Log.d(TAG, "Executing doInBackground...");
        //Preparing a simple lumVector from ArrayList
        int lumVectorSize = params[0].size();

        Log.i(TAG, "Lum Vector size is " + lumVectorSize);
        Log.i(TAG, "Lum Vector elements are:");

        int[] lumVector = new int[lumVectorSize];
        for(int i = 0; i < lumVectorSize; i++){
            lumVector[i] = params[0].get(i);
            Log.i(TAG, "[" + i + "]" + " = " + lumVector[i]);
        }

        //Processing with native method (C) and returning its result
        return mNativeMethods.pathologyDetection(lumVector);
    }

    @Override
    protected void onPreExecute() {
        Log.d(TAG, "onPreExecute called.");
    }

    @Override
    protected void onPostExecute(Integer isEmergency) {
        Log.d(TAG, "Executing onPostExecute...");
        //If the native call detects the crisis and if the listener is set
        //listener is notified so the callback is triggered!
        if(isEmergency == 1 && mEmergencyAlarmListener != null){
            mEmergencyAlarmListener.onEmergency();
        }
        Log.d(TAG, "Done with task. Return value: " + isEmergency);
    }

    //Listener setter
    public void setmEmergencyAlarmListener(IEmergencyAlarmListener mEmergencyAlarmListener) {
        this.mEmergencyAlarmListener = mEmergencyAlarmListener;
    }
}

package com.hsbsoftwares.android.app.healthdiagnostic;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.hsbsoftwares.android.app.healthdiagnostic.nativepack.NativeMethods;

import java.util.ArrayList;

/**
 * Created by Harpreet Singh Bola on 21/03/2015.
 */
public class PathologyProcessingTask extends AsyncTask<ArrayList<Integer>, Void, Integer> {

    private static final String TAG = "PathologyProcessingTask";

    private static Context mContext;

    private IEmergencyAlarmListener mEmergencyAlarmListener;

    private NativeMethods mNativeMethods;

    public PathologyProcessingTask(Context context) {
        this.mContext = context;
        mNativeMethods = new NativeMethods();
    }

    @Override
    protected Integer doInBackground(ArrayList<Integer>... params) {
        Log.d(TAG, "Executing doInBackground...");

        int lumVectorSize = params[0].size();

        Log.i(TAG, "Lum Vector size is " + lumVectorSize);
        Log.i(TAG, "Lum Vector elements are:");

        int[] lumVector = new int[lumVectorSize];
        for(int i = 0; i < lumVectorSize; i++){
            lumVector[i] = params[0].get(i);
            Log.i(TAG, "[" + i + "]" + " = " + lumVector[i]);
        }

        return mNativeMethods.pathologyDetection(lumVector);
    }

    @Override
    protected void onPreExecute() {
        Log.d(TAG, "onPreExecute called.");
    }

    @Override
    protected void onPostExecute(Integer isEmergency) {
        Log.d(TAG, "Executing onPostExecute...");
        if(isEmergency == 1 && mEmergencyAlarmListener != null){
            mEmergencyAlarmListener.onEmergency();
        }
        Log.d(TAG, "Done with task. Return value: " + isEmergency);
    }

    public void setmEmergencyAlarmListener(IEmergencyAlarmListener mEmergencyAlarmListener) {
        this.mEmergencyAlarmListener = mEmergencyAlarmListener;
    }
}

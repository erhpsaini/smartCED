package com.hsbsoftwares.android.app.healthdiagnostic.nativepack;

/**
 * Created by Harpreet Singh Bola on 20/03/2015.
 */
public class NativeMethods {
    //Constructor
    public NativeMethods(){
    }

    //Native method for detecting Epilepsy and Clonic crisis
    public native int pathologyDetection(int[] lumVector);
}

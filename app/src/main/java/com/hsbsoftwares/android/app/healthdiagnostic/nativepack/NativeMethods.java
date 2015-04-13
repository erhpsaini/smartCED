package com.hsbsoftwares.android.app.healthdiagnostic.nativepack;


public class NativeMethods {
    //Constructor
    public NativeMethods(){
    }

    //Native method for detecting Epilepsy and Clonic crisis
    public native int pathologyDetection(int[] lumVector);
}

package com.hsbsoftwares.android.app.healthdiagnostic;


//Interface: Observer pattern for creating custom listener for listening to crisis alarm
public interface IEmergencyAlarmListener {

    //Callback method that will be triggered on emergency occurred
    public void onEmergency();
}

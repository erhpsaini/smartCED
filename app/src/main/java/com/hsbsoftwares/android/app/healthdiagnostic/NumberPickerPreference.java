package com.hsbsoftwares.android.app.healthdiagnostic;

import android.content.Context;
import android.content.DialogInterface;
import android.content.res.TypedArray;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.NumberPicker;

//Number picker used to select threshold value
public class NumberPickerPreference extends DialogPreference {

    NumberPicker picker;
    Integer initialValue;

    //Constructor
    public NumberPickerPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);
        this.picker = (NumberPicker)view.findViewById(R.id.pref_num_picker);
        picker.setMaxValue(255);
        picker.setMinValue(0);
        if ( this.initialValue != null ) picker.setValue(initialValue);
    }
    @Override
    public void onClick(DialogInterface dialog, int which) {
        super.onClick(dialog, which);
        //If user confirms save the new value
        if ( which == DialogInterface.BUTTON_POSITIVE ) {
            this.initialValue = picker.getValue();
            persistInt( initialValue );
            callChangeListener( initialValue );
        }
    }
    @Override
    protected void onSetInitialValue(boolean restorePersistedValue,
                                     Object defaultValue) {
        int def = ( defaultValue instanceof Number ) ? (Integer)defaultValue
                : ( defaultValue != null ) ? Integer.parseInt(defaultValue.toString()) : 1;
        if ( restorePersistedValue ) {
            this.initialValue = getPersistedInt(def);
        }
        else this.initialValue = (Integer)defaultValue;
    }
    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return a.getInt(index, 1);
    }
}

package com.hsbsoftwares.android.app.healthdiagnostic;

import android.content.Context;
import android.hardware.Camera;
import android.util.AttributeSet;
import android.util.Log;

import org.opencv.android.JavaCameraView;

import java.util.List;

/**
 * Created by Harpreet Singh Bola on 24/02/2015.
 */
public class CameraView extends JavaCameraView {

    private static final String TAG = "CameraView";

    // The chosen resolution will be stored in mSize.
    Camera.Size mSize;

    public CameraView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public List<Camera.Size> getResolutionList() {
        return mCamera.getParameters().getSupportedPreviewSizes();
    }

    public void setResolution(Camera.Size resolution) {
        disconnectCamera();
        mMaxHeight = resolution.height;
        mMaxWidth = resolution.width;
        connectCamera(getWidth(), getHeight());
    }

    public Camera.Size getResolution() {
        return mCamera.getParameters().getPreviewSize();
    }

    /*
     *Method to set the base resolution to work with to have good performances.
     */
    public void setBaseResolution() {
        Camera.Parameters params = mCamera.getParameters();

        // Check what resolutions are supported by your camera
        List<Camera.Size> sizes = params.getSupportedPreviewSizes();

        // Iterate through all available resolutions and choose one.
        for (Camera.Size size : sizes) {
            Log.i(TAG, "Available resolution: " + size.width + " " + size.height);
            if (size.width == 640 && size.height == 480) {
                mSize = size;
                break;
            }
        }

        disconnectCamera();
        mMaxHeight = mSize.height;
        mMaxWidth = mSize.width;
        connectCamera(getWidth(), getHeight());
        //Log.i(TAG, "Chosen resolution: "+mSize.width+" "+mSize.height);
        //params.setPictureSize(mSize.width, mSize.height);
        //mCamera.setParameters(params);
    }

    public Camera getCamera(){
        return mCamera;
    }
}

package com.hsbsoftwares.android.app.healthdiagnostic;

import android.util.Log;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

/**
 * Created by Harpreet Singh Bola on 25/02/2015.
 */
public class MotionDetection {

    private static final String TAG = "MotionDetection";

    /*
    * Algorithm to detect motion using absdiff().
    */
    public Mat detectMotion(final CameraBridgeViewBase.CvCameraViewFrame inputFrame, Mat previousFrame, Mat resultFrame, boolean firstTime ){
        //Getting the current frame in gray.
        final Mat currentGrayFrame = inputFrame.gray();

        if(firstTime){
    		/*
    		 * The first time i receive the frame it should also be a previous.
    		 */
            currentGrayFrame.copyTo(previousFrame);
            firstTime = false;
            Log.d(TAG, "First time processing: First frame set up as previous!");
        }

    	/*
    	 * Live video processing: Image processing to read the movement in the image.
    	 */
        //Calculating the absolute difference between the current and previous frame.
        Core.absdiff(currentGrayFrame, previousFrame, resultFrame);
        //Image threshold: all the pixel values greater than 60 are converted to white (255) to better enhance the movement in the image.
        Imgproc.threshold(resultFrame, resultFrame, 60, 255, Imgproc.THRESH_BINARY);
        //saving the current frame as previous.
        currentGrayFrame.copyTo(previousFrame);
        //Realising Mat to avoid memory leaks
        currentGrayFrame.release();

        return resultFrame;
    }
}

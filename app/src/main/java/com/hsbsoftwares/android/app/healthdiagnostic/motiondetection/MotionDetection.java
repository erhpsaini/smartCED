package com.hsbsoftwares.android.app.healthdiagnostic.motiondetection;

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
        //releasing Mat to avoid memory leaks
        currentGrayFrame.release();

        return resultFrame;
    }

   /*
    * Algorithm to detect motion using last three frames, this technique is called Double Diff.
    * With this method the shadows created by the movement is deleted.
    */
    public Mat detectMotion2(final CameraBridgeViewBase.CvCameraViewFrame inputFrame, Mat previousFrame, Mat thirdLastFrame, Mat resultFrame, boolean firstTime, boolean secondTime ){
        //Getting the current frame in gray.
        final Mat currentGrayFrame = inputFrame.gray();
        Mat differenceFrame = new Mat(currentGrayFrame.size(), currentGrayFrame.type());

        if(firstTime){
    		/*
    		 * The first time i receive the frame it should also be a previous and as last third.
    		 */
            currentGrayFrame.copyTo(previousFrame);
            currentGrayFrame.copyTo(thirdLastFrame);
            firstTime = false;
            Log.d(TAG, "First time processing: First frame set up as previous and third frame!");
        }

        if(secondTime){
    		/*
    		 * The second time the second frame should be set as third
    		 */
            previousFrame.copyTo(thirdLastFrame);
            secondTime = false;
        }

        //Calculating the absolute difference between the current and previous frame.
        Core.absdiff(thirdLastFrame, previousFrame, differenceFrame);//The difference between previousFrame and thirdLastFrame stored in differenceFrame.
        //Image threshold: all the pixel values greater than 60 are converted to white (255) to better enhance the movement in the image.
        Imgproc.threshold(differenceFrame, differenceFrame, 60, 255, Imgproc.THRESH_BINARY);

        //Calculating the absolute difference between the current and previous frame.
        Core.absdiff(previousFrame, currentGrayFrame, resultFrame);// Using resultFrame to store difference to avoid using unnecessary memory.
        //Image threshold: all the pixel values greater than 60 are converted to white (255) to better enhance the movement in the image.
        Imgproc.threshold(resultFrame, resultFrame, 60, 255, Imgproc.THRESH_BINARY);

        //Doing bitwise and between two differences to optimize the motion detection algorithm, in this way we are avoiding
        //the shadows/contrast created by movement
        Core.bitwise_and(differenceFrame, resultFrame, resultFrame);
        //Imgproc.threshold(mResultRgba, mResultRgba, 60, 255, Imgproc.THRESH_BINARY); // Doing something different??

        ////saving the previous frame as last of three recent frames.
        previousFrame.copyTo(thirdLastFrame);
        //saving the current frame as previous.
        currentGrayFrame.copyTo(previousFrame);

        //Releasing memory
        currentGrayFrame.release();
        differenceFrame.release();

        return resultFrame;
    }
}

package com.hsbsoftwares.android.app.healthdiagnostic.motiondetection;

import android.util.Log;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

/**
 * Class dedicated for motion detection algorithms using gray frames with one channel.
 */
public class MotionDetection {

    private static final String TAG = "MotionDetection";

    private static boolean  mFirstTime = true;
    private static boolean  mSecondTime = true;

    private static Mat  mPreviousFrame;
    private static Mat  mLastThirdFrame;
    private static Mat  mResultFrame;

    public MotionDetection(int frameWidth, int frameHeight, int frameType){
        mPreviousFrame = new Mat(frameWidth, frameHeight, frameType);
        mLastThirdFrame = new Mat(frameWidth, frameHeight, frameType);
        mResultFrame = new Mat(frameWidth, frameHeight, frameType);
    }


    public static void setmFirstTime(boolean mFirstTime) {
        MotionDetection.mFirstTime = mFirstTime;
    }

    public static void setmSecondTime(boolean mSecondTime) {
        MotionDetection.mSecondTime = mSecondTime;
    }

    /*
     * Algorithm to detect motion using absdiff().
     */
    public Mat detectMotion(final Mat currentGrayFrame){

        if(mFirstTime){
    		/*
    		 * The first time i receive the frame it should also be a previous.
    		 */
            currentGrayFrame.copyTo(mPreviousFrame);
            mFirstTime = false;
            Log.d(TAG, "First time processing: First frame set up as previous!");
        }

    	/*
    	 * Live video processing: Image processing to read the movement in the image.
    	 */
        //Calculating the absolute difference between the current and previous frame.
        Core.absdiff(currentGrayFrame, mPreviousFrame, mResultFrame);
        //Image threshold: all the pixel values greater than 60 are converted to white (255) to better enhance the movement in the image.
        Imgproc.threshold(mResultFrame, mResultFrame, 60, 255, Imgproc.THRESH_BINARY);
        //saving the current frame as previous.
        currentGrayFrame.copyTo(mPreviousFrame);

        return mResultFrame;
    }

    /*
    * Algorithm to detect motion using last three frames, this technique is called Double Diff.
    * With this method the shadows created by the movement is deleted.
    */
    public Mat detectMotion2(final Mat currentGrayFrame){
        //Getting the current frame in gray.
        //final Mat currentGrayFrame = inputFrame.gray();
        Mat differenceFrame = new Mat(currentGrayFrame.size(), currentGrayFrame.type());

        if(mFirstTime){
    		/*
    		 * The first time i receive the frame it should also be a previous and as last third.
    		 */
            currentGrayFrame.copyTo(mPreviousFrame);
            currentGrayFrame.copyTo(mLastThirdFrame);
            mFirstTime = false;
            Log.d(TAG, "First time processing: First frame set up as previous and third frame!");
        }

        if(mSecondTime){
    		/*
    		 * The second time the second frame should be set as third
    		 */
            mPreviousFrame.copyTo(mLastThirdFrame);
            mSecondTime = false;
        }

        //Calculating the absolute difference between the current and previous frame.
        Core.absdiff(mLastThirdFrame, mPreviousFrame, differenceFrame);//The difference between previousFrame and thirdLastFrame stored in differenceFrame.
        //Image threshold: all the pixel values greater than 60 are converted to white (255) to better enhance the movement in the image.
        Imgproc.threshold(differenceFrame, differenceFrame, 60, 255, Imgproc.THRESH_BINARY);

        //Calculating the absolute difference between the current and previous frame.
        Core.absdiff(mPreviousFrame, currentGrayFrame, mResultFrame);// Using resultFrame to store difference to avoid using unnecessary memory.
        //Image threshold: all the pixel values greater than 60 are converted to white (255) to better enhance the movement in the image.
        Imgproc.threshold(mResultFrame, mResultFrame, 60, 255, Imgproc.THRESH_BINARY);

        //Doing bitwise and between two differences to optimize the motion detection algorithm, in this way we are avoiding
        //the shadows/contrast created by movement
        Core.bitwise_and(differenceFrame, mResultFrame, mResultFrame);
        //Imgproc.threshold(mResultRgba, mResultRgba, 60, 255, Imgproc.THRESH_BINARY); // Doing something different??

        ////saving the previous frame as last of three recent frames.
        mPreviousFrame.copyTo(mLastThirdFrame);
        //saving the current frame as previous.
        currentGrayFrame.copyTo(mPreviousFrame);

        //Releasing memory
        differenceFrame.release();

        return mResultFrame;
    }

    public void releaseMemory(){
        mPreviousFrame.release();
        mLastThirdFrame.release();
        mResultFrame.release();
    }
}

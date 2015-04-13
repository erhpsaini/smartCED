package com.hsbsoftwares.android.app.healthdiagnostic;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.gesture.GestureOverlayView;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.Toast;

import com.hsbsoftwares.android.app.healthdiagnostic.motiondetection.MotionDetection;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;


    public class CameraActivity extends Activity implements CameraBridgeViewBase.CvCameraViewListener2, View.OnTouchListener, IEmergencyAlarmListener, GestureOverlayView.OnGestureListener {

    private static final String TAG = "CameraActivity";

    //Static part of the activity/app which loads native (C) code
    static {
        try {
            System.loadLibrary("native_lib");
        } catch (UnsatisfiedLinkError e) {
            Log.e(TAG, "Failed to load native_lib");
        }
    }

    //App's context
    private static Context mContext;

    //Display size
    private static android.graphics.Point mDisplaySize;

    //Booleans used for app's state
    private static boolean mProcessingModeOn = false;
    private static boolean mMultipleViewModeOn = false;
    private static boolean mProcessButtonIsPressed = false;
    private static boolean mIsMaskCreationModeOn = false;
    private static boolean mIsMaskConfirmationHanging = false;
    //Boolean used for initial processing setup
    private static boolean mFirstTime = true;
    //Booleans used with timer
    private static boolean mTimerHasStarted = false;
    private static boolean mTimeOut = false;
    //Boolean used to activate/deactivate onTouch method's functionality
    private static boolean mIsSingleDiffDoubleDiffViewMode = false;

    //Base frame size
    private static final int BASE_FRAME_WIDTH = 640;
    private static final int BASE_FRAME_HEIGHT = 480;
    //Base frame type
    private static final int BASE_FRAME_TYPE = CvType.CV_8U;
    //Constants used with multiple view mode functionality
    private static final int RGBA_VIEW = 0;
    private static final int GRAY_SINGLE_DIFF_VIEW = 1;
    private static final int GRAY_DOUBLE_DIFF_VIEW = 2;
    private static final int SINGLE_DOUBLE_DIFF_VIEW = 3;
    //Timer constants
    private final long START_TIME = 10000;
    private final long INTERVAL = 1000;
    //Mask constants
    private static final int    CREATE_MASK = 0;
    private static final int    CLEAR_MASK = 1;

    //Timer used for collecting frames from camera for 10 seconds
    private static ProcessingCountDownTimer mMyCountDownTimer;

    //View mode value
    private static int mViewMode = RGBA_VIEW;

    //Camera and camera view: openCV
    private CameraView mOpenCvCameraView;

    //Mat for saving result frame
    private Mat mResultFrame;
    //Mat used in mask creation functionality
    private Mat mMask;

    //Motion detection class
    private static MotionDetection mMotionDetection;

    //Motion detection method, default is Single Diff
    private static String mMotionDetectionMethod = SettingsActivity.getDefaultMotionDetectionMethod();

    //Activity buttons
    private ImageButton mProcessButton;
    private ImageButton mEmergencyButton;
    private ImageButton mSettingsButton;
    private ImageButton mViewModeButton;
    private ImageButton mMaskButton;
    private ImageButton mDiscardMaskButton;
    private ImageButton mConfirmMaskButton;

    //Particular view used with mask creation functionality
    private static GestureOverlayView mGOV;

    //Used for saving touch coordinates
    private Point mPSX, mPDX;

    //ArrayList to store white pixels of frames
    private ArrayList<Integer> mLumArrayList;

    //For emergency sound alarm
    private static Ringtone mEmergencyRingtone;

    //Basic implementation of LoaderCallbackInterface
    //LoaderCallbackInterface: Interface for callback object in case of asynchronous initialization of OpenCV
    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");
                    mOpenCvCameraView.enableView();
                    mOpenCvCameraView.setOnTouchListener(CameraActivity.this);
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    public CameraActivity() {
        Log.i(TAG, "Instantiated new " + this.getClass());

        mLumArrayList = new ArrayList<Integer>();
        mPSX = new Point();
        mPDX = new Point();
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "called onCreate");

        super.onCreate(savedInstanceState);

        //Getting app's context for the future use
        mContext = getApplicationContext();

        //Setting Keep screen on when app is active
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // Set full screen view
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(R.layout.activity_camera);

        //Camera and camera view setup
        mOpenCvCameraView = (CameraView) findViewById(R.id.java_surface_view);
        //The base frame for us is 640x480 for processing performances reasons.
        mOpenCvCameraView.setMaxFrameSize(BASE_FRAME_WIDTH, BASE_FRAME_HEIGHT);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);

        mProcessButton          = (ImageButton) findViewById(R.id.processButton);
        mEmergencyButton        = (ImageButton) findViewById(R.id.emergencyButton);
        mSettingsButton         = (ImageButton) findViewById(R.id.settingsButton);
        mViewModeButton         = (ImageButton) findViewById(R.id.viewModeButton);
        mMaskButton             = (ImageButton) findViewById(R.id.maskButton);
        mDiscardMaskButton      = (ImageButton) findViewById(R.id.discardMaskButton);
        mConfirmMaskButton      = (ImageButton) findViewById(R.id.confirmMaskButton);

        mGOV = (GestureOverlayView)findViewById(R.id.gestureOverlayView);
        mGOV.addOnGestureListener(CameraActivity.this);

        //Getting display size and saving it in a member variable.
        Display display = getWindowManager().getDefaultDisplay();
        mDisplaySize = new android.graphics.Point();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            display.getSize(mDisplaySize);
        } else {
            mDisplaySize.x = display.getWidth();
            mDisplaySize.y = display.getHeight();
        }

        mMyCountDownTimer = new ProcessingCountDownTimer(START_TIME, INTERVAL);

        //Getting default alarm ringtone to be played when emergency occur
        try {
            mEmergencyRingtone = RingtoneManager.getRingtone(getApplicationContext(), RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onPause()
    {
        super.onPause();

        //Disabling camera view and releasing camera (important!!)
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();

        //Stop ringtone if app goes on pause
        if (mEmergencyRingtone.isPlaying())
            mEmergencyRingtone.stop();

        //Doing some stuff to have consistent behaviour of the app
        mProcessingModeOn = false;
        mProcessButtonIsPressed = false;
        mMyCountDownTimer.cancel();
        mTimeOut = false;
        mTimerHasStarted = false;
        mLumArrayList.clear();
        mProcessButton.setImageResource(R.drawable.start_processing_btn_img);
        mGOV.setVisibility(View.GONE);
    }

    @Override
    public void onResume()
    {
        super.onResume();
        //Asynchronous initialization of OpenCV
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_3, this, mLoaderCallback);

        //Resuming consistent state
        if(mIsMaskConfirmationHanging){
            mSettingsButton.setVisibility(View.GONE);
            mViewModeButton.setVisibility(View.GONE);
            mMaskButton.setVisibility(View.GONE);
            mProcessButton.setVisibility(View.GONE);
            mConfirmMaskButton.setVisibility(View.VISIBLE);
            mDiscardMaskButton.setVisibility(View.VISIBLE);
        }else if(mIsMaskCreationModeOn){
            mGOV.setVisibility(View.VISIBLE);
        }else {
            mSettingsButton.setVisibility(View.VISIBLE);
            mViewModeButton.setVisibility(View.VISIBLE);
            mMaskButton.setVisibility(View.VISIBLE);
        }
    }

    public void onDestroy() {
        super.onDestroy();

        //Disabling camera view and releasing camera (important!!)
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    public void onCameraViewStarted(int width, int height) {
        //Getting motion detection method to be used from shared preferences
        mMotionDetectionMethod = PreferenceManager.getDefaultSharedPreferences(getApplicationContext())
                .getString("motion_detection_algorithm_list", mMotionDetectionMethod);
        Log.i(TAG, "Motion detection method: " + mMotionDetectionMethod);

        mMotionDetection = mMotionDetection.getInstance(width, height, BASE_FRAME_TYPE);
        mMotionDetection.setmFirstTime(true);
        mMotionDetection.setmSecondTime(true);

        mFirstTime = true;
    }

    public void onCameraViewStopped() {
        Log.i(TAG, "Called onCameraViewStopped");
        //Avoiding memory leaks, important for openCV
        mMotionDetection.releaseMemory();
    }

    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        //Log.i(TAG, "Resolution using cols() & rows()" + String.valueOf(inputFrame.rgba().cols() + "x" + inputFrame.rgba().rows()));
        //Log.i(TAG, "Resolution using width() & height()" + String.valueOf(inputFrame.rgba().width() + "x" + inputFrame.rgba().height()));
        //Converting frame in to gray scale
        Mat currentGrayFrame = inputFrame.gray();
        if(mMultipleViewModeOn) {
            //If mMultipleViewModeOn show multiple view
            return createMultipleFrameView(currentGrayFrame, mViewMode);
        }else {
            //If mask is created apply it
            if (mMask != null) {
                Core.bitwise_and(currentGrayFrame, mMask, currentGrayFrame);
            }
            if(mProcessingModeOn) {//If mProcessingModeOn so go on and process
                //Detect motion with the selected algorithm
                if(mMotionDetectionMethod.equals("0")) {
                    mResultFrame = mMotionDetection.detectMotion(currentGrayFrame);
                }else {
                    mResultFrame = mMotionDetection.detectMotion2(currentGrayFrame);
                }

                //Start timer every 10 seconds to collect frames
                if(!mTimerHasStarted){
                    mTimerHasStarted = true;
                    mMyCountDownTimer.start();
                }

                //Collect frames for 10 seconds
                mLumArrayList.add(Core.countNonZero(mResultFrame));

                if(mTimeOut) {
                    ArrayList<Integer> lumArrayList = new ArrayList<Integer>(mLumArrayList);
                    //After the timeout reset the list
                    mLumArrayList.clear();
                    //Prepare and launch task avery 10 seconds
                    PathologyProcessingTask pathologyProcessingTask = new PathologyProcessingTask(mContext);
                    pathologyProcessingTask.setmEmergencyAlarmListener(this);
                    pathologyProcessingTask.execute(lumArrayList);
                    mTimeOut = false;
                }
            }else {//If processing mode is off simply show gray scale frames
                mResultFrame = currentGrayFrame;
            }

            return mResultFrame;
        }
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        Log.i(TAG, "onTouch event");
        //If mIsSingleDiffDoubleDiffViewMode is true activate touch for motion detection algorithm selection
        if(mIsSingleDiffDoubleDiffViewMode){
            int center = mDisplaySize.x/2;

            SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
            SharedPreferences.Editor editor = settings.edit();

            if(event.getX() < center){
                editor.putString("motion_detection_algorithm_list", "0");
                editor.commit();
                Toast.makeText(getApplicationContext(), "Single diff method selected!", Toast.LENGTH_SHORT).show();

            }else {
                editor.putString("motion_detection_algorithm_list", "1");
                editor.commit();
                Toast.makeText(getApplicationContext(), "Double diff method selected!", Toast.LENGTH_SHORT).show();
            }
        }

        return false;
    }

    /* Called when the user clicks the Settings button */
    public void openSettingsActivity(View view) {
        startActivity(new Intent(this, SettingsActivity.class));
    }

    public void onProcessButtonClicked(View view) {
        //Changing image resource on button clicked
        if(mProcessButtonIsPressed){
            mProcessButton.setImageResource(R.drawable.start_processing_btn_img);
        }else{
            mProcessButton.setImageResource(R.drawable.stop_processing_btn_img);
        }

        //Some stuff to have a consistent app state
        mProcessingModeOn = !mProcessingModeOn;
        mProcessButtonIsPressed = !mProcessButtonIsPressed;
        mMyCountDownTimer.cancel();
        mTimeOut = false;
        mTimerHasStarted = false;
        mLumArrayList.clear();

        if(mProcessingModeOn){
            mSettingsButton.setVisibility(View.GONE);
            mViewModeButton.setVisibility(View.GONE);
            mMaskButton.setVisibility(View.GONE);
        }else{
            mSettingsButton.setVisibility(View.VISIBLE);
            mViewModeButton.setVisibility(View.VISIBLE);
            mMaskButton.setVisibility(View.VISIBLE);
        }
    }

    public void onEmergencyButtonClicked(View view) {
        //When the button is clicked it goes off
        if(mEmergencyButton.getVisibility() == View.VISIBLE){
            mEmergencyButton.setVisibility(View.GONE);
        }
        //and ringtone playing is stopped
        if(mEmergencyRingtone.isPlaying()){
            mEmergencyRingtone.stop();
        }
    }

    /* Called when the user clicks the view mode button */
    public void showViewModePopupMenu(View view){
        final CharSequence[] items = getResources().getStringArray(R.array.view_mode_popup_list_values);

        AlertDialog.Builder builder = new AlertDialog.Builder(CameraActivity.this);
        builder.setTitle("Select view mode");
        builder.setItems(items, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                switch(which) {
                    case RGBA_VIEW:
                        mProcessButton.setVisibility(View.VISIBLE);
                        mMultipleViewModeOn = false;
                        mIsSingleDiffDoubleDiffViewMode = false;
                        mMaskButton.setEnabled(true);
                        Toast.makeText(getApplicationContext(), "Multiple view mode off.", Toast.LENGTH_SHORT).show();
                        break;
                    case GRAY_SINGLE_DIFF_VIEW:
                        mProcessButton.setVisibility(View.GONE);
                        mMultipleViewModeOn = true;
                        mViewMode = GRAY_SINGLE_DIFF_VIEW;
                        mMotionDetection.setmFirstTime(true);
                        mIsSingleDiffDoubleDiffViewMode = false;
                        mMaskButton.setEnabled(false);
                        Toast.makeText(getApplicationContext(), items[which] + " mode on.", Toast.LENGTH_SHORT).show();
                        break;
                    case GRAY_DOUBLE_DIFF_VIEW:
                        mProcessButton.setVisibility(View.GONE);
                        mMultipleViewModeOn = true;
                        mViewMode = GRAY_DOUBLE_DIFF_VIEW;
                        mMotionDetection.setmFirstTime(true);
                        mMotionDetection.setmSecondTime(true);
                        mIsSingleDiffDoubleDiffViewMode = false;
                        mMaskButton.setEnabled(false);
                        Toast.makeText(getApplicationContext(), items[which] + " mode on.", Toast.LENGTH_SHORT).show();
                        break;
                    case SINGLE_DOUBLE_DIFF_VIEW:
                        mProcessButton.setVisibility(View.GONE);
                        mMultipleViewModeOn = true;
                        mViewMode = SINGLE_DOUBLE_DIFF_VIEW;
                        mMotionDetection.setmFirstTime(true);
                        mMotionDetection.setmSecondTime(true);
                        mFirstTime = true;
                        mIsSingleDiffDoubleDiffViewMode = true;
                        mMaskButton.setEnabled(false);
                        Toast.makeText(getApplicationContext(), items[which] + " mode on.", Toast.LENGTH_SHORT).show();
                        break;
                }
            }
        });
        AlertDialog alert = builder.create();
        alert.show();
    }

    /* Called when the user clicks the mMask creation button */
    public void showMaskCreationPopupMenu(final View view){
        final CharSequence[] items = getResources().getStringArray(R.array.mask_creation_popup_list_values);

        AlertDialog.Builder builder = new AlertDialog.Builder(CameraActivity.this);
        builder.setTitle("What would you like to do?");
        builder.setItems(items, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                switch(which) {
                    case CREATE_MASK:
                        mIsMaskCreationModeOn = true;
                        if(mMask != null){
                            //Removing mask
                            mMask.release();
                            mMask = null;
                        }
                        mGOV.setVisibility(View.VISIBLE);
                        mProcessButton.setVisibility(View.GONE);
                        mSettingsButton.setVisibility(View.GONE);
                        mViewModeButton.setVisibility(View.GONE);
                        mMaskButton.setVisibility(View.GONE);
                        Toast.makeText(getApplicationContext(), "Create your mask.", Toast.LENGTH_SHORT).show();
                        break;
                    case CLEAR_MASK:
                        if(mMask != null){
                            //Removing mask
                            mMask.release();
                            mMask = null;
                        }
                        Toast.makeText(getApplicationContext(), "Mask cleared.", Toast.LENGTH_SHORT).show();
                        break;
                }
            }
        });
        AlertDialog alert = builder.create();
        alert.show();
    }

    public static Context getAppContext(){
        return mContext;
    }

    //Method for creating multiple view
    private static Mat createMultipleFrameView(final Mat currentGrayFrame, final int viewMode){
        //Making a copy of the frame
        Mat currentGrayFrameCopy = new Mat(currentGrayFrame.size(), currentGrayFrame.type());
        currentGrayFrame.copyTo(currentGrayFrameCopy);
        //Temporary frames
        Mat processedFrameSingleDiff;
        Mat processedFrameDoubleDiff;
        //Temporary right and left sub frames
        Mat subSx = currentGrayFrame.submat(0, currentGrayFrame.rows(), 0, currentGrayFrame.cols() / 2);
        Mat subDx = currentGrayFrame.submat(0, currentGrayFrame.rows(), currentGrayFrame.cols()/2, currentGrayFrame.cols());

        //Getting sub frames size
        Size subSxSize = subSx.size();
        Size subDxSize = subDx.size();

        //Resizing currentGrayFrame to adapt the left part of the original frame
        Imgproc.resize(currentGrayFrame, subSx, subSxSize);

        switch(viewMode) {
            default:
                subSx.release();
                subDx.release();
                currentGrayFrameCopy.release();
                break;
            case GRAY_SINGLE_DIFF_VIEW:
                //Processing the frame
                processedFrameSingleDiff = mMotionDetection.detectMotion(currentGrayFrameCopy);
                //and resizing it to adapt the right part of the original frame
                Imgproc.resize(processedFrameSingleDiff, subDx, subDxSize);
                //Drawing rectangle on the right side
                Core.rectangle(subDx, new Point(1, 1), new Point(subDxSize.width - 2, subDxSize.height - 2), new Scalar(255, 0, 0, 255), 2);
                processedFrameSingleDiff.release();
                subSx.release();
                subDx.release();
                currentGrayFrameCopy.release();
                break;
            case GRAY_DOUBLE_DIFF_VIEW:
                //Processing the frame
                processedFrameDoubleDiff = mMotionDetection.detectMotion2(currentGrayFrameCopy);
                //and resizing it to adapt the right part of the original frame
                Imgproc.resize(processedFrameDoubleDiff, subDx, subDxSize);
                //Drawing rectangle on the right side
                Core.rectangle(subDx, new Point(1, 1), new Point(subDxSize.width - 2, subDxSize.height - 2), new Scalar(255, 0, 0, 255), 2);
                processedFrameDoubleDiff.release();
                subSx.release();
                subDx.release();
                currentGrayFrameCopy.release();
                break;
            case SINGLE_DOUBLE_DIFF_VIEW:
                //Processing the frame
                processedFrameSingleDiff = mMotionDetection.detectMotion(currentGrayFrameCopy);
                //and resizing it to adapt the left part of the original frame
                Imgproc.resize(processedFrameSingleDiff, subSx, subSxSize);
                if(mFirstTime) {
                    mMotionDetection.setmFirstTime(true);
                    mFirstTime = false;
                }
                //Processing the frame
                processedFrameDoubleDiff = mMotionDetection.detectMotion2(currentGrayFrameCopy);
                //and resizing it to adapt the right part of the original frame
                Imgproc.resize(processedFrameDoubleDiff, subDx, subDx.size());
                //Drawing rectangle on the left and right side
                Core.rectangle(subSx, new Point(1, 1), new Point(subSxSize.width - 2, subSxSize.height - 2), new Scalar(255, 0, 0, 255), 2);
                Core.rectangle(subDx, new Point(1, 1), new Point(subDxSize.width - 2, subDxSize.height - 2), new Scalar(255, 0, 0, 255), 2);
                processedFrameSingleDiff.release();
                processedFrameDoubleDiff.release();
                subSx.release();
                subDx.release();
                currentGrayFrameCopy.release();
                break;
        }

        return currentGrayFrame;

    }

    private void createMask(Point pSX, Point pDX) {
        //User selected rectangle zone
        Rect sel = new Rect(pSX, pDX);

        //Creating mask
        mMask = Mat.zeros(mResultFrame.size(), mResultFrame.type());
        mMask.submat(sel).setTo(Scalar.all(255));
    }

    @Override
    public void onGestureStarted(GestureOverlayView overlay, MotionEvent event) {
        int cols = mResultFrame.cols();
        int rows = mResultFrame.rows();
        //Second mapping formula...working better! :)
        int width = (int)mDisplaySize.x;
        int height = (int)mDisplaySize.y;
        //Formula:
        mPSX.x = ((int)event.getX()*cols)/width;
        mPSX.y = ((int)event.getY()*rows)/height;
    }

    @Override
    public void onGesture(GestureOverlayView overlay, MotionEvent event) {

    }

    @Override
    public void onGestureEnded(GestureOverlayView overlay, MotionEvent event) {
        mIsMaskCreationModeOn = false;
        int cols = mResultFrame.cols();
        int rows = mResultFrame.rows();
        //Second mapping formula...working better! :)
        int width = (int)mDisplaySize.x;
        int height = (int)mDisplaySize.y;
        //Formula:
        mPDX.x = ((int)event.getX()*cols)/width;
        mPDX.y = ((int)event.getY()*rows)/height;

        //Checking if the two points can be used to have an rectangle on the frame
        if (mPSX.x < 0 || mPSX.y <0|| mPSX.x >= cols || mPSX.y >= rows
                || mPDX.x < 0 || mPDX.y <0|| mPDX.x >= cols || mPDX.y >= rows){
            Toast.makeText(getApplicationContext(), "Mask creation failed! Please retry.", Toast.LENGTH_SHORT);

            mGOV.setVisibility(View.GONE);
            mProcessButton.setVisibility(View.VISIBLE);
            mSettingsButton.setVisibility(View.VISIBLE);
            mViewModeButton.setVisibility(View.VISIBLE);
            mMaskButton.setVisibility(View.VISIBLE);


        }else{
            //If everything is ok then create the mask
            createMask(mPSX, mPDX);

            mGOV.setVisibility(View.GONE);
            mDiscardMaskButton.setVisibility(View.VISIBLE);
            mConfirmMaskButton.setVisibility(View.VISIBLE);
            mIsMaskConfirmationHanging = true;
        }
    }

    @Override
    public void onGestureCancelled(GestureOverlayView overlay, MotionEvent event) {

    }

    @Override
    public void onEmergency() {
        //When emergency occur show the emergency button
        if(mEmergencyButton.getVisibility() == View.GONE){
            mEmergencyButton.setVisibility(View.VISIBLE);
        }
        //and play the sound
        if(!mEmergencyRingtone.isPlaying()){
            mEmergencyRingtone.play();
        }
    }

    public void discardMask(View view){
        if(mMask != null){
            //Removing mask
            mMask.release();
            mMask = null;
        }
        mDiscardMaskButton.setVisibility(View.GONE);
        mConfirmMaskButton.setVisibility(View.GONE);
        mProcessButton.setVisibility(View.VISIBLE);
        mSettingsButton.setVisibility(View.VISIBLE);
        mViewModeButton.setVisibility(View.VISIBLE);
        mMaskButton.setVisibility(View.VISIBLE);
        mIsMaskConfirmationHanging = false;
    }

    public void confirmMask(View view){
        mDiscardMaskButton.setVisibility(View.GONE);
        mConfirmMaskButton.setVisibility(View.GONE);
        mProcessButton.setVisibility(View.VISIBLE);
        mSettingsButton.setVisibility(View.VISIBLE);
        mViewModeButton.setVisibility(View.VISIBLE);
        mMaskButton.setVisibility(View.VISIBLE);
        mIsMaskConfirmationHanging = false;
    }

    public class ProcessingCountDownTimer extends CountDownTimer {

        public ProcessingCountDownTimer(long startTime, long interval){
            super(startTime, interval);
        }

        @Override
        public void onTick(long millisUntilFinished) {
            Log.i(TAG, "" + (int) (millisUntilFinished/1000));
        }

        @Override
        public void onFinish() {
            mTimerHasStarted = false;
            mTimeOut = true;
            Log.i(TAG, "Done with timer!");
        }
    }

}

package com.hsbsoftwares.android.app.healthdiagnostic;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;

import com.hsbsoftwares.android.app.healthdiagnostic.motiondetection.MotionDetection;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;

import java.util.List;

/**
 * Created by Harpreet Singh Bola on 24/02/2015.
 */
public class CameraActivity extends Activity implements CameraBridgeViewBase.CvCameraViewListener2, View.OnTouchListener {
    private static final String TAG = "CameraActivity";

    private static Context mContext;

    private static boolean mProcessingModeOn = false;

    private static int touchNumbers;
    private static final int BASE_FRAME_WIDTH = 640;
    private static final int BASE_FRAME_HEIGHT = 480;
    private static final int BASE_FRAME_TYPE = CvType.CV_8U;

    private CameraView mOpenCvCameraView;

    private Mat mResultFrame;
    private Mat mask;

    private MotionDetection mMotionDetection;

    private static String mMotionDetectionMethod = SettingsActivity.getDefaultMotionDetectionMethod();

    Rect sel = new Rect();

    //List for storing supported Fps range
    List<int[]> supportedPreviewFpsRange;

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");
                    mOpenCvCameraView.enableView();
                    mOpenCvCameraView.setOnTouchListener(CameraActivity.this);

                    //Getting supported Fps range
                    /*supportedPreviewFpsRange = mOpenCvCameraView.getSupportedPreviewFpsRange();
                    Log.i(TAG, "FpsRange size: " + supportedPreviewFpsRange.size());
                    for (ListIterator<int[]> iter = supportedPreviewFpsRange.listIterator(); iter.hasNext();) {
                        int[] element = iter.next();
                        Log.i(TAG, "MIN FPS: " + element[0] + " MAX FPS: " + element[1]);
                    }
                    //Setting the maximum range supported which is in the last position of the list
                    mOpenCvCameraView.setSupportedPreviewFpsRange(supportedPreviewFpsRange.get(supportedPreviewFpsRange.size()-1)[0],
                            supportedPreviewFpsRange.get(supportedPreviewFpsRange.size()-1)[1]);*/
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
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "called onCreate");

        super.onCreate(savedInstanceState);

        mContext = getApplicationContext();

        ImageButton processButton;

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // Set full screen view
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(R.layout.activity_camera);

        mOpenCvCameraView = (CameraView) findViewById(R.id.java_surface_view);
        //The base frame for us is 640x480 for processing performances reasons.
        mOpenCvCameraView.setMaxFrameSize(BASE_FRAME_WIDTH, BASE_FRAME_HEIGHT);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);

        processButton = (ImageButton) findViewById(R.id.processButton);
        processButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mProcessingModeOn = !mProcessingModeOn;
            }
        });

        //ArrayList<View> views = new ArrayList<View>();
        //views.add(findViewById(R.id.processButton));
        //views.add(findViewById(R.id.settingsButton));
        //mOpenCvCameraView.addTouchables(views);
    }

    @Override
    public void onPause()
    {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onResume()
    {
        super.onResume();
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_3, this, mLoaderCallback);
    }

    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    public void onCameraViewStarted(int width, int height) {
        //Getting supported Fps range
        /*supportedPreviewFpsRange = mOpenCvCameraView.getSupportedPreviewFpsRange();
        Log.i(TAG, "FpsRange size: " + supportedPreviewFpsRange.size());
        for (ListIterator<int[]> iter = supportedPreviewFpsRange.listIterator(); iter.hasNext();) {
            int[] element = iter.next();
            Log.i(TAG, "MIN FPS: " + element[0] + " MAX FPS: " + element[1]);
        }
        //Setting the maximum range supported which is in the last position of the list
        mOpenCvCameraView.setSupportedPreviewFpsRange(supportedPreviewFpsRange.get(supportedPreviewFpsRange.size()-1)[0],
                supportedPreviewFpsRange.get(supportedPreviewFpsRange.size()-1)[1]);*/
        mMotionDetectionMethod = PreferenceManager.getDefaultSharedPreferences(getApplicationContext())
                .getString("motion_detection_algorithm_list", mMotionDetectionMethod);
        Log.i(TAG, "Motion detection method: " + mMotionDetectionMethod);

        mMotionDetection = mMotionDetection.getInstance(width, height, BASE_FRAME_TYPE);
        mMotionDetection.setmFirstTime(true);
        mMotionDetection.setmSecondTime(true);
    }

    public void onCameraViewStopped() {
        Log.i(TAG, "Called onCameraViewStopped");
        //Avoiding memory leaks.
        mMotionDetection.releaseMemory();
    }

    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        //Log.i(TAG, "Resolution using cols() & rows()" + String.valueOf(inputFrame.rgba().cols() + "x" + inputFrame.rgba().rows()));
        //Log.i(TAG, "Resolution using width() & height()" + String.valueOf(inputFrame.rgba().width() + "x" + inputFrame.rgba().height()));

        Mat currentGrayFrame = inputFrame.gray();

        if(mProcessingModeOn) {
            if(mMotionDetectionMethod.equals("0")) {
                mResultFrame = mMotionDetection.detectMotion(currentGrayFrame);
            }else {
                mResultFrame = mMotionDetection.detectMotion2(currentGrayFrame);
            }
        }else {
            mResultFrame = inputFrame.rgba();
            if (mask != null) {
                Core.bitwise_and(mResultFrame, mask, mResultFrame);
            }
        }
        //releasing Mat to avoid memory leaks
        currentGrayFrame.release();
        return mResultFrame;
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        Log.i(TAG, "onTouch event");

        //2 touch point mask
        touchNumbers++;

        if(touchNumbers != 3){
            int cols = mResultFrame.cols();
            int rows = mResultFrame.rows();

            int xOffset = (mOpenCvCameraView.getWidth() - cols) / 2;
            int yOffset = (mOpenCvCameraView.getHeight() - rows) / 2;

            int x = (int)event.getX() - xOffset;
            int y = (int)event.getY() - yOffset;
            if (x<0||y<0||x>=cols||y>=rows) return false;
            if ((sel.x==0 && sel.y==0) || (sel.width!=0 && sel.height!=0))
            {
                mask = null;
                sel.x=x; sel.y=y;
                sel.width = sel.height = 0;
            } else {
                sel.width = x - sel.x;
                sel.height = y - sel.y;
                if ( sel.width <= 0 || sel.height <= 0 ) { // invalid, clear it all
                    sel.x=sel.y=sel.width=sel.height = 0;
                    mask = null;
                    return false;
                }
                mask = Mat.zeros(mResultFrame.size(), mResultFrame.type());
                mask.submat(sel).setTo(Scalar.all(255));
            }
            Log.w("touch",sel.toString());
        }else{
            touchNumbers = 0;
            mask = null;
        }
        return false;
    }

    /** Called when the user clicks the Settings button */
    public void openSettingsActivity(View view) {
        startActivity(new Intent(this, SettingsActivity.class));
    }

    public static Context getAppContext(){
        return mContext;
    }
}

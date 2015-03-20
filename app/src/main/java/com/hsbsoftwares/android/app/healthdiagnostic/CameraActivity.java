package com.hsbsoftwares.android.app.healthdiagnostic;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
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
import java.util.List;

/**
 * Created by Harpreet Singh Bola on 24/02/2015.
 */
public class CameraActivity extends Activity implements CameraBridgeViewBase.CvCameraViewListener2, View.OnTouchListener {
    private static final String TAG = "CameraActivity";

    private static Context mContext;
    //Display size
    private static android.graphics.Point mDisplaySize;

    //private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private static boolean mProcessingModeOn = false;
    private static boolean mMultipleViewModeOn = false;
    private static boolean mFirstTime = true;
    //Booleans used with timer
    private static boolean mTimerHasStarted = false;
    private static boolean mIsCounting = false;

    private static int touchNumbers;
    private static final int BASE_FRAME_WIDTH = 640;
    private static final int BASE_FRAME_HEIGHT = 480;
    private static final int BASE_FRAME_TYPE = CvType.CV_8U;
    private static final int RGBA_VIEW = 0;
    private static final int GRAY_SINGLE_DIFF_VIEW = 1;
    private static final int GRAY_DOUBLE_DIFF_VIEW = 2;
    private static final int SINGLE_DOUBLE_DIFF_VIEW = 3;
    //Timer constants
    private final long START_TIME = 10000;
    private final long INTERVAL = 1000;

    private static ProcessingCountDownTimer MmyCountDownTimer;

    private static int mViewMode = RGBA_VIEW;

    private CameraView mOpenCvCameraView;

    private Mat mResultFrame;
    private Mat mask;

    private static MotionDetection mMotionDetection;

    private static String mMotionDetectionMethod = SettingsActivity.getDefaultMotionDetectionMethod();

    ImageButton mProcessButton;

    Rect sel = new Rect();

    //List for storing supported Fps range
    List <int[]> supportedPreviewFpsRange;
    //ArrayList to store white pixels of frames
    ArrayList <Integer> mLumArrayList;

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
        //paint.setColor(Color.GREEN);
        //paint.setStyle(Paint.Style.STROKE);
        mLumArrayList = new ArrayList<Integer>();
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "called onCreate");

        super.onCreate(savedInstanceState);

        mContext = getApplicationContext();

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

        mProcessButton = (ImageButton) findViewById(R.id.processButton);
        mProcessButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mProcessingModeOn = !mProcessingModeOn;
            }
        });

        //Getting display size and saving it in a member variable.
        Display display = getWindowManager().getDefaultDisplay();
        mDisplaySize = new android.graphics.Point();
        display.getSize(mDisplaySize);

        MmyCountDownTimer = new ProcessingCountDownTimer(START_TIME, INTERVAL);

        //ArrayList<View> views = new ArrayList<View>();
        //views.add(findViewById(R.id.mProcessButton));
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

        mFirstTime = true;
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
        if(mMultipleViewModeOn) {
            return createMultipleFrameView(currentGrayFrame, mViewMode);
        }else {
            if(mProcessingModeOn) {
                if(mMotionDetectionMethod.equals("0")) {
                    mResultFrame = mMotionDetection.detectMotion(currentGrayFrame);
                }else {
                    mResultFrame = mMotionDetection.detectMotion2(currentGrayFrame);
                }
                if(!mTimerHasStarted){
                    mTimerHasStarted = true;
                    MmyCountDownTimer.start();
                    mIsCounting = true;
                }
                if(mIsCounting){
                    mLumArrayList.add(mLumArrayList.size(), Core.countNonZero(mResultFrame));
                }else{
                    Log.i(TAG, "Size of Lum list is " + mLumArrayList.size());
                }
            }else {
                mResultFrame = currentGrayFrame;
                if (mask != null) {
                    Core.bitwise_and(mResultFrame, mask, mResultFrame);
                }
            }
            //releasing Mat to avoid memory leaks
            //currentGrayFrame.release();
            return mResultFrame;
        }
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        Log.i(TAG, "onTouch event");

        //2 touch point mask
        touchNumbers++;

        if(touchNumbers != 3){
            int cols = mResultFrame.cols();
            int rows = mResultFrame.rows();

            //Mapping formula from openCV's official example
            /*int xOffset = (mOpenCvCameraView.getWidth() - cols) / 2;
            int yOffset = (mOpenCvCameraView.getHeight() - rows) / 2;
            int x = (int)event.getX() - xOffset;
            int y = (int)event.getY() - yOffset;*/

            //Second mapping formula...working better! :)
            int width = (int)mDisplaySize.x;
            int height = (int)mDisplaySize.y;
            //Formula.
            int x = ((int)event.getX()*cols)/width;
            int y = ((int)event.getY()*rows)/height;

            // Check whether the object holds a valid surface
            /*if (mOpenCvCameraView.getmSurfaceHolder().getSurface().isValid()) {
                // Start editing the surface
                Canvas canvas = mOpenCvCameraView.getmSurfaceHolder().lockCanvas();
                canvas.drawColor(Color.BLACK);
                //canvas.drawCircle(event.getX(), event.getY(), 100, paint);
                mOpenCvCameraView.getmSurfaceHolder().unlockCanvasAndPost(canvas);
            }*/

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
            Log.w("touch", sel.toString());
        }else{
            touchNumbers = 0;
            mask = null;
        }
        return false;
    }

    /* Called when the user clicks the Settings button */
    public void openSettingsActivity(View view) {
        startActivity(new Intent(this, SettingsActivity.class));
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
                        Toast.makeText(getApplicationContext(), "Multiple view mode off.", Toast.LENGTH_SHORT).show();
                        break;
                    case GRAY_SINGLE_DIFF_VIEW:
                        mProcessButton.setVisibility(View.INVISIBLE);
                        mMultipleViewModeOn = true;
                        mViewMode = GRAY_SINGLE_DIFF_VIEW;
                        mMotionDetection.setmFirstTime(true);
                        Toast.makeText(getApplicationContext(), items[which] + " mode on.", Toast.LENGTH_SHORT).show();
                        break;
                    case GRAY_DOUBLE_DIFF_VIEW:
                        mProcessButton.setVisibility(View.INVISIBLE);
                        mMultipleViewModeOn = true;
                        mViewMode = GRAY_DOUBLE_DIFF_VIEW;
                        mMotionDetection.setmFirstTime(true);
                        mMotionDetection.setmSecondTime(true);
                        Toast.makeText(getApplicationContext(), items[which] + " mode on.", Toast.LENGTH_SHORT).show();
                        break;
                    case SINGLE_DOUBLE_DIFF_VIEW:
                        mProcessButton.setVisibility(View.INVISIBLE);
                        mMultipleViewModeOn = true;
                        mViewMode = SINGLE_DOUBLE_DIFF_VIEW;
                        mMotionDetection.setmFirstTime(true);
                        mMotionDetection.setmSecondTime(true);
                        mFirstTime = true;
                        Toast.makeText(getApplicationContext(), items[which] + " mode on.", Toast.LENGTH_SHORT).show();
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

    private static Mat createMultipleFrameView(final Mat currentGrayFrame, final int viewMode){
        Mat currentGrayFrameCopy = new Mat(currentGrayFrame.size(), currentGrayFrame.type());
        currentGrayFrame.copyTo(currentGrayFrameCopy);
        Mat processedFrameSingleDiff;
        Mat processedFrameDoubleDiff;
        Mat subSx = currentGrayFrame.submat(0, currentGrayFrame.rows(), 0, currentGrayFrame.cols() / 2);
        Mat subDx = currentGrayFrame.submat(0, currentGrayFrame.rows(), currentGrayFrame.cols()/2, currentGrayFrame.cols());

        Size subSxSize = subSx.size();
        Size subDxSize = subDx.size();

        Imgproc.resize(currentGrayFrame, subSx, subSxSize);

        switch(viewMode) {
            default:
                subSx.release();
                subDx.release();
                currentGrayFrameCopy.release();
                break;
            case GRAY_SINGLE_DIFF_VIEW:
                processedFrameSingleDiff = mMotionDetection.detectMotion(currentGrayFrameCopy);
                Imgproc.resize(processedFrameSingleDiff, subDx, subDxSize);
                Core.rectangle(subDx, new Point(1, 1), new Point(subDxSize.width - 2, subDxSize.height - 2), new Scalar(255, 0, 0, 255), 2);
                processedFrameSingleDiff.release();
                subSx.release();
                subDx.release();
                currentGrayFrameCopy.release();
                break;
            case GRAY_DOUBLE_DIFF_VIEW:
                processedFrameDoubleDiff = mMotionDetection.detectMotion2(currentGrayFrameCopy);
                Imgproc.resize(processedFrameDoubleDiff, subDx, subDxSize);
                Core.rectangle(subDx, new Point(1, 1), new Point(subDxSize.width - 2, subDxSize.height - 2), new Scalar(255, 0, 0, 255), 2);
                processedFrameDoubleDiff.release();
                subSx.release();
                subDx.release();
                currentGrayFrameCopy.release();
                break;
            case SINGLE_DOUBLE_DIFF_VIEW:
                processedFrameSingleDiff = mMotionDetection.detectMotion(currentGrayFrameCopy);
                Imgproc.resize(processedFrameSingleDiff, subSx, subSxSize);
                if(mFirstTime) {
                    mMotionDetection.setmFirstTime(true);
                    mFirstTime = false;
                }
                processedFrameDoubleDiff = mMotionDetection.detectMotion2(currentGrayFrameCopy);
                Imgproc.resize(processedFrameDoubleDiff, subDx, subDx.size());
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

    public class ProcessingCountDownTimer extends CountDownTimer {

        public ProcessingCountDownTimer(long startTime, long interval){
            super(startTime, interval);
        }

        @Override
        public void onTick(long millisUntilFinished) {
            Log.i(TAG, "" + (int) (millisUntilFinished/1000));
            //Toast.makeText(getApplicationContext(), "" + (int) (millisUntilFinished/1000), Toast.LENGTH_SHORT);
        }

        @Override
        public void onFinish() {
            //mTimerHasStarted = false;
            mIsCounting = false;
            Log.i(TAG, "Done with timer!");
            //Toast.makeText(getApplicationContext(), "Done!", Toast.LENGTH_SHORT);
        }
    }
}

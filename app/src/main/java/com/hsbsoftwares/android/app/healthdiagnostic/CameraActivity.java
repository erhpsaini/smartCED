package com.hsbsoftwares.android.app.healthdiagnostic;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.gesture.GestureOverlayView;
import android.hardware.Camera;
import android.location.Address;
import android.location.Geocoder;
import android.location.LocationManager;
import android.media.MediaRecorder;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
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

//import com.hsbsoftwares.android.app.healthdiagnostic.common.media.CameraHelper;
import com.hsbsoftwares.android.app.healthdiagnostic.db.model.Crisis;
import com.hsbsoftwares.android.app.healthdiagnostic.db.helper.DatabaseHandler;
import com.hsbsoftwares.android.app.healthdiagnostic.gps.GPSTracker;
import com.hsbsoftwares.android.app.healthdiagnostic.listviewactivity.ListViewActivity;
import com.hsbsoftwares.android.app.healthdiagnostic.motiondetection.MotionDetection;
import com.hsbsoftwares.android.app.healthdiagnostic.statistics.StatisticsActivity;

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


import org.opencv.highgui.Highgui;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;


public class CameraActivity extends Activity implements CameraBridgeViewBase.CvCameraViewListener2,
        View.OnTouchListener, IEmergencyAlarmListener, GestureOverlayView.OnGestureListener {

    private String countryCode = null;
    private String countryName = null;
    private String thoroughfare;
    private String featureName;
    private String AdminArea;
    // GPSTracker class
    GPSTracker gps;
    Geocoder gcd;
    private boolean isRecording = false;
    private MediaRecorder mMediaRecorder;
    private LocationManager locationManager;
    private double latitude;
    private double longitude;
    private String startDate;
    private String endDate;
    private String locality;
    private String country;
    private Address address;
    private String mCurrentPhotoPath;
    //private TextureView mPreview;
    //private CameraView mPreview;
    private static final String TAG2 = "myCameraView";
    private String mPictureFileName;
    private Camera mCamera;
    private static DatabaseHandler databaseHandler;
    private long l1;
    private long l2;
    private Date d1;
    private Date d2;
    private SimpleDateFormat  sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    /**********************************************************************************************/

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
    private static boolean mProcessingModeOn;/*false*/
    private static boolean mMultipleViewModeOn;/*false*/
    private static boolean mProcessButtonIsPressed;/*false*/
    private static boolean mIsMaskCreationModeOn;/*false*/
    private static boolean mIsMaskConfirmationHanging;/*false*/
    //Boolean used for initial processing setup
    private static boolean mFirstTime;/*true*/
    //Booleans used with timer
    private static boolean mTimerHasStarted;/*false*/
    private static boolean mTimeOut;/*false*/
    //Boolean used to activate/deactivate onTouch method's functionality
    private static boolean mIsSingleDiffDoubleDiffViewMode;/*false*/

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
    private Mat currentRGBAFrame;
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
    private ImageButton mlistViewButton;
    private ImageButton mchartViewButton;

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

        initVariables();

        /******************************************************************************************/
        // create class object
        gps = new GPSTracker(CameraActivity.this);
        //databaseHandler = DatabaseHandler.getInstance(this);
        if (gps.canGetLocation()) {

            String msg;
            latitude = gps.getLatitude();
            longitude = gps.getLongitude();
            try {
                address = getAddress(latitude, longitude);
                locality = address.getLocality();
                country = address.getCountryName();
                //insert();
            } catch (IOException e) {
                e.printStackTrace();
            }


//            List<Crisis> crisis = databaseHandler.getAllCrisis();
//            for (Crisis cn : crisis) {
//                String log = "Id: "+cn.getId()+", Date start: " + cn.getStartDate() + ", Date end: "
//                        + cn.getEndDate() + ", latitude: " + cn.getLatitude() + ", longitude: " + cn.getLongitude()
//                        + ", Locality: " + cn.getLocality()+ ", Country: " + cn.getCountry();
//                // Writing Contacts to log
//                Log.d("Name ", log);
//
//            }


            /*
            try {
                //Locality = gps.getAddress();
                locality = getAddress(latitude, longitude).getLocality();
                countryCode = getAddress(latitude, longitude).getCountryCode();
                countryName = getAddress(latitude, longitude).getCountryName();
                thoroughfare = getAddress(latitude,longitude).getThoroughfare();
                featureName = getAddress(latitude, longitude).getAdminArea();
                AdminArea = getAddress(latitude, longitude).getAdminArea();

            } catch (IOException e) {
                e.printStackTrace();
            }*/

            // \n is for new line
            // + "\nLocality" + Locality + "\nCountry code: " + CountryCode + "\nCountry name: " + CountryName
            /*
            Toast.makeText(getApplicationContext(),
                    "Your Location is - \nLat: " + latitude + "\nLong: " + longitude + "\nThoroughfare: " + thoroughfare + "\nLocality: " + locality +
                            "\nCountry name: " + countryName + "\nCountry code: " + countryCode + "\nFeatureName: " + featureName + "\nAdminArea: " + AdminArea,
                    Toast.LENGTH_LONG).show();
            msg = "Your Location is - \nLat: " + latitude + "\nLong: " + longitude + "\nThoroughfare: " + thoroughfare + "\nLocality: " + locality +
                    "\nCountry name: " + countryName + "\nCountry code: " + countryCode + "\nFeatureName: " + featureName + "\nAdminArea: " + AdminArea;
            Log.i(TAG, msg);
            */
            Toast.makeText(getApplicationContext(),
                    "Your Location is - \nLat: " + latitude + "\nLong: " + longitude + "\nLocality: " + locality + "\nCountry: " + country,
                    Toast.LENGTH_LONG).show();
            msg = "Your Location is - \nLat: " + latitude + "\nLong: " + longitude + "\nLocality: " + locality + "\nCountry: " + country;
            Log.i(TAG, msg);
        } else {
            // can't get location
            // GPS or Network is not enabled
            // Ask user to enable GPS/network in settings
            gps.showSettingsAlert();
        }

        /******************************************************************************************/

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
        //mPreview = (CameraView) findViewById(R.id.java_surface_view);

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
        mlistViewButton         = (ImageButton) findViewById(R.id.listViewButton);
        mchartViewButton        = (ImageButton) findViewById(R.id.chartViewButton);
        //mPreview                = (TextureView) findViewById(R.id.surface_view);

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

        //log();
        //insert();
        /*
        databaseHandler = DatabaseHandler.getInstance(getAppContext());
        List<DailyAverage> dailyAverage = databaseHandler.getDailyAverage();
        for(DailyAverage da : dailyAverage){
            String log = "Id: " + da.getId() + " Day: " + da.getDays()
                    + " Average Crisis Duration: " + da.getAverageCrisisDuration()
                    + " Number Of Crisis: " + da.getNumberOfCrisis();
            Log.d("Name: ", log);
        }
        */

        /*
        databaseHandler = DatabaseHandler.getInstance(getAppContext());

        Address address = null;
        List<NumberCrisisPerState> numberCrisisPerState = databaseHandler.getNumberCrisisPerState();
        //String tabcountry [] = new String[25];
        //int tabNumberOfCrisis [] = new int [1];
        for (NumberCrisisPerState ncps  : numberCrisisPerState){
            try {
                address = gps.getAddress(ncps.getLatitude(), ncps.getLongitude());
            } catch (IOException e) {
                e.printStackTrace();
            }
            //tabcountry [25] = address.getCountryName();
            String log = "Id: " + ncps.getId() + ", Latitude: " + ncps.getLatitude()
                    + " Longitude: " + ncps.getLongitude() + " Country: " + address.getCountryName()
                    + " Number of crisis: " + ncps.getNumberOfCrisis();
            Log.d("Name: ", log);
        }*/
    }

    @Override
    public void onPause() {
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

//        // if we are using MediaRecorder, release it first
//        releaseMediaRecorder();
//        // release the camera immediately on pause event
//        releaseCamera();
    }

    @Override
    public void onResume() {
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
            mlistViewButton.setVisibility(View.VISIBLE);
            mchartViewButton.setVisibility(View.VISIBLE);
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
        currentRGBAFrame = inputFrame.rgba();
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
                    final ArrayList<Integer> lumArrayList = new ArrayList<Integer>(mLumArrayList);
                    //After the timeout reset the list
                    mLumArrayList.clear();
                    //Prepare and launch task avery 10 seconds
                    CameraActivity.this.runOnUiThread(new Runnable() {
                        public void run() {
                            PathologyProcessingTask pathologyProcessingTask = new PathologyProcessingTask(mContext);
                            pathologyProcessingTask.setmEmergencyAlarmListener(CameraActivity.this);
                            pathologyProcessingTask.execute(lumArrayList);
                        }
                    });

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

//        List<Crisis> crisis2 = databaseHandler.getAllCrisis();
//        for (Crisis cn : crisis2) {
//            int update = databaseHandler.updateCrisi2(cn);
//            String log = "Id: "+cn.getId()+", Date start: " + cn.getStartDate() + ", Date end: "
//                    + cn.getEndDate() + " , latitude: " + cn.getLatitude() + ", longitude: " + cn.getLongitude()
//                    + ", Locality: " + cn.getLocality()+ ", Country: " + cn.getCountry()
//                    + " Update: " + update;
//            // Writing Contacts to log
//            Log.d("Name2 ", log);
//
//        }

        return false;
    }

    public void openListViewActivity(View view){
        startActivity(new Intent(this, ListViewActivity.class));
    }
    public void openStatisticsActivity(View view){
        startActivity(new Intent(this, StatisticsActivity.class));
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
            mlistViewButton.setVisibility(View.GONE);
            mchartViewButton.setVisibility(View.GONE);


            // BEGIN_INCLUDE(prepare_start_media_recorder)

            //new MediaPrepareTask().execute(null, null, null);

            // END_INCLUDE(prepare_start_media_recorder)
        }else{
            mSettingsButton.setVisibility(View.VISIBLE);
            mViewModeButton.setVisibility(View.VISIBLE);
            mMaskButton.setVisibility(View.VISIBLE);
            mlistViewButton.setVisibility(View.VISIBLE);
            mchartViewButton.setVisibility(View.VISIBLE);
            // BEGIN_INCLUDE(stop_release_media_recorder)

//            // stop recording and release camera
//            mMediaRecorder.stop();  // stop the recording
//            releaseMediaRecorder(); // release the MediaRecorder object
//            mCamera.lock();         // take camera access back from MediaRecorder
//
//             //inform the user that recording has stopped
//            //setCaptureButtonText("Capture");
//            isRecording = false;
//            releaseCamera();
//            // END_INCLUDE(stop_release_media_recorder)
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
                switch (which) {
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
                switch (which) {
                    case CREATE_MASK:
                        mIsMaskCreationModeOn = true;
                        if (mMask != null) {
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
                        if (mMask != null) {
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
        Mat subDx = currentGrayFrame.submat(0, currentGrayFrame.rows(), currentGrayFrame.cols() / 2, currentGrayFrame.cols());

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

    /**********************************************************************************************/
    /**
     * Asynchronous task for preparing the {@link android.media.MediaRecorder} since it's a long blocking
     * operation.
     */
    /*
    class MediaPrepareTask extends AsyncTask<Void, Void, Boolean> {
        @Override
        protected Boolean doInBackground(Void... voids) {
            // initialize video camera
            if (prepareVideoRecorder()) {
                // Camera is available and unlocked, MediaRecorder is prepared,
                // now you can start recording
                mMediaRecorder.start();

                isRecording = true;
            } else {
                // prepare didn't work, release the camera
                releaseMediaRecorder();
                return false;
            }
            return true;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (!result) {
                CameraActivity.this.finish();
            }
            // inform the user that recording has started
            //setCaptureButtonText("Stop");

        }
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    private boolean prepareVideoRecorder(){

        releaseCamera();
        // BEGIN_INCLUDE (configure_preview)
        mCamera = CameraHelper.getDefaultCameraInstance();

        // We need to make sure that our preview and recording video size are supported by the
        // camera. Query camera to find all the sizes and choose the optimal size given the
        // dimensions of our preview surface.
        Camera.Parameters parameters = mCamera.getParameters();
        List<Camera.Size> mSupportedPreviewSizes = parameters.getSupportedPreviewSizes();
        Camera.Size optimalSize = CameraHelper.getOptimalPreviewSize(mSupportedPreviewSizes,
                mPreview.getWidth(), mPreview.getHeight());

        // Use the same size for recording profile.
        CamcorderProfile profile = CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH);
        profile.videoFrameWidth = optimalSize.width;
        profile.videoFrameHeight = optimalSize.height;

        // likewise for the camera object itself.
        parameters.setPreviewSize(profile.videoFrameWidth, profile.videoFrameHeight);
        mCamera.setParameters(parameters);
        try {
            // Requires API level 11+, For backward compatibility use {@link setPreviewDisplay}
            // with {@link SurfaceView}
            mCamera.setPreviewTexture(mPreview.getSurfaceTexture());
        } catch (IOException e) {
            Log.e(TAG, "Surface texture is unavailable or unsuitable" + e.getMessage());
            return false;
        }
        // END_INCLUDE (configure_preview)


        // BEGIN_INCLUDE (configure_media_recorder)
        mMediaRecorder = new MediaRecorder();

        // Step 1: Unlock and set camera to MediaRecorder
        mCamera.unlock();
        mMediaRecorder.setCamera(mCamera);

        // Step 2: Set sources
        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT );
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);

        // Step 3: Set a CamcorderProfile (requires API Level 8 or higher)
        mMediaRecorder.setProfile(profile);

        // Step 4: Set output file
        mMediaRecorder.setOutputFile(CameraHelper.getOutputMediaFile(
                CameraHelper.MEDIA_TYPE_VIDEO).toString());
        // END_INCLUDE (configure_media_recorder)

        // Step 5: Prepare configured MediaRecorder
        try {
            mMediaRecorder.prepare();
        } catch (IllegalStateException e) {
            Log.d(TAG, "IllegalStateException preparing MediaRecorder: " + e.getMessage());
            releaseMediaRecorder();
            return false;
        } catch (IOException e) {
            Log.d(TAG, "IOException preparing MediaRecorder: " + e.getMessage());
            releaseMediaRecorder();
            return false;
        }
        return true;
    }
    private void releaseCamera(){
        if (mCamera != null){
            // release the camera for other applications
            mCamera.release();
            mCamera = null;
        }
    }
    private void releaseMediaRecorder(){
        if (mMediaRecorder != null) {
            // clear recorder configuration
            mMediaRecorder.reset();
            // release the recorder object
            mMediaRecorder.release();
            mMediaRecorder = null;
            // Lock camera for later use i.e taking it back from MediaRecorder.
            // MediaRecorder doesn't need it anymore and we will release it if the activity pauses.
            mCamera.lock();
        }
    }
    */
    @Override
    public void onInit() {
        startDate = getDateTime();
        try {
            d1 = sdf.parse(startDate);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        Log.i(TAG, "Called onInit... StartDate: " + startDate);

        matToJpeg(currentRGBAFrame);
    }
    public void matToJpeg(Mat inputFrame){
        Log.d("matToJpeg", "take an image");
        //Mat currentGrayFrame = inputFrame.gray();
        //Mat currentRgbaFrame = inputFrame;
        //Mat outputFrame = null;
        //Imgproc.cvtColor(inputFrame, inputFrame, Imgproc.COLOR_GRAY2RGB);
        //Highgui.imwrite("/sdcard/imagetest.jpg", inputFrame);
        try {
            Highgui.imwrite(createImageFile().getAbsolutePath(),inputFrame);
        } catch (IOException e) {
            e.printStackTrace();
        }
//        VideoCapture video = new VideoCapture();
//        video.read(inputFrame);
        //return currentGrayFrame;
    }
    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        //mCurrentPhotoPath = "file:" + image.getAbsolutePath();
        mCurrentPhotoPath = image.getAbsolutePath();
        Log.d("matToJpeg", "Path: " + mCurrentPhotoPath);
        return image;
    }
    @Override
    public void onStopEmergency() {
        endDate = getDateTime();
        databaseHandler = DatabaseHandler.getInstance(getAppContext());
        Log.i(TAG, "Called onStopEmergency... EndDate: " + endDate);
        try {

            // keep track of execution time
            //long lStartTime = System.nanoTime();
            d2 = sdf.parse(endDate);
            // Get msec from each, and subtract.
            long diff = d2.getTime() - d1.getTime();

            Log.i(TAG, "Elapsed Time: d1=" + d1 + " d2=" + d2 + " diff =" + diff);

            Log.d("Insert: ", "Inserting ..");
            databaseHandler.addCrisi(new Crisis(startDate, endDate, latitude, longitude, locality, country, mCurrentPhotoPath));

            // Reading all contacts
            Log.d("Reading: ", "Reading all crisises..");
            List<Crisis> crisises = databaseHandler.getAllCrisis();

            for (Crisis cn : crisises) {
                String log = "Id: "+cn.getId()+" ,Date start: " + cn.getStartDate() + " , Date end: "
                        + cn.getEndDate() + " , latitude: " + cn.getLatitude() + " , longitude: " + cn.getLongitude()
                        + ", Elapstime: " + cn.getElapsedTime();
                // Writing Contacts to log
                Log.d("Name: ", log);

            }

            // execution finised
            //long lEndTime = System.nanoTime();

            // display execution time
            //timeElapsed = lEndTime - lStartTime;

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private String getDateTime() {
        SimpleDateFormat dateFormat = new SimpleDateFormat(
                "yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        Date date = new Date();
        return dateFormat.format(date);
    }
    public Address getAddress(double latitude, double longitude) throws IOException {
        if (latitude == 0 || longitude == 0) {
            return new Address(null);
        }
        gcd=new Geocoder(this);
        //gcd=new Geocoder(getAppContext(), Locale.getDefault());
        Address address=null;
        List<Address> addresses=gcd.getFromLocation(latitude, longitude, 1);
        if (addresses.size() > 0) {
            address=addresses.get(0);
        }
        return address;
    }
    /**********************************************************************************************/

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
            //Temporary solution for stopping the ringtone play
            if((int)(millisUntilFinished/1000) == 6){
                if(mEmergencyRingtone.isPlaying()) {
                    mEmergencyRingtone.stop();
                    mEmergencyButton.setVisibility(View.GONE);
                }
            }
        }

        @Override
        public void onFinish() {
            mTimerHasStarted = false;
            mTimeOut = true;
            Log.i(TAG, "Done with timer!");
        }
    }

    private void initVariables(){
        //Booleans used for app's state
        mProcessingModeOn = false;
        mMultipleViewModeOn = false;
        mProcessButtonIsPressed = false;
        mIsMaskCreationModeOn = false;
        mIsMaskConfirmationHanging = false;
        //Boolean used for initial processing setup
        mFirstTime = true;
        //Booleans used with timer
        mTimerHasStarted = false;
        mTimeOut = false;
        //Boolean used to activate/deactivate onTouch method's functionality
        mIsSingleDiffDoubleDiffViewMode = false;
    }

    private void log(){
        databaseHandler = DatabaseHandler.getInstance(getAppContext());
        List<Crisis> crisises = databaseHandler.getAllCrisis();

        for (Crisis cn : crisises) {
            String log = "Id: "+cn.getId()+" ,Date start: " + cn.getStartDate() + " , Date end: "
                    + cn.getEndDate() + " , latitude: " + cn.getLatitude() + " , longitude: " + cn.getLongitude()
                    + ", Elapstime: " + cn.getElapsedTime();
            // Writing Contacts to log
            Log.d("Name: ", log);

        }
    }

    private void insert(){
        endDate = getDateTime();
        databaseHandler = DatabaseHandler.getInstance(this);
        //Log.i(TAG, "Called onStopEmergency... EndDate: " + endDate);

        //databaseHandler.addCrisi(new Crisis(startDate, endDate, latitude, longitude, locality, country));
        //databaseHandler.addCrisi(new Crisis("2012-08-05 19:26:16", "2012-08-05 19:26:24", -11.8666667,-67.2333333));databaseHandler.addCrisi(new Crisis("2012-08-05 19:26:16", "2012-08-05 19:26:24", -11.8666667,-67.2333333, locality, country));
//        databaseHandler.addCrisi(new Crisis("2012-08-05 19:26:16", "2012-08-05 19:26:24", 48.85661400000001,2.3522219000000177, "Paris", "France"));
//        databaseHandler.addCrisi(new Crisis("2012-08-05 20:26:16", "2012-08-05 20:27:44", 41.90278349999999,12.496365500000024, "Rome", "Italy"));
//        databaseHandler.addCrisi(new Crisis("2012-08-05 21:26:16", "2012-08-05 21:26:04", 45.5016889,-73.56725599999999, "Montreal", "QC Canada"));
//        databaseHandler.addCrisi(new Crisis("2012-08-05 22:26:16", "2012-08-05 22:36:27", 4.0510564,9.767868700000008, "Douala", "Cameroon"));
//        databaseHandler.addCrisi(new Crisis("2012-08-05 23:26:16", "2012-08-05 23:27:11", 55.755826,37.6173, "Moscow", "Russia"));
//        databaseHandler.addCrisi(new Crisis("2012-08-06 08:26:16", "2012-08-06 08:26:33", 39.904211,116.40739499999995, "Beijing", "China"));
//        databaseHandler.addCrisi(new Crisis("2012-08-06 09:26:16", "2012-08-06 09:27:08", 35.6894875,139.69170639999993, "Tokyo", "Japan"));
//        databaseHandler.addCrisi(new Crisis("2012-08-06 10:26:16", "2012-08-06 10:28:44", 28.6139391, 77.20902120000005, "New Delhi", "India"));
//        databaseHandler.addCrisi(new Crisis("2012-08-07 19:26:16", "2012-08-07 19:26:24", -33.9248685,18.424055299999964, "Capetown", "South Africa"));
//        databaseHandler.addCrisi(new Crisis("2012-08-07 20:26:16", "2012-08-07 20:27:44", -33.9248685,18.424055299999964, "Capetown", "South Africa"));
//        databaseHandler.addCrisi(new Crisis("2012-08-07 21:26:16", "2012-08-07 21:26:04", 41.90278349999999,12.496365500000024, "Rome", "Italy"));
//        databaseHandler.addCrisi(new Crisis("2012-08-08 22:26:16", "2012-08-08 22:36:27", 48.85661400000001,2.3522219000000177, "Paris", "France"));
//        databaseHandler.addCrisi(new Crisis("2012-08-08 23:26:16", "2012-08-08 23:27:11", 41.90278349999999,12.496365500000024, "Rome", "Italy"));
//        databaseHandler.addCrisi(new Crisis("2012-08-08 08:26:16", "2012-08-08 08:26:33", 48.85661400000001,2.3522219000000177, "Paris", "France"));
//        databaseHandler.addCrisi(new Crisis("2012-08-09 09:26:16", "2012-08-09 09:27:08", 41.90278349999999,12.496365500000024, "Rome", "Italy"));
//        databaseHandler.addCrisi(new Crisis("2012-08-09 10:26:16", "2012-08-09 10:28:44", 35.6894875,139.69170639999993, "Tokyo", "Japan"));
//
//        databaseHandler.addCrisi(new Crisis("2012-07-05 19:26:16", "2012-07-05 19:26:24", 41.90278349999999,12.496365500000024, "Rome", "Italy"));
//        databaseHandler.addCrisi(new Crisis("2012-07-05 20:26:16", "2012-07-05 20:27:44", 35.6894875,139.69170639999993, "Tokyo", "Japan"));
//        databaseHandler.addCrisi(new Crisis("2012-07-05 21:26:16", "2012-07-05 21:26:04", 45.5016889,-73.56725599999999, "Montreal", "QC Canada"));
//        databaseHandler.addCrisi(new Crisis("2012-07-05 22:26:16", "2012-07-05 22:36:27", 4.0510564,9.767868700000008, "Douala", "Cameroon"));
//        databaseHandler.addCrisi(new Crisis("2012-07-05 23:26:16", "2012-07-05 23:27:11", 4.0510564,9.767868700000008, "Douala", "Cameroon"));
//        databaseHandler.addCrisi(new Crisis("2012-07-06 08:26:16", "2012-07-06 08:26:33", 48.85661400000001,2.3522219000000177, "Paris", "France"));
//        databaseHandler.addCrisi(new Crisis("2012-07-06 09:26:16", "2012-07-06 09:27:08", 39.904211,116.40739499999995, "Beijing", "China"));
//        databaseHandler.addCrisi(new Crisis("2012-07-06 10:26:16", "2012-07-06 10:28:44", 39.904211,116.40739499999995, "Beijing", "China"));
//        databaseHandler.addCrisi(new Crisis("2012-07-07 19:26:16", "2012-07-07 19:26:24", 4.0510564,9.767868700000008, "Douala", "Cameroon"));
//        databaseHandler.addCrisi(new Crisis("2012-07-07 20:26:16", "2012-07-07 20:27:44", 41.90278349999999,12.496365500000024, "Rome", "Italy"));
//        databaseHandler.addCrisi(new Crisis("2012-07-07 21:26:16", "2012-07-07 21:26:04", 41.90278349999999,12.496365500000024, "Rome", "Italy"));
//        databaseHandler.addCrisi(new Crisis("2012-07-08 22:26:16", "2012-07-08 22:36:27", 4.0510564,9.767868700000008, "Douala", "Cameroon"));
//        databaseHandler.addCrisi(new Crisis("2012-07-08 23:26:16", "2012-07-08 23:27:11", 35.6894875,139.69170639999993, "Tokyo", "Japan"));
//        databaseHandler.addCrisi(new Crisis("2012-07-08 08:26:16", "2012-07-08 08:26:33", 39.904211,116.40739499999995, "Beijing", "China"));
//        databaseHandler.addCrisi(new Crisis("2012-07-09 09:26:16", "2012-07-09 09:27:08", 28.6139391, 77.20902120000005, "New Delhi", "India"));
//        databaseHandler.addCrisi(new Crisis("2012-07-09 10:26:16", "2012-07-09 10:28:44", 41.90278349999999,12.496365500000024, "Rome", "Italy"));
//
//        databaseHandler.addCrisi(new Crisis("2013-02-05 19:26:16", "2013-02-05 19:26:24", -33.9248685,18.424055299999964, "Capetown", "South Africa"));
//        databaseHandler.addCrisi(new Crisis("2013-02-05 20:26:16", "2013-02-05 20:27:44", 28.6139391, 77.20902120000005, "New Delhi", "India"));
//        databaseHandler.addCrisi(new Crisis("2013-06-05 21:26:16", "2013-06-05 21:26:04", -33.9248685,18.424055299999964, "Capetown", "South Africa"));
//        databaseHandler.addCrisi(new Crisis("2013-06-05 22:26:16", "2013-06-05 22:36:27", 28.6139391, 77.20902120000005, "New Delhi", "India"));
//        databaseHandler.addCrisi(new Crisis("2013-06-05 23:26:16", "2013-06-05 23:27:11", 39.904211,116.40739499999995, "Beijing", "China"));
//        databaseHandler.addCrisi(new Crisis("2013-06-06 08:26:16", "2013-06-06 08:26:33", 39.904211,116.40739499999995, "Beijing", "China"));
//        databaseHandler.addCrisi(new Crisis("2013-06-06 09:26:16", "2013-06-06 09:27:08", 39.904211,116.40739499999995, "Beijing", "China"));
//        databaseHandler.addCrisi(new Crisis("2013-06-06 10:26:16", "2013-06-06 10:28:44", 4.0510564,9.767868700000008, "Douala", "Cameroon"));
//        databaseHandler.addCrisi(new Crisis("2013-06-07 19:26:16", "2013-06-07 19:26:24", 4.0510564,9.767868700000008, "Douala", "Cameroon"));
//        databaseHandler.addCrisi(new Crisis("2013-06-07 20:26:16", "2013-06-07 20:27:44", -33.9248685,18.424055299999964, "Capetown", "South Africa"));
//        databaseHandler.addCrisi(new Crisis("2013-06-07 21:26:16", "2013-06-07 21:26:04", -33.9248685,18.424055299999964, "Capetown", "South Africa"));
//        databaseHandler.addCrisi(new Crisis("2013-06-08 22:26:16", "2013-06-08 22:36:27", 4.0510564,9.767868700000008, "Douala", "Cameroon"));
//        databaseHandler.addCrisi(new Crisis("2013-06-08 23:26:16", "2013-06-08 23:27:11", 41.90278349999999,12.496365500000024, "Rome", "Italy"));
//        databaseHandler.addCrisi(new Crisis("2013-06-08 08:26:16", "2013-06-08 08:26:33", 45.5016889,-73.56725599999999, "Montreal", "QC Canada"));
//        databaseHandler.addCrisi(new Crisis("2013-06-09 09:26:16", "2013-06-09 09:27:08", 45.5016889,-73.56725599999999, "Montreal", "QC Canada"));
//        databaseHandler.addCrisi(new Crisis("2013-06-09 10:26:16", "2013-06-09 10:28:44", 45.5016889,-73.56725599999999, "Montreal", "QC Canada"));
//
//        databaseHandler.addCrisi(new Crisis("2014-07-08 22:26:16", "2014-07-08 22:36:27", -33.9248685,18.424055299999964, "Capetown", "South Africa"));
//        databaseHandler.addCrisi(new Crisis("2014-07-08 23:26:16", "2014-07-08 23:27:11", 35.6894875,139.69170639999993, "Tokyo", "Japan"));
//        databaseHandler.addCrisi(new Crisis("2014-07-08 08:26:16", "2014-07-08 08:26:33", 39.904211,116.40739499999995, "Beijing", "China"));
//        databaseHandler.addCrisi(new Crisis("2014-07-09 09:26:16", "2014-07-09 09:27:08", 28.6139391, 77.20902120000005, "New Delhi", "India"));
//        databaseHandler.addCrisi(new Crisis("2014-07-09 10:26:16", "2014-07-09 10:28:44", 41.90278349999999,12.496365500000024, "Rome", "Italy"));
//
//        databaseHandler.addCrisi(new Crisis("2015-10-05 19:26:16", "2015-10-05 19:26:24", -33.9248685,18.424055299999964, "Capetown", "South Africa"));
//        databaseHandler.addCrisi(new Crisis("2015-10-05 20:26:16", "2015-10-05 20:27:44", 28.6139391, 77.20902120000005, "New Delhi", "India"));
//        databaseHandler.addCrisi(new Crisis("2015-10-05 21:26:16", "2015-10-05 21:26:04", -33.9248685,18.424055299999964, "Capetown", "South Africa"));
//        databaseHandler.addCrisi(new Crisis("2015-10-05 22:26:16", "2015-10-05 22:36:27", 28.6139391, 77.20902120000005, "New Delhi", "India"));
//        databaseHandler.addCrisi(new Crisis("2015-10-05 23:26:16", "2015-10-05 23:27:11", 48.85661400000001,2.3522219000000177, "Paris", "France"));
//        databaseHandler.addCrisi(new Crisis("2015-11-06 08:26:16", "2015-11-06 08:26:33", 48.85661400000001,2.3522219000000177, "Paris", "France"));
//        databaseHandler.addCrisi(new Crisis("2015-11-06 09:26:16", "2015-11-06 09:27:08", 48.85661400000001,2.3522219000000177, "Paris", "France"));
//        databaseHandler.addCrisi(new Crisis("2015-11-06 10:26:16", "2015-11-06 10:28:44", 4.0510564,9.767868700000008, "Douala", "Cameroon"));
        databaseHandler.addCrisi(new Crisis("2012-08-05 19:26:16", "2012-08-05 19:26:24", 48.85661400000001,2.3522219000000177, "Paris", "France", "/mnt/sdcard/Pictures/JPEG_20151128_124837_-98223709.jpg"));
        databaseHandler.addCrisi(new Crisis("2012-08-05 20:26:16", "2012-08-05 20:27:44", 41.90278349999999,12.496365500000024, "Rome", "Italy", "/mnt/sdcard/Pictures/JPEG_20151128_124837_-98223709.jpg"));
        databaseHandler.addCrisi(new Crisis("2012-08-05 21:26:16", "2012-08-05 21:26:04", 45.5016889,-73.56725599999999, "Montreal", "QC Canada", "/mnt/sdcard/Pictures/JPEG_20151128_124837_-98223709.jpg"));
        databaseHandler.addCrisi(new Crisis("2012-08-05 22:26:16", "2012-08-05 22:36:27", 4.0510564,9.767868700000008, "Douala", "Cameroon", "/mnt/sdcard/Pictures/JPEG_20151128_124837_-98223709.jpg"));
        databaseHandler.addCrisi(new Crisis("2012-08-05 23:26:16", "2012-08-05 23:27:11", 55.755826,37.6173, "Moscow", "Russia", "/mnt/sdcard/Pictures/JPEG_20151128_124837_-98223709.jpg"));
        databaseHandler.addCrisi(new Crisis("2012-08-06 08:26:16", "2012-08-06 08:26:33", 39.904211,116.40739499999995, "Beijing", "China", "/mnt/sdcard/Pictures/JPEG_20151128_124837_-98223709.jpg"));
        databaseHandler.addCrisi(new Crisis("2012-08-06 09:26:16", "2012-08-06 09:27:08", 35.6894875,139.69170639999993, "Tokyo", "Japan", "/mnt/sdcard/Pictures/JPEG_20151128_124837_-98223709.jpg"));
        databaseHandler.addCrisi(new Crisis("2012-08-06 10:26:16", "2012-08-06 10:28:44", 28.6139391, 77.20902120000005, "New Delhi", "India", "/mnt/sdcard/Pictures/JPEG_20151128_124837_-98223709.jpg"));
        databaseHandler.addCrisi(new Crisis("2012-08-07 19:26:16", "2012-08-07 19:26:24", -33.9248685,18.424055299999964, "Capetown", "South Africa", "/mnt/sdcard/Pictures/JPEG_20151128_124837_-98223709.jpg"));
        databaseHandler.addCrisi(new Crisis("2012-08-07 20:26:16", "2012-08-07 20:27:44", -33.9248685,18.424055299999964, "Capetown", "South Africa", "/mnt/sdcard/Pictures/JPEG_20151128_124837_-98223709.jpg"));
        databaseHandler.addCrisi(new Crisis("2012-08-07 21:26:16", "2012-08-07 21:26:04", 41.90278349999999,12.496365500000024, "Rome", "Italy", "/mnt/sdcard/Pictures/JPEG_20151128_124837_-98223709.jpg"));
        databaseHandler.addCrisi(new Crisis("2012-08-08 22:26:16", "2012-08-08 22:36:27", 48.85661400000001,2.3522219000000177, "Paris", "France", "/mnt/sdcard/Pictures/JPEG_20151128_124837_-98223709.jpg"));
        databaseHandler.addCrisi(new Crisis("2012-08-08 23:26:16", "2012-08-08 23:27:11", 41.90278349999999,12.496365500000024, "Rome", "Italy", "/mnt/sdcard/Pictures/JPEG_20151128_124837_-98223709.jpg"));
        databaseHandler.addCrisi(new Crisis("2012-08-08 08:26:16", "2012-08-08 08:26:33", 48.85661400000001,2.3522219000000177, "Paris", "France", "/mnt/sdcard/Pictures/JPEG_20151128_124837_-98223709.jpg"));
        databaseHandler.addCrisi(new Crisis("2012-08-09 09:26:16", "2012-08-09 09:27:08", 41.90278349999999,12.496365500000024, "Rome", "Italy", "/mnt/sdcard/Pictures/JPEG_20151128_124837_-98223709.jpg"));
        databaseHandler.addCrisi(new Crisis("2012-08-09 10:26:16", "2012-08-09 10:28:44", 35.6894875,139.69170639999993, "Tokyo", "Japan", "/mnt/sdcard/Pictures/JPEG_20151128_124837_-98223709.jpg"));

        databaseHandler.addCrisi(new Crisis("2012-07-05 19:26:16", "2012-07-05 19:26:24", 41.90278349999999,12.496365500000024, "Rome", "Italy", "/mnt/sdcard/Pictures/JPEG_20151128_124837_-98223709.jpg"));
        databaseHandler.addCrisi(new Crisis("2012-07-05 20:26:16", "2012-07-05 20:27:44", 35.6894875,139.69170639999993, "Tokyo", "Japan", "/mnt/sdcard/Pictures/JPEG_20151128_124837_-98223709.jpg"));
        databaseHandler.addCrisi(new Crisis("2012-07-05 21:26:16", "2012-07-05 21:26:04", 45.5016889,-73.56725599999999, "Montreal", "QC Canada", "/mnt/sdcard/Pictures/JPEG_20151128_124837_-98223709.jpg"));
        databaseHandler.addCrisi(new Crisis("2012-07-05 22:26:16", "2012-07-05 22:36:27", 4.0510564,9.767868700000008, "Douala", "Cameroon", "/mnt/sdcard/Pictures/JPEG_20151128_124837_-98223709.jpg"));
        databaseHandler.addCrisi(new Crisis("2012-07-05 23:26:16", "2012-07-05 23:27:11", 4.0510564,9.767868700000008, "Douala", "Cameroon", "/mnt/sdcard/Pictures/JPEG_20151128_124837_-98223709.jpg"));
        databaseHandler.addCrisi(new Crisis("2012-07-06 08:26:16", "2012-07-06 08:26:33", 48.85661400000001,2.3522219000000177, "Paris", "France", "/mnt/sdcard/Pictures/JPEG_20151128_124837_-98223709.jpg"));
        databaseHandler.addCrisi(new Crisis("2012-07-06 09:26:16", "2012-07-06 09:27:08", 39.904211,116.40739499999995, "Beijing", "China", "/mnt/sdcard/Pictures/JPEG_20151128_124837_-98223709.jpg"));
        databaseHandler.addCrisi(new Crisis("2012-07-06 10:26:16", "2012-07-06 10:28:44", 39.904211,116.40739499999995, "Beijing", "China", "/mnt/sdcard/Pictures/JPEG_20151128_124837_-98223709.jpg"));
        databaseHandler.addCrisi(new Crisis("2012-07-07 19:26:16", "2012-07-07 19:26:24", 4.0510564,9.767868700000008, "Douala", "Cameroon", "/mnt/sdcard/Pictures/JPEG_20151128_124837_-98223709.jpg"));
        databaseHandler.addCrisi(new Crisis("2012-07-07 20:26:16", "2012-07-07 20:27:44", 41.90278349999999,12.496365500000024, "Rome", "Italy", "/mnt/sdcard/Pictures/JPEG_20151128_124837_-98223709.jpg"));
        databaseHandler.addCrisi(new Crisis("2012-07-07 21:26:16", "2012-07-07 21:26:04", 41.90278349999999,12.496365500000024, "Rome", "Italy", "/mnt/sdcard/Pictures/JPEG_20151128_124837_-98223709.jpg"));
        databaseHandler.addCrisi(new Crisis("2012-07-08 22:26:16", "2012-07-08 22:36:27", 4.0510564,9.767868700000008, "Douala", "Cameroon", "/mnt/sdcard/Pictures/JPEG_20151128_124837_-98223709.jpg"));
        databaseHandler.addCrisi(new Crisis("2012-07-08 23:26:16", "2012-07-08 23:27:11", 35.6894875,139.69170639999993, "Tokyo", "Japan", "/mnt/sdcard/Pictures/JPEG_20151128_124837_-98223709.jpg"));
        databaseHandler.addCrisi(new Crisis("2012-07-08 08:26:16", "2012-07-08 08:26:33", 39.904211,116.40739499999995, "Beijing", "China", "/mnt/sdcard/Pictures/JPEG_20151128_124837_-98223709.jpg"));
        databaseHandler.addCrisi(new Crisis("2012-07-09 09:26:16", "2012-07-09 09:27:08", 28.6139391, 77.20902120000005, "New Delhi", "India", "/mnt/sdcard/Pictures/JPEG_20151128_124837_-98223709.jpg"));
        databaseHandler.addCrisi(new Crisis("2012-07-09 10:26:16", "2012-07-09 10:28:44", 41.90278349999999,12.496365500000024, "Rome", "Italy", "/mnt/sdcard/Pictures/JPEG_20151128_124837_-98223709.jpg"));

        databaseHandler.addCrisi(new Crisis("2013-02-05 19:26:16", "2013-02-05 19:26:24", -33.9248685,18.424055299999964, "Capetown", "South Africa", "/mnt/sdcard/Pictures/JPEG_20151128_124837_-98223709.jpg"));
        databaseHandler.addCrisi(new Crisis("2013-02-05 20:26:16", "2013-02-05 20:27:44", 28.6139391, 77.20902120000005, "New Delhi", "India", "/mnt/sdcard/Pictures/JPEG_20151128_124837_-98223709.jpg"));
        databaseHandler.addCrisi(new Crisis("2013-06-05 21:26:16", "2013-06-05 21:26:04", -33.9248685,18.424055299999964, "Capetown", "South Africa", "/mnt/sdcard/Pictures/JPEG_20151128_124837_-98223709.jpg"));
        databaseHandler.addCrisi(new Crisis("2013-06-05 22:26:16", "2013-06-05 22:36:27", 28.6139391, 77.20902120000005, "New Delhi", "India", "/mnt/sdcard/Pictures/JPEG_20151128_124837_-98223709.jpg"));
        databaseHandler.addCrisi(new Crisis("2013-06-05 23:26:16", "2013-06-05 23:27:11", 39.904211,116.40739499999995, "Beijing", "China", "/mnt/sdcard/Pictures/JPEG_20151128_124837_-98223709.jpg"));
        databaseHandler.addCrisi(new Crisis("2013-06-06 08:26:16", "2013-06-06 08:26:33", 39.904211,116.40739499999995, "Beijing", "China", "/mnt/sdcard/Pictures/JPEG_20151128_124837_-98223709.jpg"));
        databaseHandler.addCrisi(new Crisis("2013-06-06 09:26:16", "2013-06-06 09:27:08", 39.904211,116.40739499999995, "Beijing", "China", "/mnt/sdcard/Pictures/JPEG_20151128_124837_-98223709.jpg"));
        databaseHandler.addCrisi(new Crisis("2013-06-06 10:26:16", "2013-06-06 10:28:44", 4.0510564,9.767868700000008, "Douala", "Cameroon", "/mnt/sdcard/Pictures/JPEG_20151128_124837_-98223709.jpg"));
        databaseHandler.addCrisi(new Crisis("2013-06-07 19:26:16", "2013-06-07 19:26:24", 4.0510564,9.767868700000008, "Douala", "Cameroon", "/mnt/sdcard/Pictures/JPEG_20151128_124837_-98223709.jpg"));
        databaseHandler.addCrisi(new Crisis("2013-06-07 20:26:16", "2013-06-07 20:27:44", -33.9248685,18.424055299999964, "Capetown", "South Africa", "/mnt/sdcard/Pictures/JPEG_20151128_124837_-98223709.jpg"));
        databaseHandler.addCrisi(new Crisis("2013-06-07 21:26:16", "2013-06-07 21:26:04", -33.9248685,18.424055299999964, "Capetown", "South Africa", "/mnt/sdcard/Pictures/JPEG_20151128_124837_-98223709.jpg"));
        databaseHandler.addCrisi(new Crisis("2013-06-08 22:26:16", "2013-06-08 22:36:27", 4.0510564,9.767868700000008, "Douala", "Cameroon", "/mnt/sdcard/Pictures/JPEG_20151128_124837_-98223709.jpg"));
        databaseHandler.addCrisi(new Crisis("2013-06-08 23:26:16", "2013-06-08 23:27:11", 41.90278349999999,12.496365500000024, "Rome", "Italy", "/mnt/sdcard/Pictures/JPEG_20151128_124837_-98223709.jpg"));
        databaseHandler.addCrisi(new Crisis("2013-06-08 08:26:16", "2013-06-08 08:26:33", 45.5016889,-73.56725599999999, "Montreal", "QC Canada", "/mnt/sdcard/Pictures/JPEG_20151128_124837_-98223709.jpg"));
        databaseHandler.addCrisi(new Crisis("2013-06-09 09:26:16", "2013-06-09 09:27:08", 45.5016889,-73.56725599999999, "Montreal", "QC Canada", "/mnt/sdcard/Pictures/JPEG_20151128_124837_-98223709.jpg"));
        databaseHandler.addCrisi(new Crisis("2013-06-09 10:26:16", "2013-06-09 10:28:44", 45.5016889,-73.56725599999999, "Montreal", "QC Canada", "/mnt/sdcard/Pictures/JPEG_20151128_124837_-98223709.jpg"));

        databaseHandler.addCrisi(new Crisis("2014-07-08 22:26:16", "2014-07-08 22:36:27", -33.9248685,18.424055299999964, "Capetown", "South Africa", "/mnt/sdcard/Pictures/JPEG_20151128_124837_-98223709.jpg"));
        databaseHandler.addCrisi(new Crisis("2014-07-08 23:26:16", "2014-07-08 23:27:11", 35.6894875,139.69170639999993, "Tokyo", "Japan", "/mnt/sdcard/Pictures/JPEG_20151128_124837_-98223709.jpg"));
        databaseHandler.addCrisi(new Crisis("2014-07-08 08:26:16", "2014-07-08 08:26:33", 39.904211,116.40739499999995, "Beijing", "China", "/mnt/sdcard/Pictures/JPEG_20151128_124837_-98223709.jpg"));
        databaseHandler.addCrisi(new Crisis("2014-07-09 09:26:16", "2014-07-09 09:27:08", 28.6139391, 77.20902120000005, "New Delhi", "India", "/mnt/sdcard/Pictures/JPEG_20151128_124837_-98223709.jpg"));
        databaseHandler.addCrisi(new Crisis("2014-07-09 10:26:16", "2014-07-09 10:28:44", 41.90278349999999,12.496365500000024, "Rome", "Italy", "/mnt/sdcard/Pictures/JPEG_20151128_124837_-98223709.jpg"));

        databaseHandler.addCrisi(new Crisis("2015-10-05 19:26:16", "2015-10-05 19:26:24", -33.9248685,18.424055299999964, "Capetown", "South Africa", "/mnt/sdcard/Pictures/JPEG_20151128_124837_-98223709.jpg"));
        databaseHandler.addCrisi(new Crisis("2015-10-05 20:26:16", "2015-10-05 20:27:44", 28.6139391, 77.20902120000005, "New Delhi", "India", "/mnt/sdcard/Pictures/JPEG_20151128_124837_-98223709.jpg"));
        databaseHandler.addCrisi(new Crisis("2015-10-05 21:26:16", "2015-10-05 21:26:04", -33.9248685,18.424055299999964, "Capetown", "South Africa", "/mnt/sdcard/Pictures/JPEG_20151128_124837_-98223709.jpg"));
        databaseHandler.addCrisi(new Crisis("2015-10-05 22:26:16", "2015-10-05 22:36:27", 28.6139391, 77.20902120000005, "New Delhi", "India", "/mnt/sdcard/Pictures/JPEG_20151128_124837_-98223709.jpg"));
        databaseHandler.addCrisi(new Crisis("2015-10-05 23:26:16", "2015-10-05 23:27:11", 48.85661400000001,2.3522219000000177, "Paris", "France", "/mnt/sdcard/Pictures/JPEG_20151128_124837_-98223709.jpg"));
        databaseHandler.addCrisi(new Crisis("2015-11-06 08:26:16", "2015-11-06 08:26:33", 48.85661400000001,2.3522219000000177, "Paris", "France", "/mnt/sdcard/Pictures/JPEG_20151128_124837_-98223709.jpg"));
        databaseHandler.addCrisi(new Crisis("2015-11-06 09:26:16", "2015-11-06 09:27:08", 48.85661400000001,2.3522219000000177, "Paris", "France", "/mnt/sdcard/Pictures/JPEG_20151128_124837_-98223709.jpg"));
        databaseHandler.addCrisi(new Crisis("2015-11-06 10:26:16", "2015-11-06 10:28:44", 4.0510564,9.767868700000008, "Douala", "Cameroon", "/mnt/sdcard/Pictures/JPEG_20151128_124837_-98223709.jpg"));


        // Reading all contacts
        Log.d("Reading: ", "Reading all crisises..");
        List<Crisis> crisises = databaseHandler.getAllCrisis();

        for (Crisis cn : crisises) {
            String log = "Id: "+cn.getId()+" ,Date start: " + cn.getStartDate() + " , Date end: "
                    + cn.getEndDate() + " , latitude: " + cn.getLatitude() + " , longitude: " + cn.getLongitude()
                    + ", Elapstime: " + cn.getElapsedTime();
            // Writing Contacts to log
            Log.d("Name: ", log);
        }
    }
}

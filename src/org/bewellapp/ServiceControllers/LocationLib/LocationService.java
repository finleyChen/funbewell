package org.bewellapp.ServiceControllers.LocationLib;

//import android.os.PowerManager;
import java.io.File;
import java.io.FileOutputStream; //import java.io.IOException;  
import java.io.IOException;
import java.io.OutputStreamWriter; //import android.text.format.Time;
import java.io.RandomAccessFile;

import android.app.Service; //import android.app.Activity;
//import android.content.Context;
//import android.os.Bundle;
//import android.widget.TextView;
import android.widget.RemoteViews;
import android.widget.TextView;
import android.widget.Toast;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.AudioFormat;
import android.os.Binder;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
//import android.hardware.Sensor;
//import android.hardware.SensorEvent;
//import android.hardware.SensorEventListener;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.telephony.TelephonyManager;
import android.util.Log;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import org.bewellapp.Ml_Toolkit_Application;
import org.bewellapp.ServiceControllers.AudioLib.MyPhoneStateListener;
import org.bewellapp.ServiceControllers.LocationLib.MyLocationManager;
import org.bewellapp.ServiceControllers.LocationLib.MyLocationManager.LocationResult;
import org.bewellapp.ServiceControllers.UploadingLib.InfoObject;
import org.bewellapp.ServiceControllers.UploadingLib.UploadingServiceIntelligent;
import org.bewellapp.Storage.ML_toolkit_object;
import org.bewellapp.Storage.MiscUtilityFunctions;
import org.bewellapp.Storage.MySDCardStateListener;
import org.bewellapp.wallpaper.WellnessSummaryActivity;

//import com.skyhookwireless.wps.WPSAuthentication;
import com.skyhookwireless.wps.*;



//import com.google.android.maps.GeoPoint;

//import com.example.android.apis.app.AccelerometerManager;

// Need the following import to get access to the app resources, since this
// class is in a sub-package.
import edu.dartmouthcs.UtilLibs.MyDataTypeConverter;
import org.bewellapp.R;
//import org.bewellapp.ServiceControllers.AccelerometerLib.AccelerometerManager;

public class LocationService extends Service{
	private static Context CONTEXT;

	private static final Class[] mStartForegroundSignature = new Class[] {
		int.class, Notification.class };
	private static final Class[] mStopForegroundSignature = new Class[] { boolean.class };

	public static final String ACTION_FOREGROUND = "org.bewellapp.ServiceControllers.LocationLib.FOREGROUND";
	public static final String ACTION_BACKGROUND = "org.bewellapp.ServiceControllers.LocationLib.BACKGROUND";

	private static NotificationManager mNM;
	private Method mStartForeground;
	private Method mStopForeground;

	private Object[] mStartForegroundArgs = new Object[2];
	private Object[] mStopForegroundArgs = new Object[1];

	private FileOutputStream fOut;
	//private OutputStreamWriter osw;
	public static boolean Foreground_on;

	public long no_of_records;
	public static int curr_no_of_records;
	public static boolean Activity_on;
	private static Notification notification;
	//private RehearsalAudioRecorder ar;
	private Ml_Toolkit_Application appState;
	static final String TAG = "LOCATION";

	//binder
	private final IBinder binder = new LocationBinder(); 

	//location status
	public String inferred_location_Status = "Scanning";
	private Thread t; 
	private Location location; 
	private LocationManager locationManager;
	private String provider;
	private double[] location_data;
	private ML_toolkit_object location_object;


	//location manager codes
	private MyLocationManager myLocationManager;
	private LocationResult locationResult;
	private boolean isLocationAvialable;
	private double lat;
	private double lng;
	
	public static long sLAST_LOCATION_MILLIS;
	public static double sLATITUDE = 0;
	public static double sLONGITUDE = 0;

	//timer
	//private Timer locationTimer;
	//private final long SkyhookDelay = 3*60*1000;
	//private final long SkyhookDelay = 10*1000;

	//new timer code
	private Handler mHandler = new Handler();

	//location sensing interval
	private final int rateAccelSampling = 1000*60*60*2;		// 2-hour

	private boolean abnormalRestart = false;

	private RemoteViews contentView;

	private boolean locationAlreadyFound = false;


	/** Called when the activity is first created. */
	@Override
	public void onCreate() {

		// Register our receiver for the ACTION_SCREEN_OFF action. This will make our receiver
		// code be called whenever the phone enters standby mode.
		//IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_OFF);
		//registerReceiver(AccelerometerManager.mReceiver, filter);

		location_data = new double[2];
		location_object = null;

		// screen will stay on during this section
		appState = (Ml_Toolkit_Application) getApplicationContext();
		appState.locationService = this;

		no_of_records = 0;
		//curr_no_of_records = 0;
		CONTEXT = this;

		mNM = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		try {
			mStartForeground = getClass().getMethod("startForeground",
					mStartForegroundSignature);
			mStopForeground = getClass().getMethod("stopForeground",
					mStopForegroundSignature);
		} catch (NoSuchMethodException e) {
			// Running on an older platform.
			mStartForeground = mStopForeground = null;
		}

//
//		Toast.makeText(this,
//				"Location Service Started ",
//				Toast.LENGTH_SHORT).show();

		//starting location recording

		//String context = Context.LOCATION_SERVICE;
		//locationManager = (LocationManager)getSystemService(context);

		//Criteria criteria = new Criteria();
		//criteria.setAccuracy(Criteria.ACCURACY_FINE);
		//criteria.setAltitudeRequired(false);
		//criteria.setBearingRequired(false);
		//criteria.setCostAllowed(true);
		//criteria.setPowerRequirement(Criteria.POWER_LOW);
		//provider = locationManager.getBestProvider(criteria, true);

		//intiateIfItIsAnIrregularStuff();


		//myLocationManager = new MyLocationManager(this);
		myLocationManager = new MyLocationManager_GoogleNetworkEnabled(this);

		locationResult = new MyLocationManager.LocationResult(){

			@Override
			public void gotLocation(Location loc) {
				// TODO Auto-generated method stub
				//if(location != null)
				//tv.setText("" + location.getLatitude() + " " + location.getLongitude());
				//else
				//tv.setText("No location");
				if(loc == null){ 
					isLocationAvialable = false;
					inferred_location_Status  = "No location found \n" + MiscUtilityFunctions.now();
				}
				else
				{
					//means a location has been found already
					locationAlreadyFound = true;


					isLocationAvialable = true;
					location_data[0] = lat =  loc.getLatitude();
					location_data[1] = lng = loc.getLongitude();
					location_object = null;
					//System.gc();
					location_object =  appState.mMlToolkitObjectPool.borrowObject().setValues(System.currentTimeMillis()+appState.timeOffset, 2, MyDataTypeConverter.toByta(location_data));
					appState.ML_toolkit_buffer.insert(location_object);//inserting into the buffer

					inferred_location_Status  = ""+ myLocationManager.provider + "\n" + MiscUtilityFunctions.now() +" \nLat:" + location_data[0] + " \n Long:" + location_data[1];
					//inferred_location_Status  = ""+ myLocationManager.provider + "\nLat:" + location_data[0] + " \n Long:" + location_data[1];
					
					sLAST_LOCATION_MILLIS = System.currentTimeMillis();
					sLATITUDE = location_data[0];
					sLONGITUDE = location_data[1];
				}
				//runOnUiThread(locationTimer_Tick);

				updateNotificationArea();

			}      

			@Override
			public void gotLocation(double[] loc) {
				// TODO Auto-generated method stub
				//if(location != null)
				//tv.setText("" + location.getLatitude() + " " + location.getLongitude());
				//else
				//tv.setText("No location");
				/*inferred_location_Status  = "Location found";
				 */
				if(locationAlreadyFound == false){
					if(loc == null){ 
						isLocationAvialable = false;
						inferred_location_Status  = "No location found Skyhook:\n"+ MiscUtilityFunctions.now();
					}

					else
					{

						locationAlreadyFound = true;


						isLocationAvialable = true;
						location_data[0] = lat =  loc[0];
						location_data[1] = lng = loc[1];
						location_object = null;
						//System.gc();
						location_object =  appState.mMlToolkitObjectPool.borrowObject().setValues(System.currentTimeMillis()+appState.timeOffset, 2, MyDataTypeConverter.toByta(location_data));
						appState.ML_toolkit_buffer.insert(location_object);//inserting into the buffer

						inferred_location_Status  = ""+ myLocationManager.provider + "\n" + MiscUtilityFunctions.now() +" \nLat:" + location_data[0] + " \n Long:" + location_data[1];
						sLAST_LOCATION_MILLIS = System.currentTimeMillis();
						sLATITUDE = location_data[0];
						sLONGITUDE = location_data[1];
					}
					//runOnUiThread(locationTimer_Tick);

				}
				updateNotificationArea();
			} 
		};


		
		//appState.location_text = "Scanning";
		//myLocationManager.getLocation(LocationService.this, locationResult );
		//mHandler.removeCallbacks(mUpdateTimeTask);
		//mHandler.postDelayed(mUpdateTimeTask, rateAccelSampling);
		//updateNotificationArea(); //if other sensors are off then it will be updated
		//mHandler.postDelayed(mUpdateTimeTask, 1000*30);

		/*locationTimer = new Timer();
		locationTimer.schedule(new TimerTask() {
			@Override
			public void run() {
				//locationTimerMethod();
				myLocationManager.getLocation(LocationService.this, locationResult );
			}

		}, 0,1000*60*10); // every 10 minutes */


		//handler timer


		contentView = new RemoteViews(getPackageName(), R.layout.notification_layout);
		Activity_on = true;
		Foreground_on = true;
	}

	private Runnable mUpdateTimeTask = new Runnable() {
		public void run() {
			appState.location_text = "Scanning";

			locationAlreadyFound = false;

			myLocationManager.getLocation(LocationService.this, locationResult );
			//mHandler.postAtTime(this,1000*60*1);
			mHandler.removeCallbacks(mUpdateTimeTask);
			mHandler.postDelayed(mUpdateTimeTask, rateAccelSampling);
			//mHandler.postDelayed(mUpdateTimeTask, 1000*60*10);
			//mHandler.postDelayed(mUpdateTimeTask, 1000*30);
		}
	};

	public static Context getContext() {
		return CONTEXT;
	}

	// This is the old onStart method that will be called on the pre-2.0
	// platform. On 2.0 or later we override onStartCommand() so this
	// method will not be called.
	@Override
	public void onStart(Intent intent, int startId) {
		handleCommand(intent);

		appState.location_text = "Scanning";
		updateNotificationArea();		
		myLocationManager.getLocation(LocationService.this, locationResult );
		mHandler.removeCallbacks(mUpdateTimeTask);
		mHandler.postDelayed(mUpdateTimeTask, rateAccelSampling);
		//if other sensors are off then it will be updated
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {

		if(intent == null)
		{

			abnormalRestart = true;
			//appState.mlt.destroy();
			stopSelf();
			//return 
			return START_NOT_STICKY;
		}



		handleCommand(intent);

		//start sensing
		appState.location_text = " Scanning ";
		updateNotificationArea(); //if other sensors are off then it will be updated
		myLocationManager.getLocation(LocationService.this, locationResult );
		mHandler.removeCallbacks(mUpdateTimeTask);
		mHandler.postDelayed(mUpdateTimeTask, rateAccelSampling);


		// We want this service to continue running until it is explicitly
		// stopped, so return sticky.
		return START_STICKY;
		//return START_NOT_STICKY;
		//return START_REDELIVER_INTENT;
	}

	void handleCommand(Intent intent) {
		if (ACTION_FOREGROUND.equals(intent.getAction())) {
			// In this sample, we'll use the same text for the ticker and the
			// expanded notification

			appState.location_text = "Scanning";
			contentView = new RemoteViews(getPackageName(), R.layout.notification_layout);
			//notification = new Notification(R.drawable.logo,"BeWell",System.currentTimeMillis());//(R.drawable.icon, text, System.currentTimeMillis());
			notification = new Notification(R.drawable.notification_fish,getString(R.string.app_name),System.currentTimeMillis());

			//contentView.setImageViewResource(R.id.status_icon, R.drawable.icon);
			/*
			contentView.setTextViewText(R.id.accel_text, "Accel On:" + " (" + appState.accel_no_of_records + ")");
			contentView.setTextColor(R.id.accel_text, Color.argb(128, 0, 115, 0));
			contentView.setTextViewText(R.id.audio_text, "Audio on:" + " (" + appState.audio_no_of_records + ")");
			contentView.setTextColor(R.id.audio_text, Color.argb(128, 0, 115, 0));
			contentView.setTextViewText(R.id.location_text, "Location on:" + " (" + appState.location_text + ")");
			contentView.setTextColor(R.id.location_text, Color.argb(128, 0, 115, 0));
			*/
			//contentView.setTextViewText(R.id.accel_text, "Audio on:" + " (" + appState.audio_no_of_records + ")");
			//contentView.setT
			notification.contentView = contentView;
			/*PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
			new Intent(this, org.bewellapp.main_activity.class), 0);*/
			PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
					new Intent(this, WellnessSummaryActivity.class), 0);

			notification.contentIntent = contentIntent;

			startForegroundCompat(R.string.CUSTOM_VIEW,notification);


		} else if (ACTION_BACKGROUND.equals(intent.getAction())) {
			stopForegroundCompat(R.string.foreground_service_started_location);
			//stopForegroundCompat(2);

		}
	}

	public void callStartForegroundCompat(){
		startForegroundCompat(R.string.CUSTOM_VIEW,notification);
		updateNotificationArea();
	}

	/**
	 * This is a wrapper around the new startForeground method, using the older
	 * APIs if it is not available.
	 */
	public void startForegroundCompat(int id, Notification notification) {
		// If we have the new startForeground API, then use it.
		Foreground_on = true;
		if (mStartForeground != null) {
			mStartForegroundArgs[0] = Integer.valueOf(id);
			mStartForegroundArgs[1] = notification;
			try {
				mStartForeground.invoke(this, mStartForegroundArgs);
			} catch (InvocationTargetException e) {
				// Should not happen.
				Log.w("ApiDemos", "Unable to invoke startForeground", e);
			} catch (IllegalAccessException e) {
				// Should not happen.
				Log.w("ApiDemos", "Unable to invoke startForeground", e);
			}
			return;
		}

		// Fall back on the old API.
//		setForeground(true);
//		mNM.notify(id, notification);
		Log.e("NOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOO", "NOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOO");
	}

	/**
	 * This is a wrapper around the new stopForeground method, using the older
	 * APIs if it is not available.
	 */
	public void stopForegroundCompat(int id) {
		Foreground_on = false;
		// If we have the new stopForeground API, then use it.
		if (mStopForeground != null) {
			mStopForegroundArgs[0] = Boolean.TRUE;
			try {
				mStopForeground.invoke(this, mStopForegroundArgs);
			} catch (InvocationTargetException e) {
				// Should not happen.
				Log.w("ApiDemos", "Unable to invoke stopForeground", e);
			} catch (IllegalAccessException e) {
				// Should not happen.
				Log.w("ApiDemos", "Unable to invoke stopForeground", e);
			}
			return;
		}

		// Fall back on the old API. Note to cancel BEFORE changing the
		// foreground state, since we could be killed at that point.
//		if (!appState.getServiceController().areServiceUsingNotificationArea()) {
//			mNM.cancel(id);
//		}
//		setForeground(false);
		Log.e("NOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOO", "NOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOO");
	}

	@Override
	public void onDestroy() {
		// Make sure our notification is gone.
		stopForegroundCompat(R.string.CUSTOM_VIEW);

		try{
			mHandler.removeCallbacks(mUpdateTimeTask);
		}
		catch(Exception ex){}

		appState.locationService = null;


	}


	@Override
	public IBinder onBind(Intent intent) {
		return binder;
	}

	public class LocationBinder extends Binder{
		public LocationService getService(){
			return LocationService.this;
		}
	}


	public void updateNotificationArea(){
		//if (no_of_records % 100 == 0 && Foreground_on == true) {


		appState.location_text = myLocationManager.provider;

		contentView = new RemoteViews(getPackageName(), R.layout.notification_layout);
		/*
		if(appState.accelSensorOn){
			contentView.setTextViewText(R.id.accel_text, "Accel On:" + " (" + appState.accel_no_of_records + ")");
			contentView.setTextColor(R.id.accel_text, Color.argb(128, 0, 115, 0));
		}
		else
		{
			contentView.setTextViewText(R.id.accel_text, "Accel Off");
			contentView.setTextColor(R.id.accel_text, Color.argb(128, 115, 0, 0));
		}

		if(appState.audioSensorOn){
			contentView.setTextViewText(R.id.audio_text, "Audio On:" + " (" + appState.audio_no_of_records + ")");
			contentView.setTextColor(R.id.audio_text, Color.argb(128, 0, 115, 0));
		}
		else
		{
			contentView.setTextViewText(R.id.audio_text, "Audio off");
			contentView.setTextColor(R.id.audio_text, Color.argb(128, 115, 0, 0));
		}

		contentView.setTextViewText(R.id.location_text, "Location On:" + " (" + appState.location_text + ")");
		contentView.setTextColor(R.id.location_text, Color.argb(128, 0, 115, 0));
		*/
		//contentView.setTextViewText(R.id.accel_text, "Audio on:" + " (" + appState.audio_no_of_records + ")");
		//contentView.setT

		notification.contentView = contentView;
		/*PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
		new Intent(this, org.bewellapp.main_activity.class), 0);*/
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
				new Intent(this, WellnessSummaryActivity.class), 0);
		notification.contentIntent = contentIntent;

		//startForegroundCompat(R.string.CUSTOM_VIEW,notification);
		mNM.notify(R.string.CUSTOM_VIEW, notification);
		//mNM.notify(2, notification);
		//*/

		//}
	}
}

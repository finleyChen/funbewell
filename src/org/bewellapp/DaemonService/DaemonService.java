package org.bewellapp.DaemonService;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.bewellapp.Ml_Toolkit_Application;
import org.bewellapp.main_activity;
import org.bewellapp.BeWellSuggestion.BeWellSuggestionAlarm;
import org.bewellapp.CrashHandlers.CrashRestartAlarm;
import org.bewellapp.RestartService24Hours.SchduleRestartAlarm;
import org.bewellapp.ScoreComputation.ScoreComputationAlarm;
import org.bewellapp.ScoreComputation.ScoreComputationDBAdapter;
import org.bewellapp.ScoreComputation.ScoreComputationService;
import org.bewellapp.ScoreComputation.ScoreObject;
import org.bewellapp.ServiceControllers.ServiceController;
import org.bewellapp.ServiceControllers.AudioLib.MyPhoneStateListener;
import org.bewellapp.ServiceControllers.LocationLib.LocationService;
import org.bewellapp.ServiceControllers.LocationLib.MyLocationManager;
import org.bewellapp.ServiceControllers.UploadingLib.InfoObject;
import org.bewellapp.Storage.ML_toolkit_object;
import org.bewellapp.Storage.MiscUtilityFunctions;
import org.bewellapp.Storage.MySDCardStateListener;
import org.bewellapp.wallpaper.WellnessSummaryActivity;

import edu.dartmouthcs.UtilLibs.MyDataTypeConverter;
import org.bewellapp.R;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.backup.RestoreObserver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.location.Location;
import android.os.Handler;
import android.os.IBinder;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;


public class DaemonService extends Service {


	private Ml_Toolkit_Application appState;
	private static ServiceController serviceController;
	private Thread t;
	static final String TAG = "DaemonService";

	private static Context CONTEXT;

	private static final Class[] mStartForegroundSignature = new Class[] {
		int.class, Notification.class };
	private static final Class[] mStopForegroundSignature = new Class[] { boolean.class };

	public static final String ACTION_FOREGROUND = "org.bewellapp.DaemonService.FOREGROUND";
	public static final String ACTION_BACKGROUND = "org.bewellapp.DaemonService.BACKGROUND";

	private static NotificationManager mNM;
	private Method mStartForeground;
	private Method mStopForeground;

	private Object[] mStartForegroundArgs = new Object[2];
	private Object[] mStopForegroundArgs = new Object[1];


	private static Notification notification;
	private RemoteViews contentView;

	//new timer code
	private Handler mHandler = new Handler();

	//
	//private final int rateNotification = 1000*60*5;
	private final int rateNotification = 1000*30;

	public void onCreate() {

		// Register our receiver for the ACTION_SCREEN_OFF action. This will make our receiver
		// code be called whenever the phone enters standby mode.
		//IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_OFF);
		//registerReceiver(AccelerometerManager.mReceiver, filter);



		// screen will stay on during this section
		appState = (Ml_Toolkit_Application) getApplicationContext();
		appState.daemonService = this;
		
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


//		Toast.makeText(this,
//				"Daemon Service Started ",
//				Toast.LENGTH_SHORT).show();


		contentView = new RemoteViews(getPackageName(), R.layout.notification_layout);
		//Activity_on = true;
		//Foreground_on = true;
		SchduleRestartAlarm.scheduleRestartOfService(appState);

		//score computation alarm
		//ScoreComputationAlarm.scheduleRestartOfService(appState, 1);
		ScoreComputationAlarm.scheduleRestartOfService(appState);

		//get suggestion alarm
		BeWellSuggestionAlarm.scheduleRestartOfService(appState);

		//initiate bewell values
		appState.beWellScoreAdapter = new ScoreComputationDBAdapter(this,  "bewellScore_1.dbr_ms");	
		appState.beWellScoreAdapter.open();
		ScoreObject scoreObject = ScoreComputationDBAdapter.getBeWellScores(appState.beWellScoreAdapter);



		appState.current_physical_score = scoreObject.physicalScoreCurrent;
		appState.amount_of_different_all_physical_activities = scoreObject.physicalScoreAllActivityCount;
		appState.amount_of_different_physical_activity =  scoreObject.physicalScoreDifferntActivityCount;
		appState.prev_physical_score = scoreObject.physicalScorePrev;

		appState.current_social_score = scoreObject.socialScoreCurrent;
		appState.amount_of_different_all_voice_activities = scoreObject.socialScoreAllActivityCount;
		appState.amount_of_different_voice_activity = scoreObject.socialScoreDifferntActivityCount;
		appState.prev_social_score = scoreObject.socialScorePrev;


		appState.current_sleep_score = scoreObject.sleepScoreCurrent;
		appState.prev_sleep_score = scoreObject.sleepScorePrev;
		appState.amount_of_sleeping_duration = scoreObject.sleepTimeInMillisecond;
		appState.sleeping_duration_record_time = scoreObject.sleepTimeRecordDataTime;
		

		//update last_of variables 
		System.arraycopy(appState.amount_of_different_physical_activity, 0, appState.last_of_different_physical_activity, 0, appState.amount_of_different_physical_activity.length);
		System.arraycopy(appState.amount_of_different_voice_activity, 0, appState.last_of_different_voice_activity, 0, appState.amount_of_different_voice_activity.length);
		appState.last_of_different_all_voice_activities = appState.amount_of_different_all_voice_activities;

		Log.e("Social Score Deamon","" + appState.current_social_score + " " + appState.prev_social_score + "  " + appState.amount_of_different_all_voice_activities);

		appState.beWellScoreAdapter.close();

	}


	// This is the old onStart method that will be called on the pre-2.0
	// platform. On 2.0 or later we override onStartCommand() so this
	// method will not be called.
	@Override
	public void onStart(Intent intent, int startId) {
		//boolean no_check = false;
		//if(intent == null)
		//no_check = true;
		handleCommand(intent);

		mHandler.removeCallbacks(mUpdateTimeTask);
		mHandler.postDelayed(mUpdateTimeTask, rateNotification);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {




		//boolean no_check = false;
		//if(intent == null)
		//	no_check = true;	
		if(intent == null)
		{
			CrashRestartAlarm.scheduleRestartOfService(this);
			//appState.mlt.destroy();
			stopSelf();
			return START_NOT_STICKY;
		}

		handleCommand(intent);
		mHandler.removeCallbacks(mUpdateTimeTask);
		mHandler.postDelayed(mUpdateTimeTask, rateNotification);



		// We want this service to continue running until it is explicitly
		// stopped, so return sticky.
		//return START_STICKY;
		return START_STICKY;
	}

	private Runnable mUpdateTimeTask = new Runnable() {
		public void run() {
			//appState.location_text = "Scanning";
			//myLocationManager.getLocation(LocationService.this, locationResult );
			updateNotificationArea();
			//mHandler.postAtTime(this,1000*60*1);
			mHandler.removeCallbacks(mUpdateTimeTask);
			mHandler.postDelayed(mUpdateTimeTask, rateNotification);
			//mHandler.postDelayed(mUpdateTimeTask, 1000*60*10);
			//mHandler.postDelayed(mUpdateTimeTask, 1000*30);		


		}
	};

	void handleCommand(Intent intent) {
		if (ACTION_FOREGROUND.equals(intent.getAction())) {

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
			notification.contentView = contentView;
			/*PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
					new Intent(this, org.bewellapp.main_activity.class), 0);*/
			PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
					new Intent(this, WellnessSummaryActivity.class), 0);

			notification.contentIntent = contentIntent;

			startForegroundCompat(R.string.CUSTOM_VIEW,notification);

		} else if (ACTION_BACKGROUND.equals(intent.getAction())) {
			stopForegroundCompat(R.string.foreground_service_started_aud);
			//stopForegroundCompat(2);

		}

		//start a timer

	}


	/**
	 * This is a wrapper around the new startForeground method, using the older
	 * APIs if it is not available.
	 */
	public void startForegroundCompat(int id, Notification notification) {
		// If we have the new startForeground API, then use it.
		//Foreground_on = true;
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

		Log.e("NOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOO", "NOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOO");
		// Fall back on the old API.
//		setForeground(true);
//		mNM.notify(id, notification);
	}

	public void callStartForegroundCompat(){
		startForegroundCompat(R.string.CUSTOM_VIEW,notification);
		updateNotificationArea();
	}

	public void stopForegroundCompat(int id) {
		//Foreground_on = false;
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
	public IBinder onBind(Intent arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();

		try{
			stopForegroundCompat(R.string.CUSTOM_VIEW);
			mHandler.removeCallbacks(mUpdateTimeTask);
		}
		catch(Exception ex){}

		appState.daemonService = null;


		//Log.i(TAG, "self destruct done");
	}


	public void updateNotificationArea(){
		//if (no_of_records % 100 == 0 && Foreground_on == true) {

		//String text = "No of samples";

		// Set the info for the views that show in the notification panel.
		/*PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
					new Intent(this, org.bewellapp.main_activity.class), 0);


			//notification.setLatestEventInfo(this, "Audio Test ", text, contentIntent);


			// Set the info for the views that show in the notification panel.
			notification.setLatestEventInfo(this, "Audio Test ", text
					+ " " + appState.audio_no_of_records, contentIntent);

			mNM.notify(R.string.foreground_service_started_aud, notification);*/
		//mNM.notify(2, notification);
		//*/

		//String text = "Audio On:";
		// Set the info for the views that show in the notification panel.
		//PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
		//	new Intent(this, org.bewellapp.main_activity.class), 0);

		// Set the info for the views that show in the notification panel.
		//notification.setLatestEventInfo(this, "Accelerometer Test", text
		//		+ " " + curr_no_of_records, contentIntent);
		//mNM.notify(R.string.foreground_service_started, notification);
		//mNM.notify(1, notification);

		//notification = new Notification(R.drawable.logo,"BeWell",System.currentTimeMillis());//(R.drawable.icon, text, System.currentTimeMillis());
		//contentView.setImageViewResource(R.id.status_icon, R.drawable.logo);

		contentView = new RemoteViews(getPackageName(), R.layout.notification_layout);
		try{
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

			if(appState.locationSensorOn){
				contentView.setTextViewText(R.id.location_text, "Location On:" + " (" + appState.location_text + ")");					
				contentView.setTextColor(R.id.location_text, Color.argb(128, 0, 115, 0));
			}
			else
			{
				contentView.setTextViewText(R.id.location_text, "Location off");					
				contentView.setTextColor(R.id.location_text, Color.argb(128, 115, 0, 0));
			}
			*/

			notification.contentView = contentView;
			/*PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
		new Intent(this, org.bewellapp.main_activity.class), 0);*/
			PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
					new Intent(this, WellnessSummaryActivity.class), 0);

			notification.contentIntent = contentIntent;


			//startForegroundCompat(R.string.CUSTOM_VIEW,notification);
			mNM.notify(R.string.CUSTOM_VIEW,notification);
		}catch (Exception e){
			Log.e(TAG, "Update Notification " + e.toString());
		}

		//}
	}

}




package org.bewellapp.MomentarySampling;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Timer;
import java.util.TimerTask;

import org.bewellapp.Ml_Toolkit_Application;
import org.bewellapp.CrashHandlers.CrashRestartAlarm;
import org.bewellapp.RestartService24Hours.SchduleRestartAlarm;
import org.bewellapp.ServiceControllers.ServiceController;
import org.bewellapp.Storage.ML_toolkit_object;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.provider.MediaStore.Audio;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;
import org.bewellapp.R;


public class MomentarySamplingService extends Service {


	private Ml_Toolkit_Application appState;
	private static ServiceController serviceController;
	private Thread t;
	static final String TAG = "DaemonService";

	private static Context CONTEXT;

	private static final Class[] mStartForegroundSignature = new Class[] {
		int.class, Notification.class };
	private static final Class[] mStopForegroundSignature = new Class[] { boolean.class };

	public static final String ACTION_FOREGROUND = "org.bewellapp.MomentarySamplingService.FOREGROUND";
	public static final String ACTION_BACKGROUND = "org.bewellapp.MomentarySamplingService.BACKGROUND";

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
	private final int rateNotification = 1000*60*30;
	private Timer notificationUpdateTimer;
	
	//
	public static int labelCountAvailable = 0;

	public void onCreate() {

		// Register our receiver for the ACTION_SCREEN_OFF action. This will make our receiver
		// code be called whenever the phone enters standby mode.
		//IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_OFF);
		//registerReceiver(AccelerometerManager.mReceiver, filter);



		// screen will stay on during this section
		appState = (Ml_Toolkit_Application) getApplicationContext();
		appState.momentarySamplingService = this;


		appState.ms_db_adapter = new MyMomentarySamplingDBAdapter(this,  "momemtary_sampling4.dbr_ms");	
		appState.ms_db_adapter.open();



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
//				"MomentarySampling Service Started ",
//				Toast.LENGTH_SHORT).show();


		contentView = new RemoteViews(getPackageName(), R.layout.notification_layout);
		//Activity_on = true;
		//Foreground_on = true;
		SchduleRestartAlarm.scheduleRestartOfService(appState);
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
		put_sample_in_db();
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
		put_sample_in_db();
		mHandler.removeCallbacks(mUpdateTimeTask);
		mHandler.postDelayed(mUpdateTimeTask, rateNotification);


		//update notification area with a timer every 3 seconds
		notificationUpdateTimer = new Timer();
		notificationUpdateTimer.schedule(new TimerTask() {
			@Override
			public void run() {
				//locationTimerMethod();
				//battery_level_Tv = (TextView) findViewById(R.id.battery_level);
				updateMyView();
			}



		}, 0,3000);
		

		// We want this service to continue running until it is explicitly
		// stopped, so return sticky.
		//return START_STICKY;
		return START_STICKY;
	}
	
	private void updateMyView() {
		// TODO Auto-generated method stub
		updateNotificationArea();
	}
	
	

	private Runnable mUpdateTimeTask = new Runnable() {
		public void run() {
			//appState.location_text = "Scanning";
			//myLocationManager.getLocation(LocationService.this, locationResult );

			put_sample_in_db();
			//mHandler.postAtTime(this,1000*60*1);
			mHandler.removeCallbacks(mUpdateTimeTask);
			mHandler.postDelayed(mUpdateTimeTask, rateNotification);
			//mHandler.postDelayed(mUpdateTimeTask, 1000*60*10);
			//mHandler.postDelayed(mUpdateTimeTask, 1000*30);
		}
	};

	void handleCommand(Intent intent) {
		if (ACTION_FOREGROUND.equals(intent.getAction())) {

			notification = new Notification(R.drawable.sample,getString(R.string.app_name),System.currentTimeMillis());//(R.drawable.icon, text, System.currentTimeMillis());
			/*notification = new Notification(R.drawable.logo,"BeWell",System.currentTimeMillis());//(R.drawable.icon, text, System.currentTimeMillis());
			//contentView.setImageViewResource(R.id.status_icon, R.drawable.icon);
			contentView.setTextViewText(R.id.accel_text, "Accel On:" + " (" + appState.accel_no_of_records + ")");
			contentView.setTextColor(R.id.accel_text, Color.argb(128, 0, 115, 0));
			contentView.setTextViewText(R.id.audio_text, "Audio on:" + " (" + appState.audio_no_of_records + ")");
			contentView.setTextColor(R.id.audio_text, Color.argb(128, 0, 115, 0));
			contentView.setTextViewText(R.id.location_text, "Location on:" + " (" + appState.location_text + ")");
			contentView.setTextColor(R.id.location_text, Color.argb(128, 0, 115, 0));
			notification.contentView = contentView;
			PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
					new Intent(this, org.bewellapp.main_activity.class), 0);
			notification.contentIntent = contentIntent;*/
			PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
					new Intent(this, org.bewellapp.MomentarySampling.momentary_list.class), 0);
			
			notification.setLatestEventInfo(this, "Momentary Sampling", "Unlabeled Samples " + labelCountAvailable
					+ " " , contentIntent);
			
			startForegroundCompat(R.string.foreground_service_started_aud,notification);

		} else if (ACTION_BACKGROUND.equals(intent.getAction())) {
			stopForegroundCompat(R.string.foreground_service_started_aud);
			//stopForegroundCompat(2);

		}

		//start a timer

	}


	protected void put_sample_in_db() {
		// TODO Auto-generated method stub
		//for(int i = 0; i<10; i++)
		//int i = 0;
		//{
		long current_time = System.currentTimeMillis();
		long rowID = appState.ms_db_adapter.insertEntryMomertarySampling(current_time, current_time, 
				current_time, -1, "none","not_available","not_available");
		//}
		labelCountAvailable = MyMomentarySamplingDBAdapter.getAvailableCount(appState.ms_db_adapter);
		
		
		String inferred_status = "rowID=" + rowID + ",sample_time=" + current_time + ",stress_start_time=" + current_time +
			",stress_end_time=" + current_time + ",stress_level=" + -1 + ",location=" + "none" 
			+ ",label_stress=" + "not_available" +
			",inferred_stress=" + "not_available";
		
		ML_toolkit_object momentary_sample_point = appState.mMlToolkitObjectPool.borrowObject().setValues(System.currentTimeMillis()+appState.timeOffset,
				12, true ,inferred_status);
		appState.ML_toolkit_buffer.insert(momentary_sample_point);
		
		updateNotificationArea();
		vibrateNotification();

	}


	/**
	 * This is a wrapper around the new startForeground method, using the older
	 * APIs if it is not available.
	 */
	public void startForegroundCompat(int id, Notification notification) {
		// If we have the new startForeground API, then use it.
		//Foreground_on = true;
		Log.i("MomenteryServicesss","Make   Foreground  ");
		if (mStartForeground != null) {
			mStartForegroundArgs[0] = Integer.valueOf(id);
			mStartForegroundArgs[1] = notification;
			try {
				mStartForeground.invoke(this, mStartForegroundArgs);
				//updateNotificationArea();
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
//		mNM.cancel(id);
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

		appState.momentarySamplingService = null;


		//Log.i(TAG, "self destruct done");
	}


	public void updateNotificationArea(){
		//if (no_of_records % 100 == 0 && Foreground_on == true) {

		String text = "Unlabeled samples";

		// Set the info for the views that show in the notification panel.
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
				new Intent(this, org.bewellapp.MomentarySampling.momentary_list.class), 0);


		//notification.setLatestEventInfo(this, "Audio Test ", text, contentIntent);


		// Set the info for the views that show in the notification panel.
		notification.setLatestEventInfo(this, "Momentary Sampling", text 
				+ " " +  labelCountAvailable, contentIntent);

		//notification.vibrate = new long[]{100, 200, 100, 500};
		
		
		
		mNM.notify(R.string.foreground_service_started_aud, notification);
		//mNM.notify(2, notification);
		//

	
  
		//}
	}
	
	
	private void vibrateNotification()
	{
		NotificationManager nManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE); 
		Notification n = new Notification();

		// Now we set the vibrate member variable of our Notification
		// After a 100ms delay, vibrate for 200ms then pause for another
		//100ms and then vibrate for 500ms
		n.vibrate = new long[]{ 0, 300, 200, 300, 400, 300 , 600, 300};
		
		n.ledOnMS  = 200;    //Set led blink (Off in ms)
        n.ledOffMS = 200;    //Set led blink (Off in ms)
        n.ledARGB = 0x9400d4;   //Set led color
        n.flags = Notification.FLAG_SHOW_LIGHTS;

        n.defaults = Notification.DEFAULT_SOUND;
		//n.sound = Uri.withAppendedPath(Audio.Media.INTERNAL_CONTENT_URI, ringtoneId);
        
		nManager.notify(0, n);
	}

}




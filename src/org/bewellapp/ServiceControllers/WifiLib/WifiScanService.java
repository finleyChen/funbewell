package org.bewellapp.ServiceControllers.WifiLib;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import org.bewellapp.Ml_Toolkit_Application;
import org.bewellapp.CrashHandlers.CrashRestartAlarm;
import org.bewellapp.ServiceControllers.ServiceController;
import org.bewellapp.Storage.ML_toolkit_object;
import org.bewellapp.wallpaper.WellnessSummaryActivity;

import org.bewellapp.R;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;


public class WifiScanService extends Service {


	private Ml_Toolkit_Application appState;
	private static ServiceController serviceController;
	private Thread t;
	static final String TAG = "DaemonService";

	private static Context CONTEXT;

	private static final Class[] mStartForegroundSignature = new Class[] {
		int.class, Notification.class };
	private static final Class[] mStopForegroundSignature = new Class[] { boolean.class };

	public static final String ACTION_FOREGROUND = "org.bewellapp.WifiScanService.FOREGROUND";
	public static final String ACTION_BACKGROUND = "org.bewellapp.WifiScanService.BACKGROUND";

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
	private final int rateWifiUpdate = 1000*60;//60*3;
	private final int timeBeforeUnregisterReceiver = 1000*15;

	private WifiManager mainWifi;
	private WifiReceiver receiverWifi;
	private List<ScanResult> wifiList;
	private StringBuilder sb = new StringBuilder();

	private boolean wifiWasEnabledBefore = true;
	public ML_toolkit_object wifi_object;


	class WifiReceiver extends BroadcastReceiver {
		public void onReceive(Context c, Intent intent) {
			sb = new StringBuilder();
			wifiList = mainWifi.getScanResults();

			if(wifiList == null){
				if(wifiWasEnabledBefore == false)
					mainWifi.setWifiEnabled(false);

				return;
			}

			for(int i = 0; i < wifiList.size(); i++){
				sb.append(new Integer(i+1).toString() + ".");
				sb.append((wifiList.get(i)).toString());
				sb.append("\n");
			}

			//wifi will be turned if it was turned off before
			if(wifiWasEnabledBefore == false)
				mainWifi.setWifiEnabled(false);

			Log.w("WifiService", "Scan service completed " + sb.length());		
			appState.numberOfWifiScans++;

			//enter into the database
			wifi_object =  appState.mMlToolkitObjectPool.borrowObject().setValues(System.currentTimeMillis()+appState.timeOffset, 9, sb.toString().getBytes());
			appState.ML_toolkit_buffer.insert(wifi_object);

			//no new updata was necessary
			//receiverWifi
			//unregisterReceiver(receiverWifi);


			//sb.
			//mainText.setText(sb);
		}
	}


	public void onCreate() {

		// Register our receiver for the ACTION_SCREEN_OFF action. This will make our receiver
		// code be called whenever the phone enters standby mode.
		//IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_OFF);
		//registerReceiver(AccelerometerManager.mReceiver, filter);



		// screen will stay on during this section
		appState = (Ml_Toolkit_Application) getApplicationContext();
		appState.wifiScanService = this;
		appState.numberOfWifiScans = 0;
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
//				"Wifi Service Started ",
//				Toast.LENGTH_SHORT).show();


		contentView = new RemoteViews(getPackageName(), R.layout.notification_layout);
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

		mHandler.removeCallbacks(mUpdateTimeTask,rateWifiUpdate);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {

		if(intent == null)
		{
			CrashRestartAlarm.scheduleRestartOfService(this);
			//appState.mlt.destroy();
			stopSelf();
			return START_NOT_STICKY;
		}

		handleCommand(intent);

		Thread t = new Thread() {
			public void run() {
				startWifiScan();
			}
		};
		t.start();

		mHandler.removeCallbacks(mUpdateTimeTask);
		mHandler.postDelayed(mUpdateTimeTask, rateWifiUpdate);



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

			Thread t = new Thread() {
				public void run() {
					startWifiScan();
				}
			};
			t.start();

			mHandler.removeCallbacks(mUpdateTimeTask);
			mHandler.postDelayed(mUpdateTimeTask, rateWifiUpdate);
			//mHandler.postDelayed(mUpdateTimeTask, 1000*60*10);
			//mHandler.postDelayed(mUpdateTimeTask, 1000*30);
		}
	};

	private Runnable mUpdateTimeTaskStopReceiver = new Runnable() {
		public void run() {
			try{
				Log.w("WIFI SERVICE", "Unregistered Wifi Connection");
				unregisterReceiver(receiverWifi);}
			catch(Exception ex){}
		}
	};

	private void startWifiScan()
	{
		//start war driving
		//mainText = (TextView) findViewById(R.id.mainText);
		mainWifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);

		if(mainWifi.isWifiEnabled()==false){
			mainWifi.setWifiEnabled(true);
			this.wifiWasEnabledBefore = false;//so make sure it is turned off again after return
		}
		else
			this.wifiWasEnabledBefore = true;

		Log.w("WIFI SERVICE", "Starting wifi driving");

		receiverWifi = new WifiReceiver();
		registerReceiver(receiverWifi, new IntentFilter(
				WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
		boolean scanInitiated = mainWifi.startScan();


		if(scanInitiated)
		{
			//start a timer to stop scan after 15 seconds
			mHandler.removeCallbacks(mUpdateTimeTaskStopReceiver);
			mHandler.postDelayed(mUpdateTimeTaskStopReceiver, timeBeforeUnregisterReceiver);

		}
		else
			unregisterReceiver(receiverWifi);

	}

	void handleCommand(Intent intent) {
		if (ACTION_FOREGROUND.equals(intent.getAction())) {

			contentView = new RemoteViews(getPackageName(), R.layout.notification_layout);
			//notification = new Notification(R.drawable.logo,"BeWell",System.currentTimeMillis());//(R.drawable.icon, text, System.currentTimeMillis());
			notification = new Notification(R.drawable.notification_fish,getString(R.string.app_name),System.currentTimeMillis());

			//contentView.setImageViewResource(R.id.status_icon, R.drawable.icon);
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

			//contentView.setTextViewText(R.id.audio_text, "Audio On:" + " (" + appState.audio_no_of_records + ")");					
			//contentView.setTextColor(R.id.audio_text, Color.argb(128, 0, 115, 0));

			if(appState.locationSensorOn){
				contentView.setTextViewText(R.id.location_text, "Location On:" + " (" + appState.location_text + ")");					
				contentView.setTextColor(R.id.location_text, Color.argb(128, 0, 115, 0));
			}
			else
			{
				contentView.setTextViewText(R.id.location_text, "Location off");					
				contentView.setTextColor(R.id.location_text, Color.argb(128, 115, 0, 0));
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

		appState.wifiScanService = null;


		//Log.i(TAG, "self destruct done");
	}


	public void updateNotificationArea(){
		//if (no_of_records % 100 == 0 && Foreground_on == true) {

		//String text = "No of samples";

		// Set the info for the views that show in the notification panel.
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




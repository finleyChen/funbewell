package org.bewellapp.ServiceControllers.BluetoothLib;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.bewellapp.Ml_Toolkit_Application;
import org.bewellapp.CrashHandlers.CrashRestartAlarm;
import org.bewellapp.RestartService24Hours.SchduleRestartAlarm;
import org.bewellapp.ServiceControllers.ServiceController;
import org.bewellapp.Storage.ML_toolkit_object;
import org.bewellapp.wallpaper.WellnessSummaryActivity;

import org.bewellapp.R;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;


public class BluetoothService extends Service {


	private Ml_Toolkit_Application appState;
	private static ServiceController serviceController;
	private Thread t;
	//static final String TAG = "DaemonService";

	private static Context CONTEXT;

	private static final Class[] mStartForegroundSignature = new Class[] {
		int.class, Notification.class };
	private static final Class[] mStopForegroundSignature = new Class[] { boolean.class };

	public static final String ACTION_FOREGROUND = "org.bewellapp.BluetoothService.FOREGROUND";
	public static final String ACTION_BACKGROUND = "org.bewellapp.BluetoothService.BACKGROUND";

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
	private final int rateNotification = 1000*60*5;
	private final int timeBeforeDeviceTrunOff = 1000*30;

	private BluetoothAdapter mBluetoothAdapter;
	private boolean bluetoothRegistered;

	private boolean bluetoothWasEnabled = false;
	private String devices_found_text = "";
	private ML_toolkit_object bluetooth_object;
	private final String TAG = "Mltoolkit_bluetooth"; 

	public void onCreate() {

		// Register our receiver for the ACTION_SCREEN_OFF action. This will make our receiver
		// code be called whenever the phone enters standby mode.
		//IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_OFF);
		//registerReceiver(AccelerometerManager.mReceiver, filter);



		// screen will stay on during this section
		appState = (Ml_Toolkit_Application) getApplicationContext();
		appState.bluetoothService = this;

		appState.numberOfBluetoothScans = 0;

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
//				"Bluetooth Service Started ",
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

		startBlueToothLookup();

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

		startBlueToothLookup();

		mHandler.removeCallbacks(mUpdateTimeTask);
		mHandler.postDelayed(mUpdateTimeTask, rateNotification);



		// We want this service to continue running until it is explicitly
		// stopped, so return sticky.
		//return START_STICKY;
		return START_STICKY;
	}

	private void startBlueToothLookup() {
		// TODO Auto-generated method stub
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		if (mBluetoothAdapter == null) {
			// Device does not support Bluetooth

			//return immediately by giving a message of not having a bluetooth device
			return;
		}

		//if bluetooth not enabled
		if (!mBluetoothAdapter.isEnabled()) {

			bluetoothWasEnabled = false;
			//enable is an asynchronous call that will return immediately if bluetooth initialization is started already
			//false will be returned immediately if there is a failure--- for example if the phone is in Airplane mode
			boolean bluetooth_status = mBluetoothAdapter.enable();

			//if bluetooth cannot be enabled
			if(bluetooth_status == false)
			{
				//Toast.makeText(this, "Bluetooth cannot enabled", Toast.LENGTH_SHORT).show();		    
				//finish();
				return;
			}	
			//Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			//startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);		    
			//Toast.makeText(this, "Bluetooth Enabled", Toast.LENGTH_SHORT).show();		    
			IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
			registerReceiver(mReceiverDeviceInitiatted, filter); // Don't forget to unregister during onDestroy
			bluetoothRegistered = true;
			//finish();




		}
		else{//bluetooth is already enabled so start discovery
			//Toast.makeText(this, "Bluetooth already enabled", Toast.LENGTH_SHORT).show();
			//finish();		
			bluetoothWasEnabled = true;
			bluetoothRegistered = false;

			mBluetoothAdapter.startDiscovery();
			IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
			filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
			filter.addAction(BluetoothDevice.ACTION_FOUND);
			registerReceiver(mReceiverDeviceDiscovered, filter);

			//Log.d("Bluetooth Trial", "Bluetooth Enabled " + text);
		}

		//schedule turn off bluetooth and discovery listener
		//after 30 seconds
		mHandler.removeCallbacks(mUpdateTimeTaskUnregisterReceiver);
		mHandler.postDelayed(mUpdateTimeTaskUnregisterReceiver, timeBeforeDeviceTrunOff);


	}

	private Runnable mUpdateTimeTaskUnregisterReceiver = new Runnable() {
		public void run() {
			//appState.location_text = "Scanning";
			//myLocationManager.getLocation(LocationService.this, locationResult );
			//updateNotificationArea();
			//mHandler.postAtTime(this,1000*60*1);
			//startBlueToothLookup();

			//mHandler.removeCallbacks(mUpdateTimeTask);
			//mHandler.postDelayed(mUpdateTimeTask, rateNotification);
			//mHandler.postDelayed(mUpdateTimeTask, 1000*60*10);
			//mHandler.postDelayed(mUpdateTimeTask, 1000*30);
			try{

				unregisterReceiver( mReceiverDeviceDiscovered);						
			}
			catch(Exception ex){
				Log.w(TAG,"Blueturned off error" + ex.toString());
			}
			finally{//if mReceiverDeviceDiscovered unregistered because of an error we still continue to unregister others
				try{
					if(bluetoothRegistered) unregisterReceiver(mReceiverDeviceInitiatted);					
				}
				catch(Exception ex){
					Log.w(TAG,"Blueturned off error" + ex.toString());
				}
				finally{//if mReceiverDeviceInitiatted unregistered because of an error we still continue to unregister others
					//if(bluetoothRegistered) unregisterReceiver(mReceiverDeviceInitiatted);

					try{
						if(bluetoothWasEnabled == false)
							mBluetoothAdapter.disable();
						Log.w(TAG,"Blueturned offffffffffffffffffffffffff");}
					catch(Exception ex){
						Log.w(TAG,"Blueturned off error" + ex.toString());
					}
				}
				//if(bluetoothWasEnabled == false)
				//mBluetoothAdapter.disable();
				//Log.w(TAG,"Blueturned offffffffffffffffffffffffff");
			}
		}
	};

	//start discovery to return
	// Create a BroadcastReceiver for ACTION_FOUND
	private final BroadcastReceiver mReceiverDeviceInitiatted = new BroadcastReceiver() {
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			// When discovery finds a device



			//IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
			//registerReceiver(mReceiver, filter); // Don't forget to unregister during onDestroy

			if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
				// Get the BluetoothDevice object from the Intent
				//BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
				// Add the name and address to an array adapter to show in a ListView
				// mArrayAdapter.add(device.getName() + "\n" + device.getAddress());
				int text = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,1);

				//means bluetooth isn't on yet
				if(text != BluetoothAdapter.STATE_ON){
					//means bluetooth isn't on
					//unregisterReceiver(mReceiverDeviceInitiatted);
					return;
				}


				IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
				filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
				filter.addAction(BluetoothDevice.ACTION_FOUND);
				registerReceiver(mReceiverDeviceDiscovered, filter);
				mBluetoothAdapter.startDiscovery();
				//unregisterReceiver(mReceiverDeviceInitiatted);	        		        
				Log.d(TAG, "Bluetooth Enabled .. Starting Discovery" + text);
			}
		}
	};

	//start discovery to return
	// Create a BroadcastReceiver for ACTION_FOUND
	private final BroadcastReceiver mReceiverDeviceDiscovered = new BroadcastReceiver() {
		private int devices_found;		

		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			// When discovery finds a device



			//IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
			//registerReceiver(mReceiver, filter); // Don't forget to unregister during onDestroy

			if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {  
				devices_found = 0;
				Log.d(TAG, "Bluetooth Discovery Started ");
			}
			else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) { 
				//devices_found = 0;
				if(devices_found == 0){
					Log.d(TAG, "No devices found");
					devices_found_text = "No devices found";
				}
				//else

				devices_found = 0;
				Log.d("Bluetooth Trial", "Bluetooth Discovery Finished ");

				//write to database here
				bluetooth_object =  appState.mMlToolkitObjectPool.borrowObject().setValues(System.currentTimeMillis()+appState.timeOffset, 10, devices_found_text.toString().getBytes());
				appState.ML_toolkit_buffer.insert(bluetooth_object);

				appState.numberOfBluetoothScans++;

				//unregister and disable bluetooth control
				//unregisterReceiver( mReceiverDeviceDiscovered);	    
			}
			else if(BluetoothDevice.ACTION_FOUND.equals(action)){
				BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
				// If it's already paired, skip it, because it's been listed
				// already	        
				if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
					Log.w(TAG, "" + device.getName() + " " + device.getAddress());
					devices_found++;
					devices_found_text = devices_found_text + devices_found + ". " + device.getName() + " " + device.getAddress() + "\n";
				}
			}
		}
	};


	private Runnable mUpdateTimeTask = new Runnable() {
		public void run() {
			//appState.location_text = "Scanning";
			//myLocationManager.getLocation(LocationService.this, locationResult );
			updateNotificationArea();
			//mHandler.postAtTime(this,1000*60*1);
			startBlueToothLookup();

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

		Log.e("NOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOO", "NOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOO");
		// Fall back on the old API. Note to cancel BEFORE changing the
		// foreground state, since we could be killed at that point.
//		if (!appState.getServiceController().areServiceUsingNotificationArea()) {
//			mNM.cancel(id);
//		}
//		setForeground(false);
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

		appState.bluetoothService = null;


		//Log.i(TAG, "self destruct done");
	}


	public void updateNotificationArea(){

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




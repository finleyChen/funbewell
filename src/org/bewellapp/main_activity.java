package org.bewellapp;

//import com.simple.gui.R;

import java.util.Timer;
import java.util.TimerTask;

import org.bewellapp.ScoreComputation.ScoreComputationService;
import org.bewellapp.ServiceControllers.ServiceController;
import org.bewellapp.ServiceControllers.AudioLib.MyPhoneStateListener;
import org.bewellapp.ServiceControllers.UploadingLib.InfoObject;
import org.bewellapp.Storage.MySDCardStateListener;
import org.bewellapp.wallpaper.GLWallpaperService;
import org.bewellapp.wallpaper.WellnessSummaryActivity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import org.bewellapp.R;

public class main_activity extends Activity {

	private static ServiceController serviceController;
	public static Context context;
	private TextView accelTextView;  
	private TextView audioTextView;
	private TextView locationTextView;
	private ImageView accelImageView;
	private ImageView audioImageView;
	private ImageView locationImageView;
	private TextView debugTextView;
	private Ml_Toolkit_Application appState;
	private boolean allServiceStopped;

	public boolean[] sensor_status;
	private static final int SENSOR_STATUS_REQUEST = 0;
	//public ServiceConnection aceelServConnection;
	//public ServiceConnection audioServConnection;
	//public ServiceConnection locationServConnection;

	private boolean activity_paused;


	//binder objects
	//public static org.bewellapp.ServiceControllers.AccelerometerLib.AccelerometerService accelServiceBinder;
	//public static org.bewellapp.ServiceControllers.AudioLib.AudioService audioServiceBinder;
	//public static org.bewellapp.ServiceControllers.LocationLib.LocationService locationServiceBinder;

	//previous state strings;
	private boolean prev_location_status = false;
	private String prev_audio_status = "Not Available";
	private String prev_accel_status = "Not Available";


	/*//Disabled for Bewell
	private TextView wifiTextView;	 
	private TextView bluetoothTextView;
	private Timer wifiBluetoothTimer;
	 */

	private Timer accelTimer;//timer for acceleromter time update
	private Timer audioTimer;
	private Timer locationTimer;

	public main_activity()
	{
		//context = this;
		//serviceController = new ServiceController(context);

	}

	//inference results
	private boolean showInference = true;
	private TextView accelTextViewDC;
	private TextView audioTextViewDC;

	private static final String TAG = "ML_Toolkit";
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		appState = (Ml_Toolkit_Application)getApplicationContext();
		appState.set_main_activity_Context(this);
		serviceController = appState.getServiceController();


		Log.i(TAG, "onCreate ...");
		setContentView(R.layout.main);

		//sensor status controlling
		sensor_status = new boolean[3];
		for(int i = 0;i < sensor_status.length;++i)
			sensor_status[i] = true;


		// try to display a activity
		/*Button btn_label = (Button) findViewById(R.id.button_label_activity);
		btn_label.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				Intent intent = new Intent();
				//intent.putExtra("name", editText.getText().toString());
				intent.setAction("org.bewellapp.LABEL");
				startActivity(intent);
			}
		});

		// try to display a activity
		Button btn_sensor = (Button) findViewById(R.id.button_sensor_activity);
		btn_sensor.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				Intent intent = new Intent();
				intent.putExtra("sensor_status", sensor_status);
				intent.setAction("org.bewellapp.SENSOR");
				//startActivity(intent);
				startActivityForResult(intent,SENSOR_STATUS_REQUEST);
			}
		});*/

		accelTextView = (TextView) findViewById(R.id.button_status_acc);
		audioTextView = (TextView) findViewById(R.id.button_status_audio);
		accelTextViewDC = (TextView) findViewById(R.id.text_accel);
		audioTextViewDC = (TextView) findViewById(R.id.text_audio);
		locationTextView = (TextView) findViewById(R.id.button_status_location);

		//Disabled for Bewell
		//wifiTextView = (TextView) findViewById(R.id.button_status_wifi);
		//bluetoothTextView = (TextView) findViewById(R.id.button_status_bluetooth);


		accelImageView = (ImageView) findViewById(R.id.status_icon_accel);
		audioImageView = (ImageView) findViewById(R.id.status_icon_audio);
		locationImageView = (ImageView) findViewById(R.id.status_icon_location);

		//debugTextView = (TextView) findViewById(R.id.Debug_textView);
		//debugTextView.setText("\n\n\nDebug Info" + this.appState.db_adapter.getPathOfDatabase());

		//initialzing the database
		//this.appState.db_adapter.open();
		//debugTextView.setText("");
		//debugTextView.setText("\n\n\nDebug Info" + this.appState.db_adapter.getPathOfDatabase());
		//debugTextView.setText("\n\n\nDebug Info" + this.appState.currentFileName);



		//main activity will never start the sensing servcices twice
		if(appState.applicatin_starated == false && 1 < 0){


			//only at start intialize ml toolkit
			//appState.mlt = new MLToolkitInterface(); 

			//MLToolkitInterface.audio_config audio_Cfg = appState.mlt.new audio_config(appState.AUDIO_SAMPLERATE,appState.AUDIO_SAMPLES_REQUIRED, 20,14);
			//MLToolkitInterface.acc_config acc_Cfg = appState.mlt.new acc_config(appState.ACCEL_SAMPLERATE,appState.ACCEL_SAMPLES_REQUIRED);
			//appState.mlt.init(acc_Cfg, audio_Cfg, null);




			appState.applicatin_starated = true;


			//getting the IMEI
			TelephonyManager mTelephonyMgr = (TelephonyManager)getSystemService(TELEPHONY_SERVICE);
			appState.IMEI = mTelephonyMgr.getDeviceId();
			appState.initializeDataBase();
			appState.db_adapter.open();
			Log.w("DBNAME", "DB NAME" + this.appState.db_adapter.getPathOfDatabase());
			//debugTextView.setText("\n\n\nDebug Info" + this.appState.db_adapter.getPathOfDatabase());

			appState.initializeMLtoolkitConfiguration();



			//this.bindAudioService();

			//try to start the services
			//start the accelerometer service

			if(appState.savedAccelSensorOn == true)
				serviceController.startAccelerometerSensor();

			//this.appState.setAccelerometerTurnedAtSensorSelctionScreen(true);
			//this.bindAccelService();



			//start the gps service
			if(appState.savedLocationSensorOn == true)
				serviceController.startLocationSensor();
			//this.bindLocationService();

			/* // Diabled for Bewell
			//start the Bluetooth service
			if(appState.savedBluetoothSensorOn == true)
				serviceController.startBluetoothService();

			//start the Wifi service
			if(appState.savedWifiSensorOn == true)
				serviceController.startWifiService();
			 */


			//start the audio service
			if(appState.savedAudioSensorOn == true)
				serviceController.startAudioSensor();

			serviceController.startDaemonService();
			serviceController.startNTPTimestampsService();
			serviceController.startMomentarySamplingService();
			serviceController.startSleepService();
			serviceController.startAppMonitor();

			//start uploading service
			//serviceController.startUploading();

			//Toast.makeText(this,
			//	this.appState.db_adapter.getPathOfDatabase(),
			//Toast.LENGTH_SHORT).show();

			//start connectivty manager
			//appState.connec = (ConnectivityManager)appState.getSystemService(Context.CONNECTIVITY_SERVICE);
			//NetworkInfo info = mlobj.connec.getActiveNetworkInfo();
			appState.infoObj = new InfoObject(this, appState);
			//appState.phoneCallInfo = new MyPhoneStateListener(this,appState);
			appState.phoneCallInfo = new MyPhoneStateListener(this,appState);
			appState.mySDCardStateInfo = new MySDCardStateListener(this, appState);

			//application started .... service started

			//initialize allServiceStopped
			allServiceStopped = false;
		}

		//still to start binding
		this.activity_paused = true;

		//create null object, tries to throw a null pointer exception


		//appState.errorReporter.CheckErrorAndSendMail(main_activity.this);
		//send error reporting email


	}



	private void enableLocationUpdate()
	{
		//start a timer to update the gui
		locationTimer = new Timer();
		locationTimer.schedule(new TimerTask() {
			@Override
			public void run() {
				locationTimerMethod();
			}

		}, 0,500); 

	}

	/*
	private void enableWifiBluetoothUpdate() {
		wifiBluetoothTimer = new Timer();
		wifiBluetoothTimer.schedule(new TimerTask() {
			@Override
			public void run() {
				wifiBluetoothTimerMethod();
			}

		}, 0, 500);
	}*/

	private void enableAudioUpdate()
	{
		//start a timer to update the gui
		audioTimer = new Timer();
		audioTimer.schedule(new TimerTask() {
			@Override
			public void run() {
				audioTimerMethod();
			}

		}, 0, 250);
		//appState.accelerometerSensor = true;
	}


	private void enableAccelUpdate()
	{
		//start a timer to update the gui
		accelTimer = new Timer();
		accelTimer.schedule(new TimerTask() {
			@Override
			public void run() {
				aceelTimerMethod();
			}

		}, 0, 500);
		//appState.accelerometerSensor = true;
	}


	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data){
		if(requestCode == SENSOR_STATUS_REQUEST){
			if(resultCode == RESULT_CANCELED){
				sensor_status = data.getBooleanArrayExtra("sensor_status");
			}
		}	
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// TODO Auto-generated method stub
		super.onCreateOptionsMenu(menu);
		menu.add(0, Menu.FIRST + 1, 1, "Settings");
		menu.add(0, Menu.FIRST + 2, 2, "Upload");
		//menu.add(0, Menu.FIRST + 2, 2, "Survey");
		//menu.add(0, Menu.FIRST + 3, 3, "About");
		menu.add(0, Menu.FIRST + 3, 3, "Status");
		//menu.add(0, Menu.FIRST + 4, 4, "Quit");
		//menu.add(0, Menu.FIRST + 4, 4, "Label");
//		menu.add(0, Menu.FIRST + 4, 4, "Start");
//		menu.add(0, Menu.FIRST + 5, 5, "Stop");
		menu.add(0, Menu.FIRST + 4, 4, "Scores");
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// TODO Auto-generated method stub
		super.onOptionsItemSelected(item);
		switch (item.getItemId()) {

		case Menu.FIRST + 2: { //upload status
			Toast.makeText(this, "Data remaining to upload:\n" + appState.fileList.size() + " MB",
					Toast.LENGTH_SHORT).show();			
			break;
		}

		case Menu.FIRST + 1: {
			// Open a survey activity
			//Intent intent = new Intent();
			//intent.putExtra("sensor_status", sensor_status);
			//intent.setAction("org.bewellapp.SURVEY");
			//startActivity(intent);
			//startActivityForResult(intent,SENSOR_STATUS_REQUEST);

			if(appState.all_sesning_disabled == true)
			{
				Toast.makeText(this, "All sesning disabled. Please contact the admin.",
						Toast.LENGTH_SHORT).show();	
				return true;
			}

			Intent intent = new Intent();
			//intent.putExtra("sensor_status", sensor_status);
			intent.setAction("org.bewellapp.SENSOR");
			//intent.setAction("org.bewellapp.MomentarySampling.momentary_list");
			//startActivity(intent);
			startActivity(intent);
			break;
		}

//		case Menu.FIRST + 5: {
//			/*//Toast.makeText(this, " Sensor Group  &  PAC Group\n Depart. of Computer Science\n Dartmouth College",
//			//	Toast.LENGTH_LONG).show();
//			Intent intent = new Intent();
//			//intent.putExtra("sensor_status", sensor_status);
//			intent.setAction("org.bewellapp.ABOUT");
//			startActivity(intent);*/
//
//			Intent intent = new Intent();
//			//intent.putExtra("name", editText.getText().toString());
//			intent.setAction("org.bewellapp.LABEL");
//			startActivity(intent);
//
//			break;
//		}

		/*case Menu.FIRST + 4: {
			//Toast.makeText(this, " Sensor Group  &  PAC Group\n Depart. of Computer Science\n Dartmouth College",
			//	Toast.LENGTH_LONG).show();
			Intent intent = new Intent();
			//intent.putExtra("sensor_status", sensor_status);
			intent.setAction("org.bewellapp.ABOUT");
			startActivity(intent);

			Intent intent = new Intent();
			//intent.putExtra("name", editText.getText().toString());
			intent.setAction("org.bewellapp.LABEL");
			startActivity(intent);



			//appState.startQuitSequence();
			//finish();
			//break;
		}*/

		case Menu.FIRST + 3: {
			//System.exit(0);
			//break;
			Intent intent = new Intent();
			//intent.putExtra("sensor_status", sensor_status);
			intent.setAction("org.bewellapp.STATUS");
			startActivity(intent);
			//startActivityForResult(intent,SENSOR_STATUS_REQUEST);
			break;
		}
		
		// begin add for Andrew's demo purpose

//		case Menu.FIRST + 4: {
//			//Start all services
//
//			if (allServiceStopped)
//			{
//				//start the audio service
//				if(appState.savedAudioSensorOn == true)
//				{
//					appState.audioForceLocked = false;
//					appState.getServiceController().startAudioSensor();
//				}
//					
//				//start the accelerometer service
//
//				if(appState.savedAccelSensorOn == true)
//					appState.getServiceController().startAccelerometerSensor();
//
//				//start the gps service
//				if(appState.savedLocationSensorOn == true)
//					appState.getServiceController().startLocationSensor();
//				
//				//start the NTPTimestamps service
//				serviceController.startNTPTimestampsService();
//
//				
//				allServiceStopped = false;
//			}
//			
//			else
//			{
//				Toast.makeText(this, " Services are not stopped!",
//						Toast.LENGTH_LONG).show();
//			}
//			
//			break;
//		}
		  
//		case Menu.FIRST + 5: {
//			//Stop all services
//
//			if (!allServiceStopped)
//			{
//				appState.audioForceLocked = true; 
//				appState.getServiceController().stopAudioSensor();
//				appState.getServiceController().stopAccelerometerSensor();
//				appState.getServiceController().stopLocationSensor();
//				appState.getServiceController().stopNTPTimestampsService();
//				
//				allServiceStopped = true;
//			}
//			
//			else
//			{
//				Toast.makeText(this, " Services are already stopped!",
//						Toast.LENGTH_LONG).show();
//			}
//			
//			break;
//		}
		  
		 //end add for Andrew's demo purpose
		
		case Menu.FIRST + 4: {
			Intent intent = new Intent(main_activity.this, WellnessSummaryActivity.class);			
			startActivity(intent);
			break;
		}
		
		}
		return true;
	}


	@Override
	public void onStart() {
		super.onStart();
		Log.i(TAG, "onStart ...");
	}

	@Override
	public void onResume() {
		super.onResume();


		//bind the listener when come back to the screen
		/*if(appState.getAccelerometerTurnedAtSensorSelctionScreen() == true)
		{
			//Intent bindIntent = new Intent(main_activity.this,org.bewellapp.ServiceControllers.AccelerometerLib.AccelerometerService.class);			
			//bindService(bindIntent,aceelServConnection,Context.BIND_AUTO_CREATE);
			bindAccelService();
			appState.setAccelerometerTurnedAtSensorSelctionScreen(false);
			appState.accelerometerSensor = true;
		}*/
		//accelerometer sensing status change
		//means accel sensor status was changed in the label screen to on status
		/*if(sensor_status[1] == true  && accelServiceBinder == null)
		{
			//connect again
			Intent bindIntent = new Intent(main_activity.this,org.bewellapp.ServiceControllers.AccelerometerLib.AccelerometerService.class);			
			bindService(bindIntent,aceelServConnection,Context.BIND_AUTO_CREATE);
		}
		else if(sensor_status[1] == false  && accelServiceBinder != null)
		{
			unbindService(aceelServConnection);
		}*/

		try{
			if(appState.errorReporter.bIsThereAnyErrorFile(main_activity.this))
			{
				Log.e("main_activity", "there are errors to repport");
				AlertDialog.Builder builder = new AlertDialog.Builder(this);
				builder.setMessage("This program crashed before unexpectedly.\nDo you want to send a crash report?")
				.setCancelable(false)
				.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						Thread t = new Thread() {
							public void run() {
								appState.errorReporter.CheckErrorAndSendMail(main_activity.this);
							}
						};
						t.start();
					}
				})
				.setNegativeButton("No", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						appState.errorReporter.deleteErrorFiles();
						dialog.cancel();
					}
				});
				AlertDialog alert = builder.create();
				alert.show();
				//appState.errorReporter.CheckErrorAndSendMail(this);
			}
		}
		catch(Exception ex){
			Log.e("main_activity", "no error log ");
		}



		enableUIUpdates();


		//change the Accel and audio textview



		prev_accel_status = "Not Available";//to make an immediate change in status if any
		prev_audio_status = "Not Available";//to make an immediate change in status if any


		if(appState.enableAccelDutyCycling)
			accelTextViewDC.setText("Accelerometer - DC");
		else
			accelTextViewDC.setText("Accelerometer");

		if(appState.enableAudioDutyCycling)
			audioTextViewDC.setText("Audio - DC");
		else
			audioTextViewDC.setText("Audio");

		//bindings are online so start reading the values again
		this.activity_paused = false;
		//Log.i(TAG, "onResume ...");
	}


	/* //Disabled for Bewell
	protected void wifiBluetoothTimerMethod() {
		// TODO Auto-generated method stub
		this.runOnUiThread(wifiBluetoothTimer_Tick);
	}

	private Runnable wifiBluetoothTimer_Tick = new Runnable() {
		public void run() {

			//This method runs in the same thread as the UI.    	       
			//Log.i(TAG, "accel_timer_executing");

			//Do something to the UI thread here
			if (appState.wifiSensorOn == false){// || accelServiceBinder == null || activity_paused == true){
				wifiTextView.setText("Wifi: Not Available");				
			}
			else
			{
				wifiTextView.setText("Wifi ON: # of samples - " + appState.numberOfWifiScans);
			}


			//accelTextView.sett
			if (appState.bluetoothSensorOn == false){// || accelServiceBinder == null || activity_paused == true){
				bluetoothTextView.setText("Bluetooth: Not Available");				
			}
			else
			{
				bluetoothTextView.setText("Bluetooth ON: # of samples - " + appState.numberOfBluetoothScans);
			}
		}
	};
	 */

	@Override
	public void onPause() {
		super.onPause();	

		//binds will be void .... so stop showing results
		this.activity_paused = true;

		disableUIUpdates();

		Log.i(TAG, "onPause ...");
	}

	@Override
	public void onStop() {
		super.onStop();
		Log.i(TAG, "onStop ...");
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		Log.i(TAG, "onDestroy ...");
	}

	@Override
	public void onRestart() {
		super.onDestroy();
		Log.i(TAG, "onRestart ...");
	}




	/*

	private ServiceConnection aceelServConnection = new ServiceConnection() {

		@Override
		public void onServiceDisconnected(ComponentName name) {
			// TODO Auto-generated method stub
			accelServiceBinder = null;
		}

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			// TODO Auto-generated method stub
			accelServiceBinder = ((org.bewellapp.ServiceControllers.AccelerometerLib.AccelerometerService.AccelerometerBinder)service).getService();						
		}
	};
	 */

	//accel timer; per 500ms update
	//
	protected void aceelTimerMethod() {
		//this method will run on the timer thread

		// TODO Auto-generated method stub
		//this will run on the UI thread
		this.runOnUiThread(accelTimer_Tick);
	}

	private Runnable accelTimer_Tick = new Runnable() {
		public void run() {

			//This method runs in the same thread as the UI.    	       
			//Log.i(TAG, "accel_timer_executing");

			//Do something to the UI thread here
			if (appState.accelSensorOn == false ||  activity_paused == true){
				accelTextView.setText("Not Available");
				accelImageView.setImageResource(R.drawable.cross);
				//setAccelImage("Not Available");
			}
			//accelTextView.sett
			else{
				//accelTextView.setText(accelServiceBinder.inferred_status);
				if(showInference == false){
					accelTextView.setText("Accel. On");
					accelImageView.setImageResource(R.drawable.tick);
				}
				else{
					accelTextView.setText(appState.accelerometer_inference);
					setAccelImage(appState.accelerometer_inference);
				}

				//Log.i(TAG, "accel_sensor_executing");
			}
		}
	};

	protected void setAccelImage(String inferred_status) {
		// TODO Auto-generated method stub
		//String current_image; 
		if(!this.prev_accel_status.equals(inferred_status)){
			prev_accel_status = inferred_status;

			if(inferred_status.equals("Not Available")){
				this.accelImageView.setImageResource(R.drawable.cross);
			}
			else{
				if(this.showInference == false)
					this.accelImageView.setImageResource(R.drawable.tick);
				else
				{
					if(inferred_status.equals("unknown")){
						this.accelImageView.setImageResource(R.drawable.help_001);
					}
					else if(inferred_status.equals("stationary")){
						this.accelImageView.setImageResource(R.drawable.stationary_001);
					}
					else if(inferred_status.equals("driving")){
						this.accelImageView.setImageResource(R.drawable.car_001);
					}
					else if(inferred_status.equals("walking")){
						this.accelImageView.setImageResource(R.drawable.walking_001);
					}
					else if(inferred_status.equals("running")){
						this.accelImageView.setImageResource(R.drawable.running_001);
					}
					else if(inferred_status.equals("vehicle")){
						this.accelImageView.setImageResource(R.drawable.car_001);
					}
					else if(inferred_status.equals("error")){
						this.accelImageView.setImageResource(R.drawable.confused_001);
					}
					else if(inferred_status.equals("DutyCycling OFF")){
						this.accelImageView.setImageResource(R.drawable.pause);
					}
				}			
			}
			/*
			if(inferred_status.equals("Not Available")){
				this.accelImageView.setImageResource(R.drawable.cross);
			}
			else if(inferred_status.equals("unknown")){
				this.accelImageView.setImageResource(R.drawable.help_001);
			}
			else if(inferred_status.equals("stationary")){
				this.accelImageView.setImageResource(R.drawable.stationary_001);
			}
			else if(inferred_status.equals("walking")){
				this.accelImageView.setImageResource(R.drawable.walking_001);
			}
			else if(inferred_status.equals("running")){
				this.accelImageView.setImageResource(R.drawable.running_001);
			}
			else if(inferred_status.equals("vehicle")){
				this.accelImageView.setImageResource(R.drawable.car_001);
			}
			else if(inferred_status.equals("error")){
				this.accelImageView.setImageResource(R.drawable.confused_001);
			}*/

		}

	}

	//audio timer; per 500ms update
	//
	protected void audioTimerMethod() {
		//this method will run on the timer thread

		// TODO Auto-generated method stub
		//this will run on the UI thread
		this.runOnUiThread(audioTimer_Tick);
	}





	private Runnable audioTimer_Tick = new Runnable() {
		public void run() {

			//This method runs in the same thread as the UI.    	       
			//Log.i(TAG, "accel_timer_executing");

			//Do something to the UI thread here
			if (appState.audioSensorOn == false ||  activity_paused == true){
				audioTextView.setText("Not Available");
				audioImageView.setImageResource(R.drawable.cross);
			}
			//accelTextView.sett
			else{
				//audioTextView.setText(audioServiceBinder.inferred_audio_Status);
				//if(appState.enableAudioDutyCycling){
				//	audioTextView.setText("Audio On:\n Recording " + ((appState.audioService!=null)?"ON":"Off"));
				//	audioImageView.setImageResource(R.drawable.tick);
				//}
				//else{
				if(showInference == false){
					audioTextView.setText("Audio On");
					audioImageView.setImageResource(R.drawable.tick);
				}
				else
				{
					audioTextView.setText(appState.audio_inference);
					setAudioImage(appState.audio_inference);
				}
				//}
				//if(appState.audioService != null)
				//setAudioImage(appState.audioService.inferred_audio_Status);
				//else
				//audioImageView.setImageResource(R.drawable.tick);
				//Log.e(TAG, "calling audio update _executing " + appState.audioServiceStarted);
			}
		}
	};

	protected void setAudioImage(String inferred_status) {
		// TODO Auto-generated method stub
		if(!this.prev_audio_status.equals(inferred_status)){
			prev_audio_status = inferred_status;

			if(inferred_status.equals("Not Available"))
				this.audioImageView.setImageResource(R.drawable.cross);
			else{
				if(this.showInference == false)
					this.audioImageView.setImageResource(R.drawable.tick);
				else{
					if(inferred_status.equals("error")){
						this.audioImageView.setImageResource(R.drawable.confused_001);
					}
					else if(inferred_status.equals("voice")){
						this.audioImageView.setImageResource(R.drawable.talking_001);
					}
					else if(inferred_status.equals("noise")){
						this.audioImageView.setImageResource(R.drawable.noise_001);
					}
					else if(inferred_status.equals("silence")){
						this.audioImageView.setImageResource(R.drawable.quiet_001);
					}
					else if(inferred_status.equals("DutyCycling OFF")){
						this.audioImageView.setImageResource(R.drawable.pause);
					}
				}
			}
			/*if(inferred_status.equals("Not Available")){
				this.audioImageView.setImageResource(R.drawable.cross);
			}
			else if(inferred_status.equals("error")){
				this.audioImageView.setImageResource(R.drawable.confused_001);
			}
			else if(inferred_status.equals("voice")){
				this.audioImageView.setImageResource(R.drawable.talking_001);
			}
			else if(inferred_status.equals("noise")){
				this.audioImageView.setImageResource(R.drawable.noise_001);
			}
			else if(inferred_status.equals("silence")){
				this.audioImageView.setImageResource(R.drawable.quiet_001);
			}*/
		}
	}



	protected void locationTimerMethod() {
		//this method will run on the timer thread

		// TODO Auto-generated method stub
		//this will run on the UI thread
		this.runOnUiThread(locationTimer_Tick);
	}




	private Runnable locationTimer_Tick = new Runnable() {
		public void run() {

			//This method runs in the same thread as the UI.    	       
			//Log.i(TAG, "accel_timer_executing");

			//Do something to the UI thread here
			if (appState.locationSensorOn == false ||  activity_paused == true){
				locationTextView.setText("Not available");
				setLocationImage(false);
			}
			//accelTextView.sett
			else{
//				locationTextView.setText(appState.locationService.inferred_location_Status);
//				if(appState.locationService.inferred_location_Status.startsWith("No location found")){
//					setLocationImage(false);
//				}
//				else
//					setLocationImage(appState.locationSensorOn);
				//Log.i(TAG, "location_sensor_executing");
			}
		}
	};

	protected void setLocationImage(boolean locationSensorOnn) {
		// TODO Auto-generated method stub
		if(this.prev_location_status != locationSensorOnn)
		{
			prev_location_status = locationSensorOnn;
			if(locationSensorOnn)
				this.locationImageView.setImageResource(R.drawable.tick);
			else
				this.locationImageView.setImageResource(R.drawable.cross);
		}

	}

	private void enableUIUpdates() {
		//means only will try to ring if service is running
		//problematic because we need to update UI even when there is no
		//service to show "Not available"
		//if(appState.locationSensorOn)
		this.enableLocationUpdate();
		//if(appState.accelSensorOn)
		this.enableAccelUpdate();
		//if(appState.audioSensorOn)
		this.enableAudioUpdate();

		//Disabled for Bewell
		//enableWifiBluetoothUpdate();
	}

	private void disableUIUpdates() {
		if (locationTimer != null) {
			locationTimer.cancel();
			locationTimer = null;
		}
		if (accelTimer != null) {
			accelTimer.cancel();
			accelTimer = null;
		}
		if (audioTimer != null) {
			audioTimer.cancel();
			audioTimer = null;
		}

		/* //Disabled for Bewell
		if (wifiBluetoothTimer != null) {
			wifiBluetoothTimer.cancel();
			wifiBluetoothTimer = null;
		}
		 */
	}

}
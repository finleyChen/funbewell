package org.bewellapp;

import org.bewellapp.ServiceControllers.ServiceController;
import org.bewellapp.ServiceControllers.AudioLib.MyPhoneStateListener;
import org.bewellapp.ServiceControllers.UploadingLib.InfoObject;
import org.bewellapp.Storage.MySDCardStateListener;
import org.bewellapp.wallpaper.WellnessSummaryActivity;

import org.bewellapp.R;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

public class dashBoardActivity extends Activity {

	private boolean alreadyStopped;
	private String pauseString;
	private String startString;
	private int threshold = 10;		//button click threshold: x seconds
	private long lastBtnClick;
	private long currentBtnClick;

	//for app launch
	private static ServiceController serviceController;
	public static Context context;
	private Ml_Toolkit_Application appState;
	public boolean[] sensor_status;
	private static final int SENSOR_STATUS_REQUEST = 0;
	private boolean activity_paused;


	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.dash);

		//cut here
		SharedPreferences.Editor spe = getSharedPreferences(Ml_Toolkit_Application.SHARED_PREF, MODE_PRIVATE).edit();
		spe.putBoolean(Ml_Toolkit_Application.SP_WELCOMED, true);
		spe.commit();
		
		pauseString = getString(R.string.pauseString);
		startString = getString(R.string.startString);

		appState = (Ml_Toolkit_Application)getApplicationContext();
		//appState.set_summary_activity_Context(this);
		serviceController = appState.getServiceController();

		/*
		//stop/start sensing button
		final ImageButton sensingBtn = (ImageButton) findViewById(R.id.sensingButton);
		final TextView tv = (TextView) findViewById(R.id.dashTextView2);
		//set Button status
		alreadyStopped = getSharedPreferences(Ml_Toolkit_Application.SHARED_PREF, MODE_PRIVATE).getBoolean(Ml_Toolkit_Application.SP_ISSTOPPED, false);
		if (alreadyStopped)
		{
			sensingBtn.setImageResource(R.drawable.resume_sensing);
			tv.setText(startString);
		}
		else
		{
			sensingBtn.setImageResource(R.drawable.pause_sensing);
			tv.setText(pauseString);
		}

		//onClick event
		sensingBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View arg0) {

				lastBtnClick = currentBtnClick;
				currentBtnClick = System.currentTimeMillis();
				//if less than 10 seconds
				if (currentBtnClick - lastBtnClick <= threshold * 1000)
				{
					String text = String.format("You operated too fast :)\nPlease wait for %d seconds", threshold);
					Toast.makeText(getApplicationContext(), text, Toast.LENGTH_SHORT).show();
					return;
				}

				String text = tv.getText().toString();

				if (text.equalsIgnoreCase(pauseString))
					//stop sensing
				{
					//update text
					sensingBtn.setImageResource(R.drawable.resume_sensing);
					tv.setText(startString);

					appState.audioForceLocked = true; 
					serviceController.stopAudioSensor();
					serviceController.stopAccelerometerSensor();
					serviceController.stopLocationSensor();
					serviceController.stopNTPTimestampsService();
					serviceController.stopSleepService();
					serviceController.stopAppMonitor();
					appState.daemonService.stopSelf();

					//record status
					SharedPreferences.Editor spe = getSharedPreferences(Ml_Toolkit_Application.SHARED_PREF, MODE_PRIVATE).edit();
					spe.putBoolean(Ml_Toolkit_Application.SP_ISSTOPPED, true);
					spe.commit();
					
					appState.writeToPropertiesFile(appState.audioSensorOn, appState.accelSensorOn, appState.locationSensorOn, appState.savedRawSensorOn,appState.savedWifiSensorOn, appState.savedBluetoothSensorOn,
							appState.enableAccelDutyCycling, appState.enableAudioDutyCycling, appState.rawAccelOn);

				}
				else
					//start sensing
				{
					//update text
					sensingBtn.setImageResource(R.drawable.pause_sensing);
					tv.setText(pauseString);

					appState.audioForceLocked = false; 
					serviceController.startAudioSensor();
					serviceController.startAccelerometerSensor();
					serviceController.startLocationSensor();
					serviceController.startNTPTimestampsService();
					serviceController.startSleepService();
					serviceController.startAppMonitor();
					serviceController.startDaemonService();


					//record status
					SharedPreferences.Editor spe = getSharedPreferences(Ml_Toolkit_Application.SHARED_PREF, MODE_PRIVATE).edit();
					spe.putBoolean(Ml_Toolkit_Application.SP_ISSTOPPED, false);
					spe.commit();
					
					appState.writeToPropertiesFile(appState.audioSensorOn, appState.accelSensorOn, appState.locationSensorOn, appState.savedRawSensorOn,appState.savedWifiSensorOn, appState.savedBluetoothSensorOn,
							appState.enableAccelDutyCycling, appState.enableAudioDutyCycling, appState.rawAccelOn);

				}

			}
		});
		

		//score button
		ImageButton scoreBtn = (ImageButton) findViewById(R.id.scoreButton);
		scoreBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View arg0) {
				Intent intent = new Intent(dashBoardActivity.this, WellnessSummaryActivity.class);			
				startActivity(intent);
			}
		});

*/
		//web button
		ImageButton webBtn = (ImageButton) findViewById(R.id.webButton);
		webBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View arg0) {
				String url = "https://biorhythm.cs.dartmouth.edu";
				Intent in = new Intent(Intent.ACTION_VIEW);
				in.setData(Uri.parse(url));
				startActivity(in);		
			}
		});
		
		//sign-in button
		ImageButton signInBtn = (ImageButton) findViewById(R.id.registerButton);
		signInBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View arg0) {
				//sign-in
				SharedPreferences sp = getSharedPreferences(Ml_Toolkit_Application.SHARED_PREF, MODE_PRIVATE);
				if (sp.getBoolean(Ml_Toolkit_Application.SP_ISREGISTERED, false)) {
					//already signed in
					Toast.makeText(getApplicationContext(), "You are already signed in.\nThanks!", Toast.LENGTH_SHORT).show();
				}
				else{
					Intent in = new Intent("org.bewellapp.SignIn");
					startActivity(in);
				}
			}
		});

		//score button
//		Button inferenceBtn = (Button) findViewById(R.id.btnInference);
//		inferenceBtn.setOnClickListener(new View.OnClickListener() {
//			@Override
//			public void onClick(View arg0) {
//				Intent intent = new Intent(dashBoardActivity.this, main_activity.class);			
//				startActivity(intent);
//			}
//		});
				
		//
		//for app launch
		//
		//sensor status controlling
		sensor_status = new boolean[3];
		for(int i = 0;i < sensor_status.length;++i)
			sensor_status[i] = true;

		//launch activity will never start the sensing servcices twice
		if(appState.applicatin_starated == false){

			appState.applicatin_starated = true;


			//getting the IMEI
			TelephonyManager mTelephonyMgr = (TelephonyManager)getSystemService(TELEPHONY_SERVICE);
			appState.IMEI = mTelephonyMgr.getDeviceId();
			appState.initializeDataBase();
			appState.db_adapter.open();
			Log.w("DBNAME", "DB NAME" + this.appState.db_adapter.getPathOfDatabase());
			//debugTextView.setText("\n\n\nDebug Info" + this.appState.db_adapter.getPathOfDatabase());


			appState.initializeMLtoolkitConfiguration();

			if(appState.savedAccelSensorOn == true)
				serviceController.startAccelerometerSensor();


			//start the gps service
			if(appState.savedLocationSensorOn == true)
				serviceController.startLocationSensor();
			//this.bindLocationService();


			//start the audio service
			if(appState.savedAudioSensorOn == true)
				serviceController.startAudioSensor();

			if (!alreadyStopped)
			{
				serviceController.startDaemonService();
				serviceController.startNTPTimestampsService();
				serviceController.startMomentarySamplingService();
				serviceController.startSleepService();
				serviceController.startAppMonitor();
			}
			
			serviceController.startFunfService();
			appState.infoObj = new InfoObject(this, appState);
			//appState.phoneCallInfo = new MyPhoneStateListener(this,appState);
			appState.phoneCallInfo = new MyPhoneStateListener(this,appState);
			appState.mySDCardStateInfo = new MySDCardStateListener(this, appState);

			//application started .... service started
		}

		//still to start binding
		this.activity_paused = true;


	}
}

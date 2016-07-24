package org.bewellapp.BootUpServices;

import org.bewellapp.Ml_Toolkit_Application;
import org.bewellapp.ServiceControllers.ServiceController;
import org.bewellapp.ServiceControllers.AudioLib.MyPhoneStateListener;
import org.bewellapp.ServiceControllers.UploadingLib.InfoObject;
import org.bewellapp.Storage.MySDCardStateListener;
//import edu.dartmouthcs.sensorlab.MLToolkitInterface;
//import edu.dartmouthcs.sensorlab.MLToolkitInterface.acc_config;
//import edu.dartmouthcs.sensorlab.MLToolkitInterface.audio_config;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;


public class BootAutoStartUpService extends Service {


	private Ml_Toolkit_Application appState;
	private static ServiceController serviceController;
	private Thread t;
	static final String TAG = "MLtoolkitBootupStarterasfddddddddddddddddddddddddd";
	public void onCreate() {
		super.onCreate();
		appState = (Ml_Toolkit_Application)getApplicationContext();
		serviceController = appState.getServiceController();


		if(appState.applicatin_starated == false){

			//only at start intialize ml toolkit
			//appState.mlt = new MLToolkitInterface(); 

			//MLToolkitInterface.audio_config audio_Cfg = appState.mlt.new audio_config(appState.AUDIO_SAMPLERATE,appState.AUDIO_SAMPLES_REQUIRED, 20,14);
			//MLToolkitInterface.acc_config acc_Cfg = appState.mlt.new acc_config(appState.ACCEL_SAMPLERATE,appState.ACCEL_SAMPLES_REQUIRED);
			//appState.mlt.init(acc_Cfg, audio_Cfg, null);
			
			
			//application started .... service started
			appState.applicatin_starated = true;

			//getting the IMEI
			TelephonyManager mTelephonyMgr = (TelephonyManager)getSystemService(TELEPHONY_SERVICE);
			appState.IMEI = mTelephonyMgr.getDeviceId();
			appState.initializeDataBase();
			appState.db_adapter.open();
			//debugTextView.setText("\n\n\nDebug Info" + this.appState.db_adapter.getPathOfDatabase());

			appState.initializeMLtoolkitConfiguration();

			//start the audio service
			appState.savedAudioSensorOn = true;
			if(appState.savedAudioSensorOn == true)
				serviceController.startAudioSensor();
			//this.bindAudioService();

			//try to start the services
			//start the accelerometer service
			appState.savedAccelSensorOn = true;
			if(appState.savedAccelSensorOn == true)
				serviceController.startAccelerometerSensor();
			//this.appState.setAccelerometerTurnedAtSensorSelctionScreen(true);
			//this.bindAccelService();

			//start the gps service
			appState.savedLocationSensorOn = true;
			if(appState.savedLocationSensorOn == true)
				serviceController.startLocationSensor();
			//this.bindLocationService();
			
			
			/* Disabled for Bewell 
			//start the Bluetooth service			
			if(appState.savedBluetoothSensorOn == true)
				serviceController.startBluetoothService();
			
			//start the Wifi service
			if(appState.savedWifiSensorOn == true)
				serviceController.startWifiService();
			*/

			
			serviceController.startDaemonService();
			serviceController.startNTPTimestampsService();
			serviceController.startMomentarySamplingService();
			serviceController.startSleepService();
			serviceController.startAppMonitor();
			
			serviceController.startFunfService();
			//Log.i(TAG, "location started");
			//still to start binding
			//this.activity_paused = true;
			
			//record status
			SharedPreferences.Editor spe = getSharedPreferences(Ml_Toolkit_Application.SHARED_PREF, MODE_PRIVATE).edit();
			spe.putBoolean(Ml_Toolkit_Application.SP_ISSTOPPED, false);
			spe.commit();

			appState.writeToPropertiesFile(appState.audioSensorOn, appState.accelSensorOn, appState.locationSensorOn, appState.savedRawSensorOn,appState.savedWifiSensorOn, appState.savedBluetoothSensorOn,
					appState.enableAccelDutyCycling, appState.enableAudioDutyCycling, appState.rawAccelOn);

			//start uploading service
			//serviceController.startUploading();

			//Toast.makeText(this,
			//	this.appState.db_adapter.getPathOfDatabase(),
			//Toast.LENGTH_SHORT).show();

			//start connectivty manager
			//appState.connec = (ConnectivityManager)appState.getSystemService(Context.CONNECTIVITY_SERVICE);
			//NetworkInfo info = mlobj.connec.getActiveNetworkInfo();
			appState.infoObj = new InfoObject(this, appState);
			appState.phoneCallInfo = new MyPhoneStateListener(this,appState);
			appState.mySDCardStateInfo = new MySDCardStateListener(this, appState);
			



			t = new Thread() {
				public void run(){
					//wait for 5 seconds and stop
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						//e.printStackTrace();
					}
					//Toast.makeText(BootAutoStartUpService.this, "StartUp service exiting \n",  Toast.LENGTH_SHORT).show();
					stopSelf();
					Log.i(TAG, "self destruct");
				}
			};
			//t.setPriority(Thread.NORM_PRIORITY+1);
			t.start();
			Toast.makeText(BootAutoStartUpService.this, "StartUp service exiting",  Toast.LENGTH_SHORT).show();
		}
		else
		{
			stopSelf();
			Log.i(TAG, "self destruct");
		}

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
		Log.i(TAG, "self destruct done");
	}
}




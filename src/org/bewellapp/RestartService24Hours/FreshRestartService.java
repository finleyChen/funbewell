package org.bewellapp.RestartService24Hours;

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
import android.os.IBinder;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;


public class FreshRestartService extends Service {


	private Ml_Toolkit_Application appState;
	private static ServiceController serviceController;
	private Thread t;
	static final String TAG = "MLtoolkitBootupStarterasfddddddddddddddddddddddddd";
	public void onCreate() {
		super.onCreate();
		appState = (Ml_Toolkit_Application)getApplicationContext();
		serviceController = appState.getServiceController();
		//Toast.makeText(FreshRestartService.this, "Fresh StartUp service started",  Toast.LENGTH_SHORT).show();

		if(appState.applicatin_starated == true){

			//only at start intialize ml toolkit
			//appState.mlt = new MLToolkitInterface(); 

			//MLToolkitInterface.audio_config audio_Cfg = appState.mlt.new audio_config(appState.AUDIO_SAMPLERATE,appState.AUDIO_SAMPLES_REQUIRED, 20,14);
			//MLToolkitInterface.acc_config acc_Cfg = appState.mlt.new acc_config(appState.ACCEL_SAMPLERATE,appState.ACCEL_SAMPLES_REQUIRED);
			//appState.mlt.init(acc_Cfg, audio_Cfg, null);
			
			
			//application started .... service started
			appState.applicatin_starated = true;


			//start the audio service
			if(appState.savedAudioSensorOn == true)
				serviceController.startAudioSensor();
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
			
			/* Disabled for BeWell
			//start the Bluetooth service
			if(appState.savedBluetoothSensorOn == true)
				serviceController.startBluetoothService();
			
			//start the Wifi service
			if(appState.savedWifiSensorOn == true)
				serviceController.startWifiService();
			*/
			
			
			serviceController.startNTPTimestampsService();
			serviceController.startSleepService();
			serviceController.startFunfService();
			serviceController.startAppMonitor();
			//this.bindLocationService();

			//serviceController.startDaemonService();
			//Log.i(TAG, "location started");
			//still to start binding
			//this.activity_paused = true;


			//start uploading service
			//serviceController.startUploading();

			//Toast.makeText(this,
			//	this.appState.db_adapter.getPathOfDatabase(),
			//Toast.LENGTH_SHORT).show();
			

			SchduleRestartAlarm.scheduleRestartOfService(appState);

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
			Toast.makeText(FreshRestartService.this, "Freash StartUp service exiting",  Toast.LENGTH_SHORT).show();
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




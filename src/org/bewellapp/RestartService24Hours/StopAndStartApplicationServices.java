package org.bewellapp.RestartService24Hours;

import org.bewellapp.Ml_Toolkit_Application;
import org.bewellapp.ServiceControllers.ServiceController;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;


public class StopAndStartApplicationServices extends Service {


	private Ml_Toolkit_Application appState;
	private static ServiceController serviceController;
	private Thread t;
	static final String TAG = "MLtoolkitBootupStarterasfddddddddddddddddddddddddd";
	public void onCreate() {
		super.onCreate();
		appState = (Ml_Toolkit_Application)getApplicationContext();
		serviceController = appState.getServiceController();
		//Toast.makeText(StopAndStartApplicationServices.this, "Auto StartUp Boot Sequence started ..stopping",  Toast.LENGTH_SHORT).show();

		if(appState.applicatin_starated == true){

			appState.getServiceController().stopUploadingIntellgent();

			appState.getServiceController().stopAudioSensor();
			appState.getServiceController().stopAccelerometerSensor();
			appState.getServiceController().stopLocationSensor();
			
			// Diabled for BeWell
			//appState.getServiceController().stopBluetoothSensor();
			//appState.getServiceController().stopWifiSensor();
			
			
			//appState.getServiceController().stopD
			//appState.mlt.destroy();
			//start an alarm manager to start service again
			//crash handler code is a perfect candidate so the codes are just 
			AlarmtToRestart.scheduleRestartOfService(appState,1);
			
			//appState.applicatin_starated =false;
			//Toast.makeText(StopAndStartApplicationServices.this, "StartUp service exiting",  Toast.LENGTH_SHORT).show();
			stopSelf();
			Log.i(TAG, "self destruct");
			
			
			
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




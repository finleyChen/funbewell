package org.bewellapp.Storage;

import java.util.Timer;
import java.util.TimerTask;

import org.bewellapp.Ml_Toolkit_Application;
import org.bewellapp.ServiceControllers.ServiceController;
import org.bewellapp.Storage.ML_toolkit_object;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.NetworkInfo.State;
import android.telephony.TelephonyManager;
import android.util.Log;

public class MySDCardStateListener{

	//public int battery_level;
	//public int charging_state;
	//public String wifi_status;	
	private Context Activity_Context;
	private Ml_Toolkit_Application appState;
	//private ServiceController serviceController; 


	private static final String TAG = "Phone_state_listener: ";

	public MySDCardStateListener(Context Activity_Context, Ml_Toolkit_Application mlobj)
	{
		this.Activity_Context = Activity_Context;
		this.appState = mlobj;

		//start batttery receiver	
		IntentFilter intentFilter = new IntentFilter(); 
		intentFilter.addAction(Intent.ACTION_MEDIA_MOUNTED); 
		intentFilter.addAction(Intent.ACTION_MEDIA_UNMOUNTED); 
		intentFilter.addDataScheme("file"); 
		intentFilter.addDataAuthority("*", null); 
		this.appState.registerReceiver(SDCardStateReceiver, intentFilter);


		//this.appState.registerReceiver( outgoingCallReceiver, new IntentFilter(Intent.ACTION_NEW_OUTGOING_CALL) );

		//start connection receiver
		//IntentFilter intentFilter = new IntentFilter("android.net.conn.CONNECTIVITY_CHANGE");
		//appState.registerReceiver(connectivityReceiver, intentFilter);

		//battery_level = 0;
		//charging_state = 0;
		//wifi_status = "none";
		//this.appState.phone_call_on = false;
		Log.i("SD card listener", "initiated ");
	}

	private final BroadcastReceiver SDCardStateReceiver = new BroadcastReceiver() {
		@Override 
		public void onReceive(Context context, Intent intent) { 
			try { 
				//bIsUnmount              = !bIsUnmount; 
				if (intent.getAction().equals(Intent.ACTION_MEDIA_UNMOUNTED)){ 
					Log.i("SD card", "Unmounted");
					//means usb drive in a laptop
					appState.SDCardMounted = true;
					if(appState.currentOpenSDCardFile != null)
						appState.currentOpenSDCardFile.close();
					
					Thread.sleep(2000);
					
					appState.getServiceController().stopUploadingIntellgent();

					appState.getServiceController().stopAudioSensor();
					appState.getServiceController().stopAccelerometerSensor();
					appState.getServiceController().stopLocationSensor();
					appState.getServiceController().stopAppMonitor();
					
					//for bewell
					//appState.getServiceController().stopBluetoothSensor();
					//appState.getServiceController().stopWifiSensor();
					
					appState.getServiceController().stopNTPTimestampsService();


				}else if(intent.getAction().equals(Intent.ACTION_MEDIA_MOUNTED)){ 
					Log.i("SD cardddddddddddddd", "Mounted");
					
					if(appState.fileListInitialzied == false){
						appState.fileListInitialzied = true;
						appState.intiializFiles();
						appState.getFileList();	
						Log.i("SD card-examplee", "File list initiated SD card ");
					}
						
					
					
					
					
					appState.SDCardMounted = false;
					//appState.getServiceController().stopUploadingIntellgent();

					//start the audio service
					if(appState.savedAudioSensorOn == true)
						appState.getServiceController().startAudioSensor();
					//this.bindAudioService();

					//try to start the services
					//start the accelerometer service

					if(appState.savedAccelSensorOn == true)
						appState.getServiceController().startAccelerometerSensor();

					//start the gps service
					if(appState.savedLocationSensorOn == true)
						appState.getServiceController().startLocationSensor();
					//this.bindLocationService();
					
					/* Diabled for BeWell
					//start the Bluetooth service
					if(appState.savedBluetoothSensorOn == true)
						appState.getServiceController().startBluetoothService();
					
					//start the Wifi service
					if(appState.savedWifiSensorOn == true)
						appState.getServiceController().startWifiService();
					*/
					
					appState.getServiceController().startNTPTimestampsService();

				} 
				else{

				}
			} catch (Exception e) { 
				Log.i("SD cardddddddddddd", e.toString()); 
			} 
		}
	};

}
package org.bewellapp.BootUpServices;

//import com.example.UpdaterService;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;


public class BootReceiver extends BroadcastReceiver {
	static final String TAG = "BootReceiver";
	//private final Ml_Toolkit_Application applicationCONTEXT;
	//public static final ComponentName MLT_SERVICE_COMPONENT = new ComponentName(
		//	"org.bewellapp.BootUpServices",
	//"org.bewellapp.BootUpServices.BootAutoStartUpService");
	//			android:process="org.bewellapp"
	//<receiver android:enabled="true"
		//android:name="org.bewellapp.BootUpServices.BootUpBroadcastReceiver"
		//android:process="org.bewellapp" android:permission="android.permission.RECEIVE_BOOT_COMPLETED">
	
	@Override
	public void onReceive(Context context, Intent intent) {
		Log.d(TAG, "onReceiveIntent");
		//if("android.intent.action.BOOT_COMPLETED".equals(intent.getAction())){
			//applicationCONTEXT = (Ml_Toolkit_Application)getApplicationContext();
			// TODO Auto-generated method stub
			Log.d(TAG, "onReceiveIntentttt");
			Log.d(TAG, "start service");		 
			//intent.setClass(this.applicationCONTEXT, AccelerometerService.class);
			//Intent serviceIntent = new Intent();
			//serviceIntent.setClass(context, BootAutoStartUpService.class);
			//serviceIntent.setComponent(MLT_SERVICE_COMPONENT);
			//context.startService(serviceIntent);
			//Toast.makeText(context, "OlympicsReminder service has started!", Toast.LENGTH_LONG).show();
			context.startService(new Intent(context, BootAutoStartUpService.class));
		//}
	}
}
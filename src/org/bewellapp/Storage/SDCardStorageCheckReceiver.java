package org.bewellapp.Storage;

import org.bewellapp.Ml_Toolkit_Application;

import org.bewellapp.R;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Environment;
import android.os.StatFs;
import android.util.Log;

public class SDCardStorageCheckReceiver extends BroadcastReceiver
{  
	private Ml_Toolkit_Application appState;
	private static final int HELLO_ID = 411;
	@Override
	public void onReceive(Context context, Intent intent) {
		// TODO Auto-generated method stub
		
		StatFs stat = new StatFs(Environment.getExternalStorageDirectory().getPath());
		long bytesAvailable = (long)stat.getBlockSize() *(long)stat.getBlockCount();
		long megAvailable = bytesAvailable / 1048576;
		
		appState = (Ml_Toolkit_Application)context.getApplicationContext();
		
		Log.w("SD CARD STORAGE", "checked for SD card storage  ......................" + megAvailable);
		
		if(megAvailable < 100)
		{
			//stop all the services
			appState.getServiceController().stopAudioSensor();
			appState.getServiceController().stopAccelerometerSensor();
			appState.getServiceController().stopLocationSensor();
			
			//for bewell
			//appState.getServiceController().stopBluetoothSensor();
			//appState.getServiceController().stopWifiSensor();
			
			appState.getServiceController().stopNTPTimestampsService();
			
			appState.all_sesning_disabled = true;
			
			//post a notification
			NotificationManager notificationManager = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);

			int icon = R.drawable.cross;
			CharSequence text = "No Space Warning";
			CharSequence contentTitle = "No Space Left. Sensing stopped";
			CharSequence contentText = "Press here to mail admin";
			long when = System.currentTimeMillis();

			//Intent intent2 = new Intent(this, NotificationViewer.class);
			
			Intent sendIntent = new Intent(Intent.ACTION_SEND);
			String subject = "[BeWell] Running out of Space";
			String body = "Running out of space. Requesting Assistance";
			sendIntent.putExtra(Intent.EXTRA_EMAIL,
					new String[] {"mashfiqui.r.s@gmail.com"});
			sendIntent.putExtra(Intent.EXTRA_TEXT, body);
			sendIntent.putExtra(Intent.EXTRA_SUBJECT, subject);
			sendIntent.setType("message/rfc822");
			
			PendingIntent contentIntent = PendingIntent.getActivity(context, 0, sendIntent, 0);

			Notification notification = new Notification(icon,text,when);

			notification.setLatestEventInfo(context, contentTitle, contentText, contentIntent);

			notificationManager.notify(HELLO_ID, notification);
		}
		else //if there is no space warning then check later
			SDCardStorageCheckAlarm.scheduleRestartOfService(context, 60);
		
	}
}
package org.bewellapp.Storage;

import java.util.Calendar;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class SDCardStorageCheckAlarm{	
	public static void scheduleRestartOfService(Context context,int whenToStartFromNow_inMinutes)
	{
		// get a Calendar object with current time
		 Calendar cal = Calendar.getInstance();
		 // add 5 minutes to the calendar object
		 
		 //45 second later, We will check for SD card 
		 
		 if(whenToStartFromNow_inMinutes == 0)
			 cal.add(Calendar.SECOND, 20);
		 else
			 cal.add(Calendar.MINUTE, whenToStartFromNow_inMinutes);
		 
		 //cal.add(Calendar.SECOND, 30);
		 
		 //Intent intent = new Intent(ctx, AlarmReceiver.class);
		 //intent.putExtra("alarm_message", "O'Doyle Rules!");
		 // In reality, you would want to have a static variable for the request code instead of 192837
		 //PendingIntent sender = PendingIntent.getBroadcast(this, 192837, intent, PendingIntent.FLAG_UPDATE_CURRENT);
		 
		 PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, 
				 new Intent("org.bewellapp.Storage.SDCardStorageCheckReceiver"), 0);
		 
		 //Intent intent = new Intent(AlarmController.this, OneShotAlarm.class);
         //PendingIntent sender = PendingIntent.getBroadcast(AlarmController.this,
           //
		 //0, intent, 0);
		 
		 Log.w("SD CARD STORAGE", "Alarm Started  ......................");

		 // Get the AlarmManager service
		 AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		 am.set(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pendingIntent);
	}
}
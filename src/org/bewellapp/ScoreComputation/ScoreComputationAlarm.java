package org.bewellapp.ScoreComputation;

import java.util.Calendar;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

public class ScoreComputationAlarm {
	public static void scheduleRestartOfService(Context context)
	{
		// get a Calendar object with current time
		 Calendar cal = Calendar.getInstance();
		 // add frequency hours to the calendar object
		 cal.add(Calendar.HOUR, 2);
		 cal.set(Calendar.MINUTE, 3);	//so it triggers the service in every XX:03:00. 
		 //cal.add(Calendar.SECOND, 60);
		 //Intent intent = new Intent(ctx, AlarmReceiver.class);
		 //intent.putExtra("alarm_message", "O'Doyle Rules!");
		 // In reality, you would want to have a static variable for the request code instead of 192837
		 //PendingIntent sender = PendingIntent.getBroadcast(this, 192837, intent, PendingIntent.FLAG_UPDATE_CURRENT);
		 
		 PendingIntent pendingIntent = PendingIntent.getService(context, 0, 
				 new Intent("org.bewellapp.ScoreComputation.ScoreComputationService"), PendingIntent.FLAG_CANCEL_CURRENT);
		 

		 // Get the AlarmManager service
		 AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		 am.set(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pendingIntent);
	}
	
	public static void scheduleRestartOfService(Context context,int whenToStartFromNow)
	{
		// get a Calendar object with current time
		 Calendar cal = Calendar.getInstance();
		// add frequency hours to the calendar object
		 cal.add(Calendar.HOUR, whenToStartFromNow);
		 //Intent intent = new Intent(ctx, AlarmReceiver.class);
		 //intent.putExtra("alarm_message", "O'Doyle Rules!");
		 // In reality, you would want to have a static variable for the request code instead of 192837
		 //PendingIntent sender = PendingIntent.getBroadcast(this, 192837, intent, PendingIntent.FLAG_UPDATE_CURRENT);
		 
		 PendingIntent pendingIntent = PendingIntent.getService(context, 0, 
				 new Intent("org.bewellapp.ScoreComputation.ScoreComputationService"), PendingIntent.FLAG_CANCEL_CURRENT);
		 

		 // Get the AlarmManager service
		 AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		 am.set(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pendingIntent);
	}
}

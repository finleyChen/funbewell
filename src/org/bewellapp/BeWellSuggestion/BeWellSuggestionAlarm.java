package org.bewellapp.BeWellSuggestion;

import java.util.Calendar;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

public class BeWellSuggestionAlarm {

	public static void scheduleRestartOfService(Context context)
	{
		// get a Calendar object with current time
		Calendar cal = Calendar.getInstance();
		 // add frequency hours to the calendar object
		 cal.add(Calendar.HOUR, 1);
		 cal.set(Calendar.MINUTE, 5);	//so it triggers the service in every XX:05:00. 

		PendingIntent pendingIntent = PendingIntent.getService(context, 0, 
				new Intent("org.bewellapp.BeWellSuggestion.BeWellSuggestionService"), 0);


		// Get the AlarmManager service
		AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		am.set(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pendingIntent);
	}
}

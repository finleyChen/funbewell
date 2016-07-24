package org.bewellapp.ServiceControllers.BestSleepLib;

import java.util.Calendar;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

public class BestSleepAlarm {


	public static void scheduleRestartOfService(Context context)
	{
		// get a Calendar object with current time
		Calendar cal = Calendar.getInstance();
		// add frequency hours to the calendar object
		cal.add(Calendar.HOUR, 24);
		//cal.add(Calendar.MINUTE, 5);

		PendingIntent pendingIntent = PendingIntent.getService(context, 0, 
				new Intent("org.bewellapp.ServiceControllers.BestSleepLib.BestSleepComputationService"), PendingIntent.FLAG_CANCEL_CURRENT);

		// Get the AlarmManager service
		AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		am.set(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pendingIntent);
	}
}

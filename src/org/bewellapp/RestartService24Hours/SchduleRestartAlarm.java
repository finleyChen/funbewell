package org.bewellapp.RestartService24Hours;

import java.util.Calendar;

import org.bewellapp.Ml_Toolkit_Application;
import org.bewellapp.Storage.ML_toolkit_object;


import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class SchduleRestartAlarm{
	//public static ML_toolkit_object appState;
	public static void scheduleRestartOfService(Context context)
	{
		//appState = (ML_toolkit_object)context;
		// get a Calendar object with current time
		Calendar cal = Calendar.getInstance();
		//Calendar now =cal;
		// add 5 minutes to the calendar object
		//cal.add(Calendar.SECOND, 30);

		//cal.add(Calendar.HOUR, 24);
		Log.i("CURRENT REFRESH TIMEEEEEEEEE", ""+ (cal.get(Calendar.MONTH) + 1) + "-"+ cal.get(Calendar.DATE)+ "-"
				+ cal.get(Calendar.YEAR));
		
		
		//if(Ml_Toolkit_Application.DayCounter!=0)
		cal.add(Calendar.DATE,1);
		

		Ml_Toolkit_Application.DayCounter++;

		cal.set(Calendar.HOUR_OF_DAY, 2);
		cal.set(Calendar.MINUTE, 15);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);
		//cal.add(Calendar.SECOND, 30);


		//Intent intent = new Intent(ctx, AlarmReceiver.class);
		//intent.putExtra("alarm_message", "O'Doyle Rules!");
		// In reality, you would want to have a static variable for the request code instead of 192837
		//PendingIntent sender = PendingIntent.getBroadcast(this, 192837, intent, PendingIntent.FLAG_UPDATE_CURRENT);

		PendingIntent pendingIntent = PendingIntent.getService(context, 0, 
				new Intent("org.bewellapp.RestartService24Hours.StopAndStartApplicationServices"), 0);


		// Get the AlarmManager service
		AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		am.set(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pendingIntent);
		//cal.add(Calendar.DATE,Ml_Toolkit_Application.DayCounter);
		Log.i("NEXT REFRESH TIMEEEEEEEEEE", ""+ (cal.get(Calendar.MONTH) + 1) + "-"+ cal.get(Calendar.DATE)+ "-"
				+ cal.get(Calendar.YEAR));

	}
}
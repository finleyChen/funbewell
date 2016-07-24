package org.bewellapp.ServiceControllers.AudioLib;

import java.util.GregorianCalendar;

import org.bewellapp.Ml_Toolkit_Application;
import org.bewellapp.ServiceControllers.AppLib.AppStatsDBAdapter;
import org.bewellapp.ServiceControllers.AppLib.AppStatsDBAdapter.PhoneUsageType;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.util.Log;

public class MyPhoneStateListener {

	private Context context;
	private Ml_Toolkit_Application appState;

	private long mCallStart = 0;
	private boolean mCallRunning = false;

	private static final String TAG = "Phone_state_listener: ";

	public MyPhoneStateListener(Context Activity_Context, Ml_Toolkit_Application mlobj) {
		this.context = Activity_Context;
		this.appState = mlobj;

		IntentFilter intentFilter = new IntentFilter("android.intent.action.PHONE_STATE");
		this.appState.registerReceiver(phoneStateReceiver, intentFilter);

		this.appState.registerReceiver(outgoingCallReceiver, new IntentFilter(Intent.ACTION_NEW_OUTGOING_CALL));

		this.appState.phone_call_on = false;
		mCallRunning = false;
		Log.i(TAG, "initiated ");
	}

	private final BroadcastReceiver phoneStateReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String extra = intent.getStringExtra(android.telephony.TelephonyManager.EXTRA_STATE);

			// means call running
			if (extra.equals(android.telephony.TelephonyManager.EXTRA_STATE_OFFHOOK))
			{
				Log.i(TAG, "call underway ... ");

			}

			// means call running
			if (extra.equals(android.telephony.TelephonyManager.EXTRA_STATE_RINGING))
			{

				// strategy if the phone is ringing then stop the audio service
				Log.i(TAG, "call ringing ... ");
				// if sensor is on then turn it off
				mCallStart = System.currentTimeMillis();
				mCallRunning = true;
//				if (appState.audioSensorOn == true)
//				{
//					Log.i(TAG, "call ringing ... stop Audio sensor");
//					appState.getServiceController().stopAudioSensor();
//				}

			}

			if (extra.equals(android.telephony.TelephonyManager.EXTRA_STATE_IDLE)) {
				// strategy if the phone call end then start the audio service
				Log.i(TAG, "call ended ... ");
				if (mCallRunning) {
					mCallRunning = false;
					long callDuration = System.currentTimeMillis() - mCallStart;
					callDuration = callDuration/1000;
					SharedPreferences sp = context.getSharedPreferences(Ml_Toolkit_Application.SHARED_PREF, Context.MODE_PRIVATE);
					int numsec = sp.getInt(Ml_Toolkit_Application.SP_SOCIAL_PHONE_CALLS_SECONDS, 0);
					numsec += callDuration;
					SharedPreferences.Editor editor = sp.edit();
					editor.putInt(Ml_Toolkit_Application.SP_SOCIAL_PHONE_CALLS_SECONDS, numsec);
					editor.commit();
					AppStatsDBAdapter statDB = new AppStatsDBAdapter(context);
					statDB.open();
					try {
						statDB.addSecondsToUsage(new GregorianCalendar(), PhoneUsageType.USAGE_PHONE_CALL, callDuration);
					} finally {
						statDB.close();
					}
				}
//				if (appState.audioSensorOn == false && appState.audioForceLocked == false) {
//					// if sensor is on then off it, else nothing to do
//					// or for the initial state, where there is no phone call
//					// but these method will be called
//					appState.getServiceController().startAudioSensor();
//				}
			}

		}
	};

	private final BroadcastReceiver outgoingCallReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
//			Log.i(TAG, "outgoing call about to be underway ... ");
//
//			try {
//				Log.i(TAG, "In try " + appState.audioSensorOn + " " + appState.audio_release);
//				// if sensor is on then turn it off
//				if (appState.audioSensorOn == true)
//				{
//					Log.i(TAG, "Stopping Sesnor ");
//					appState.getServiceController().stopAudioSensor();
//					mCallStart = System.currentTimeMillis();
//					mCallRunning = true;
//					Thread.sleep(1500);
//					Log.i(TAG, "Sucessful wait " + appState.audio_release);
//				}
//			} catch (InterruptedException e) {
//				Log.e(TAG, "No wait " + e.toString());
//			}

		}
	};
}
package org.bewellapp.NTPTimeStampService;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.text.NumberFormat;

import org.apache.commons.net.ntp.NTPUDPClient;
import org.apache.commons.net.ntp.NtpUtils;
import org.apache.commons.net.ntp.NtpV3Packet;
import org.apache.commons.net.ntp.TimeInfo;
import org.apache.commons.net.ntp.TimeStamp;
import org.bewellapp.Ml_Toolkit_Application;
import org.bewellapp.CrashHandlers.CrashRestartAlarm;
import org.bewellapp.RestartService24Hours.SchduleRestartAlarm;
import org.bewellapp.ServiceControllers.ServiceController;
import org.bewellapp.ServiceControllers.AudioLib.MyPhoneStateListener;
import org.bewellapp.ServiceControllers.LocationLib.LocationService;
import org.bewellapp.ServiceControllers.LocationLib.MyLocationManager;
import org.bewellapp.ServiceControllers.UploadingLib.InfoObject;
import org.bewellapp.Storage.ML_toolkit_object;
import org.bewellapp.Storage.MiscUtilityFunctions;
import org.bewellapp.Storage.MySDCardStateListener;
import org.bewellapp.Storage.SDCardStorageCheckAlarm;
import org.bewellapp.wallpaper.WellnessSummaryActivity;

import edu.dartmouthcs.UtilLibs.MyDataTypeConverter;
import org.bewellapp.R;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.backup.RestoreObserver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.location.Location;
import android.os.Handler;
import android.os.IBinder;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;


public class NTPTimeStampService extends Service {


	private Ml_Toolkit_Application appState;
	private static ServiceController serviceController;
	private Thread t;
	static final String TAG = "NTPTimeStampService";

	private static Context CONTEXT;

	private static final Class[] mStartForegroundSignature = new Class[] {
		int.class, Notification.class };
	private static final Class[] mStopForegroundSignature = new Class[] { boolean.class };

	public static final String ACTION_FOREGROUND = "org.bewellapp.NTPTimeStampService.FOREGROUND";
	public static final String ACTION_BACKGROUND = "org.bewellapp.NTPTimeStampService.BACKGROUND";

	private static NotificationManager mNM;
	private Method mStartForeground;
	private Method mStopForeground;

	private Object[] mStartForegroundArgs = new Object[2];
	private Object[] mStopForegroundArgs = new Object[1];
	private ML_toolkit_object NTP_object;


	private static Notification notification;
	private RemoteViews contentView;

	//new timer code
	private Handler mHandler = new Handler();

	//
	private final int rateNotification = 1000*60*1;

	public void onCreate() {

		// Register our receiver for the ACTION_SCREEN_OFF action. This will make our receiver
		// code be called whenever the phone enters standby mode.
		//IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_OFF);
		//registerReceiver(AccelerometerManager.mReceiver, filter);



		// screen will stay on during this section
		appState = (Ml_Toolkit_Application) getApplicationContext();
		appState.nTPTimeStampService = this;

		//curr_no_of_records = 0;
		CONTEXT = this;

		mNM = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		try {
			mStartForeground = getClass().getMethod("startForeground",
					mStartForegroundSignature);
			mStopForeground = getClass().getMethod("stopForeground",
					mStopForegroundSignature);
		} catch (NoSuchMethodException e) {
			// Running on an older platform.
			mStartForeground = mStopForeground = null;
		}

//
//		Toast.makeText(this,
//				"NTP Service Started ",
//				Toast.LENGTH_SHORT).show();


		contentView = new RemoteViews(getPackageName(), R.layout.notification_layout);
		//Activity_on = true;
		//Foreground_on = true;
		SchduleRestartAlarm.scheduleRestartOfService(appState);
		SDCardStorageCheckAlarm.scheduleRestartOfService(appState, 0);
	}


	// This is the old onStart method that will be called on the pre-2.0
	// platform. On 2.0 or later we override onStartCommand() so this
	// method will not be called.
	@Override
	public void onStart(Intent intent, int startId) {
		//boolean no_check = false;
		//if(intent == null)
		//no_check = true;
		handleCommand(intent);

		mHandler.removeCallbacks(mUpdateTimeTask);
		mHandler.postDelayed(mUpdateTimeTask, rateNotification);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {

		//boolean no_check = false;
		//if(intent == null)
		//	no_check = true;	
		if(intent == null)
		{
			CrashRestartAlarm.scheduleRestartOfService(this);
			//appState.mlt.destroy();
			stopSelf();
			return START_NOT_STICKY;
		}

		handleCommand(intent);
		mHandler.removeCallbacks(mUpdateTimeTask);
		mHandler.postDelayed(mUpdateTimeTask, rateNotification);



		// We want this service to continue running until it is explicitly
		// stopped, so return sticky.
		//return START_STICKY;
		return START_STICKY;
	}

	private Runnable mUpdateTimeTask = new Runnable() {
		public void run() {
			//appState.location_text = "Scanning";
			//myLocationManager.getLocation(LocationService.this, locationResult );

			//gets NTP stats and puts them into the database
			getNTPtimes();

			updateNotificationArea();								
			//mHandler.postAtTime(this,1000*60*1);
			mHandler.removeCallbacks(mUpdateTimeTask);
			mHandler.postDelayed(mUpdateTimeTask, rateNotification);
			//mHandler.postDelayed(mUpdateTimeTask, 1000*60*10);
			//mHandler.postDelayed(mUpdateTimeTask, 1000*30);
		}
	};


	protected void getNTPtimes() {
		// TODO Auto-generated method stub

		Thread t = new Thread(new Runnable(){
			public void run() {
				NTPUDPClient client = new NTPUDPClient();
				// We want to timeout if a response takes longer than 10 seconds
				client.setDefaultTimeout(10000);
				try {
					client.open();
					for (int i = 0; i < 1; i++)
					{
						//Log.d("NTPTEST",);

						try {
							InetAddress hostAddr = InetAddress.getByName("nist1-ny.ustiming.org");
							Log.d("NTPTEST","> " + hostAddr.getHostName() + "/" + hostAddr.getHostAddress());

							TimeInfo info = client.getTime(hostAddr);
							processResponse(info);
							Log.d("NTPTEST","DONEEEEEEEEEEEEEEeeeeeeeeeee Right");
						} catch (IOException ioe) {
							ioe.printStackTrace();
							Log.d("NTPTEST","DONEEEEEEEEEEEEEEeeeeeeeeeee wrong1");
						}
					}
				} catch (SocketException e) {
					e.printStackTrace();
					Log.d("NTPTEST","DONEEEEEEEEEEEEEEeeeeeeeeeee wrong2");
				}

				client.close();

			}
			//tv.setText("Hello, Android finished");

		});
		t.start();


	}


	private final NumberFormat numberFormat = new java.text.DecimalFormat("0.00");

	/**
	 * Process <code>TimeInfo</code> object and print its details.
	 * @param info <code>TimeInfo</code> object.
	 */
	public void processResponse(TimeInfo info)
	{
		NtpV3Packet message = info.getMessage();
		int stratum = message.getStratum();
		String refType;
		if (stratum <= 0)
			refType = "(Unspecified or Unavailable)";
		else if (stratum == 1)
			refType = "(Primary Reference; e.g., GPS)"; // GPS, radio clock, etc.
		else
			refType = "(Secondary Reference; e.g. via NTP or SNTP)";
		// stratum should be 0..15...
		Log.d("NTPTEST"," Stratum: " + stratum + " " + refType);

		int version = message.getVersion();
		int li = message.getLeapIndicator();
		Log.d("NTPTEST"," leap=" + li + ", version="
				+ version + ", precision=" + message.getPrecision());


		Log.d("NTPTEST"," mode: " + message.getModeName() + " (" + message.getMode() + ")");

		int poll = message.getPoll();
		// poll value typically btwn MINPOLL (4) and MAXPOLL (14)
		Log.d("NTPTEST"," poll: " + (poll <= 0 ? 1 : (int) Math.pow(2, poll))
				+ " seconds" + " (2 ** " + poll + ")");

		double disp = message.getRootDispersionInMillisDouble();
		Log.d("NTPTEST"," rootdelay=" + numberFormat.format(message.getRootDelayInMillisDouble())
				+ ", rootdispersion(ms): " + numberFormat.format(disp));


		int refId = message.getReferenceId();
		String refAddr = NtpUtils.getHostAddress(refId);
		String refName = null;
		if (refId != 0) {
			if (refAddr.equals("127.127.1.0")) {
				refName = "LOCAL"; // This is the ref address for the Local Clock
			} else if (stratum >= 2) {
				// If reference id has 127.127 prefix then it uses its own reference clock
				// defined in the form 127.127.clock-type.unit-num (e.g. 127.127.8.0 mode 5
				// for GENERIC DCF77 AM; see refclock.htm from the NTP software distribution.
				if (!refAddr.startsWith("127.127")) {
					try {
						InetAddress addr = InetAddress.getByName(refAddr);
						String name = addr.getHostName();
						if (name != null && !name.equals(refAddr))
							refName = name;
					} catch (UnknownHostException e) {
						// some stratum-2 servers sync to ref clock device but fudge stratum level higher... (e.g. 2)
						// ref not valid host maybe it's a reference clock name?
						// otherwise just show the ref IP address.
						refName = NtpUtils.getReferenceClock(message);
					}
				}
			} else if (version >= 3 && (stratum == 0 || stratum == 1)) {
				refName = NtpUtils.getReferenceClock(message);
				// refname usually have at least 3 characters (e.g. GPS, WWV, LCL, etc.)
			}
			// otherwise give up on naming the beast...
		}
		if (refName != null && refName.length() > 1)
			refAddr += " (" + refName + ")";
		Log.d("NTPTEST"," Reference Identifier:\t" + refAddr);


		TimeStamp refNtpTime = message.getReferenceTimeStamp();
		Log.d("NTPTEST"," Reference Timestamp:\t" + refNtpTime + "  " + refNtpTime.toDateString());


		// Originate Time is time request sent by client (t1)
		TimeStamp origNtpTime = message.getOriginateTimeStamp();
		Log.d("NTPTEST"," Originate Timestamp:\t" + origNtpTime + "  " + origNtpTime.toDateString());


		long destTime = info.getReturnTime();
		// Receive Time is time request received by server (t2)
		TimeStamp rcvNtpTime = message.getReceiveTimeStamp();
		Log.d("NTPTEST"," Receive Timestamp:\t" + rcvNtpTime + "  " + rcvNtpTime.toDateString());


		// Transmit time is time reply sent by server (t3)
		TimeStamp xmitNtpTime = message.getTransmitTimeStamp();
		Log.d("NTPTEST"," Transmit Timestamp:\t" + xmitNtpTime + "  " + xmitNtpTime.toDateString() + " " + xmitNtpTime.getTime());


		// Destination time is time reply received by client (t4)
		TimeStamp destNtpTime = TimeStamp.getNtpTime(destTime);
		Log.d("NTPTEST"," Destination Timestamp:\t" + destNtpTime + "  " + destNtpTime.toDateString());


		info.computeDetails(); // compute offset/delay if not already done
		Long offsetValue = info.getOffset();
		Long delayValue = info.getDelay();
		String delay = (delayValue == null) ? "N/A" : delayValue.toString();
		String offset = (offsetValue == null) ? "N/A" : offsetValue.toString();

		Log.d("NTPTEST"," Roundtrip delay(ms)=" + delay
				+ ", clock offset(ms)=" + offset); // offset in ms

		//tv.setText(sb.toString());		

		String str_ntp = ""+refAddr+","+refNtpTime.toDateString().replace(',', ' ')+","+refNtpTime.getTime()+","+origNtpTime.toDateString().replace(',', ' ')+","+origNtpTime.getTime()+","
		+rcvNtpTime.toDateString().replace(',', ' ')+","+rcvNtpTime.getTime()+","+xmitNtpTime.toDateString().replace(',', ' ')+
		","+xmitNtpTime.getTime()+","+destNtpTime.toDateString().replace(',', ' ')+","
		+destNtpTime.getTime()
		+","+delay+","+offset;

		this.NTP_object = null;
		//System.gc();
		//this.accel_object =  new ML_toolkit_object(timestamp, 1, MyDataTypeConverter.toByta(accel_data));
		this.NTP_object =  appState.mMlToolkitObjectPool.borrowObject().setValues(System.currentTimeMillis()+appState.timeOffset, 11, str_ntp.getBytes());
		appState.ML_toolkit_buffer.insert(this.NTP_object);//inserting into the buffer
	}




	void handleCommand(Intent intent) {
		if (ACTION_FOREGROUND.equals(intent.getAction())) {

			contentView = new RemoteViews(getPackageName(), R.layout.notification_layout);
			//notification = new Notification(R.drawable.logo,"BeWell",System.currentTimeMillis());//(R.drawable.icon, text, System.currentTimeMillis());
			notification = new Notification(R.drawable.notification_fish,getString(R.string.app_name),System.currentTimeMillis());

			//contentView.setImageViewResource(R.id.status_icon, R.drawable.icon);
			/*
			if(appState.accelSensorOn){
				contentView.setTextViewText(R.id.accel_text, "Accel On:" + " (" + appState.accel_no_of_records + ")");
				contentView.setTextColor(R.id.accel_text, Color.argb(128, 0, 115, 0));
			}
			else
			{
				contentView.setTextViewText(R.id.accel_text, "Accel Off");
				contentView.setTextColor(R.id.accel_text, Color.argb(128, 115, 0, 0));
			}

			//contentView.setTextViewText(R.id.audio_text, "Audio On:" + " (" + appState.audio_no_of_records + ")");					
			//contentView.setTextColor(R.id.audio_text, Color.argb(128, 0, 115, 0));

			if(appState.locationSensorOn){
				contentView.setTextViewText(R.id.location_text, "Location On:" + " (" + appState.location_text + ")");					
				contentView.setTextColor(R.id.location_text, Color.argb(128, 0, 115, 0));
			}
			else
			{
				contentView.setTextViewText(R.id.location_text, "Location off");					
				contentView.setTextColor(R.id.location_text, Color.argb(128, 115, 0, 0));
			}

			if(appState.audioSensorOn){
				contentView.setTextViewText(R.id.audio_text, "Audio On:" + " (" + appState.audio_no_of_records + ")");
				contentView.setTextColor(R.id.audio_text, Color.argb(128, 0, 115, 0));
			}
			else
			{
				contentView.setTextViewText(R.id.audio_text, "Audio off");
				contentView.setTextColor(R.id.audio_text, Color.argb(128, 115, 0, 0));
			}
			*/

			notification.contentView = contentView;
			/*PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
			new Intent(this, org.bewellapp.main_activity.class), 0);*/
			PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
					new Intent(this, WellnessSummaryActivity.class), 0);

			notification.contentIntent = contentIntent;

			startForegroundCompat(R.string.CUSTOM_VIEW,notification);

		} else if (ACTION_BACKGROUND.equals(intent.getAction())) {
			stopForegroundCompat(R.string.foreground_service_started_aud);
			//stopForegroundCompat(2);

		}

		//start a timer

	}




	/**
	 * This is a wrapper around the new startForeground method, using the older
	 * APIs if it is not available.
	 */
	public void startForegroundCompat(int id, Notification notification) {
		// If we have the new startForeground API, then use it.
		//Foreground_on = true;
		if (mStartForeground != null) {
			mStartForegroundArgs[0] = Integer.valueOf(id);
			mStartForegroundArgs[1] = notification;
			try {
				mStartForeground.invoke(this, mStartForegroundArgs);
			} catch (InvocationTargetException e) {
				// Should not happen.
				Log.w("ApiDemos", "Unable to invoke startForeground", e);
			} catch (IllegalAccessException e) {
				// Should not happen.
				Log.w("ApiDemos", "Unable to invoke startForeground", e);
			}
			return;
		}

		// Fall back on the old API.
//		setForeground(true);
//		mNM.notify(id, notification);
		Log.e("NOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOO", "NOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOO");
	}

	public void callStartForegroundCompat(){
		startForegroundCompat(R.string.CUSTOM_VIEW,notification);
		updateNotificationArea();
	}

	public void stopForegroundCompat(int id) {
		//Foreground_on = false;
		// If we have the new stopForeground API, then use it.
		if (mStopForeground != null) {
			mStopForegroundArgs[0] = Boolean.TRUE;
			try {
				mStopForeground.invoke(this, mStopForegroundArgs);
			} catch (InvocationTargetException e) {
				// Should not happen.
				Log.w("ApiDemos", "Unable to invoke stopForeground", e);
			} catch (IllegalAccessException e) {
				// Should not happen.
				Log.w("ApiDemos", "Unable to invoke stopForeground", e);
			}
			return;
		}

		// Fall back on the old API. Note to cancel BEFORE changing the
		// foreground state, since we could be killed at that point.
//		if (!appState.getServiceController().areServiceUsingNotificationArea()) {
//			mNM.cancel(id);
//		}
//		setForeground(false);
		Log.e("NOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOO", "NOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOO");
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

		try{
			stopForegroundCompat(R.string.CUSTOM_VIEW);
			mHandler.removeCallbacks(mUpdateTimeTask);
		}
		catch(Exception ex){}

		appState.nTPTimeStampService = null;


		//Log.i(TAG, "self destruct done");
	}


	public void updateNotificationArea(){
		//if (no_of_records % 100 == 0 && Foreground_on == true) {

		//String text = "No of samples";

		contentView = new RemoteViews(getPackageName(), R.layout.notification_layout);
		try{
			/*
			if(appState.accelSensorOn){
				contentView.setTextViewText(R.id.accel_text, "Accel On:" + " (" + appState.accel_no_of_records + ")");
				contentView.setTextColor(R.id.accel_text, Color.argb(128, 0, 115, 0));
			}
			else
			{
				contentView.setTextViewText(R.id.accel_text, "Accel Off");
				contentView.setTextColor(R.id.accel_text, Color.argb(128, 115, 0, 0));
			}

			if(appState.audioSensorOn){
				contentView.setTextViewText(R.id.audio_text, "Audio On:" + " (" + appState.audio_no_of_records + ")");
				contentView.setTextColor(R.id.audio_text, Color.argb(128, 0, 115, 0));
			}
			else
			{
				contentView.setTextViewText(R.id.audio_text, "Audio off");
				contentView.setTextColor(R.id.audio_text, Color.argb(128, 115, 0, 0));
			}

			if(appState.locationSensorOn){
				contentView.setTextViewText(R.id.location_text, "Location On:" + " (" + appState.location_text + ")");					
				contentView.setTextColor(R.id.location_text, Color.argb(128, 0, 115, 0));
			}
			else
			{
				contentView.setTextViewText(R.id.location_text, "Location off");					
				contentView.setTextColor(R.id.location_text, Color.argb(128, 115, 0, 0));
			}
			*/

			notification.contentView = contentView;
			/*PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
		new Intent(this, org.bewellapp.main_activity.class), 0);*/
			PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
					new Intent(this, WellnessSummaryActivity.class), 0);

			notification.contentIntent = contentIntent;


			//startForegroundCompat(R.string.CUSTOM_VIEW,notification);
			mNM.notify(R.string.CUSTOM_VIEW,notification);
		}catch (Exception e){
			Log.e(TAG, "Update Notification " + e.toString());
		}

		//}
	}

}




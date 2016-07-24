/**
 * 
 */
package org.bewellapp.ServiceControllers.BestSleepLib;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.Calendar;

import org.bewellapp.Ml_Toolkit_Application;
import org.bewellapp.ScoreComputation.ScoreComputationDBAdapter;
import org.bewellapp.ServiceControllers.AccelerometerLib.AccelerometerManager;
import org.bewellapp.Storage.ML_toolkit_object;

import edu.dartmouthcs.UtilLibs.MyDataTypeConverter;
import org.bewellapp.R;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

public class BestSleepService extends Service {

    /**
     * Best-effort Sleep service
     */

    private Ml_Toolkit_Application appState;
    public static final String ACTION = "org.bewellapp.ServiceControllers.BestSleepLib.BestSleepService";
    private static final String TAG = "BestSleep";
    private static final double maxDuration = 10; // 10 hours
    private static final double minDuration = 1; // 1 hours
    private SensorManager sensorManager;
    private Sensor mLightSensor;
    private float maxLight;
    private int isDark;
    private long beginDarkMillis;
    private long endDarkMillis;
    private int isLocked;
    private long screenOnMillis;
    private long screenOffMillis;
    private long phoneOnMillis;
    private long phoneOffMillis;
    private static final String BEWELL_PHONEOFF_FILENAME = "phone_off.txt";
    private int isCharging;
    private long chargingOnMillis;
    private long chargingOffMillis;
    public static double groundTruthDuration;

    public BestSleepService() {
	// TODO Auto-generated constructor stub
    }

    public void onCreate() {
	super.onCreate();
	appState = (Ml_Toolkit_Application) getApplicationContext();

	// light sensor
	sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
	mLightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
	isDark = -1; // initiate as neither 0 nor 1
	beginDarkMillis = endDarkMillis = 0;
	screenOnMillis = screenOffMillis = 0;
	phoneOnMillis = phoneOffMillis = 0;
	writePhoneOffTime(phoneOffMillis);
	chargingOnMillis = chargingOffMillis = 0;

	if (mLightSensor != null) {

	    maxLight = mLightSensor.getMaximumRange();
	    Log.d(TAG, "max lighting: " + Float.toString(maxLight));

	    sensorManager.registerListener(lightSensorEventListener,
		    mLightSensor, SensorManager.SENSOR_DELAY_NORMAL);
	}

	// Screen Lock
	isLocked = -1; // initiate as neither 0 nor 1
	IntentFilter screenFilter = new IntentFilter();
	screenFilter.addAction(Intent.ACTION_SCREEN_ON);
	screenFilter.addAction(Intent.ACTION_SCREEN_OFF);
	this.appState.registerReceiver(screenLockReceiver, screenFilter);

	// Phone shut-down
	IntentFilter shutFilter = new IntentFilter();
	shutFilter.addAction(Intent.ACTION_SHUTDOWN);
	shutFilter.addAction(Intent.ACTION_BOOT_COMPLETED);
	this.appState.registerReceiver(phoneOffReceiver, shutFilter);

	// Battery charging
	isCharging = -1;
	this.appState.registerReceiver(batteryReceiver, new IntentFilter(
		Intent.ACTION_BATTERY_CHANGED));

	// Alarm for computing durations
	Calendar cal = Calendar.getInstance();
	Calendar cal_now = Calendar.getInstance();
	// add frequency hours to the calendar object
	cal.set(Calendar.HOUR_OF_DAY, 9);
	cal.set(Calendar.MINUTE, 0);
	if (cal.before(cal_now)) {
	    cal.add(Calendar.DATE, 1);
	}

	PendingIntent pendingIntent = PendingIntent
		.getService(
			appState,
			0,
			new Intent(
				"org.bewellapp.ServiceControllers.BestSleepLib.BestSleepComputationService"),
			PendingIntent.FLAG_CANCEL_CURRENT);
	// Get the AlarmManager service
	AlarmManager am = (AlarmManager) appState
		.getSystemService(Context.ALARM_SERVICE);
	am.set(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pendingIntent);

    }

    // light sensor listener
    private SensorEventListener lightSensorEventListener = new SensorEventListener() {

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
	    // TODO Auto-generated method stub
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
	    // TODO Auto-generated method stub
	    if (event.sensor.getType() == Sensor.TYPE_LIGHT) {
		float currentReading = event.values[0];

		if (currentReading <= 20)
		// phone is in dark condition
		{
		    if (isDark == 1) // if already in dark
			return;

		    beginDarkMillis = System.currentTimeMillis();
		    isDark = 1;
		}

		else
		// phone is in light condition
		{
		    if (isDark == 0) // if already in light
			return;

		    endDarkMillis = System.currentTimeMillis();
		    isDark = 0;

		    if (beginDarkMillis != 0) // means we have a beginning point
					      // before
		    {
			double duration = (double) (endDarkMillis - beginDarkMillis)
				/ (1000 * 60 * 60); // unit: hours
			Log.d(TAG,
				"dark duration is: "
					+ Double.toString(duration));

			if (duration >= minDuration && duration <= maxDuration) {
			    /*
			     * Store result in the database
			     */

			    ML_toolkit_object darkingObj = appState.mMlToolkitObjectPool
				    .borrowObject();
			    darkingObj.setValues(endDarkMillis, 14,
				    MyDataTypeConverter.toByta(duration));
			    appState.ML_toolkit_buffer.insert(darkingObj);

			    // bewell database adapter
			    appState.beWellScoreAdapter2 = new ScoreComputationDBAdapter(
				    BestSleepService.this,
				    "bewellScore_2.dbr_ms");
			    appState.beWellScoreAdapter2.open();
			    appState.beWellScoreAdapter2
				    .insertEntryBeWellDuration(endDarkMillis,
					    14, duration);
			    appState.beWellScoreAdapter2.close();
			}
		    }
		}

	    }
	}

    };

    // screen lock receiver
    private final BroadcastReceiver screenLockReceiver = new BroadcastReceiver() {
	@Override
	public void onReceive(Context context, Intent intent) {
	    try {

		if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
		    // screen is on
		    Log.d(TAG, "Screen On.");
		    if (isLocked == 0) // if screen is already on
			return;

		    isLocked = 0;
		    screenOnMillis = System.currentTimeMillis();
		    if (screenOffMillis != 0) // means we have a screenOff
					      // before
		    {
			double duration = (double) (screenOnMillis - screenOffMillis)
				/ (1000 * 60 * 60); // unit: hours
			Log.d(TAG,
				"screen off duration is: "
					+ Double.toString(duration));

			if (duration >= minDuration && duration <= maxDuration) {
			    /*
			     * Store result in the database
			     */

			    ML_toolkit_object screenOffObj = appState.mMlToolkitObjectPool
				    .borrowObject();
			    screenOffObj.setValues(screenOnMillis, 15,
				    MyDataTypeConverter.toByta(duration));
			    appState.ML_toolkit_buffer.insert(screenOffObj);

			    // bewell database adapter
			    appState.beWellScoreAdapter2 = new ScoreComputationDBAdapter(
				    BestSleepService.this,
				    "bewellScore_2.dbr_ms");
			    appState.beWellScoreAdapter2.open();
			    appState.beWellScoreAdapter2
				    .insertEntryBeWellDuration(screenOnMillis,
					    15, duration);
			    appState.beWellScoreAdapter2.close();

			}
		    }
		}

		else if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
		    // screen is off
		    Log.d(TAG, "Screen Off.");

		    if (isLocked == 1)
			return;

		    screenOffMillis = System.currentTimeMillis();
		    if(screenOffMillis > screenOnMillis && screenOnMillis > 0) {
		        long onDuration = (screenOffMillis - screenOnMillis)/1000;
		        //Toast.makeText(getApplicationContext(), "Screen_ON from "+screenOnMillis/1000 +
		        //    " to " + screenOffMillis/1000 + ", duration=" + onDuration, Toast.LENGTH_SHORT).show();
		        //Log.e("Screen_ON","from "+screenOnMillis/1000 + " to " + screenOffMillis/1000 + ", duration=" + onDuration);
		        ML_toolkit_object screenOnObj = appState.mMlToolkitObjectPool
		                .borrowObject();
		        screenOnObj.setValues(screenOnMillis, 100, String.valueOf(onDuration).getBytes());
		        appState.ML_toolkit_buffer.insert(screenOnObj);
		    }
		    isLocked = 1;
		}
	    } catch (Exception e) {
		Log.i(TAG, "Screen Lock receiver" + e.toString());
	    }
	}
    };

    // phone off receiver
    private final BroadcastReceiver phoneOffReceiver = new BroadcastReceiver() {
	@Override
	public void onReceive(Context context, Intent intent) {
	    try {

		if (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
		    // screen is on
		    Log.d(TAG, "Phone Bootup.");
		    phoneOnMillis = System.currentTimeMillis();
		    // read file
		    phoneOffMillis = readPhoneOffTime();

		    if (phoneOffMillis != 0) // means we have a shutdown before
		    {
			double duration = (double) (phoneOnMillis - phoneOffMillis)
				/ (1000 * 60 * 60); // unit: hours
			Log.d(TAG,
				"phone off duration is: "
					+ Double.toString(duration));

			if (duration >= minDuration && duration <= maxDuration) {
			    /*
			     * Store result in the database
			     */
			    /*
			     * ML_toolkit_object phoneOffObj =
			     * appState.mMlToolkitObjectPool.borrowObject();
			     * phoneOffObj.setValues(phoneOnMillis, 16,
			     * MyDataTypeConverter.toByta(duration));
			     * appState.ML_toolkit_buffer.insert(phoneOffObj);
			     */

			    // bewell database adapter
			    appState.beWellScoreAdapter2 = new ScoreComputationDBAdapter(
				    BestSleepService.this,
				    "bewellScore_2.dbr_ms");
			    appState.beWellScoreAdapter2.open();
			    appState.beWellScoreAdapter2
				    .insertEntryBeWellDuration(phoneOnMillis,
					    16, duration);
			    appState.beWellScoreAdapter2.close();
			}
		    }
		}

		else if (intent.getAction().equals(Intent.ACTION_SHUTDOWN)) {
		    // screen is off
		    Log.d(TAG, "phone off signal.");
		    phoneOffMillis = System.currentTimeMillis();
		    // write file
		    writePhoneOffTime(phoneOffMillis);

		}
	    } catch (Exception e) {
		Log.i(TAG, "Phone off receiver" + e.toString());
	    }
	}
    };

    protected void writePhoneOffTime(long millis) {
	FileLock lock = null;
	try {
	    Log.i(TAG, "Writing phone off time to file");
	    // Get a file channel for the file
	    File file = new File(Environment.getExternalStorageDirectory(),
		    BEWELL_PHONEOFF_FILENAME);
	    if (file.exists()) {
		file.delete();
	    }
	    FileChannel channel = new RandomAccessFile(file, "rw").getChannel();

	    // Use the file channel to create a lock on the file.
	    // This method blocks until it can retrieve the lock.
	    lock = channel.lock();
	    ByteBuffer bb = ByteBuffer.allocate(1024);
	    bb.putLong(millis);
	    bb.flip();
	    channel.write(bb);

	    // Release the lock
	    lock.release();

	    // Close the file
	    channel.close();
	} catch (Exception e) {
	    e.printStackTrace();
	} finally {
	    if (null != lock) {
		try {
		    lock.release();
		} catch (IOException e) {
		}
	    }
	}
    }

    protected static long readPhoneOffTime() {
	long result;
	FileLock lock = null;
	try {
	    Log.i(TAG, "Reading phone off time from file");
	    // Get a file channel for the file
	    File file = new File(Environment.getExternalStorageDirectory(),
		    BEWELL_PHONEOFF_FILENAME);
	    if (!file.exists() || !file.canRead()) {
		throw new Exception(String.format("File %s does not exist",
			file.getAbsolutePath()));
	    }
	    FileChannel channel = new RandomAccessFile(file, "rw").getChannel();

	    // Use the file channel to create a lock on the file.
	    // This method blocks until it can retrieve the lock.
	    lock = channel.lock();
	    ByteBuffer bb = ByteBuffer.allocate(1024);
	    channel.read(bb);
	    bb.flip();
	    result = bb.getLong();

	    // Release the lock
	    lock.release();

	    // Close the file
	    channel.close();
	} catch (Exception e) {
	    result = 0;

	} finally {
	    if (null != lock) {
		try {
		    lock.release();
		} catch (IOException e) {
		}
	    }
	}
	return result;
    }

    protected static double computePhoneOffDuration() {

	double result = 0;

	long OnMillis = System.currentTimeMillis();
	// read file
	long OffMillis = readPhoneOffTime();

	if (OffMillis != 0) // means we have a shutdown before
	{
	    double duration = (double) (OnMillis - OffMillis)
		    / (1000 * 60 * 60); // unit: hours
	    Log.d(TAG, "phone off duration is: " + Double.toString(duration));

	    if (duration >= minDuration && duration <= maxDuration) {
		result = duration - 0.5;
	    }
	}

	return result;
    }

    // battery receiver
    private final BroadcastReceiver batteryReceiver = new BroadcastReceiver() {
	@Override
	public void onReceive(Context context, Intent intent) {
	    // int level = intent.getIntExtra( "level", 0 );
	    int charging_state = intent.getIntExtra("plugged", -1);
	    // Log.d(TAG, "charging_state:" + Integer.toString(charging_state));
	    try {

		if (charging_state == 0) {
		    // phone is uncharged
		    Log.d(TAG, "Phone uncharged.");

		    if (isCharging == 0) // if already uncharged
			return;

		    chargingOffMillis = System.currentTimeMillis();
		    isCharging = 0;

		    if (chargingOnMillis != 0) // means we have a charging
					       // before
		    {
			double duration = (double) (chargingOffMillis - chargingOnMillis)
				/ (1000 * 60 * 60); // unit: hours
			Log.d(TAG,
				"charging duration is: "
					+ Double.toString(duration));

			if(chargingOffMillis > chargingOnMillis && chargingOnMillis > 0) {
			    long onDuration = (chargingOffMillis - chargingOnMillis)/1000;
			    Log.e("Charging_ON","from "+chargingOnMillis/1000 + " to " + chargingOffMillis/1000 + ", duration=" + onDuration);
			    //Toast.makeText(getApplicationContext(),
			    //   "Charged from "+chargingOnMillis/1000 + " to " + chargingOffMillis/1000 +
			    //   ", duration=" + onDuration, Toast.LENGTH_SHORT).show();
			    ML_toolkit_object screenOnObj = appState.mMlToolkitObjectPool
			            .borrowObject();
			    screenOnObj.setValues(chargingOnMillis, 101,
			            String.valueOf(onDuration).getBytes());
			    appState.ML_toolkit_buffer.insert(screenOnObj);
			}
			 
			if (duration >= minDuration && duration <= maxDuration) {
			    /*
			     * Store result in the database
			     */

			    ML_toolkit_object chargingObj = appState.mMlToolkitObjectPool
				    .borrowObject();
			    chargingObj.setValues(chargingOffMillis, 17,
				    MyDataTypeConverter.toByta(duration));
			    appState.ML_toolkit_buffer.insert(chargingObj);

			    // bewell database adapter
			    appState.beWellScoreAdapter2 = new ScoreComputationDBAdapter(
				    BestSleepService.this,
				    "bewellScore_2.dbr_ms");
			    appState.beWellScoreAdapter2.open();
			    appState.beWellScoreAdapter2
				    .insertEntryBeWellDuration(
					    chargingOffMillis, 17, duration);
			    appState.beWellScoreAdapter2.close();
			}
		    }
		}

		else if (charging_state != -1) {
		    // phone is charged
		    // Log.d(TAG, "phone charged.");

		    if (isCharging == 1)
			return;

		    chargingOnMillis = System.currentTimeMillis();
		    isCharging = 1;
		}
	    } catch (Exception e) {
		Log.i(TAG, "Phone off receiver" + e.toString());
	    }

	}
    };

    @Override
    public void onDestroy() {
	super.onDestroy();

	if (screenLockReceiver != null)
	    this.appState.unregisterReceiver(screenLockReceiver);
	if (phoneOffReceiver != null)
	    this.appState.unregisterReceiver(phoneOffReceiver);
	if (batteryReceiver != null)
	    this.appState.unregisterReceiver(batteryReceiver);
	if (sensorManager != null && lightSensorEventListener != null)
	    sensorManager.unregisterListener(lightSensorEventListener);
    }

    /*
     * (non-Javadoc)
     * 
     * @see android.app.Service#onBind(android.content.Intent)
     */
    @Override
    public IBinder onBind(Intent intent) {
	// TODO Auto-generated method stub
	return null;
    }

}

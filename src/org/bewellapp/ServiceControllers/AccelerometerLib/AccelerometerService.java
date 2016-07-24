package org.bewellapp.ServiceControllers.AccelerometerLib;

//import android.os.PowerManager;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.apache.commons.math.stat.descriptive.moment.Mean;
import org.apache.commons.math.stat.descriptive.moment.StandardDeviation;
import org.apache.commons.math.stat.descriptive.rank.Max;
import org.apache.commons.math.stat.descriptive.rank.Min;
import org.apache.commons.pool.BasePoolableObjectFactory;
import org.apache.commons.pool.PoolUtils;
import org.apache.commons.pool.impl.StackObjectPool;
import org.bewellapp.Ml_Toolkit_Application;
import org.bewellapp.R;
import org.bewellapp.Storage.CircularBufferFeatExtractionInference;
import org.bewellapp.Storage.ML_toolkit_object;
import org.bewellapp.wallpaper.WellnessSummaryActivity;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.SensorManager;
import android.os.BatteryManager;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;
import edu.dartmouth.cs.funbewell.AwesomePipeline;
import edu.dartmouth.cs.mltoolkit.processing.AccFeatureExtraction;
import edu.dartmouth.cs.mltoolkit.processing.AccInference;
import edu.dartmouthcs.UtilLibs.MyDataTypeConverter;


public class AccelerometerService extends Service implements
		AccelerometerListener {
	private static final String TAG = "XY_Accelerometer_init";

	private static Context CONTEXT;

	private static final Class[] mStartForegroundSignature = new Class[] {
			int.class, Notification.class };
	private static final Class[] mStopForegroundSignature = new Class[] { boolean.class };

	public static final String ACTION_FOREGROUND = "org.bewellapp.ServiceControllers.AccelerometerLib.FOREGROUND";
	public static final String ACTION_BACKGROUND = "org.bewellapp.ServiceControllers.AccelerometerLib.BACKGROUND";

	private NotificationManager mNM;
	private Method mStartForeground;
	private Method mStopForeground;

	private Object[] mStartForegroundArgs = new Object[2];
	private Object[] mStopForegroundArgs = new Object[1];

	private FileOutputStream fOut;
	private OutputStreamWriter osw;
	public static boolean Foreground_on;
	public static AccelerometerService accelServ;

	private int no_of_records;
	// public static int curr_no_of_records;
	public static boolean Activity_on;
	private Notification notification;
	private final IBinder binder = new AccelerometerBinder();
	public String inferred_status;
	public String prev_inferred_status;

	// acclerometer inference callback
	private Handler accelCallbackHandler;
	// private final int ACCEL_SAMPLERATE=32;
	// private final int ACCEL_SAMPLES_REQUIRED=128;
	// private MLToolkitInterface mlt;
	// private MLToolkitInterface2 mlt;
	// private MLToolkitInterfaceAccel mlt;
	private Thread t;
	private Ml_Toolkit_Application appState;
	private ML_toolkit_object accel_object;
	private double[] accel_data;
	private ML_toolkit_object accel_features;
	private ML_toolkit_object accel_inference;

	// remote view
	private RemoteViews contentView;

	// for abnormal restart
	private boolean abnormalRestart = false;

	// power lock
	private PowerManager pm;
	private PowerManager.WakeLock wl;

	// accelerometer data
	private CircularBufferFeatExtractionInference<AccelerometerData> cirBuffer;
	private AccelerometerData accelFromQueueData = new AccelerometerData();

	// inference variables
	private int num = -1;
	private final int len = 128;
	private final int samplingRate = 32;
	private AccFeatureExtraction acc = null;
	private double[] x = new double[len];
	private double[] y = new double[len];
	private double[] z = new double[len];
	private double[] accFeature;
	
	private enum Inference{STATIONARY,WALKING,CYCLING,RUNNING,DRIVING,ERROR};
	private int[] voteInference = new int[6];
	//sliding window for inference buffer
	private int[] inferenceBuffer = new int[5];

	private Handler mHandler = new Handler();

	private int sync_id_counter = 0;
	// use this to do mean smoothing
	private int activityCount = 0;
	private int smoothedInference = 0;

	private static FeatureExtractor sFeatureExtractor = new FeatureExtractor();
	private static final long MAX_EVENTS_DURATION_MSEC = SleepClassifier.WINDOW_LENGTH_MS;
	private long mStartmillis;
	private long mNumSamples;
	private static SleepClassifier sSleepClassifier = new SleepClassifierImpl();

	private BatteryReceiver mBatteryReceiver;

	private long prev_time_to_get_activity_duration, current_time_to_get_activity_duration;
	private long activity_unit_duration;

	private AccelerometerDataPool mAccelerometerDataPool;

	static class AccelerometerData{
		public long timestamp;
		public int sync_id;
		double x, y, z;

		public AccelerometerData setValues (double x, double y, double z, long timestamp, int sync_id){
			this.x = x;
			this.y = y;
			this.z = z;
			this.timestamp = timestamp;
			this.sync_id = sync_id;
			return this;
		}
	}


	//reader of data and feature extraction, inference thread
	public class MyQueuePopper extends Thread {

		CircularBufferFeatExtractionInference<AccelerometerData> obj;

		// AudioFeatureExtraction af;
		// double[] audioFrameFeature;// = new
		// double[af.getFrame_feature_size()];
		// double [] audioWindowFeature;// = new
		// double[af.getWindow_feature_size()];

		public MyQueuePopper(
				CircularBufferFeatExtractionInference<AccelerometerData> obj) {
			this.obj = obj;

			// feature computation
			// PrivacySensitiveFeatures.data = new
			// Complex[AUDIO_SAMPLES_REQUIRED];
			// af = new AudioFeatureExtraction(512,20,14,8000);
			// audioFrameFeature = new double[af.getFrame_feature_size()];
			// audioWindowFeature = new double[af.getWindow_feature_size()];

		}

		@Override
		public void run() {
			// TODO Auto-generated method stub
			while (true) {
				// q.put(i++);

				// Log.d("InferenceTAG",
				// "Thread starteddddddddddddddddddddddddd" );
				accelFromQueueData = obj.deleteAndHandleData();
				// System.arraycopy(ShortAndDouble(audioFromQueueData.data), 0,
				// PrivacySensitiveFeatures.data, 0,
				// audioFromQueueData.data.length);
				// PrivacySensitiveFeatures.data =
				// ShortAndDouble(audioFromQueueData.data);
				// Log.e(" Aceel data " ," read " + accelFromQueueData.timestamp
				// + " " + Arrays.toString(accelFromQueueData.data));

				// inference results
				num++;
				int tmp = num % len;
				x[tmp] = accelFromQueueData.x / 9.8;
				y[tmp] = accelFromQueueData.y / 9.8;
				z[tmp] = accelFromQueueData.z / 9.8;
				if (num > 0 && tmp == acc.getFrameSize() - 1) {
					// Log.e(TAG, "acc : " + num);
					if (acc.getAccFeatures(x, y, z, accFeature) == 0) {

						// Log.d(TAG, Arrays.toString(accFeature));

						// save features

						// accel_features = new
						// ML_toolkit_object(System.currentTimeMillis()+appState.timeOffset,
						// 4, MyDataTypeConverter.toByta(accFeature));
						accel_features = appState.mMlToolkitObjectPool
								.borrowObject().setValues(
										accelFromQueueData.timestamp, 4,
										MyDataTypeConverter.toByta(accFeature),
										accelFromQueueData.sync_id);
						appState.ML_toolkit_buffer.insert(accel_features);

						int act = AccInference
								.gaussianAccClassifier(accFeature);

						// means we are already in a dutycyle turn off
						// so turn off sensing
						if (inDutyCycle == false
								&& appState.enableAccelDutyCycling == true) {
							mAccelerometerDataPool
									.returnObject(accelFromQueueData);
							appState.accelerometer_inference = "DutyCycling OFF";
							continue;
						}

						// 0 = unknown, 1 = stationary, 2 = driving, 3 =
						// walking, 4 = running,
						// 5 = cycling, 6 = error
						// for "amount_of_different_physical_activity"

						//update the inference buffer: 1-step 5-size sliding window
						//also, get the smoothed results based on the previous results. 
						int i=0;
						while(true)
						{
							//the whole buffer has been initialized. 
							if(inferenceBuffer[4]!=-1)
									break;
							if(inferenceBuffer[i]==-1){
								inferenceBuffer[i]=act;
								
								break;
							}
							i=i+1;
						}
						//Log.i("inference", String.valueOf(act));
						if(inferenceBuffer[4]!=-1)
						{
							inferenceBuffer[0]=inferenceBuffer[1];
							inferenceBuffer[1]=inferenceBuffer[2];
							inferenceBuffer[2]=inferenceBuffer[3];
							inferenceBuffer[3]=inferenceBuffer[4];
							inferenceBuffer[4]=act;
//							Log.i("Activity","Activity Type = " + inferenceBuffer[0] );
//							Log.i("Activity","Activity Type = " + inferenceBuffer[1] );
//							Log.i("Activity","Activity Type = " + inferenceBuffer[2] );
//							Log.i("Activity","Activity Type = " + inferenceBuffer[3] );
//							Log.i("Activity","Activity Type = " + inferenceBuffer[4] );
						}
//						//voting
						for(int j=0;j<=4;j++)
						{
							switch(inferenceBuffer[j])
							{
								case (0):
									voteInference[Inference.STATIONARY.ordinal()]=voteInference[Inference.STATIONARY.ordinal()]+1;
									break;
								case (1):
									voteInference[Inference.WALKING.ordinal()]=voteInference[Inference.WALKING.ordinal()]+1;
									break;
								case (2):
									voteInference[Inference.CYCLING.ordinal()]=voteInference[Inference.CYCLING.ordinal()]+1;
									break;
								case (3):
									voteInference[Inference.RUNNING.ordinal()]=voteInference[Inference.RUNNING.ordinal()]+1;
									break;
								case (4):
									voteInference[Inference.DRIVING.ordinal()]=voteInference[Inference.DRIVING.ordinal()]+1;
									break;
								case (5):
									voteInference[Inference.ERROR.ordinal()]=voteInference[Inference.ERROR.ordinal()]+1;
									break;
							}
							
						}
						voteInference[Inference.CYCLING.ordinal()] /= 2;
						voteInference[Inference.DRIVING.ordinal()] /= 2;
//						//get the highest vote for the lastest inference.
						int maximum=voteInference[0];
						int maxIdx=0;
						for(int j=1;j<=4;j++)
						{
							if(voteInference[j]>maximum)
							{
								maximum=voteInference[j];
								maxIdx=j;
							}	
							//Log.i("VOTE",String.valueOf(voteInference[j]));
							
						}
						smoothedInference = maxIdx;	
						
						//get the smoothed result based on the previous 4 results in the buffer.
						current_time_to_get_activity_duration = System
								.currentTimeMillis();
						long duration=current_time_to_get_activity_duration-prev_time_to_get_activity_duration;
						//notify other service that a new activity is available
						sendMessage(current_time_to_get_activity_duration / 1000, duration, smoothedInference);
						
						//clear the vote counter
						voteInference[0]=voteInference[1]=voteInference[2]=voteInference[3]=voteInference[4]=voteInference[5]=0;	
						
						switch (smoothedInference) {
						case(0):
							//curLabel = "Label:stationary";
							appState.accelerometer_inference = "stationary";
						appState.amount_of_different_physical_activity[1]++;// += current_time_to_get_activity_duration - prev_time_to_get_activity_duration;
						break;
						case(1):
							//curLabel = "Label:walking";
							appState.accelerometer_inference = "walking";
						appState.amount_of_different_physical_activity[3]+= current_time_to_get_activity_duration - prev_time_to_get_activity_duration;
						Log.i("timetimetimetimetime",String.valueOf(current_time_to_get_activity_duration - prev_time_to_get_activity_duration));
						break;
						case(2):
							//curLabel = "Label:cycling";
							//appState.accelerometer_inference = "cycling";
							appState.accelerometer_inference = "unknown";
						appState.amount_of_different_physical_activity[5]++;
						break;				
						case(3):
							//curLabel = "Label:running";
							appState.accelerometer_inference = "running";
						appState.amount_of_different_physical_activity[4]+= current_time_to_get_activity_duration - prev_time_to_get_activity_duration;
						Log.i("timetimetimetimetime",String.valueOf(current_time_to_get_activity_duration - prev_time_to_get_activity_duration));
						break;
						case(4):
							//curLabel = "Label:driving";
							//appState.accelerometer_inference = "driving";
							appState.accelerometer_inference = "unknown";
						appState.amount_of_different_physical_activity[2]++;
						break;
						default:
							//curLabel = "Label:null";
							appState.accelerometer_inference = "error";
							appState.amount_of_different_physical_activity[6]++;
							break;
							}
						prev_time_to_get_activity_duration = current_time_to_get_activity_duration;
							// Fanglin: just comment here, need to restore it
							// when
							// finish smoothing code.
							// long duration =
							// current_time_to_get_activity_duration
							// - prev_time_to_get_activity_duration;
							// //appState.amount_of_different_all_physical_activities+=
							// current_time_to_get_activity_duration -
							// prev_time_to_get_activity_duration;
							// //prev_time_to_get_activity_duration =
							// current_time_to_get_activity_duration;
							// sendMessage(current_time_to_get_activity_duration/1000,duration,
							// act);
							// everything gets saved
							// accel_inference = new
							// ML_toolkit_object(System.currentTimeMillis()+appState.timeOffset,
							// 6, true, appState.accelerometer_inference);

							accel_inference = appState.mMlToolkitObjectPool
									.borrowObject().setValues(
											accelFromQueueData.timestamp, 6,
											true,
											appState.accelerometer_inference,
											accelFromQueueData.sync_id);
							appState.ML_toolkit_buffer.insert(accel_inference);


					}
				}

				//sleep score computation
				sFeatureExtractor.increment(accelFromQueueData);
				mNumSamples++;
				if (mNumSamples % 100 == 0) {
					long elapsedmss = System.currentTimeMillis() - mStartmillis;
					if (elapsedmss > MAX_EVENTS_DURATION_MSEC) {
						if (elapsedmss < MAX_EVENTS_DURATION_MSEC * 1.2) {
							/*
							 * Check that we collected data for no more than
							 * WINDOW * 1.2. This ensures that we do not
							 * classify accelerations that were collected over
							 * an extremely long timespan (for example, due to
							 * very long duty cycling, service shutdowns and so
							 * on.
							 */
							// reset counters and gett classification
							mStartmillis = System.currentTimeMillis();
							AccelFeatures af = sFeatureExtractor.getResult();
							SleepState sleepState = sSleepClassifier.classify(af);
							sFeatureExtractor.clear();
							mNumSamples = 0;

							/*
							 * Store result in the database
							 */
							ML_toolkit_object sleepingObj = appState.mMlToolkitObjectPool.borrowObject();
							if (sleepState == SleepState.SLEEPING) {
								sleepingObj.setValues(mStartmillis, 13, new byte[] { 1 });
							} else {
								sleepingObj.setValues(mStartmillis, 13, new byte[] { 0 });
							}
							appState.ML_toolkit_buffer.insert(sleepingObj);


							//sleep duration measurement

							// *************************************
							// insert here code for sleeping score
							// we get here each WINDOW_LENGTH_MS millisecond
							// (by default, every 5 minutes). sleepState is the
							// currently detected state: SLEEPING or AWAKE
							// *************************************
							// sleepState == SleepState.SLEEPING
							// sleepState == SleepState.AWAKE

							//I will assume the duration is "elapsedmss" which I think is a better measurement
							//if(sleepState == SleepState.SLEEPING) 
							//appState.amount_of_sleeping_duration += SleepClassifier.WINDOW_LENGTH_MS;


							Log.i("SleepScores","Sleep state = " + sleepState );

						} else {
							mNumSamples = 0;
							mStartmillis = System.currentTimeMillis();
							sFeatureExtractor.clear();
						}
					}
				}
				mAccelerometerDataPool.returnObject(accelFromQueueData);


			}
		}
	}
	private MyQueuePopper myQueuePopper;


	protected void sendMessage(long timestamp,long duration, int activity)
	{
	    Intent intent = new Intent(
		    AwesomePipeline.class.getName());
	    intent.putExtra("timestamp", timestamp);
	    intent.putExtra("duration", duration);
	    intent.putExtra("activityid", (long)activity);
	    sendBroadcast(intent);
	}
	    
	/** Called when the activity is first created. */
	@Override
	public void onCreate() {
		inferenceBuffer[0]=inferenceBuffer[1]=inferenceBuffer[2]=inferenceBuffer[3]=inferenceBuffer[4]=-1;
		mAccelerometerDataPool = new AccelerometerDataPool();

		// Register our receiver for the ACTION_SCREEN_OFF action. This will make our receiver
		// code be called whenever the phone enters standby mode.
		IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_OFF);
		registerReceiver(AccelerometerManager.mReceiver, filter);

		//getting a reference to the application object
		appState = (Ml_Toolkit_Application)getApplicationContext();
		appState.acclerometerService = this;


		//get partial wakelock
		pm = (PowerManager) getSystemService(POWER_SERVICE);
		wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Howaya");
		wl.acquire();


		//set time to get future duration
		prev_time_to_get_activity_duration = System.currentTimeMillis();


		//initializing accel data
		accel_data = new double[3];

		// screen will stay on during this section

		no_of_records = 10000000;
		appState.accel_no_of_records = 0;
		CONTEXT = this;
		accelServ = this;

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
		//		Toast.makeText(this,
		//				"Service Started " + AccelerometerManager.isSupported(),
		//				Toast.LENGTH_SHORT).show();


		Activity_on = true;
		Foreground_on = true;
		appState.accelerometer_inference = this.inferred_status = "Not Available";
		this.prev_inferred_status = this.inferred_status;
		contentView = new RemoteViews(getPackageName(), R.layout.notification_layout);


		//circular buffer code
		//add a new buffer for putting accel-stuff
		cirBuffer = new CircularBufferFeatExtractionInference<AccelerometerService.AccelerometerData>(null, 100);
		myQueuePopper = new MyQueuePopper(cirBuffer);
		myQueuePopper.start();

		//initiate feature extraction
		acc = new AccFeatureExtraction(len,samplingRate);
		accFeature = new double[acc.getFeatureSize()];
		

		mBatteryReceiver = new BatteryReceiver();
		registerReceiver(mBatteryReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
	}

	public static Context getContext() {
		return CONTEXT;
	}

	// This is the old onStart method that will be called on the pre-2.0
	// platform. On 2.0 or later we override onStartCommand() so this
	// method will not be called.
	@Override
	public void onStart(Intent intent, int startId) {
		handleCommand(intent);

		//start accelerometer listening code
		if (AccelerometerManager.isSupported()) {
			t = new Thread() {
				public void run(){

					//set the acclerometer sampling rate based on rooted or not
					if(appState.isPhoneRooted == true)
						AccelerometerManager.rate = 3;
					else
						AccelerometerManager.rate = SensorManager.SENSOR_DELAY_GAME;

					AccelerometerManager.startListening(accelServ);
				}
			};
			t.setPriority(Thread.NORM_PRIORITY+1);
			t.start();
		}
		//duty cycling code

		if(appState.enableAccelDutyCycling){
			inDutyCycle = true;  //means inside dutycycle
			mHandler.removeCallbacks(mUpdateTimeTask);
			//mHandler.postDelayed(mUpdateTimeTask, 30*1000);

			//accelerometer sensing has already started. Thus sense for appState.accelDutyCyclingSensingInterval milliseconds
			mHandler.postDelayed(mUpdateTimeTask, appState.accelDutyCyclingSensingInterval.longValue());
		}

	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		if(intent == null)
		{
			abnormalRestart = true;
			stopSelf();
			//appState.mlt.destroy();
			//return 
			return START_NOT_STICKY;
		}

		handleCommand(intent);


		//start accelerometer listening code
		if (AccelerometerManager.isSupported()) {
			t = new Thread() {
				public void run(){

					//set the acclerometer sampling rate based on rooted or not
					if(appState.isPhoneRooted == true)
						AccelerometerManager.rate = 3;
					else
						AccelerometerManager.rate = SensorManager.SENSOR_DELAY_GAME;

					AccelerometerManager.startListening(accelServ);
				}
			};
			t.setPriority(Thread.NORM_PRIORITY+1);
			t.start();
		}


		//duty cycling code
		if(appState.enableAccelDutyCycling){
			inDutyCycle = true;  //means inside dutycycle
			mHandler.removeCallbacks(mUpdateTimeTask);
			//mHandler.postDelayed(mUpdateTimeTask, 30*1000);

			//accelerometer sensing has already started. Thus sense for appState.accelDutyCyclingSensingInterval milliseconds
			mHandler.postDelayed(mUpdateTimeTask, appState.accelDutyCyclingSensingInterval.longValue());
		}



		// We want this service to continue running until it is explicitly
		// stopped, so return sticky.		
		return START_STICKY;
		//return START_NOT_STICKY;
		//return START_REDELIVER_INTENT;
	}


	private boolean inDutyCycle;
	private Runnable mUpdateTimeTask = new Runnable() {
		public void run() {
			//appState.location_text = "Scanning";																							

			//locationAlreadyFound = false;

			//myLocationManager.getLocation(LocationService.this, locationResult );
			//mHandler.postAtTime(this,1000*60*1);

			if(inDutyCycle)
			{

				try
				{	
					//for debug
					//Toast.makeText(this, "AS: Battery level is: " + mBatteryReceiver.currentEnergyLevel.toString() , Toast.LENGTH_LONG).show();	

					// use here the battery manager
					// to calibrate duty cycling based on battery level

					if (mBatteryReceiver.currentEnergyLevel == ENERGYLEVEL.HIGH)
						//no duty-cycle
					{
						appState.accelDutyCyclingRestartInterval.set(3 * 60 * 1000);		
					}	
					else if (mBatteryReceiver.currentEnergyLevel == ENERGYLEVEL.LOW)
						//heavy duty-cycle
					{
						appState.accelDutyCyclingRestartInterval.set(3 * 60 * 1000);
					}
					else
						//light duty-cycle
					{
						appState.accelDutyCyclingRestartInterval.set(3 * 60 * 1000);
					}
				}
				catch(Exception ex)
				{
					Log.d(TAG, ex.toString());
				}


				mHandler.removeCallbacks(mUpdateTimeTask);
				//now stop. start after accelDutyCyclingRestartInterval-accelDutyCyclingSensingInterval ms
				//mHandler.postDelayed(mUpdateTimeTask, appState.accelDutyCyclingRestartInterval.longValue() - appState.accelDutyCyclingSensingInterval.longValue());
				mHandler.postDelayed(mUpdateTimeTask, 3 * 60 * 1000);

				//t.stop();
				AccelerometerManager.stopListening();

				inDutyCycle = false;
				appState.accelerometer_inference = "DutyCycling OFF";
				Log.d(TAG, "DutyCycling OFF");

			}
			else
			{

				mHandler.removeCallbacks(mUpdateTimeTask);
				//accelerometer sensing has already started. Thus sense for appState.accelDutyCyclingSensingInterval milliseconds
				mHandler.postDelayed(mUpdateTimeTask, appState.accelDutyCyclingSensingInterval.longValue());//start end every 30 seconds


				if (AccelerometerManager.isSupported()) {
					t = new Thread() {
						public void run(){

							//set the acclerometer sampling rate based on rooted or not
							if(appState.isPhoneRooted == true)
								AccelerometerManager.rate = 3;
							else
								AccelerometerManager.rate = SensorManager.SENSOR_DELAY_GAME;

							AccelerometerManager.startListening(accelServ);
						}
					};
					t.setPriority(Thread.NORM_PRIORITY+1);
					t.start();
				}
				//Toast.makeText(appState.acclerometerService,
				//"Accelerometer sensing started DC",
				//Toast.LENGTH_SHORT).show();
				inDutyCycle = true;

				Log.d(TAG, "DutyCycling ON");

			}





			//mHandler.postDelayed(mUpdateTimeTask, 1000*60*10);
			//mHandler.postDelayed(mUpdateTimeTask, 1000*30);
		}
	};

	void handleCommand(Intent intent) {
		if (ACTION_FOREGROUND.equals(intent.getAction())) {
			// In this sample, we'll use the same text for the ticker and the
			// expanded notification
			String text = "Accel On: ";// getText(R.string.foreground_service_started);
			contentView = new RemoteViews(getPackageName(), R.layout.notification_layout);
			//notification = new Notification(R.drawable.logo,"BeWell",System.currentTimeMillis());//(R.drawable.icon, text, System.currentTimeMillis());
			notification = new Notification(R.drawable.notification_fish,getString(R.string.app_name),System.currentTimeMillis());

			//contentView.setImageViewResource(R.id.status_icon, R.drawable.icon);
			/*
			contentView.setTextViewText(R.id.accel_text, "Accel On:" + " (" + appState.accel_no_of_records + ")");
			contentView.setTextColor(R.id.accel_text, Color.argb(128, 0, 115, 0));
			contentView.setTextViewText(R.id.audio_text, "Audio on:" + " (" + appState.audio_no_of_records + ")");
			contentView.setTextColor(R.id.audio_text, Color.argb(128, 0, 115, 0));
			contentView.setTextViewText(R.id.location_text, "Location on:" + " (" + appState.location_text + ")");
			contentView.setTextColor(R.id.location_text, Color.argb(128, 0, 115, 0));
			 */
			//contentView.setTextViewText(R.id.accel_text, "Audio on:" + " (" + appState.audio_no_of_records + ")");
			//contentView.setT
			notification.contentView = contentView;
			/*PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
			new Intent(this, org.bewellapp.main_activity.class), 0);*/
			PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
					new Intent(this, WellnessSummaryActivity.class), 0);
			notification.contentIntent = contentIntent;

			startForegroundCompat(R.string.CUSTOM_VIEW,notification);
			mStartmillis = System.currentTimeMillis();
			mNumSamples = 0;

			//startForegroundCompat(1,
			//	notification);


		} else if (ACTION_BACKGROUND.equals(intent.getAction())) {
			stopForegroundCompat(R.string.foreground_service_started);
			//stopForegroundCompat(1);
		}
	}


	public void callStartForegroundCompat(){
		startForegroundCompat(R.string.CUSTOM_VIEW,notification);

		try{
			contentView = new RemoteViews(getPackageName(), R.layout.notification_layout);
			//update notification area
			/*
			contentView.setTextViewText(R.id.accel_text, "Accel On:" + " (" + appState.accel_no_of_records + ")");
			contentView.setTextColor(R.id.accel_text, Color.argb(128, 0, 115, 0));

			if(appState.audioSensorOn){
				contentView.setTextViewText(R.id.audio_text, "Audio on:" + " (" + appState.audio_no_of_records + ")");
				contentView.setTextColor(R.id.audio_text, Color.argb(128, 0, 115, 0));
			}
			else
			{
				contentView.setTextViewText(R.id.audio_text, "Audio off");
				contentView.setTextColor(R.id.audio_text, Color.argb(128, 115, 0, 0));
			}

			if(appState.locationSensorOn){
				contentView.setTextViewText(R.id.location_text, "Location on:" + " (" + appState.location_text + ")");					
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
	}

	/**
	 * This is a wrapper around the new startForeground method, using the older
	 * APIs if it is not available.
	 */
	public void startForegroundCompat(int id, Notification notification) {
		// If we have the new startForeground API, then use it.
		Foreground_on = true;
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

	/**
	 * This is a wrapper around the new stopForeground method, using the older
	 * APIs if it is not available.
	 */
	public void stopForegroundCompat(int id) {
		Foreground_on = false;
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
	public void onDestroy() {
		// Make sure our notification is gone.
		super.onDestroy();
		mAccelerometerDataPool.close();
		unregisterReceiver(AccelerometerManager.mReceiver);
		unregisterReceiver(mBatteryReceiver);
		mHandler.removeCallbacks(mUpdateTimeTask);
		//t.stop();
		if(abnormalRestart == false){
			//end the current inferred activity
			//accel_inference =  new ML_toolkit_object(System.currentTimeMillis()+appState.timeOffset, 6, false ,inferred_status);
			//appState.ML_toolkit_buffer.insert(accel_inference);

			//accel_inference =  new ML_toolkit_object(System.currentTimeMillis()+appState.timeOffset, 6, true ,"unknown");
			//appState.ML_toolkit_buffer.insert(accel_inference);

			AccelerometerManager.stopListening();		
			stopForegroundCompat(R.string.CUSTOM_VIEW);
			appState.acclerometerService = null;
		}
		//stopForegroundCompat(1);

		//release powerlock
		wl.release();
	}


	//binders for service //// to send data to the front end
	@Override
	public IBinder onBind(Intent intent) {
		return binder;
	}

	public class AccelerometerBinder extends Binder{
		public AccelerometerService getService(){
			return AccelerometerService.this;
		}
	}

	/**
	 * onShake callback
	 */
	public void onShake(float force) {
		Toast.makeText(this, "Phone shaked : " + force, 1000).show();
	}


	/**
	 * onAccelerationChanged callback
	 */
	public long tempTimestamp;

	public void onAccelerationChanged(long timestamp, float x, float y, float z) {

		try {
			appState.accel_no_of_records++;

			this.accel_data[0] = x;
			this.accel_data[1] = y;
			this.accel_data[2] = z;

			this.accel_object = null;
			tempTimestamp = System.currentTimeMillis() + appState.timeOffset;

			if (appState.rawAccelOn) {
				this.accel_object = appState.mMlToolkitObjectPool.borrowObject().setValues(tempTimestamp, 1,
						MyDataTypeConverter.toByta(accel_data), (++this.sync_id_counter) % 16384);
				// inserting into the buffer
				appState.ML_toolkit_buffer.insert(this.accel_object);
			}

			cirBuffer.insert(mAccelerometerDataPool.borrowObject().setValues(x, y, z, tempTimestamp,
					this.sync_id_counter % 16384));

		} catch (Exception e) {
			e.printStackTrace();
		}

		try {
			if (appState.accel_no_of_records % 54000 == 1 && Foreground_on == true) {
				contentView = new RemoteViews(getPackageName(), R.layout.notification_layout);
				/*
				contentView.setTextViewText(R.id.accel_text, "Accel On:" + " (" + appState.accel_no_of_records + ")");
				contentView.setTextColor(R.id.accel_text, Color.argb(128, 0, 115, 0));

				if (appState.audioSensorOn) {
					contentView.setTextViewText(R.id.audio_text, "Audio On:" + " (" + appState.audio_no_of_records
							+ ")");
					contentView.setTextColor(R.id.audio_text, Color.argb(128, 0, 115, 0));
				} else {
					contentView.setTextViewText(R.id.audio_text, "Audio off");
					contentView.setTextColor(R.id.audio_text, Color.argb(128, 115, 0, 0));
				}

				if (appState.locationSensorOn) {
					contentView.setTextViewText(R.id.location_text, "Location On:" + " (" + appState.location_text
							+ ")");
					contentView.setTextColor(R.id.location_text, Color.argb(128, 0, 115, 0));
				} else {
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

				// startForegroundCompat(R.string.CUSTOM_VIEW,notification);
				mNM.notify(R.string.CUSTOM_VIEW, notification);

			}
		} catch (Exception ex) {
		}

	}


	private static class AccelerometerDataFactory extends BasePoolableObjectFactory {
		public Object makeObject() {
			return new AccelerometerData();
		}
	}

	private static class AccelerometerDataPool extends StackObjectPool {
		public AccelerometerDataPool() {
			super(new AccelerometerDataFactory());
			try {
				PoolUtils.prefill(this, 200);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		@Override
		public AccelerometerData borrowObject() {
			try {
				return (AccelerometerData) super.borrowObject();
			} catch (Exception e) {
				e.printStackTrace();
				return null;
			}
		}

		@Override
		public void returnObject(Object obj) {
			try {
				super.returnObject(obj);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		@Override
		public void close() {
			try {
				super.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	static class FeatureExtractor {

		Max maxx = new Max();
		Max maxy = new Max();
		Max maxz = new Max();
		Mean meanx = new Mean();
		Mean meany = new Mean();
		Mean meanz = new Mean();
		Mean meanxyabsdiff = new Mean();
		Min minx = new Min();
		Min miny = new Min();
		Min minz = new Min();
		SignalEnergy energyx = new SignalEnergy();
		SignalEnergy energyy = new SignalEnergy();
		SignalEnergy energyz = new SignalEnergy();
		StandardDeviation stdevx = new StandardDeviation(false);
		StandardDeviation stdevy = new StandardDeviation(false);
		StandardDeviation stdevz = new StandardDeviation(false);

		/**
		 * Resets the internal state of the current instance. Call it to start extracting features of a new batch of SensorEvents.
		 */
		public void clear() {
			maxx.clear();
			maxy.clear();
			maxz.clear();
			minx.clear();
			miny.clear();
			minz.clear();
			meanx.clear();
			meany.clear();
			meanz.clear();
			meanxyabsdiff.clear();
			stdevx.clear();
			stdevy.clear();
			stdevz.clear();
		}

		/**
		 * Returns the features of the current batch of SensorEvents. Warning:
		 * getResult() can be time consuming. Calling it from the UI thread may be a bad idea.
		 * 
		 * @return The features of the collected SensorEvents.
		 */
		public AccelFeatures getResult() {
			AccelFeatures features = new AccelFeatures();
			features.energyx = energyx.getResult();
			features.energyy = energyy.getResult();
			features.energyz = energyz.getResult();
			features.maxx = maxx.getResult();
			features.maxy = maxy.getResult();
			features.maxz = maxz.getResult();
			features.minx = minx.getResult();
			features.miny = miny.getResult();
			features.minz = minz.getResult();
			features.meanx = meanx.getResult();
			features.meany = meany.getResult();
			features.meanz = meanz.getResult();
			features.meanxyabsdiff = Math.abs(features.meanx - features.meany);
			features.stdevx = stdevx.getResult();
			features.stdevy = stdevy.getResult();
			features.stdevz = stdevz.getResult();
			return features;
		}

		/**
		 * Updates the internal state of this extractor with a new SensorEvent
		 * 
		 * @param event
		 *            SensorEvent to evaluate.
		 */
		public void increment(AccelerometerData data) {
			energyx.increment(data.x);
			energyy.increment(data.y);
			energyz.increment(data.z);
			maxx.increment(data.x);
			maxy.increment(data.y);
			maxz.increment(data.z);
			meanx.increment(data.x);
			meany.increment(data.y);
			meanz.increment(data.z);
			meanxyabsdiff.increment(Math.abs(data.x - data.y));
			minx.increment(data.x);
			miny.increment(data.y);
			minz.increment(data.z);
			stdevx.increment(data.x);
			stdevy.increment(data.y);
			stdevz.increment(data.z);
		}
	}


	private static enum ENERGYLEVEL {
		LOW, MEDIUM, HIGH;
	}


	private class BatteryReceiver extends BroadcastReceiver {

		protected  ENERGYLEVEL currentEnergyLevel;
		private final float THRESH_ENERGY_LOW = 0.45f;
		private final float THRESH_ENERGY_MEDIUM = 0.85f;

		public BatteryReceiver() {
			currentEnergyLevel = ENERGYLEVEL.MEDIUM;
		}

		@Override
		public void onReceive(Context context, Intent intent) {
			if (!intent.getAction().equals(Intent.ACTION_BATTERY_CHANGED)) {
				currentEnergyLevel = ENERGYLEVEL.MEDIUM;
				return;
			}
			int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
			float scale = (float)intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
			int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);

			switch (status) {
			case BatteryManager.BATTERY_STATUS_FULL:
			case BatteryManager.BATTERY_STATUS_CHARGING:
				currentEnergyLevel = ENERGYLEVEL.HIGH;
				break;
			case BatteryManager.BATTERY_STATUS_NOT_CHARGING:
			case BatteryManager.BATTERY_STATUS_DISCHARGING:
			{
				float batteryLevel = level/scale;
				if (batteryLevel < THRESH_ENERGY_LOW) {
					currentEnergyLevel = ENERGYLEVEL.LOW;
				} else if (batteryLevel < THRESH_ENERGY_MEDIUM) {
					currentEnergyLevel = ENERGYLEVEL.MEDIUM;
				} else {
					currentEnergyLevel = ENERGYLEVEL.HIGH;
				}
			}
			break;
			default:
				currentEnergyLevel = ENERGYLEVEL.MEDIUM;
			}
		}
	}
}

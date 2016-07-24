package org.bewellapp;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Properties;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.pool.BasePoolableObjectFactory;
import org.apache.commons.pool.PoolUtils;
import org.apache.commons.pool.impl.StackObjectPool;
import org.bewellapp.CrashHandlers.ErrorReporter;
import org.bewellapp.DaemonService.DaemonService;
import org.bewellapp.MomentarySampling.MomentarySamplingService;
import org.bewellapp.MomentarySampling.MyMomentarySamplingDBAdapter;
import org.bewellapp.NTPTimeStampService.NTPTimeStampService;
import org.bewellapp.ScoreComputation.ScoreComputationDBAdapter;
import org.bewellapp.ServiceControllers.ServiceController;
import org.bewellapp.ServiceControllers.AccelerometerLib.AccelerometerService;
import org.bewellapp.ServiceControllers.AppLib.AppMonitorScreenStatusReceiver;
import org.bewellapp.ServiceControllers.AudioLib.AudioService;
import org.bewellapp.ServiceControllers.AudioLib.MyPhoneStateListener;
import org.bewellapp.ServiceControllers.BluetoothLib.BluetoothService;
import org.bewellapp.ServiceControllers.LocationLib.LocationService;
import org.bewellapp.ServiceControllers.UploadingLib.InfoObject;
import org.bewellapp.ServiceControllers.WifiLib.WifiScanService;
import org.bewellapp.Storage.CircularBuffer;
import org.bewellapp.Storage.ML_toolkit_object;
import org.bewellapp.Storage.MyDBAdapter;
import org.bewellapp.Storage.MySDCardStateListener;
import org.bewellapp.utils.ArrayDeque;
import org.bewellapp.wallpaper.WellnessSummaryActivity;

import android.app.Application;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import edu.dartmouthcs.UtilLibs.WriteToLogFiles;

public class Ml_Toolkit_Application extends Application {
	public static Context context;
	private ServiceController serviceController;
	public main_activity main_activity_Context;
	public WellnessSummaryActivity summary_activity_Context;

	public NotificationManager mNM;
	// public MLToolkitInterface mlt;

	public final int ACCEL_SAMPLERATE = 32;
	public final int ACCEL_SAMPLES_REQUIRED = 128;
	public final int AUDIO_SAMPLES_REQUIRED = 512;
	public final int AUDIO_SAMPLERATE = 8000;

	// buffer to store data
	public CircularBuffer<ML_toolkit_object> ML_toolkit_buffer;

	// databse adapter
	public MyDBAdapter db_adapter;
	// database primary key
	public int database_primary_key_id;

	// write after how many entries
	public final int writeAfterThisManyValues = 50;// 0;
	public final int maximumDbSize = 128*1024;// 1000000

	// write to a file
	// public OutputStream outFile;
	public ObjectOutputStream dataOutputFile;
	public String currentFileName;
	public InfoObject infoObj;
	public List<String> fileList;

	// upload
	public boolean uploadRunning;

	// flags to see whether sensors in turned on/off
	public boolean audioSensorOn;
	public boolean accelSensorOn;
	public boolean locationSensorOn;
	public boolean wifiSensorOn;
	public boolean bluetoothSensorOn;

	//
	public boolean phone_call_on;

	// phone state object
	public MyPhoneStateListener phoneCallInfo;

	// SD card state listener
	public MySDCardStateListener mySDCardStateInfo;

	// application started once ... to ensure that services started only once
	// needed for Pending notifications in the main screen
	public boolean applicatin_starated;

	// all the labeling states
	public boolean stationaryOngoing;
	public boolean walkingOngoing;
	public boolean runningOngoing;
	public boolean drivingOngoing;
	public boolean talkingOngoing;

	// IMEI
	public String IMEI;

	// audio release true or false;
	public boolean audio_release = false;

	// raw audio flag
	public boolean rawAudioOn;
	// raw accel flag
	public boolean rawAccelOn;

	// audio force locked
	public boolean audioForceLocked;

	// is phone rooted
	public final boolean isPhoneRooted;

	// saved sensors in turned on/off
	public boolean savedAudioSensorOn;
	public boolean savedAccelSensorOn;
	public boolean savedLocationSensorOn;
	public boolean savedRawSensorOn;
	public boolean savedWifiSensorOn = false;
	public boolean savedBluetoothSensorOn =false;
	public boolean savedRawAccelOn;

	public boolean audioServiceStarted = false;

	// time zone offset
	public long timeOffset;

	// current number of records
	public long accel_no_of_records;
	public long audio_no_of_records;
	public String location_text;

	// service pointers
	public LocationService locationService;
	public AudioService audioService;
	public AccelerometerService acclerometerService;
	public DaemonService daemonService;
	public WifiScanService wifiScanService;
	public BluetoothService bluetoothService;
	public NTPTimeStampService nTPTimeStampService;
	public MomentarySamplingService momentarySamplingService;
	public AppMonitorScreenStatusReceiver appMonitorScreenStatusReceiver;
	// public AudioDutyCycleService audioDutyCylcingService;

	public OutputStream currentOpenSDCardFile;

	// SD card mounted
	public boolean SDCardMounted;

	public boolean fileListInitialzied = false;
	private Handler mHandler = new Handler();
	private final int rateAccelSampling = 1000 * 15;

	// crash handler object
	public ErrorReporter errorReporter;

	// counter for next restart/stop of services
	public static int DayCounter = 0;

	// file count
	public int file_count = 0;

	// url for webservice
	public String WebServiceURL = "metro2.cs.dartmouth.edu/mm_io/upload";
	public String WebserviceSuggestionURL = "metro2.cs.dartmouth.edu/mm_io/online";

	public WriteToLogFiles wrtlfile;

	public int numberOfWifiScans = 0;
	public int numberOfBluetoothScans = 0;

	public boolean all_sesning_disabled = false;

	public MyMomentarySamplingDBAdapter ms_db_adapter;

	// duty cycling codes
	public boolean enableAudioDutyCycling = false;
	public boolean enableAccelDutyCycling = false;
	public boolean savedEnableAudioDutyCycling = false;
	public boolean savedEnableAccelDutyCycling = false;


	public AtomicInteger audioDutyCyclingRestartInterval = new AtomicInteger(2 * 60 * 1000);// every X
	// minutes
	// sensing will
	// start
	public AtomicInteger audioDutyCyclingSensingInterval = new AtomicInteger(1 * 60 * 1000);// sensing will
	// continue
	// for Y minutes
	// after start

	public AtomicInteger accelDutyCyclingRestartInterval = new AtomicInteger(2 * 60 * 1000);// every X
	// minutes
	// sensing will
	// start
	public AtomicInteger accelDutyCyclingSensingInterval = new AtomicInteger(1 * 60 * 1000);// sensing will
	// continue
	// for Y minutes
	// after start

	// inference results
	public String accelerometer_inference = "Not Available";
	public String audio_inference = "Not Available";

	// bewell score computation
	public int frequency;	//computation frequency

	public double current_social_score;
	public double prev_social_score;
	public long amount_of_different_physical_activity[]; // different physical
	// activities, will
	// be an array
	public long amount_of_different_all_physical_activities;
	public final static int NO_PHYSICAL_ACTIVITY = 7;	
	//different physical
	//activities for the
	// 24-hour sliding window
	public ArrayDeque<long[]> sliding_of_different_physical_activity;	
	public long [] last_of_different_physical_activity;		//values for last time window 	

	public double current_physical_score;
	public double prev_physical_score;
	public long amount_of_different_voice_activity[]; // different voice
	// activities, will be
	// an array
	public long amount_of_different_all_voice_activities;
	public final static int NO_VOICE_ACTIVITY = 4;
	public static final String SHARED_PREF = "bewell_pref";
	public static final String SP_UPLOAD_SCORES = "bewell_pref";
	public static final String SP_USERNAME = "bewell_username";
	public static final String SP_PASSWORD = "bewell_password";
	public static final String SP_ISREGISTERED = "bewell_is_registered";
	public static final String SP_WEB_SCORES_URL = "bewell_addscores_url";
	public static final String SP_WEB_LOGIN_URL = "bewell_login_url";
	public static final String SP_WEB_REGISTER_URL = "bewell_register_url";
	public static final String SP_WEB_PHONESTATS_URL = "bewell_phonestats_url";
	public static final String SP_WEB_UPLOAD_DB_URL = "bewell_db_upload_url";
	public static final String SP_ISSTOPPED = "bewell_is_stopped";
	public static final String SP_SOCIAL_PHONE_CALLS_SECONDS = "bewell_social_phone_seconds";
	public static final String SP_SOCIAL_APPS_SECONDS = "bewell_social_apps_seconds";
	public static final String DEFAULT_SP_DOMAIN_NAME="biorhythm.cs.dartmouth.edu";
	public static final String DEFAULT_SP_ADDSCORES_URL = "https://"+DEFAULT_SP_DOMAIN_NAME+"/addScores";
	public static final String DEFAULT_SP_ADDPHONESTATS_URL = "https://"+DEFAULT_SP_DOMAIN_NAME+"/addPhoneStats";
	public static final String DEFAULT_SP_REGISTER_URL      = "https://"+DEFAULT_SP_DOMAIN_NAME+"/register";
	public static final String DEFAULT_SP_LOGIN_URL         = "https://"+DEFAULT_SP_DOMAIN_NAME+"/remoteLogin";
	public static final String DEFAULT_SP_UPLOAD_DB_URL     = "https://"+DEFAULT_SP_DOMAIN_NAME+"/upload";
	public static final String SP_WELCOMED = "bewell_welcomed";

	//sliding window values
	public ArrayDeque<Long> sliding_of_different_all_voice_activities;
	// different voice
	//activities for the
	// 24-hour sliding window
	public ArrayDeque<long[]> sliding_of_different_voice_activity;
	public long last_of_different_all_voice_activities;		//values for last time window	
	public long[] last_of_different_voice_activity;		//values for last time window


	public double current_sleep_score;
	public double prev_sleep_score;
	//long for number of ms in sleep
	public long amount_of_sleeping_duration; //in millisecond
	public long sleeping_duration_record_time; // in millisecond


	// bewell db_adapter
	public ScoreComputationDBAdapter beWellScoreAdapter;
	public ScoreComputationDBAdapter beWellScoreAdapter2;

	public MlToolkitObjPool mMlToolkitObjectPool;

	public Ml_Toolkit_Application() {
		context = this;
		serviceController = new ServiceController(context);
		// accelerometerTurnedAtSensorSelctionScreen = false;
		// accelerometerSensor = false;
		// audioSensor = false;

		// initializeMLtoolkit();
		// mlt = new MLToolkitInterface();

		uploadRunning = false;

		// phone call running
		phone_call_on = false;

		// set application started to false
		applicatin_starated = false;

		// create the circular buffer
		ML_toolkit_buffer = new CircularBuffer<ML_toolkit_object>(context, 6000);// circular
		// buffer
		// size

		// create new database and get the database adapter
		// initializeDataBase();

		// file write codes

		// intiializFiles();

		// inititializeInfoObject();

		// files currently available
		fileList = Collections.synchronizedList(new ArrayList<String>());

		// getFileList();

		// set all the activity labels
		stationaryOngoing = false;
		walkingOngoing = false;
		runningOngoing = false;
		drivingOngoing = false;
		talkingOngoing = false;

		// IMEI
		IMEI = "dummy";

		// raw audio on flag
		rawAudioOn = true;
		rawAccelOn = false;

		// if necessary then service controller will start them and fix the
		// values and
		// that will be determined by the current state
		audioSensorOn = false;
		accelSensorOn = false;
		locationSensorOn = false;
		wifiSensorOn = false;
		bluetoothSensorOn = false;

		// audio force locked
		audioForceLocked = false;

		// is phone rooted
		this.isPhoneRooted = getRootInformation();

		// remember the last state the phone was in
		// initializeMLtoolkitConfiguration();

		// get the time zone offset
		timeZoneOffset();

		// number of records
		this.accel_no_of_records = 0;
		this.audio_no_of_records = 0;
		this.location_text = "";

		// service intialization
		this.locationService = null;
		this.audioService = null;
		this.acclerometerService = null;
		this.daemonService = null;

		// SD card file
		currentOpenSDCardFile = null;

		// SD card mounted
		this.SDCardMounted = false;

		mHandler.removeCallbacks(mUpdateTimeTask);
		mHandler.postDelayed(mUpdateTimeTask, rateAccelSampling);// wait 15
		// seconds
		// and start
		// updating
		// file list

		// register crash handler
		errorReporter = new ErrorReporter();
		errorReporter.Init(this);

		// initialize the bewellscores
		// NO_VOICE_ACTIVITY = 4;
		// NO_PHYSICAL_ACTIVITY = 7;
		initialize_bewell_scores();

		mMlToolkitObjectPool = new MlToolkitObjPool();
	}

	private void initialize_bewell_scores() {
		// need to go for databases to store the values properly

		this.frequency = 1;
		this.sliding_of_different_physical_activity = new ArrayDeque<long []>();
		this.sliding_of_different_voice_activity = new ArrayDeque<long []>();
		this.sliding_of_different_all_voice_activities = new ArrayDeque<Long>(); 

		this.last_of_different_physical_activity = new long[NO_PHYSICAL_ACTIVITY];
		for (int i = 0; i < NO_PHYSICAL_ACTIVITY; i++)
			this.last_of_different_physical_activity[i] = 0;
		this.last_of_different_all_voice_activities = 0;
		this.last_of_different_voice_activity = new long[NO_VOICE_ACTIVITY];
		for (int i = 0; i < NO_VOICE_ACTIVITY; i++)
			this.last_of_different_voice_activity[i] = 0;



		// 0 = unknown, 1 = stationary, 2 = driving, 3 = walking, 4 = running,
		// 5 = cycling, 6 = error
		amount_of_different_physical_activity = new long[NO_PHYSICAL_ACTIVITY];
		amount_of_different_all_physical_activities = 0;
		for (int i = 0; i < NO_PHYSICAL_ACTIVITY; i++)
			amount_of_different_physical_activity[i] = 0;

		// 0 = silence, 1 = noise, 2 = voice, 3 = error
		amount_of_different_voice_activity = new long[NO_VOICE_ACTIVITY];
		amount_of_different_all_voice_activities = 0;
		for (int i = 0; i < NO_VOICE_ACTIVITY; i++)
			amount_of_different_voice_activity[i] = 0;
	}

	// a timer to initialize file list
	private Runnable mUpdateTimeTask = new Runnable() {
		public void run() {

			if (fileListInitialzied == false) {
				fileListInitialzied = true;
				intiializFiles();// creating file list
				getFileList();
				getUrlLocation();
				Log.i("SD card-examplee", "File list initiated Initiator ");

				// start writing to the log file
				// print log messages
				wrtlfile = new WriteToLogFiles();
				wrtlfile.startLogThread();
			}
			// mHandler.postDelayed(mUpdateTimeTask, 1000*60*10);
			// mHandler.postDelayed(mUpdateTimeTask, 1000*30);
		}
	};

	private boolean getRootInformation() {
		// TODO Auto-generated method stub
//		if ((Build.ID).equals("MASTER"))
//			return true;

		return false;
	}

	public void getFileList() {
		// TODO Auto-generated method stub
		File folder = new File(Environment.getExternalStorageDirectory(), "mltoolkit");
		File[] listOfFiles = folder.listFiles();

		if (listOfFiles != null) {// if sd card is not avaiable
			for (int i = 0; i < listOfFiles.length; i++) {
				if (listOfFiles[i].isFile()) {
					// System.out.println("File " + listOfFiles[i].getName());
					try {
						fileList.add(listOfFiles[i].getCanonicalPath());
					} catch (IOException e) {
						// TODO Auto-generated catch block
						// e.printStackTrace();
					}
				}
			}
		}

		folder = new File(Environment.getExternalStorageDirectory(), "mltoolkit_uploaded");
		listOfFiles = folder.listFiles();

		if (listOfFiles != null) {// if sd card is not avaiable
			for (int i = 0; i < listOfFiles.length; i++) {
				if (listOfFiles[i].isFile()) {
					// System.out.println("File " + listOfFiles[i].getName());
					// try {
					// fileList.add(listOfFiles[i].getCanonicalPath());
					this.file_count++;
					// }
				}
			}
		}
	}

	private void inititializeInfoObject() {
		// TODO Auto-generated method stub
		// infoObj = new InfoObject(this);

	}

	public void intiializFiles() {
		// TODO Auto-generated method stub
		File f = new File(Environment.getExternalStorageDirectory(), "mltoolkit");
		if (f.exists() == false)
			f.mkdir();

		f = new File(Environment.getExternalStorageDirectory(),"mltoolkit_uploaded");
		if (f.exists() == false)
			f.mkdir();

		f = new File(Environment.getExternalStorageDirectory(), "mltoolkit_config");
		if (f.exists() == false)
			f.mkdir();
	}

	protected void getUrlLocation() {
		// TODO Auto-generated method stub
		Properties prop = new Properties();
		String fileName =  new File(new File(Environment.getExternalStorageDirectory(), "mltoolkit_config"), "mltoolkit_url.txt").getAbsolutePath();
		// String fileName = "mltoolkit_states.config";

		// String fileName = "mltoolkit_states.config";
		InputStream is;
		try {
			// is = new FileInputStream(fileName );
			// is = openFileInput(fileName);
			is = new FileInputStream(fileName);
			prop.load(is);
			this.WebServiceURL = prop.getProperty("URL");
			is.close();

		} catch (Exception e) {
			// TODO Auto-generated catch block
			// e.printStackTrace();

			// means file doesn't exist
			// start writing the files
			prop = new Properties();
			// String fileName = "mltoolkit_states.config";
			// String fileName = "mltoolkit_states.config";
			OutputStream outs;
			try {
				// outs = openFileOutput(fileName,MODE_PRIVATE);
				outs = new FileOutputStream(fileName);
				// prop.load(is);
				prop.setProperty("URL", this.WebServiceURL);
				prop.store(outs, "ML Toolkit URL Config file");
				outs.close();
			} catch (Exception ex) {
				// TODO Auto-generated catch block
				ex.printStackTrace();
				Log.w("FAILED", "FILE WRITE FAILED " + ex.toString());
			}
		}
	}

	public void initializeDataBase() {
	    // delete broken data base files
	    //deleteBrokenDbrFileInInternalMemory();

	    // TODO Auto-generated method stub
	    this.database_primary_key_id = 0;
	    // db_adapter = new MyDBAdapter(this,"myDatabase_" +
	    // MiscUtilityFunctions.now() + ".db");
	    // db_adapter = new MyDBAdapter(this,"ML_" +"_"+ getUniqueId() +"_" +
	    // java.lang.System.currentTimeMillis() + ".db");
	    String oldDbFilenameString = getLastDbrFileInInternalMemory();

	    if(!oldDbFilenameString.equals("")){
	        db_adapter = new MyDBAdapter(this, oldDbFilenameString);
	    } else  {
	        long my_time = java.lang.System.currentTimeMillis();

	        // If GMT 0 is desired uncomment the following
	        // my_time = my_time/1000 + timeOffset/1000;
	        my_time = (long) (my_time /= 1000);
	        db_adapter = new MyDBAdapter(this, my_time + "_" + IMEI + ".dbr");
	        // Log.w("DBNAME", "DB NAME" + db_adapter.getPathOfDatabase());
	    } 

	}

	private String getLastDbrFileInInternalMemory() {
	    String filename ="";
	    String d = "/data/data/org.bewellapp/databases/";
        String e = ".dbr";
        ExtensionFilter filter = new ExtensionFilter(e);
        File dir = new File(d);

        String[] list = dir.list(filter);
        if (list == null || list.length == 0)
            return "";

        for (int i = 0; i < list.length; i++) {
            filename = d + list[i];
        }
        
	    return filename;
	}
	
	private void deleteBrokenDbrFileInInternalMemory() {
		// TODO Auto-generated method stub
		String d = "/data/data/org.bewellapp/databases/";
		String e = ".dbr";
		ExtensionFilter filter = new ExtensionFilter(e);
		File dir = new File(d);

		String[] list = dir.list(filter);
		File file;
		if (list == null || list.length == 0)
			return;

		for (int i = 0; i < list.length; i++) {
			file = new File(d + list[i]);
			file.delete();
			// System.out.print(file);
			// System.out.println( "  deleted " + isdeleted);
		}

		Log.w("DELETEDD", "FILE Deleted " + list.length);

	}

	class ExtensionFilter implements FilenameFilter {

		private String extension;

		public ExtensionFilter(String extension) {
			this.extension = extension;
		}

		public boolean accept(File dir, String name) {
			return (name.endsWith(extension));
		}
	}

	public ServiceController getServiceController() {
		return this.serviceController;
		// serviceController = new ServiceController(context);
	}

	public void set_summary_activity_Context(Context context) {
		summary_activity_Context = (WellnessSummaryActivity) context;
	}

	public WellnessSummaryActivity get_summary_activity_Context() {
		return summary_activity_Context;
	}

	public void set_main_activity_Context(Context context) {
		main_activity_Context = (main_activity) context;
	}

	public main_activity get_main_activity_Context() {
		return main_activity_Context;// = (main_activity)context;
	}

	// ml toolkit
	public void initializeMLtoolkitConfiguration() {
		Properties prop = new Properties();
		String fileName = "mltoolkit_states_idiap.config";

		// String fileName = "mltoolkit_states.config";
		InputStream is;
		try {
			// is = new FileInputStream(fileName );
			is = openFileInput(fileName);
			prop.load(is);
			this.savedAudioSensorOn = Boolean.parseBoolean(prop.getProperty("Audio"));
			this.audioForceLocked = !this.savedAudioSensorOn;// because this
			// like the
			// setting in
			// the sensor
			// screen

			this.savedAccelSensorOn = Boolean.parseBoolean(prop.getProperty("Accel"));
			//this.savedLocationSensorOn = Boolean.parseBoolean(prop.getProperty("Location"));
			//Disabled for BeWell
			//this.savedWifiSensorOn = Boolean.parseBoolean(prop.getProperty("Wifi"));
			//this.savedBluetoothSensorOn = Boolean.parseBoolean(prop.getProperty("Bluetooth"));


			this.savedEnableAccelDutyCycling = Boolean.parseBoolean(prop.getProperty("AccelDutyCycling"));
			this.savedEnableAudioDutyCycling = Boolean.parseBoolean(prop.getProperty("AudioDutyCycling"));

			//this.savedEnableAccelDutyCycling = true; // enable duty cycling
			
			// start dutycyles
			this.enableAccelDutyCycling = this.savedEnableAccelDutyCycling;
			this.enableAudioDutyCycling = this.savedEnableAudioDutyCycling;

			rawAudioOn = this.savedRawSensorOn = Boolean.parseBoolean(prop.getProperty("Raw_Audio"));
			rawAccelOn = this.savedRawAccelOn = Boolean.parseBoolean(prop.getProperty("Raw_Accel"));

		} catch (Exception e) {
			// TODO Auto-generated catch block
			// e.printStackTrace();

			// means file doesn't exist
			// start writing the files

			boolean alreadyStopped = getSharedPreferences(Ml_Toolkit_Application.SHARED_PREF, MODE_PRIVATE).getBoolean(Ml_Toolkit_Application.SP_ISSTOPPED, false);

			if (!alreadyStopped)
			{
				this.savedAudioSensorOn = true;
				this.savedAccelSensorOn = true;
				this.savedLocationSensorOn = false;
			}
			else
			{
				this.savedAudioSensorOn = false;
				this.savedAccelSensorOn = false;
				this.savedLocationSensorOn = false;
			}
			
			this.savedRawSensorOn = false; // no default raw audio recording

			//Disabled for BeWell 
			//this.savedWifiSensorOn = false;
			//this.savedBluetoothSensorOn = false;


			//this.savedEnableAccelDutyCycling = false; // no duty cycling
			//this.savedEnableAudioDutyCycling = false; // no duty cycling
			this.savedEnableAccelDutyCycling = true; // enable duty cycling
			this.savedEnableAudioDutyCycling = true; // enable duty cycling
			this.savedRawAccelOn = false;
		}
		writeToPropertiesFile(this.savedAudioSensorOn, this.savedAccelSensorOn, this.savedLocationSensorOn,
				this.savedRawSensorOn, this.savedWifiSensorOn, this.savedBluetoothSensorOn,
				this.savedEnableAccelDutyCycling, this.savedEnableAudioDutyCycling, this.savedRawAccelOn);
	}

	public void writeToPropertiesFile(boolean savedAudioSensorOn2, boolean savedAccelSensorOn2,
			boolean savedLocationSensorOn2, boolean savedRawSensorOn2, boolean savedWifiSensorOn2,
			boolean savedBluetoothSensorOn2, boolean savedEnableAccelDutyCycling2, boolean savedEnableAudioDutyCycling2, boolean savedRawAccelOn) {
		// TODO Auto-generated method stub

		Properties prop = new Properties();
		String fileName = "mltoolkit_states_idiap.config";
		// String fileName = "mltoolkit_states.config";
		OutputStream outs;
		try {

			this.savedAudioSensorOn = savedAudioSensorOn2;
			this.savedAccelSensorOn = savedAccelSensorOn2;
			this.savedLocationSensorOn = savedLocationSensorOn2;
			this.savedRawSensorOn = savedRawSensorOn2;
			this.savedWifiSensorOn = savedWifiSensorOn2;
			this.savedBluetoothSensorOn = savedBluetoothSensorOn2;
			this.savedEnableAccelDutyCycling = savedEnableAccelDutyCycling2; // no
			// duty
			// cycling
			this.savedEnableAudioDutyCycling = savedEnableAudioDutyCycling2; // no
			// duty
			// cycling

			this.audioSensorOn = savedAudioSensorOn2;
			this.accelSensorOn = savedAccelSensorOn2;
			this.locationSensorOn = savedLocationSensorOn2;
			this.rawAudioOn = savedRawSensorOn2;

			//Disabled for BeWell
			//this.wifiSensorOn = savedWifiSensorOn2;
			//this.bluetoothSensorOn = savedBluetoothSensorOn2;

			this.enableAccelDutyCycling = savedEnableAccelDutyCycling2;
			this.enableAudioDutyCycling = savedEnableAudioDutyCycling2;

			outs = openFileOutput(fileName, MODE_PRIVATE);
			// prop.load(is);
			prop.setProperty("Audio", "" + savedAudioSensorOn2);
			prop.setProperty("Accel", "" + savedAccelSensorOn2);
			prop.setProperty("Location", "" + savedLocationSensorOn2);
			prop.setProperty("Raw_Audio", "" + savedRawSensorOn2);

			//Disabled for Bewell
			//prop.setProperty("Wifi", "" + savedWifiSensorOn2);
			//prop.setProperty("Bluetooth", "" + savedBluetoothSensorOn2);

			prop.setProperty("AccelDutyCycling", "" + savedEnableAccelDutyCycling2);
			prop.setProperty("AudioDutyCycling", "" + savedEnableAudioDutyCycling2);
			prop.setProperty("Raw_Accel", "" + savedRawAccelOn);

			prop.store(outs, "ML Toolkit COnfig file");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			Log.w("FAILED", "FILE WRITE FAILED " + e.toString());
		}
	}


	public void timeZoneOffset() {
		TimeZone ids = TimeZone.getDefault();
		Calendar calendar = new GregorianCalendar(ids);
		// Uncomment the following if GMT 0 is desired instead of local time
		// timeOffset = -calendar.get(Calendar.ZONE_OFFSET) +
		// calendar.get(Calendar.DST_OFFSET);
		timeOffset = 0;
	}

	public void startQuitSequence() {

	}

	class MlToolkitObjectFactory extends BasePoolableObjectFactory {
		public Object makeObject() {
			return new ML_toolkit_object();
		}
	}

	public class MlToolkitObjPool extends StackObjectPool {
		public MlToolkitObjPool() {
			super(new MlToolkitObjectFactory());
			try {
				// Ensure that there are at least 200 ML_toolkit objects ready
				// to use
				PoolUtils.prefill(this, 200);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		@Override
		public ML_toolkit_object borrowObject() {
			try {
				return (ML_toolkit_object) super.borrowObject();
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
	}
}
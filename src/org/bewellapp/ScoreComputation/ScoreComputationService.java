package org.bewellapp.ScoreComputation;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.Calendar;
import java.util.Iterator;

import org.bewellapp.Ml_Toolkit_Application;
import org.bewellapp.ServiceControllers.LocationLib.LocationService;
import org.bewellapp.ServiceControllers.UploadingLib.UploadingServiceIntelligent;
import org.bewellapp.Storage.ML_toolkit_object;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;
import edu.dartmouthcs.UtilLibs.MyDataTypeConverter;

public class ScoreComputationService extends Service {

	public static final String ACTION_BEWELL_LWP_UPDATE_SCORE = "org.bewellapp.WellbeingScores";
	public static final String BEWELL_SCORE_PHYSICAL = "org.bewellapp.WellbeingScores.physical";
	public static final String BEWELL_SCORE_SOCIAL = "org.bewellapp.WellbeingScores.social";
	public static final String BEWELL_SCORE_SLEEP = "org.bewellapp.WellbeingScores.sleep";
	public static final String BEWELL_SCORE_PHYSICAL_OLD = "org.bewellapp.WellbeingScores.physical_old";
	public static final String BEWELL_SCORE_SOCIAL_OLD = "org.bewellapp.WellbeingScores.social_old";
	public static final String BEWELL_SCORE_SLEEP_OLD = "org.bewellapp.WellbeingScores.sleep_old";
	public static final String BEWELL_SCORE_FILENAME = "wellness_score.txt";

	private Ml_Toolkit_Application appState;
	private Thread t;
	static final String TAG = "SCORE_COMP";
	private double social_fraction;
	private double physical_fraction;
	double day_frac;
	private double duty_estimate = 1;

	private double social_score = 0;
	private double physical_score = 0;		
	private double sleep_score = 0;	//Only compute sleep scores every 24 hours
	private double prev_physical_score = 0;
	private double prev_social_score = 0;
	private double prev_sleep_score = 0;	//Only compute sleep scores every 24 hours

	private final double SOC_MIN = 0.00, SOC_DIM_MIN = 0, SOC_DIM_MAX = 100;
	private double SOC_MAX =  0.19;

	private double app_weight = 0.4, call_weight = 0.6;
	//mets score
	private final double METS_WALKING =  2.5;// / 60.0;
	private final double METS_RUNNING =  9.0;// / 60.0;

	private final double PHY_MIN = 0, PHY_DIM_MIN = 0, PHY_DIM_MAX = 100;
	private double PHY_MAX = ((7.0 * 90.0) * 7) / 7.0;


	private final double SLEEP_MIN = 2.0 * 60.0 * 60.0 * 1000;        //2 hours
	private final double SLEEP_MAX = 8.0 * 60.0 * 60.0 * 1000;        //8 hours
	private final double SLEEP_DIM_MIN = 0; 
	private final double SLEEP_DIM_MAX = 100;

	private SharedPreferences sp;
	private SharedPreferences.Editor spe;

	public void onCreate() {
		super.onCreate();
		appState = (Ml_Toolkit_Application)getApplicationContext();
		//Toast.makeText(ScoreComputationService.this, "Score Computation service started",  Toast.LENGTH_SHORT).show();

		t = new Thread() {
			public void run(){
				//wait for 5 seconds and stop
				try {
					//the score data will be initiated from Daemon service


					//update sliding window variables
					update_statistics();


					//compute the physical score
					compute_physical_score();


					//compute the social score
					compute_social_score();

					//compute the sleep score
					compute_sleep_score();

					//bewell database adapter
					appState.beWellScoreAdapter = new ScoreComputationDBAdapter(ScoreComputationService.this,  "bewellScore_1.dbr_ms");	
					appState.beWellScoreAdapter.open();
					//put data in database
					long curentTime = System.currentTimeMillis();
					appState.beWellScoreAdapter.insertEntryBeWellScore(
							curentTime, "Social", appState.prev_social_score, 
							appState.current_social_score, appState.amount_of_different_all_voice_activities, 
							MyDataTypeConverter.toByta(appState.amount_of_different_voice_activity));
					appState.beWellScoreAdapter.insertEntryBeWellScore(
							curentTime, "Physical", appState.prev_physical_score, 
							appState.current_physical_score, appState.amount_of_different_all_physical_activities, 
							MyDataTypeConverter.toByta(appState.amount_of_different_physical_activity));
					appState.beWellScoreAdapter.insertEntryBeWellScore(
							curentTime, "Sleep", appState.prev_sleep_score, 
							appState.current_sleep_score, appState.amount_of_sleeping_duration, 
							null);

					appState.beWellScoreAdapter.close();

					//send intent
					sendBewellIntent();
					appState.getServiceController().startUploadingIntellgent();


					Thread.sleep(100);															
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					//e.printStackTrace();
				}finally{
					//ScoreComputationAlarm.scheduleRestartOfService(appState);
					ScoreComputationAlarm.scheduleRestartOfService(appState, appState.frequency);
				}

				//Toast.makeText(BootAutoStartUpService.this, "StartUp service exiting \n",  Toast.LENGTH_SHORT).show();
				stopSelf();
				Log.i(TAG, " self destruct");
			}
		};
		//t.setPriority(Thread.NORM_PRIORITY+1);
		t.start();

	}


	protected void sendBewellIntent()
	{
		//temporary code. Sends an intent with well-being score
		Intent i = new Intent();
		i.setAction(ACTION_BEWELL_LWP_UPDATE_SCORE);

		//sleep_score = 60;
		//prev_sleep_score = 50;

		int iPhysicalScore = (int)Math.ceil(physical_score);
		int iSocialScore = (int)Math.ceil(social_score);
		int iSleepScore = (int)Math.ceil(sleep_score);
		int iPhysicalScoreOld = (int)Math.ceil(prev_physical_score);
		int iSocialScoreOld = (int)Math.ceil(prev_social_score);
		int iSleepScoreOld = (int)Math.ceil(prev_sleep_score);

		Log.i(TAG, String.format("Scores: phy %d(%d) soc %d(%d) sleep %d(%d)", iPhysicalScore, iPhysicalScoreOld, iSocialScore, iSocialScoreOld, iSleepScore, iSleepScoreOld));

		i.putExtra(BEWELL_SCORE_PHYSICAL, iPhysicalScore);
		i.putExtra(BEWELL_SCORE_SOCIAL, iSocialScore);
		i.putExtra(BEWELL_SCORE_SLEEP, iSleepScore);
		i.putExtra(BEWELL_SCORE_PHYSICAL_OLD, iPhysicalScoreOld);
		i.putExtra(BEWELL_SCORE_SOCIAL_OLD, iSocialScoreOld);
		i.putExtra(BEWELL_SCORE_SLEEP_OLD, iSleepScoreOld);
		//		i.putExtra(BEWELL_SCORE_PHYSICAL, (int)Math.ceil(56));
		//		i.putExtra(BEWELL_SCORE_SOCIAL, (int)Math.ceil(78));
		//		i.putExtra(BEWELL_SCORE_SLEEP, (int)Math.ceil(44));
		//		i.putExtra(BEWELL_SCORE_PHYSICAL_OLD, (int)Math.ceil(58));
		//		i.putExtra(BEWELL_SCORE_SOCIAL_OLD, (int)Math.ceil(46));
		//		i.putExtra(BEWELL_SCORE_SLEEP_OLD, (int)Math.ceil(60));

		writeScoresToFile(i.getExtras());
		insertScores(iPhysicalScore, iSocialScore, iSleepScore);
		writeToUploadFile(iPhysicalScore, iSocialScore, iSleepScore);

		if (i != null)
		{
			//appState.daemonService.sendBroadcast(i);
			appState.sendBroadcast(i);
			Log.i(TAG, " Intent sent");
		}
	}

	protected void writeScoresToFile(Bundle b) {
		FileLock lock = null;
		try {
			Log.i(TAG, "Writing scores to file");
			// Get a file channel for the file
			File file = new File(Environment.getExternalStorageDirectory(), BEWELL_SCORE_FILENAME);
			if (file.exists()) {
				file.delete();
			}
			FileChannel channel = new RandomAccessFile(file, "rw").getChannel();

			// Use the file channel to create a lock on the file.
			// This method blocks until it can retrieve the lock.
			lock = channel.lock();
			ByteBuffer bb = ByteBuffer.allocate(1024);
			bb.putInt(b.getInt(BEWELL_SCORE_PHYSICAL));
			bb.putInt(b.getInt(BEWELL_SCORE_SOCIAL));
			bb.putInt(b.getInt(BEWELL_SCORE_SLEEP));
			bb.putInt(b.getInt(BEWELL_SCORE_PHYSICAL_OLD));
			bb.putInt(b.getInt(BEWELL_SCORE_SOCIAL_OLD));
			bb.putInt(b.getInt(BEWELL_SCORE_SLEEP_OLD));
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

	protected void insertScores(int iPhysicalScore, int iSocialScore, int iSleepScore)
	{
		double [] scores = new double[3];
		scores[0] = iPhysicalScore;
		scores[1] = iSocialScore;
		scores[2] = iSleepScore;
		//Log.d(TAG, " Scores: " + Double.toString(scores[0]) + " " + Double.toString(scores[1]) + "  "+ Double.toString(scores[2]));
		long currentMillis = System.currentTimeMillis();
		ML_toolkit_object scoreObj = appState.mMlToolkitObjectPool.borrowObject();
		scoreObj.setValues(currentMillis, 21, MyDataTypeConverter.toByta(scores));
		appState.ML_toolkit_buffer.insert(scoreObj);

	}

	protected void writeToUploadFile(int iPhysicalScore, int iSocialScore, int iSleepScore)
	{
		long currentMillis = System.currentTimeMillis();
		UploadingServiceIntelligent.scoreFileLock.lock();
		try {
			FileOutputStream fos = openFileOutput(UploadingServiceIntelligent.BEWELL_UPLOAD_FILENAME, MODE_PRIVATE | MODE_APPEND);
			DataOutputStream dos = new DataOutputStream(fos);
			dos.writeInt(iPhysicalScore);
			dos.writeInt(iSocialScore);
			dos.writeInt(iSleepScore);
			dos.writeDouble(LocationService.sLATITUDE);
			dos.writeDouble(LocationService.sLONGITUDE);
			dos.writeLong(currentMillis);
			dos.flush();
			dos.close();
		} catch (IOException e)
		{
			Log.e(TAG, e.getMessage());
			e.printStackTrace();
		}
		finally {
			UploadingServiceIntelligent.scoreFileLock.unlock();
		}
	}


	public static Bundle getScores() {
		Bundle result = new Bundle();
		FileLock lock = null;
		try {
			Log.i(TAG, "Reading scores from file");
			// Get a file channel for the file
			File file = new File(Environment.getExternalStorageDirectory(), BEWELL_SCORE_FILENAME);
			if (!file.exists() || !file.canRead()) {
				throw new Exception(String.format("File %s does not exist", file.getAbsolutePath()));
			}
			FileChannel channel = new RandomAccessFile(file, "rw").getChannel();

			// Use the file channel to create a lock on the file.
			// This method blocks until it can retrieve the lock.
			lock = channel.lock();
			ByteBuffer bb = ByteBuffer.allocate(1024);
			channel.read(bb);
			bb.flip();
			result.putInt(BEWELL_SCORE_PHYSICAL, bb.getInt());
			result.putInt(BEWELL_SCORE_SOCIAL, bb.getInt());
			result.putInt(BEWELL_SCORE_SLEEP, bb.getInt());
			result.putInt(BEWELL_SCORE_PHYSICAL_OLD, bb.getInt());
			result.putInt(BEWELL_SCORE_SOCIAL_OLD, bb.getInt());
			result.putInt(BEWELL_SCORE_SLEEP_OLD, bb.getInt());

			// Release the lock
			lock.release();

			// Close the file
			channel.close();
		} catch (Exception e) {
			result.putInt(BEWELL_SCORE_PHYSICAL, 0);
			result.putInt(BEWELL_SCORE_SOCIAL, 0);
			result.putInt(BEWELL_SCORE_SLEEP, 0);
			result.putInt(BEWELL_SCORE_PHYSICAL_OLD, 0);
			result.putInt(BEWELL_SCORE_SOCIAL_OLD, 0);
			result.putInt(BEWELL_SCORE_SLEEP_OLD, 0);
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


	public void compute_social_score()
	{
		if(appState == null)
			appState = (Ml_Toolkit_Application)getApplicationContext();

		//
		//old version
		//
		/*
		appState.prev_social_score = prev_social_score = appState.current_social_score;
		social_fraction = (double)appState.amount_of_different_voice_activity[2]/(double)appState.amount_of_different_all_voice_activities;
		appState.current_social_score = social_score = SOC_DIM_MIN + (social_fraction - SOC_MIN)*(SOC_DIM_MAX - SOC_DIM_MIN)/(SOC_MAX-SOC_MIN);
		 */

		//
		//new version
		//

		appState.prev_social_score = prev_social_score = appState.current_social_score;

		//sum up all values in sliding window ArrayDeque
		long amount_of_voice, amount_of_all;
		amount_of_voice = amount_of_all = 0;
		for (Iterator<long []> it = appState.sliding_of_different_voice_activity.iterator(); it.hasNext();)
		{
			long [] different_voice_activity = it.next();
			amount_of_voice += different_voice_activity[2];		//2 is voice
		}
		for(Iterator<Long> it = appState.sliding_of_different_all_voice_activities.iterator();it.hasNext();)
		{
			Long all_voice_activities = it.next();
			amount_of_all += all_voice_activities.longValue();
		}

		if (amount_of_all == 0)
			amount_of_all = 1;
		social_fraction = (double)amount_of_voice/(double)amount_of_all; //* 0.9;	//times 0.9 because of adaptive duty-cycling
		appState.current_social_score = social_score = SOC_DIM_MIN + (social_fraction - SOC_MIN)*(SOC_DIM_MAX - SOC_DIM_MIN)/(SOC_MAX-SOC_MIN);

		//from apps and calls
		social_score += otherSocial();
		//smooth 
		appState.current_social_score = social_score = smoothScore(social_score, prev_social_score);

		//normalize
		if (social_score < 0)
			appState.current_social_score = social_score = 0;
		if (social_score > 100)
			appState.current_social_score = social_score = 100;



		Log.d(TAG, "Voice is " + Long.toString(amount_of_voice) + ". All is " + Long.toString(amount_of_all));
		Log.d(TAG, "Social score is " + Double.toString(social_score));

	}

	double otherSocial()
	//social scoring from apps and calls
	{
		double result = 0;

		sp = getSharedPreferences(Ml_Toolkit_Application.SHARED_PREF, MODE_PRIVATE);
		int numsec = sp.getInt(Ml_Toolkit_Application.SP_SOCIAL_APPS_SECONDS, 0);
		if (numsec > 3600)
			numsec = 3600;
		if (numsec > 0 && numsec < 600)
			numsec = 600;	//if there is usage, then at least 1 point
		spe = getSharedPreferences(Ml_Toolkit_Application.SHARED_PREF, MODE_PRIVATE).edit();
		spe.putInt(Ml_Toolkit_Application.SP_SOCIAL_APPS_SECONDS, 0);
		spe.commit();

		result += app_weight * (double)numsec / (60 * 10);	// 10-minute equals to 1 point
		Log.d(TAG, "result is " + Double.toString(result));

		sp = getSharedPreferences(Ml_Toolkit_Application.SHARED_PREF, MODE_PRIVATE);
		numsec = sp.getInt(Ml_Toolkit_Application.SP_SOCIAL_PHONE_CALLS_SECONDS, 0);

		if (numsec > 3600)
			numsec = 3600;
		if (numsec > 0 && numsec < 600)
			numsec = 600;	//if there is usage, then at least 1 point
		spe = getSharedPreferences(Ml_Toolkit_Application.SHARED_PREF, MODE_PRIVATE).edit();
		spe.putInt(Ml_Toolkit_Application.SP_SOCIAL_PHONE_CALLS_SECONDS, 0);
		spe.commit();

		result += call_weight * (double)numsec / (60 * 10);	// 10-minute equals to 1 point
		Log.d(TAG, "result is " + Double.toString(result));

		return result;
	}

	public void compute_physical_score()
	{

		//
		//old version
		//
		/*
		//physical_fraction = (double)appState.amount_of_different_voice_activity[2]/(double)appState.amount_of_different_all_voice_activities;
		appState.prev_physical_score = prev_physical_score = appState.current_physical_score;

		physical_fraction = 0; //this is MET
		//unit: MET / sec
		//int multiplier = 1;
		physical_fraction += METS_WALKING * appState.amount_of_different_physical_activity[3]/(1000*60); //*multiplier;
		physical_fraction += METS_RUNNING * appState.amount_of_different_physical_activity[4]/(1000*60); //*multiplier;

		//PHY_MAX = 6.0 * appState.amount_of_different_all_physical_activities;
		appState.current_physical_score = physical_score = PHY_DIM_MIN + (physical_fraction - PHY_MIN)*(PHY_DIM_MAX - PHY_DIM_MIN)/(PHY_MAX - PHY_MIN);
		 */

		//
		//new version
		//

		appState.prev_physical_score = prev_physical_score = appState.current_physical_score;
		physical_fraction = 0; //this is MET

		//sum up all values in sliding window ArrayDeque
		long amount_of_walking, amount_of_running;
		amount_of_walking = amount_of_running = 0;
		int size = appState.sliding_of_different_physical_activity.size();
		int cnt = 0;
		for (Iterator<long []> it = appState.sliding_of_different_physical_activity.iterator(); it.hasNext();)
		{
			long [] different_physical_activity = it.next();

			if (cnt < size - 2)
			{
				amount_of_walking += Math.round (0.8 * different_physical_activity[3]);
				if ((double)different_physical_activity[4]/(1000*60) >= 3)	
					amount_of_running += Math.round (0.8 * different_physical_activity[4]);
			}
			else
			{
				amount_of_walking += different_physical_activity[3];
				if ((double)different_physical_activity[4]/(1000*60) >= 3)
					amount_of_running += different_physical_activity[4];
			}

			cnt++;
		}
		physical_fraction += METS_WALKING * (double)amount_of_walking/(1000*60) * duty_estimate;	//walking is long duration 
		physical_fraction += METS_RUNNING * (double)amount_of_running/(1000*60); 

		appState.current_physical_score = physical_score = PHY_DIM_MIN + (physical_fraction - PHY_MIN)*(PHY_DIM_MAX - PHY_DIM_MIN)/(PHY_MAX - PHY_MIN);
		//normalize
		if (physical_score < 0)
			appState.current_physical_score = physical_score = 0;
		if (physical_score >= 100)
			appState.current_physical_score = physical_score = 95;
		//smooth 
		appState.current_physical_score = physical_score = smoothScore(physical_score, prev_physical_score);


		Log.d(TAG, "Running is " + Long.toString(amount_of_running) + ". Walking is " + Long.toString(amount_of_walking));
		Log.d(TAG, "Physical score is " + Double.toString(physical_score));

	}

	protected void compute_sleep_score()
	{
		Calendar cal = Calendar.getInstance();
		int hour = cal.get(Calendar.HOUR_OF_DAY);

		//only update prev_sleep_score once a day
		if (hour == 9)
			appState.prev_sleep_score = prev_sleep_score = appState.current_sleep_score;
		else
			prev_sleep_score = appState.prev_sleep_score;

		//check sleep duration
		if (appState.amount_of_sleeping_duration == 0)
		{
			try
			{
				appState.beWellScoreAdapter = new ScoreComputationDBAdapter(this,  "bewellScore_3.dbr_ms");	
				appState.beWellScoreAdapter.open();
				ScoreObject scoreObject = ScoreComputationDBAdapter.getBeWellScores(appState.beWellScoreAdapter);
				appState.amount_of_sleeping_duration = scoreObject.sleepTimeInMillisecond;
				appState.beWellScoreAdapter.close();
			}catch (Exception e){
				Log.e(TAG, "database exception: " + e.toString());
			}
		}


		//if larger than SLEEP_MAX, convert to range [0, SLEEP_MAX]
		if (appState.amount_of_sleeping_duration > SLEEP_MAX)
			appState.amount_of_sleeping_duration = (long) (SLEEP_MAX - (appState.amount_of_sleeping_duration - SLEEP_MAX));

		appState.current_sleep_score  = sleep_score = 
			SLEEP_DIM_MIN + ((SLEEP_DIM_MAX - SLEEP_DIM_MIN)/(SLEEP_MAX-SLEEP_MIN))*(appState.amount_of_sleeping_duration - SLEEP_MIN);	
		Log.i(TAG, "Sleep duration is " + Long.toString(appState.amount_of_sleeping_duration));
		//normalize
		if (sleep_score < 0)
			appState.current_sleep_score = sleep_score = 0;
		if (sleep_score > 100)
			appState.current_sleep_score = sleep_score = 100;


		Log.d(TAG, "Sleep score is " + Double.toString(sleep_score) + " Precious Sleep score is " + Double.toString(prev_sleep_score));
	}


	//smooth for the timing window
	private double smoothScore(double score, double prev_score)
	{
		double diff = score - prev_score;
		if (Math.abs(diff) > 12)
		{
			//too much
			//score = prev_score + diff * 0.67;	// smooth by 2/3
			score = prev_score + Math.signum(diff) * 12;	// round to 14
		}

		return score;
	}


	public void update_statistics()
	{
		//
		//update physical statistics
		//
		if (appState.sliding_of_different_physical_activity.size() == 24 / appState.frequency)
		{
			//remove the first element
			appState.sliding_of_different_physical_activity.removeFirst();
		}

		long [] temp = new long[Ml_Toolkit_Application.NO_PHYSICAL_ACTIVITY];
		for (int i = 0; i < Ml_Toolkit_Application.NO_PHYSICAL_ACTIVITY; ++i)
			temp[i] = appState.amount_of_different_physical_activity[i] - appState.last_of_different_physical_activity[i];
		//add to the back of ArrayDeque
		appState.sliding_of_different_physical_activity.addLast(temp);
		//update last_of variables 
		System.arraycopy(appState.amount_of_different_physical_activity, 0, appState.last_of_different_physical_activity, 0, appState.amount_of_different_physical_activity.length);
		if (appState.sliding_of_different_physical_activity.size() == 24 / appState.frequency)
		{
			//change PHY_MAX
			PHY_MAX = ((7.0 * 90.0) * 7) / 7.0;
			duty_estimate = 1.05;
		}
		else
		{
			double minutes = (double)appState.sliding_of_different_physical_activity.size() * (double)appState.frequency / 24 * 90;
			Log.d(TAG, "minute is: " + Double.toString(minutes));

			if (minutes < 45)
				minutes = 45;
			else
			{
				if (minutes < 67.5)
					minutes = 67.5;
				else
					minutes = 90;
			}

			//PHY_MAX = ((4.0 * 60.0) * 7) / 7.0;
			PHY_MAX = ((7.0 * minutes) * 7) / 7.0;
		}


		//
		//update social statistics
		//
		if (appState.sliding_of_different_voice_activity.size() == 24 / appState.frequency)
		{
			//remove the first element
			appState.sliding_of_different_voice_activity.removeFirst();
		}

		temp = new long[Ml_Toolkit_Application.NO_VOICE_ACTIVITY];
		for (int i = 0; i < Ml_Toolkit_Application.NO_VOICE_ACTIVITY; ++i)
			temp[i] = appState.amount_of_different_voice_activity[i] - appState.last_of_different_voice_activity[i];
		//add to the back of ArrayDeque
		appState.sliding_of_different_voice_activity.addLast(temp);
		//update last_of variables
		System.arraycopy(appState.amount_of_different_voice_activity, 0, appState.last_of_different_voice_activity, 0, appState.amount_of_different_voice_activity.length);



		if (appState.sliding_of_different_all_voice_activities.size() == 24 / appState.frequency)
		{
			//remove the first element 
			appState.sliding_of_different_all_voice_activities.removeFirst();
		}
		long diff = appState.amount_of_different_all_voice_activities - appState.last_of_different_all_voice_activities;
		//add to the back of ArrayDeque
		appState.sliding_of_different_all_voice_activities.addLast(new Long(diff));
		//update last_of variables
		appState.last_of_different_all_voice_activities = appState.amount_of_different_all_voice_activities;		

		//change SOC_MAX
		if (appState.sliding_of_different_physical_activity.size() <= 5)
		{
			SOC_MAX = 0.23;

		}
		else
		{
			SOC_MAX = 0.19;
		}


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
		//Toast.makeText(ScoreComputationService.this, "Score Computation service exiting",  Toast.LENGTH_SHORT).show();
		Log.i(TAG, "self destruct done");
	}
}






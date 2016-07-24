package org.bewellapp.BeWellSuggestion;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.bewellapp.Ml_Toolkit_Application;
import org.bewellapp.ScoreComputation.ScoreComputationDBAdapter;
import org.bewellapp.ScoreComputation.ScoreComputationService;
import org.bewellapp.ScoreComputation.ScoreObject;
import org.json.JSONObject;

import edu.dartmouthcs.UtilLibs.MyDataTypeConverter;
import org.bewellapp.R;
import android.app.Service;
import android.content.Intent;
import android.database.SQLException;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;

/**
 * BeWell Suggestion service. Periodically get suggestion from server and
 * broadcast it.
 * 
 * @author MuLin
 * 
 */

public class BeWellSuggestionService extends Service {

	private static final String TAG = "BeWellSuggestionService";
	public static final String BEWELL_SUGGESTION_FILENAME = "bewell_suggestion.txt";

	// private static final String cmd_get_gestion = "/suggest/get"; //old
	// webservice url
	private static final String cmd_get_gestion = "/suggest/get/dynamic";
	private String mUserAgent;
	private static final int HTTP_STATUS_OK = 200;
	public static final String ACTION_BEWELL_LWP_UPDATE_SUGGESTION = "org.bewellapp.WellbeingSuggestions";
	public static final String BEWELL_SUGGESTION = "org.bewellapp.WellbeingSuggestions.content";
	private double social_score = 0;
	private double physical_score = 0;
	private double sleep_score = 0;
	public static String sleepDurationForDebug = "";
	private static String lastSuggestion = "";

	private Ml_Toolkit_Application appState;
	private Thread t;
	private BeWellSuggestionDBAdapter mSuggestionAdapter;
	private static final int mPhysicalCounter = 44, mSocialCounter = 46, mSleepCounter = 38;

	public void onCreate() {
		super.onCreate();
		appState = (Ml_Toolkit_Application) getApplicationContext();
		mUserAgent = String.format("%s/%s (Linux; Android)", "1.0", "mm");


		t = new Thread() {
			public void run() {
				// wait for 10 minutes and stop
				try {
					
					// get suggestion from server
					//String suggestion = getSuggestionFromServer();
					String suggestion = getSuggestionFromDB();

					// write to file
					if (suggestion == null)
						suggestion = lastSuggestion;
					else
						lastSuggestion = suggestion;
					if (sleepDurationForDebug.equalsIgnoreCase(""))
					{
						getDuration();
					}
					//suggestion += "\n" + sleepDurationForDebug;
					writeSuggestionToFile(suggestion);
					//writeSuggestionToFile(sleepDurationForDebug);



					Thread.sleep(100);
				} catch (InterruptedException e) {
					Log.d("TAG", "Problem communicating with API\n" + e.toString());
				} finally {
					BeWellSuggestionAlarm.scheduleRestartOfService(appState);
				}
				stopSelf();
			}
		};
		// t.setPriority(Thread.NORM_PRIORITY+1);
		t.start();

	}

	private void getDuration()
	{
		//get sleep duration from appState or database
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
		double sleepDuration = (double)appState.amount_of_sleeping_duration / (1000.0 * 60 * 60);

		sleepDurationForDebug = String.format("Sleep Duration: %.1f hours", sleepDuration);
	}

	protected String getSuggestionFromServer() {
		try {

			// Create client and set our specific user-agent string
			HttpClient client = new DefaultHttpClient();
			HttpPost request = new HttpPost("http://" + appState.WebserviceSuggestionURL + cmd_get_gestion);
			request.setHeader("User-Agent", mUserAgent);
			JSONObject input = new JSONObject();
			input.put("phone_id", appState.IMEI);
			Bundle b = ScoreComputationService.getScores();
			if (b != null) {
				physical_score = b.getInt(ScoreComputationService.BEWELL_SCORE_PHYSICAL, 0);
				social_score = b.getInt(ScoreComputationService.BEWELL_SCORE_SOCIAL, 0);
				sleep_score = b.getInt(ScoreComputationService.BEWELL_SCORE_SLEEP, 0);
			}
			input.put("activity", physical_score);
			input.put("sleep", sleep_score);
			input.put("social", social_score);

			request.setEntity(new StringEntity(input.toString()));

			HttpResponse response1 = client.execute(request);
			// Check if server response is valid
			StatusLine status = response1.getStatusLine();
			if (status.getStatusCode() != HTTP_STATUS_OK) {
				throw new Exception("Invalid response from server: " + status.toString());
			}

			// Pull content stream from response
			HttpEntity entity = response1.getEntity();
			InputStream inputStream = entity.getContent();

			BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
			String partial = br.readLine();
			StringBuffer sb = new StringBuffer();
			while (partial != null) {
				sb.append(partial);
				partial = br.readLine();
			}
			JSONObject output = new JSONObject(sb.toString());

			String result = output.getString("text");
			Log.d(TAG, "content is: " + result);

			return result;

		} catch (Exception e) {
			return null;
		}
	}

	protected String getSuggestionFromDB() {
		String result = null;
		try{
			mSuggestionAdapter = new BeWellSuggestionDBAdapter(this,  "bewellSuggestion.dbr_ms");	
			mSuggestionAdapter.open();

			Log.d(TAG, "DB Size is " + Long.toString(mSuggestionAdapter.getDbSize()));
			if (mSuggestionAdapter.getDbSize()  <= 6000)	//put data in database
			{
				//physical
				for (int i = 0; i < mPhysicalCounter; ++i)
				{
					int id = getResources().getIdentifier("physical" + Integer.toString(i), "string", getPackageName());
					String suggeston = getResources().getString(id);
					//Log.d(TAG, "Inserted suggestion is " + suggeston);
					mSuggestionAdapter.insertEntryBeWellSuggestion("Physical", suggeston);
				}
				//social
				for (int i = 0; i < mSocialCounter; ++i)
				{
					int id = getResources().getIdentifier("social" + Integer.toString(i), "string", getPackageName());
					String suggeston = getResources().getString(id);
					//Log.d(TAG, "Inserted suggestion is " + suggeston);
					mSuggestionAdapter.insertEntryBeWellSuggestion("Social", suggeston);
				}
				//sleep
				for (int i = 0; i < mSleepCounter; ++i)
				{
					int id = getResources().getIdentifier("sleep" + Integer.toString(i), "string", getPackageName());
					String suggeston = getResources().getString(id);
					//Log.d(TAG, "Inserted suggestion is " + suggeston);
					mSuggestionAdapter.insertEntryBeWellSuggestion("Sleep", suggeston);
				}
			}

			//get data	
			String type = null;
			if (appState.current_physical_score <= appState.current_social_score && appState.current_physical_score <= appState.current_sleep_score)
				type = "Physical";
			if (appState.current_social_score <= appState.current_physical_score && appState.current_social_score <= appState.current_sleep_score)
				type = "Social";
			if (appState.current_sleep_score <= appState.current_physical_score && appState.current_sleep_score <= appState.current_social_score)
				type = "Sleep";

			result = BeWellSuggestionDBAdapter.getBeWellSuggestions(mSuggestionAdapter, type);
			Log.d(TAG, "Retrived suggestion is " + result);			

		}catch (SQLException e){
			Log.e(TAG, "SQL error " + e.toString());
		}catch (Exception e){
			Log.e(TAG, "error " + e.toString());
		}finally{
			mSuggestionAdapter.close();
		}

		return result;
	}
	protected void writeSuggestionToFile(final String suggestion) {
		//if suggestion is null, don't write to file
		if (suggestion == null)
			return;

		FileLock lock = null;
		try {
			Log.i(TAG, "Writing suggestion to file");
			// Get a file channel for the file
			File file = new File(Environment.getExternalStorageDirectory(), BEWELL_SUGGESTION_FILENAME);
			if (file.exists()) {
				file.delete();
			}
			FileChannel channel = new RandomAccessFile(file, "rw").getChannel();

			// Use the file channel to create a lock on the file.
			// This method blocks until it can retrieve the lock.
			lock = channel.lock();
			byte[] stringAsByte = suggestion.getBytes("UTF-8");
			ByteBuffer bb = ByteBuffer.allocate(stringAsByte.length);
			bb.put(stringAsByte);
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

	public static String getSuggestion() {
		String result = "No suggestions at this time.";
		FileLock lock = null;
		try {
			Log.i(TAG, "Reading suggestion from file");
			// Get a file channel for the file
			File file = new File(Environment.getExternalStorageDirectory(), BEWELL_SUGGESTION_FILENAME);
			if (!file.exists() || !file.canRead()) {
				throw new Exception(String.format("File %s does not exist", file.getAbsolutePath()));
			}
			FileChannel channel = new RandomAccessFile(file, "rw").getChannel();
			lock = channel.lock();
			ByteBuffer bb = ByteBuffer.allocate((int)channel.size());
			channel.read(bb);
			bb.flip();
			CharsetDecoder cd = Charset.forName("UTF-8").newDecoder();
			CharBuffer bufferResult = cd.decode(bb);
			result = bufferResult.toString();

			// Release the lock
			lock.release();

			// Close the file
			channel.close();
		} catch (Exception e) {
			result = "No suggestions at this time.";
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

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
	}
}

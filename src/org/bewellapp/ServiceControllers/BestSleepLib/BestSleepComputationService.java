package org.bewellapp.ServiceControllers.BestSleepLib;

import org.bewellapp.Ml_Toolkit_Application;
import org.bewellapp.BeWellSuggestion.BeWellSuggestionService;
import org.bewellapp.ScoreComputation.ScoreComputationDBAdapter;
import org.bewellapp.Storage.ML_toolkit_object;

import edu.dartmouthcs.UtilLibs.MyDataTypeConverter;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class BestSleepComputationService extends Service {
	private Ml_Toolkit_Application appState;
	private static final String TAG = "BestSleep";
	private Thread t;
	private double []regression_coef = { 0.5051, 0.2258, 0, 0.0452, 0.2222, 0};
	private static int numberOfDurations;

	public void onCreate() {
		super.onCreate();
		appState = (Ml_Toolkit_Application)getApplicationContext();
		numberOfDurations = 6;

		Log.i(TAG, "BestSleepComputationService started");
		
		//start ground-truth activity
//		Intent activityIntent = new Intent(getBaseContext(), SleepTruthActivity.class);
//		activityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//		getApplication().startActivity(activityIntent); 
		
		

		t = new Thread() {
			public void run(){

				double []durations1 = null;
				double []durations2 = null;
				//bewell database adapter
				try{
					appState.beWellScoreAdapter2 = new ScoreComputationDBAdapter(BestSleepComputationService.this,  "bewellScore_2.dbr_ms");	
					appState.beWellScoreAdapter2.open();
					durations1 = ScoreComputationDBAdapter.getAllDurations(appState.beWellScoreAdapter2);
					appState.beWellScoreAdapter2.removeAllDurationEntries();	//remove all durations
					appState.beWellScoreAdapter2.close();
					//get stationary and silence
					appState.beWellScoreAdapter2 = new ScoreComputationDBAdapter(BestSleepComputationService.this,  "bewellScore_1.dbr_ms");	
					appState.beWellScoreAdapter2.open();
					durations2 = ScoreComputationDBAdapter.getAllDurations(appState.beWellScoreAdapter2);
					appState.beWellScoreAdapter2.close();
					//put stationary and silence to durations1
					durations1[4] = durations2[4] * 3;	//because of duty-cycle
					durations1[5] = durations2[5];
				}
				catch (Exception e){
					Log.e(TAG, "database exception: " + e.toString());
				}

				//check if light sensor is available
				if (durations1[0] < 2)
					durations1[0] = Math.min(6, durations1[4]);		//if not available, use stationary duration to replace it.
				//estimate sleep duration
				double sleepDuration = estimateDuration(durations1);
				Log.d(TAG, "Estimated Sleep Duration: " + Double.toString(sleepDuration));
				if (sleepDuration <= 2)
					sleepDuration = 4;
				//check if the phone is powered off. If yes, use off-duration as sleepDuration
				durations1[2] = BestSleepService.computePhoneOffDuration();
				if (durations1[2] > 4 && durations1[2] <= 10)
					sleepDuration = durations1[2];
				
				appState.amount_of_sleeping_duration = (long)(sleepDuration * 60 * 60 * 1000);	//unit:milliseconds
				//insert into databse
				long curentTime = System.currentTimeMillis();
				appState.beWellScoreAdapter2 = new ScoreComputationDBAdapter(BestSleepComputationService.this,  "bewellScore_3.dbr_ms");	
				appState.beWellScoreAdapter2.open();
				appState.beWellScoreAdapter2.insertEntryBeWellScore(
						curentTime, "Sleep", appState.prev_sleep_score, 
						appState.current_sleep_score, appState.amount_of_sleeping_duration, 
						null);
				appState.beWellScoreAdapter2.close();
				Log.i(TAG, "Database 3 inserts successfully!!");	
				//for debug
				BeWellSuggestionService.sleepDurationForDebug = String.format("Sleep Duration: %.1f hours", sleepDuration);
			}
		};
		t.start();
		

		BestSleepAlarm.scheduleRestartOfService(appState);
		stopSelf();
	}



	public double estimateDuration(double [] durations)
	{
		double sleepDuration = 0;

		for (int i = 0; i < numberOfDurations;++i)
		{
			sleepDuration += regression_coef[i] * durations[i];
		}

		return sleepDuration;
	}






	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}

}

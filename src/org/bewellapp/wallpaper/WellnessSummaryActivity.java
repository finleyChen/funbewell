package org.bewellapp.wallpaper;

import org.bewellapp.Ml_Toolkit_Application;
import org.bewellapp.dashBoardActivity;
import org.bewellapp.BeWellSuggestion.BeWellSuggestionService;
import org.bewellapp.ScoreComputation.ScoreComputationService;
import org.bewellapp.ServiceControllers.ServiceController;
import org.bewellapp.ServiceControllers.AudioLib.MyPhoneStateListener;
import org.bewellapp.ServiceControllers.UploadingLib.InfoObject;
import org.bewellapp.Storage.MySDCardStateListener;

import org.bewellapp.R;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class WellnessSummaryActivity extends Activity {

	private final float DIM_BEHIND_AMOUNT_NORMAL = 0.4f;
	private TextView myScore;
	private TextView mySleepScore;
	private TextView mySocScore;
	private TextView myPhysScore;
	private TextView scoreDiff;
	private TextView mTvSuggestion;
	private ImageView mySleepArrow;
	private ImageView mySocArrow;
	private ImageView myPhysArrow;

	//for app launch
	private static ServiceController serviceController;
	public static Context context;
	private Ml_Toolkit_Application appState;


	public boolean[] sensor_status;
	private static final int SENSOR_STATUS_REQUEST = 0;
	private boolean activity_paused;
	private boolean isRegistered = false;
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.notification_suggestion);
		
		SharedPreferences sp = getSharedPreferences(Ml_Toolkit_Application.SHARED_PREF, MODE_PRIVATE);
		isRegistered = sp.getBoolean(Ml_Toolkit_Application.SP_ISREGISTERED, false);
		
		ImageButton btTerminate = (ImageButton) findViewById(R.id.btTerminate);
		btTerminate.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View arg0) {
			    if(isRegistered){
				finish();
				startActivity(new Intent(Intent.ACTION_MAIN).addFlags(
						Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP).addCategory(
								Intent.CATEGORY_HOME));
			    } else {
				finish();
				Intent in = new Intent("org.bewellapp.SignIn");
				startActivity(in);
			    }

			}
		});

		String _promptString="Glendower:\nI can call spirits from the vasty deep.\n\n" +
			"Hotspur:\nWhy, so can I, or so can any man;\n" + 
			"But will they come when you do call for them?\n\n" +
			"Glendower:\nWhy, I can teach you, cousin, to command The devil\n\n" +
			"Hotspur:\nAnd I can teach thee, coz, to shame the devil¡ª" + 
			"By telling the truth. Tell truth and shame the devil.";
		String prompt="";
		mTvSuggestion = (TextView) findViewById(R.id.txtWords);
		//mTvSuggestion.setText("");
		
		TextView txtSignIn = (TextView) findViewById(R.id.logintxt);
		if(isRegistered) {
		    String usrNameString=sp.getString(Ml_Toolkit_Application.SP_USERNAME,
				"john@sample.com");
		    txtSignIn.setText("User signed in: " + usrNameString);
		} else {
		    txtSignIn.setText("Please sign in now -- your data is not being uploaded");		    
		}
//		myScore = (TextView) findViewById(R.id.myScore);
//		mySleepScore = (TextView) findViewById(R.id.tvSleep);
//		mySocScore = (TextView) findViewById(R.id.tvSocial);
//		myPhysScore = (TextView) findViewById(R.id.tvPhysical);
//
//		mySleepArrow = (ImageView) findViewById(R.id.ivSleepTrend);
//		mySocArrow = (ImageView) findViewById(R.id.ivSocialTrend);
//		myPhysArrow = (ImageView) findViewById(R.id.ivPhysicalTrend);
//		scoreDiff = (TextView) findViewById(R.id.scoreDiff);
//		mTvSuggestion = (TextView) findViewById(R.id.tvSuggestion);
//
//		// Typeface tf = Typeface.createFromAsset(getAssets(), "bebas__.ttf");
//		// mySleepScore.setTypeface(tf);
//		
//		appState = (Ml_Toolkit_Application)getApplicationContext();
//		appState.set_summary_activity_Context(this);
//		serviceController = appState.getServiceController();
		
		/*
		//
		//for app launch
		//
		//sensor status controlling
		sensor_status = new boolean[3];
		for(int i = 0;i < sensor_status.length;++i)
			sensor_status[i] = true;

		//launch activity will never start the sensing servcices twice
		if(appState.applicatin_starated == false){

			appState.applicatin_starated = true;


			//getting the IMEI
			TelephonyManager mTelephonyMgr = (TelephonyManager)getSystemService(TELEPHONY_SERVICE);
			appState.IMEI = mTelephonyMgr.getDeviceId();
			appState.initializeDataBase();
			appState.db_adapter.open();
			Log.w("DBNAME", "DB NAME" + this.appState.db_adapter.getPathOfDatabase());
			//debugTextView.setText("\n\n\nDebug Info" + this.appState.db_adapter.getPathOfDatabase());


			appState.initializeMLtoolkitConfiguration();

			if(appState.savedAccelSensorOn == true)
				serviceController.startAccelerometerSensor();


			//start the gps service
			if(appState.savedLocationSensorOn == true)
				serviceController.startLocationSensor();
			//this.bindLocationService();


			//start the audio service
			if(appState.savedAudioSensorOn == true)
				serviceController.startAudioSensor();

			serviceController.startDaemonService();
			serviceController.startNTPTimestampsService();
			serviceController.startMomentarySamplingService();
			serviceController.startSleepService();

			appState.infoObj = new InfoObject(this, appState);
			//appState.phoneCallInfo = new MyPhoneStateListener(this,appState);
			appState.phoneCallInfo = new MyPhoneStateListener(this,appState);
			appState.mySDCardStateInfo = new MySDCardStateListener(this, appState);

			//application started .... service started
			;
		}

		//still to start binding
		this.activity_paused = true;
		*/
		
		

	}

	@Override
	public void onResume() {
		super.onResume();
		/*
		doDim();
		//Bundle b = getIntent().getExtras();
		Bundle b = ScoreComputationService.getScores();
		if (b != null) {
			int physicalScore = b.getInt(ScoreComputationService.BEWELL_SCORE_PHYSICAL, 0);
			int physicalScoreOld = b.getInt(ScoreComputationService.BEWELL_SCORE_PHYSICAL_OLD, 0);
			int socialScore = b.getInt(ScoreComputationService.BEWELL_SCORE_SOCIAL, 0);
			int socialScoreOld = b.getInt(ScoreComputationService.BEWELL_SCORE_SOCIAL_OLD, 0);
			int sleepScore = b.getInt(ScoreComputationService.BEWELL_SCORE_SLEEP, 0);
			int sleepScoreOld = b.getInt(ScoreComputationService.BEWELL_SCORE_SLEEP_OLD, 0);

			int totalScore = (physicalScore + socialScore + sleepScore)/3;
			int totalScoreOld = (physicalScoreOld + socialScoreOld + sleepScoreOld)/3;
			int totalScoreDiff = totalScore - totalScoreOld;

			myScore.setText(Integer.toString(totalScore));
			scoreDiff.setText(Integer.toString(totalScoreDiff));
			mySleepScore.setText(Integer.toString(sleepScore));
			myPhysScore.setText(Integer.toString(physicalScore));
			mySocScore.setText(Integer.toString(socialScore));
			setArrowImg(mySleepArrow, sleepScore, sleepScoreOld);
			setArrowImg(mySocArrow, socialScore, socialScoreOld);
			setArrowImg(myPhysArrow, physicalScore, physicalScoreOld);

			// overall > prevOverall
			if (totalScoreDiff > 0) {
				scoreDiff.setTextColor(Color.rgb(204, 255, 0));
				scoreDiff.setText("+" + totalScoreDiff);
			} else if (totalScoreDiff < 0) {
				scoreDiff.setTextColor(Color.rgb(255, 42, 0));
				scoreDiff.setText(Integer.toString(totalScoreDiff));
			} else {
				scoreDiff.setTextColor(Color.rgb(255, 216, 0));
			}
		}
		String suggestion = BeWellSuggestionService.getSuggestion();
		mTvSuggestion.setText(suggestion);
		*/
	}

	private void doDim() {
		Window win = getWindow();
		WindowManager.LayoutParams winParams = win.getAttributes();

		winParams.flags |= (WindowManager.LayoutParams.FLAG_DIM_BEHIND);
		winParams.dimAmount = DIM_BEHIND_AMOUNT_NORMAL;

		win.setAttributes(winParams);
	}

	private void setArrowImg(ImageView img, int curr, int prev) {
		if (curr > prev) {
			img.setImageResource(R.drawable.up);
		} else if (prev > curr) {
			img.setImageResource(R.drawable.down);
		} else {
			img.setImageResource(R.drawable.mid);
			img.layout(3, 5, 0, 0);
		}
	}


	/*
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// TODO Auto-generated method stub
		super.onCreateOptionsMenu(menu);
		menu.add(0, Menu.FIRST + 1, 1, "Web");
		menu.add(0, Menu.FIRST + 2, 2, "Sign In");
		//menu.add(0, Menu.FIRST + 3, 3, "Sign Up");
		menu.add(0, Menu.FIRST + 3, 3, "Start Sensing");
		menu.add(0, Menu.FIRST + 4, 4, "Stop Sensing");
		return true;
	}
	*/

	/*
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// TODO Auto-generated method stub
		super.onOptionsItemSelected(item);
		switch (item.getItemId()) {

		case Menu.FIRST + 1: { 
			//open bewell web
			//String url = "http://www.bewellapp.org";
			String url = "http://deis221.deis.unibo.it:9000/";
			Intent in = new Intent(Intent.ACTION_VIEW);
			in.setData(Uri.parse(url));
			startActivity(in);			
			break;
		}

		case Menu.FIRST + 2: {
			//sign-in
			SharedPreferences sp = getSharedPreferences(Ml_Toolkit_Application.SHARED_PREF, MODE_PRIVATE);
			if (sp.getBoolean(Ml_Toolkit_Application.SP_ISREGISTERED, false)) {
				//already signed in
				Toast.makeText(getApplicationContext(), "You are already signed in.\nThanks!", Toast.LENGTH_SHORT).show();
			}
			else{
				Intent in = new Intent("org.bewellapp.SignIn");
				startActivity(in);
			}
			break;
		}				

//		case Menu.FIRST + 3: {
//			//sign-up
//			SharedPreferences sp = getSharedPreferences(Ml_Toolkit_Application.SHARED_PREF, MODE_PRIVATE);
//			if (sp.getBoolean(Ml_Toolkit_Application.SP_ISREGISTERED, false)) {
//				//already signed up
//				Toast.makeText(getApplicationContext(), "You are already signed up.\nThanks!", Toast.LENGTH_SHORT).show();
//			}
//			else{
//				Intent in = new Intent("org.bewellapp.Policy");
//				startActivity(in);
//			}
//			break;
//		}				

		case Menu.FIRST + 3: {
			//start sensing
			boolean alreadyStopped = getSharedPreferences(Ml_Toolkit_Application.SHARED_PREF, MODE_PRIVATE).getBoolean(Ml_Toolkit_Application.SP_ISSTOPPED, false); 

			if (alreadyStopped)
			{
				appState.audioForceLocked = false; 
				serviceController.startAudioSensor();
				serviceController.startAccelerometerSensor();
				serviceController.startLocationSensor();
				serviceController.startNTPTimestampsService();
				serviceController.startSleepService();
				serviceController.startAppMonitor();
				serviceController.startDaemonService();


				//record status
				SharedPreferences.Editor spe = getSharedPreferences(Ml_Toolkit_Application.SHARED_PREF, MODE_PRIVATE).edit();
				spe.putBoolean(Ml_Toolkit_Application.SP_ISSTOPPED, false);
				spe.commit();
			}
			else
			{
				Toast.makeText(getApplicationContext(), "Sensing is already started!", Toast.LENGTH_SHORT).show();
			}

			finish();
			break;
		}
		
		
		case Menu.FIRST + 4: {
			//stop sensing
			boolean alreadyStopped = getSharedPreferences(Ml_Toolkit_Application.SHARED_PREF, MODE_PRIVATE).getBoolean(Ml_Toolkit_Application.SP_ISSTOPPED, false); 

			if (!alreadyStopped)
			{
				appState.audioForceLocked = true; 
				serviceController.stopAudioSensor();
				serviceController.stopAccelerometerSensor();
				serviceController.stopLocationSensor();
				serviceController.stopNTPTimestampsService();
				serviceController.stopSleepService();
				serviceController.stopAppMonitor();
				appState.daemonService.stopSelf();


				//record status
				SharedPreferences.Editor spe = getSharedPreferences(Ml_Toolkit_Application.SHARED_PREF, MODE_PRIVATE).edit();
				spe.putBoolean(Ml_Toolkit_Application.SP_ISSTOPPED, true);
				spe.commit();
			}
			else
			{
				Toast.makeText(getApplicationContext(), "Sensing is already stopped!", Toast.LENGTH_SHORT).show();
			}

			finish();
			break;
		}

		}

		return true;
	}
	*/

}



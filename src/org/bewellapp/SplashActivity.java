package org.bewellapp;

import org.bewellapp.R;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.Window;

public class SplashActivity extends Activity {

	/**
	 * The thread to process splash screen events
	 */
	private Thread mSplashThread;
	private String TAG = "SplashActivity";

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.splash);

		final SplashActivity sPlashScreen = this;
		

		mSplashThread = new Thread() {
			@Override
			public void run() {
//				try {
//					synchronized (this) {
//						wait(2000);		//display X seconds
//					}
//				} catch (InterruptedException ex) {
//				}

				finish();
				Log.d(TAG, "I'm here!");
				boolean isWelcomed = getSharedPreferences(Ml_Toolkit_Application.SHARED_PREF, MODE_PRIVATE).getBoolean(Ml_Toolkit_Application.SP_WELCOMED, false); 
				//Log.d(TAG, "isWelcomed: " + Boolean.toString(isWelcomed));	
				
				isWelcomed = true;
				if (isWelcomed)
					//directly go to splash activity
				{
					Intent intent = new Intent();
					intent.setClass(sPlashScreen, dashBoardActivity.class);
					intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					startActivity(intent);
				}
				else
					//go to welcome tutorial
				{
					Intent intent = new Intent();
					intent.setClass(sPlashScreen, Tut0Activity.class);
					intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					startActivity(intent);
				}
				
			}
		};

		mSplashThread.start();



	}

	@Override
	public boolean onTouchEvent(MotionEvent evt) {
		if (evt.getAction() == MotionEvent.ACTION_DOWN) {
			synchronized (mSplashThread) {
				mSplashThread.notifyAll();
			}
		}
		return true;
	}
}


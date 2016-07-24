package org.bewellapp;

import org.bewellapp.ServiceControllers.ServiceController;

import org.bewellapp.R;
import android.app.Activity;
import android.os.Bundle;


public class AboutActivity extends Activity {
	
	ServiceController serviceController;
	//private static Ml_Toolkit_Application appState;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.about);
	}
}
package org.bewellapp;

import org.bewellapp.R;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;

public class Tut0Activity extends Activity {

	private String TAG = "tot_0";
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.tut0);
		
		
		ImageButton next = (ImageButton)findViewById(R.id.tut0Next);
		next.setOnClickListener(new View.OnClickListener(){
			@Override
			public void onClick(View v){
				
				finish();
				Intent intent = new Intent();
				intent.setClass(Tut0Activity.this, Tut1Activity.class);
				intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				startActivity(intent);
			}
			
			
		});
		

	}
}

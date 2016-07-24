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

public class Tut3Activity extends Activity {

	private String TAG = "tot_3";
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.tut3);
		
		
		ImageButton next = (ImageButton)findViewById(R.id.tut3Next);
		next.setOnClickListener(new View.OnClickListener(){
			@Override
			public void onClick(View v){
		
				finish();
				Intent intent = new Intent();
				intent.setClass(Tut3Activity.this, Tut4Activity.class);
				intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				startActivity(intent);
			}
		});
		

		ImageButton back = (ImageButton)findViewById(R.id.tut3Back);
		back.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				finish();
				Intent intent = new Intent();
				intent.setClass(Tut3Activity.this, Tut2Activity.class);
				intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				startActivity(intent);
				
			}
		});
		
		
	}
}

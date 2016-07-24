package org.bewellapp;

import org.bewellapp.R;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class PolicyActivity extends Activity {

	private String TAG = "Policy";
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.policy);
		Button btnCancel = (Button)findViewById(R.id.policyCancel);
		Button btnOk = (Button)findViewById(R.id.policyOk);
		btnCancel.setOnClickListener(new View.OnClickListener(){
			@Override
			public void onClick(View v){
				finish();
			}
			
			
		});
		
		btnOk.setOnClickListener(new View.OnClickListener(){

			@Override
			public void onClick(View v){
				
				Intent in = new Intent("org.bewellapp.SignUp");
				startActivity(in);
				finish();
			}
		});
	}
}

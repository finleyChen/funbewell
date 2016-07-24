package org.bewellapp;

import org.bewellapp.ScoreComputation.ScoreComputationService;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class DebugActivity extends Activity {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.debugactivity);
		
		Button btCalcScore = (Button)findViewById(R.id.btScores);
		btCalcScore.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				Intent i = new Intent (DebugActivity.this, ScoreComputationService.class);
				startService(i);
			}
		});
	}
}

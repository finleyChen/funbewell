package org.bewellapp.ServiceControllers.BestSleepLib;

import org.bewellapp.Ml_Toolkit_Application;
import org.bewellapp.Storage.ML_toolkit_object;

import edu.dartmouthcs.UtilLibs.MyDataTypeConverter;
import org.bewellapp.R;
import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;

public class SleepTruthActivity extends Activity {
	private Ml_Toolkit_Application appState;
	private EditText sleepDurationText;
	private Button okButton;
	private static final String TAG = "BestSleep";

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.sleep);
		appState = (Ml_Toolkit_Application)getApplicationContext();

		sleepDurationText = (EditText)findViewById(R.id.SleepEditText1);
		okButton = (Button)findViewById(R.id.SleepButton1);
		okButton.setOnClickListener(new OnClickListener(){
			public void onClick(View v) {
				String text = sleepDurationText.getText().toString();
				BestSleepService.groundTruthDuration = Double.parseDouble(text);
				Log.d(TAG, "Groundtruth Sleep Duration: " + Double.toString(BestSleepService.groundTruthDuration));
				//write database
				long currentMillis = System.currentTimeMillis();
				ML_toolkit_object sleepObj = appState.mMlToolkitObjectPool.borrowObject();
				sleepObj.setValues(currentMillis, 20, MyDataTypeConverter.toByta(BestSleepService.groundTruthDuration));
				appState.ML_toolkit_buffer.insert(sleepObj);	
				finish();
			}
		}
		);
	}

}

package org.bewellapp;

import org.bewellapp.Storage.ML_toolkit_object;

import org.bewellapp.R;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.CompoundButton.OnCheckedChangeListener;

public class LabelActivity extends Activity {


	private static Ml_Toolkit_Application appState;
	private CheckBox stationaryCheckBox; 
	private CheckBox walkingCheckBox; 
	private CheckBox runningCheckBox; 
	private CheckBox drivingCheckBox; 
	private CheckBox talkingCheckBox; 

	//only one is enough
	//private ML_toolkit_object LabelActvityObj;


	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.label);

		//
		appState = (Ml_Toolkit_Application)getApplicationContext();

		//stationary
		stationaryCheckBox = (CheckBox)findViewById(R.id.StationaryCheckBox);
		walkingCheckBox = (CheckBox)findViewById(R.id.WalkingCheckBox);
		runningCheckBox = (CheckBox)findViewById(R.id.RunningBox);
		drivingCheckBox = (CheckBox)findViewById(R.id.DrivingBox);
		
		
		
		
		stationaryCheckBox.setChecked(appState.stationaryOngoing);
		if(appState.stationaryOngoing)
			stationaryCheckBox.setBackgroundColor(Color.argb(128, 255, 0, 51));		
		else
			stationaryCheckBox.setBackgroundColor(Color.argb(128, 0, 102, 204));

		stationaryCheckBox.setOnCheckedChangeListener(
				new OnCheckedChangeListener() {

					@Override
					public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
						// TODO Auto-generated method stub
						if(isChecked){
							//green
							//serviceController.startAudioSensor();
							
							if(walkingCheckBox.isChecked()){
								walkingCheckBox.setChecked(false);
							}
							if(runningCheckBox.isChecked()){
								runningCheckBox.setChecked(false);
							}
							if(drivingCheckBox.isChecked()){
								drivingCheckBox.setChecked(false);
							}
							
							appState.stationaryOngoing = true;
							buttonView.setBackgroundColor(Color.argb(128, 255, 102, 51));
							appState.ML_toolkit_buffer.insert(appState.mMlToolkitObjectPool.borrowObject().setValues(System.currentTimeMillis()+appState.timeOffset, 7, true, "stationary"));																																	
							

						
						} else {
							//red --- sensor should turn off
							//serviceController.stopAudioSensor();
							appState.stationaryOngoing = false;
							buttonView.setBackgroundColor(Color.argb(128, 0, 102, 204));
							appState.ML_toolkit_buffer.insert(appState.mMlToolkitObjectPool.borrowObject().setValues(System.currentTimeMillis()+appState.timeOffset, 8, false, "stationary"));
						}
					}
				});



		//Walking
		walkingCheckBox.setChecked(appState.walkingOngoing);
		if(appState.walkingOngoing)
			walkingCheckBox.setBackgroundColor(Color.argb(128, 255, 0, 51));		
		else
			walkingCheckBox.setBackgroundColor(Color.argb(128, 0, 102, 204));

		walkingCheckBox.setOnCheckedChangeListener(
				new OnCheckedChangeListener() {

					@Override
					public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
						// TODO Auto-generated method stub
						Log.i("walkingcheckbox","Checkbox walking " + isChecked);
						if(isChecked){
							//green
							//serviceController.startAudioSensor();
							if(stationaryCheckBox.isChecked()){
								stationaryCheckBox.setChecked(false);
							}
							if(runningCheckBox.isChecked()){
								runningCheckBox.setChecked(false);
							}
							if(drivingCheckBox.isChecked()){
								drivingCheckBox.setChecked(false);
							}
							
							
							appState.walkingOngoing = true;
							buttonView.setBackgroundColor(Color.argb(128, 255, 102, 51));
							appState.ML_toolkit_buffer.insert(appState.mMlToolkitObjectPool.borrowObject().setValues(System.currentTimeMillis()+appState.timeOffset, 7, true, "walking"));
							

						
						} else {
							//red --- sensor should turn off
							//serviceController.stopAudioSensor();
							appState.walkingOngoing = false;
							buttonView.setBackgroundColor(Color.argb(128, 0, 102, 204));
							appState.ML_toolkit_buffer.insert(appState.mMlToolkitObjectPool.borrowObject().setValues(System.currentTimeMillis()+appState.timeOffset, 8, false, "walking"));
						}
					}
				});



		//running
		runningCheckBox.setChecked(appState.runningOngoing);
		if(appState.runningOngoing)
			runningCheckBox.setBackgroundColor(Color.argb(128, 255, 0, 51));		
		else
			runningCheckBox.setBackgroundColor(Color.argb(128, 0, 102, 204));

		runningCheckBox.setOnCheckedChangeListener(
				new OnCheckedChangeListener() {

					@Override
					public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
						// TODO Auto-generated method stub
						Log.i("runningcheckbox","Checkbox running " + isChecked);
						if(isChecked){
							//green
							//serviceController.startAudioSensor();
							if(stationaryCheckBox.isChecked()){
								stationaryCheckBox.setChecked(false);
							}
							if(walkingCheckBox.isChecked()){
								walkingCheckBox.setChecked(false);
							}
							if(drivingCheckBox.isChecked()){
								drivingCheckBox.setChecked(false);
							}
							
							
							appState.runningOngoing = true;
							buttonView.setBackgroundColor(Color.argb(128, 255, 102, 51));
							appState.ML_toolkit_buffer.insert(appState.mMlToolkitObjectPool.borrowObject().setValues(System.currentTimeMillis()+appState.timeOffset, 7, true, "running"));
						} else {
							//red --- sensor should turn off
							//serviceController.stopAudioSensor();
							appState.runningOngoing = false;
							buttonView.setBackgroundColor(Color.argb(128, 0, 102, 204));
							appState.ML_toolkit_buffer.insert(appState.mMlToolkitObjectPool.borrowObject().setValues(System.currentTimeMillis()+appState.timeOffset, 8, false, "running"));
						}
					}
				});


		//Driving
		drivingCheckBox.setChecked(appState.drivingOngoing);
		if(appState.drivingOngoing)
			drivingCheckBox.setBackgroundColor(Color.argb(128, 255, 0, 51));		
		else
			drivingCheckBox.setBackgroundColor(Color.argb(128, 0, 102, 204));

		drivingCheckBox.setOnCheckedChangeListener(
				new OnCheckedChangeListener() {

					@Override
					public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
						// TODO Auto-generated method stub
						if(isChecked){
							//green
							//serviceController.startAudioSensor();
							if(stationaryCheckBox.isChecked()){
								stationaryCheckBox.setChecked(false);
							}
							if(walkingCheckBox.isChecked()){
								walkingCheckBox.setChecked(false);
							}
							if(runningCheckBox.isChecked()){
								runningCheckBox.setChecked(false);
							}
							
							appState.drivingOngoing = true;
							buttonView.setBackgroundColor(Color.argb(128, 255, 102, 51));
							appState.ML_toolkit_buffer.insert(appState.mMlToolkitObjectPool.borrowObject().setValues(System.currentTimeMillis()+appState.timeOffset, 7, true, "driving"));
						} else {
							//red --- sensor should turn off
							//serviceController.stopAudioSensor();
							appState.drivingOngoing = false;
							buttonView.setBackgroundColor(Color.argb(128, 0, 102, 204));
							appState.ML_toolkit_buffer.insert(appState.mMlToolkitObjectPool.borrowObject().setValues(System.currentTimeMillis()+appState.timeOffset, 8, false, "driving"));
						}
					}
				});


		//Talking
		talkingCheckBox = (CheckBox)findViewById(R.id.TalkingBox);
		talkingCheckBox.setChecked(appState.talkingOngoing);
		if(appState.talkingOngoing)
			talkingCheckBox.setBackgroundColor(Color.argb(128, 255, 0, 51));		
		else
			talkingCheckBox.setBackgroundColor(Color.argb(128, 0, 102, 204));

		talkingCheckBox.setOnCheckedChangeListener(
				new OnCheckedChangeListener() {

					@Override
					public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
						// TODO Auto-generated method stub
						if(isChecked){
							//green
							//serviceController.startAudioSensor();
							appState.talkingOngoing = true;
							buttonView.setBackgroundColor(Color.argb(128, 255, 102, 51));
							appState.ML_toolkit_buffer.insert(appState.mMlToolkitObjectPool.borrowObject().setValues(System.currentTimeMillis()+appState.timeOffset, 7, true, "talking"));
						} else {
							//red --- sensor should turn off
							//serviceController.stopAudioSensor();
							appState.talkingOngoing = false;
							buttonView.setBackgroundColor(Color.argb(128, 0, 102, 204));
							appState.ML_toolkit_buffer.insert(appState.mMlToolkitObjectPool.borrowObject().setValues(System.currentTimeMillis()+appState.timeOffset, 8, false, "talking"));
						}
					}
				});


		//Intent intent = getIntent();
		// String name = (String) intent.getExtras().get("name");

		//Staiona
		/*
		TextView textview = (TextView) findViewById(R.id.textview_note);


		// Stationary label
		final Button btn_stationary = (Button) findViewById(R.id.button_stationary);
		btn_stationary.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				v.setBackgroundColor(Color.argb(128, 255, 0, 0));
			}
		});

		// Walking label
		Button btn_walking = (Button) findViewById(R.id.button_walking);
		btn_walking.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				v.setBackgroundColor(Color.argb(128, 255, 0, 0));
			}
		});

		// Running label
		Button btn_running = (Button) findViewById(R.id.button_running);
		btn_running.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				v.setBackgroundColor(Color.argb(128, 255, 0, 0));
			}
		});

		// Driving label
		Button btn_driving = (Button) findViewById(R.id.button_driving);
		btn_driving.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				v.setBackgroundColor(Color.argb(128, 255, 0, 0));
			}
		});

		// Talking label
		Button btn_talking = (Button) findViewById(R.id.button_talking);
		btn_talking.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				v.setBackgroundColor(Color.argb(128, 255, 0, 0));
			}
		});

		Button btn_ok = (Button) findViewById(R.id.button_ok);
		btn_ok.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				//btn_stationary.setBackgroundColor(Color.argb(128, 255, 255, 0));
				Toast.makeText(LabelActivity.this, "Input Succeed!",
						Toast.LENGTH_SHORT).show();

			}
		});
		 */
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// TODO Auto-generated method stub
		super.onCreateOptionsMenu(menu);
		menu.add(0, Menu.FIRST + 1, 2, "Upload");
		//menu.add(0, Menu.FIRST + 2, 2, "Survey");
		//menu.add(0, Menu.FIRST + 3, 3, "About");
		menu.add(0, Menu.FIRST + 2, 3, "Status");
		menu.add(0, Menu.FIRST + 3, 1, "Back");
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// TODO Auto-generated method stub
		super.onOptionsItemSelected(item);
		switch (item.getItemId()) {
		case Menu.FIRST + 1: {
			Toast.makeText(this, "Data remaining to upload:\n" + appState.fileList.size() + " MB",
					Toast.LENGTH_SHORT).show();
			break;
		}
		/*case Menu.FIRST + 2: {
			// Open a survey activity
			Intent intent = new Intent();
			//intent.putExtra("sensor_status", sensor_status);
			intent.setAction("org.bewellapp.SURVEY");
			startActivity(intent);
			//startActivityForResult(intent,SENSOR_STATUS_REQUEST);
			break;
		}
		case Menu.FIRST + 3: {
			//Toast.makeText(this, " Sensor Group  &  PAC Group\n Depart. of Computer Science\n Dartmouth College",
			//	Toast.LENGTH_LONG).show();
			Intent intent = new Intent();
			//intent.putExtra("sensor_status", sensor_status);
			intent.setAction("org.bewellapp.ABOUT");
			startActivity(intent);
			break;
		}*/
		case Menu.FIRST + 2: {
			//System.exit(0);
			Intent intent = new Intent();
			//intent.putExtra("sensor_status", sensor_status);
			intent.setAction("org.bewellapp.STATUS");
			startActivity(intent);
			//startActivityForResult(intent,SENSOR_STATUS_REQUEST);
			break;
			//break;
		}
		case Menu.FIRST + 3: {
			//System.exit(0);
			//Intent intent = new Intent();
			//intent.putExtra("sensor_status", sensor_status);
			//intent.setAction("org.bewellapp.STATUS");
			//startActivity(intent);
			//startActivityForResult(intent,SENSOR_STATUS_REQUEST);
			finish();
			break;
			//break;
		}
		}

		return true;
	}


}

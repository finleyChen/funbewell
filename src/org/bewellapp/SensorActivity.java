package org.bewellapp;

import org.bewellapp.ServiceControllers.ServiceController;

import org.bewellapp.R;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.Toast;

public class SensorActivity extends Activity {

	private static ServiceController serviceController;
	private Ml_Toolkit_Application appState;
	private CheckBox audioCheckBox;  // = (CheckBox)findViewById(R.id.AudioCheckBox);
	private CheckBox aceelCheckBox;  // = (CheckBox)findViewById(R.id.AccelCheckBox);
	private CheckBox locationCheckBox;  // = (CheckBox)findViewById(R.id.LocationBox);
	private CheckBox rawAudioCheckBox; 
	private CheckBox wifiCheckBox; 
	private CheckBox bluetoothCheckBox; 
	private CheckBox rawAccelerometerCheckBox;
	//private OnCheckedChangeListener audioCheckListener;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.sensor);

		appState = (Ml_Toolkit_Application)getApplicationContext();
		serviceController = appState.getServiceController();

		//audio check box and listner
		audioCheckBox = (CheckBox)findViewById(R.id.AudioCheckBox);
		//initiated here because we need to disable it when there is not raw audio
		rawAudioCheckBox = (CheckBox)findViewById(R.id.RawAudioBox);

		audioCheckBox.setChecked(this.appState.audioSensorOn);
		if(this.appState.audioSensorOn)
			audioCheckBox.setBackgroundColor(Color.argb(128, 0, 255, 0));		
		else
			audioCheckBox.setBackgroundColor(Color.argb(128, 255, 0, 0));	

		audioCheckBox.setOnCheckedChangeListener(
				new OnCheckedChangeListener() {

					@Override
					public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
						// TODO Auto-generated method stub
						if(isChecked){
							//green
							appState.audioForceLocked = false;
							//rawAudioCheckBox.setVisibility(INVI)(false);
							
							rawAudioCheckBox.setVisibility(android.view.View.VISIBLE);//(false);
							//rawAudioCheckBox.setEnabled(true);
							//rawAudioCheckBox.setClickable(true);
							//rawAudioCheckBox.setBackgroundColor(Color.argb(128, 255, 0, 0));
							
							
							serviceController.startAudioSensor();
							buttonView.setBackgroundColor(Color.argb(128, 0, 255, 0));
						} else {
							//red --- sensor should turn off
							appState.audioForceLocked = true; //now that means raw was turned off by sensor screen
							
							rawAudioCheckBox.setVisibility(android.view.View.INVISIBLE);
							//rawAudioCheckBox.setEnabled(false);
							//rawAudioCheckBox.setClickable(false);
							//rawAudioCheckBox.setBackgroundColor(Color.argb(128, 255, 0, 0));
							
							
							//rawAudioCheckBox.setClickable(true);
							serviceController.stopAudioSensor();
							buttonView.setBackgroundColor(Color.argb(128, 255, 0, 0));
						}
						appState.writeToPropertiesFile(audioCheckBox.isChecked(), aceelCheckBox.isChecked(), locationCheckBox.isChecked(), rawAudioCheckBox.isChecked(),wifiCheckBox.isChecked(),bluetoothCheckBox.isChecked(),
								appState.enableAccelDutyCycling, appState.enableAudioDutyCycling, appState.rawAccelOn);
					}
				});



		aceelCheckBox = (CheckBox)findViewById(R.id.AccelCheckBox);
		//aceelCheckBox.setOnCheckedChangeListener(accelCheckListener);
		aceelCheckBox.setChecked(this.appState.accelSensorOn);
		if(this.appState.accelSensorOn)
			aceelCheckBox.setBackgroundColor(Color.argb(128, 0, 255, 0));		
		else
			aceelCheckBox.setBackgroundColor(Color.argb(128, 255, 0, 0));	

		aceelCheckBox.setOnCheckedChangeListener(
				new OnCheckedChangeListener() {

					@Override
					public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
						// TODO Auto-generated method stub
						if(isChecked){
							serviceController.startAccelerometerSensor();
							buttonView.setBackgroundColor(Color.argb(128, 0, 255, 0));
						} else {
							serviceController.stopAccelerometerSensor();
							buttonView.setBackgroundColor(Color.argb(128, 255, 0, 0));
						}
						appState.writeToPropertiesFile(audioCheckBox.isChecked(), aceelCheckBox.isChecked(), locationCheckBox.isChecked(), rawAudioCheckBox.isChecked(),wifiCheckBox.isChecked(),bluetoothCheckBox.isChecked(),
								appState.enableAccelDutyCycling, appState.enableAudioDutyCycling, appState.rawAccelOn);
					}
				});

		locationCheckBox = (CheckBox)findViewById(R.id.LocationBox);
		locationCheckBox.setChecked(this.appState.locationSensorOn);
		if(this.appState.locationSensorOn)
			locationCheckBox.setBackgroundColor(Color.argb(128, 0, 255, 0));		
		else
			locationCheckBox.setBackgroundColor(Color.argb(128, 255, 0, 0));	

		locationCheckBox.setOnCheckedChangeListener(
				new OnCheckedChangeListener() {

					@Override
					public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
//						// TODO Auto-generated method stub
//						if(isChecked){
//							serviceController.startLocationSensor();
//							buttonView.setBackgroundColor(Color.argb(128, 0, 255, 0));
//						} else {
//							serviceController.stopLocationSensor();
//							buttonView.setBackgroundColor(Color.argb(128, 255, 0, 0));
//						}
//						appState.writeToPropertiesFile(audioCheckBox.isChecked(), aceelCheckBox.isChecked(), locationCheckBox.isChecked(), rawAudioCheckBox.isChecked(),wifiCheckBox.isChecked(),bluetoothCheckBox.isChecked(),
//								appState.enableAccelDutyCycling, appState.enableAudioDutyCycling, appState.rawAccelOn);
					}
				});



		rawAudioCheckBox.setChecked(this.appState.rawAudioOn);
		if(this.appState.rawAudioOn)
			rawAudioCheckBox.setBackgroundColor(Color.argb(128, 0, 255, 0));		
		else
			rawAudioCheckBox.setBackgroundColor(Color.argb(128, 255, 0, 0));	

		rawAudioCheckBox.setOnCheckedChangeListener(
				new OnCheckedChangeListener() {

					@Override
					public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
						// TODO Auto-generated method stub
						if(isChecked){
							//serviceController.startLocationSensor();
							appState.rawAudioOn = true;
							Toast.makeText(SensorActivity.this, "Raw audio sensing turned on",
									Toast.LENGTH_SHORT).show();
							buttonView.setBackgroundColor(Color.argb(128, 0, 255, 0));
						} else {
							//serviceController.stopLocationSensor();
							appState.rawAudioOn = false;
							Toast.makeText(SensorActivity.this, "Raw audio sensing turned off",
									Toast.LENGTH_SHORT).show();
							buttonView.setBackgroundColor(Color.argb(128, 255, 0, 0));
						}
						appState.writeToPropertiesFile(audioCheckBox.isChecked(), aceelCheckBox.isChecked(), locationCheckBox.isChecked(), rawAudioCheckBox.isChecked(),wifiCheckBox.isChecked(),bluetoothCheckBox.isChecked(),
								appState.enableAccelDutyCycling, appState.enableAudioDutyCycling, appState.rawAccelOn);
					}
				});
		
		
		wifiCheckBox = (CheckBox)findViewById(R.id.WifiBox);
		wifiCheckBox.setChecked(this.appState.wifiSensorOn);
		if(this.appState.wifiSensorOn)
			wifiCheckBox.setBackgroundColor(Color.argb(128, 0, 255, 0));		
		else
			wifiCheckBox.setBackgroundColor(Color.argb(128, 255, 0, 0));	

		wifiCheckBox.setOnCheckedChangeListener(
				new OnCheckedChangeListener() {

					@Override
					public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
						// TODO Auto-generated method stub
						if(isChecked){
							serviceController.startWifiService();
							buttonView.setBackgroundColor(Color.argb(128, 0, 255, 0));
						} else {
							serviceController.stopWifiSensor();
							buttonView.setBackgroundColor(Color.argb(128, 255, 0, 0));
						}
						appState.writeToPropertiesFile(audioCheckBox.isChecked(), aceelCheckBox.isChecked(), locationCheckBox.isChecked(), rawAudioCheckBox.isChecked(),wifiCheckBox.isChecked(),bluetoothCheckBox.isChecked(),
								appState.enableAccelDutyCycling, appState.enableAudioDutyCycling, appState.rawAccelOn);
					}
				});
		wifiCheckBox.setVisibility(android.view.View.INVISIBLE);
		
		
		bluetoothCheckBox = (CheckBox)findViewById(R.id.BluetoothBox);
		bluetoothCheckBox.setChecked(this.appState.bluetoothSensorOn);
		if(this.appState.wifiSensorOn)
			bluetoothCheckBox.setBackgroundColor(Color.argb(128, 0, 255, 0));		
		else
			bluetoothCheckBox.setBackgroundColor(Color.argb(128, 255, 0, 0));	

		bluetoothCheckBox.setOnCheckedChangeListener(
				new OnCheckedChangeListener() {

					@Override
					public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
						// TODO Auto-generated method stub
						if(isChecked){
							serviceController.startBluetoothService();//							();
							buttonView.setBackgroundColor(Color.argb(128, 0, 255, 0));
						} else {
							serviceController.stopBluetoothSensor();
							buttonView.setBackgroundColor(Color.argb(128, 255, 0, 0));
						}
						appState.writeToPropertiesFile(audioCheckBox.isChecked(), aceelCheckBox.isChecked(), locationCheckBox.isChecked(), rawAudioCheckBox.isChecked(),wifiCheckBox.isChecked(),bluetoothCheckBox.isChecked(),
								appState.enableAccelDutyCycling, appState.enableAudioDutyCycling, appState.rawAccelOn);
					}
				});
		bluetoothCheckBox.setVisibility(android.view.View.INVISIBLE); //For Bewell
		

		
		rawAccelerometerCheckBox = (CheckBox)findViewById(R.id.RawAccelBox);
		rawAccelerometerCheckBox.setChecked(this.appState.rawAccelOn);
		if (this.appState.rawAccelOn) {
			rawAccelerometerCheckBox.setBackgroundColor(Color.argb(128, 0, 255, 0));
		} else {
			rawAccelerometerCheckBox.setBackgroundColor(Color.argb(128, 255, 0, 0));	
		}
		rawAccelerometerCheckBox.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if(isChecked){
					appState.rawAccelOn = true;
					Toast.makeText(SensorActivity.this, "Raw accel logging on", Toast.LENGTH_SHORT).show();
					buttonView.setBackgroundColor(Color.argb(128, 0, 255, 0));
				} else {
					appState.rawAccelOn = false;
					Toast.makeText(SensorActivity.this, "Raw accel logging on",	Toast.LENGTH_SHORT).show();
					buttonView.setBackgroundColor(Color.argb(128, 255, 0, 0));
				}
				appState.writeToPropertiesFile(audioCheckBox.isChecked(), aceelCheckBox.isChecked(), locationCheckBox.isChecked(), rawAudioCheckBox.isChecked(),wifiCheckBox.isChecked(),bluetoothCheckBox.isChecked(),
						appState.enableAccelDutyCycling, appState.enableAudioDutyCycling, appState.rawAccelOn);
			}
		});
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
		menu.add(0, Menu.FIRST + 4, 1, "DutyCycle");
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// TODO Auto-generated method stub
		super.onOptionsItemSelected(item);
		switch (item.getItemId()) {
		case Menu.FIRST + 1: {
			//int mb_size = appState.fileList.size() + appState.file_count;
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
		
		case Menu.FIRST + 4: {
			//Toast.makeText(this, " Sensor Group  &  PAC Group\n Depart. of Computer Science\n Dartmouth College",
			//	Toast.LENGTH_LONG).show();
			Intent intent = new Intent();
			//intent.putExtra("sensor_status", sensor_status);
			intent.setAction("org.bewellapp.DutyCycleActivity");
			startActivity(intent);
			break;
		}
		
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

	@Override 
	public boolean onKeyDown(int keyCode, KeyEvent event) { 
		// whether it is back key    
		if (keyCode == KeyEvent.KEYCODE_BACK) { 
			//Intent intent = getIntent();
			boolean[] sensor_status = new boolean[3];	
			//0 for audio,1 for acc,2 for location
			sensor_status[0] = audio;
			sensor_status[1] = acc;
			sensor_status[2] = location;
			//intent.putExtra("sensor_status", sensor_status);

			//setResult(RESULT_CANCELED, intent); 
			this.finish(); 
			return true; 
		}
		else { 
			return super.onKeyDown(keyCode, event); 
		} 
	} 

	public void onResume() {
		super.onResume();

		audioCheckBox.setChecked(this.appState.audioSensorOn);
		if(this.appState.audioSensorOn){
			audioCheckBox.setBackgroundColor(Color.argb(128, 0, 255, 0));	
			rawAudioCheckBox.setVisibility(android.view.View.VISIBLE);
			//rawAudioCheckBox.setEnabled(true);
			//rawAudioCheckBox.setClickable(true);
		}
		else{
			audioCheckBox.setBackgroundColor(Color.argb(128, 255, 0, 0));	
			rawAudioCheckBox.setVisibility(android.view.View.INVISIBLE);
			//rawAudioCheckBox.setEnabled(false);
			//rawAudioCheckBox.setClickable(false);
		}



		aceelCheckBox.setChecked(this.appState.accelSensorOn);
		if(this.appState.accelSensorOn)
			aceelCheckBox.setBackgroundColor(Color.argb(128, 0, 255, 0));		
		else
			aceelCheckBox.setBackgroundColor(Color.argb(128, 255, 0, 0));	



		locationCheckBox.setChecked(this.appState.locationSensorOn);
		if(this.appState.locationSensorOn)
			locationCheckBox.setBackgroundColor(Color.argb(128, 0, 255, 0));		
		else
			locationCheckBox.setBackgroundColor(Color.argb(128, 255, 0, 0));	


		rawAudioCheckBox.setChecked(this.appState.rawAudioOn);
		if(this.appState.rawAudioOn)
			rawAudioCheckBox.setBackgroundColor(Color.argb(128, 0, 255, 0));		
		else
			rawAudioCheckBox.setBackgroundColor(Color.argb(128, 255, 0, 0));
		
		
		wifiCheckBox.setChecked(this.appState.wifiSensorOn);
		if(this.appState.wifiSensorOn)
			wifiCheckBox.setBackgroundColor(Color.argb(128, 0, 255, 0));		
		else
			wifiCheckBox.setBackgroundColor(Color.argb(128, 255, 0, 0));

		
		bluetoothCheckBox.setChecked(this.appState.bluetoothSensorOn);
		if(this.appState.bluetoothSensorOn)
			bluetoothCheckBox.setBackgroundColor(Color.argb(128, 0, 255, 0));		
		else
			bluetoothCheckBox.setBackgroundColor(Color.argb(128, 255, 0, 0));



		Log.i("Sensor Activity", "Sensor onResume ...");
	}


	static boolean audio;
	static boolean acc;
	static boolean location;

};

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

public class DutyCycleActivity extends Activity {


	private static Ml_Toolkit_Application appState;
	private CheckBox audioDutyCycleCheckBox; 
	private CheckBox accelDutyCycleCheckBox;
	
	//only one is enough
	//private ML_toolkit_object LabelActvityObj;


	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.dutycycle);

		//
		appState = (Ml_Toolkit_Application)getApplicationContext();

		//stationary
		audioDutyCycleCheckBox = (CheckBox)findViewById(R.id.AudioDutyCycleTextBox);
		audioDutyCycleCheckBox.setChecked(appState.enableAudioDutyCycling);
		if(appState.enableAudioDutyCycling)
			audioDutyCycleCheckBox.setBackgroundColor(Color.argb(128, 0, 255, 0));		
		else
			audioDutyCycleCheckBox.setBackgroundColor(Color.argb(128, 255, 0, 0));

		audioDutyCycleCheckBox.setOnCheckedChangeListener(
				new OnCheckedChangeListener() {

					@Override
					public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
						// TODO Auto-generated method stub
						//means stop the previous mode
						//appState.getServiceController().stopAudioSensor(appState.enableAudioDutyCycling);

						/*try {
							Thread.sleep(1000);
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}*/

						if(isChecked){//means enabled
							appState.writeToPropertiesFile(appState.savedAudioSensorOn, appState.accelSensorOn, appState.savedLocationSensorOn, appState.savedRawSensorOn,appState.savedWifiSensorOn, appState.savedBluetoothSensorOn,
									appState.enableAccelDutyCycling, appState.enableAudioDutyCycling, appState.rawAccelOn);
							
							appState.getServiceController().stopAudioSensor();
							appState.enableAudioDutyCycling = true;
							appState.audioSensorOn = true;
							
							audioDutyCycleCheckBox.setBackgroundColor(Color.argb(128, 0, 255, 0));
							new Thread(new Runnable() {
								public void run() {
									try {
										Thread.sleep(2000);
									} catch (InterruptedException e) {
										// TODO Auto-generated catch block
										e.printStackTrace();
									}
									appState.getServiceController().startAudioSensor();
								}
							}).start();
							//appState.writeToPropertiesFile(appState.savedAudioSensorOn, appState.savedAccelSensorOn, appState.savedLocationSensorOn, appState.savedRawSensorOn, appState.savedWifiSensorOn, appState.savedBluetoothSensorOn,
									//appState.enableAccelDutyCycling, appState.enableAudioDutyCycling);

						} else {
							appState.writeToPropertiesFile(appState.savedAudioSensorOn, appState.accelSensorOn, appState.savedLocationSensorOn, appState.savedRawSensorOn,appState.savedWifiSensorOn, appState.savedBluetoothSensorOn,
									appState.enableAccelDutyCycling, appState.enableAudioDutyCycling, appState.rawAccelOn);
							
							appState.getServiceController().stopAudioSensor();
							appState.enableAudioDutyCycling = false;
							appState.audioSensorOn = true;
							
							audioDutyCycleCheckBox.setBackgroundColor(Color.argb(128, 255, 0, 0));
							new Thread(new Runnable() {
								public void run() {
									try {
										Thread.sleep(2000);
									} catch (InterruptedException e) {
										// TODO Auto-generated catch block
										e.printStackTrace();
									}
									appState.getServiceController().startAudioSensor();
								}
							}).start();

						}


						//appState.getServiceController().startAudioSensor(appState.enableAudioDutyCycling);
					}
				});


		//stationary
		accelDutyCycleCheckBox = (CheckBox)findViewById(R.id.AccelDutyCycleTextBox);
		accelDutyCycleCheckBox.setChecked(appState.enableAccelDutyCycling);
		if(appState.enableAccelDutyCycling)
			accelDutyCycleCheckBox.setBackgroundColor(Color.argb(128, 0, 255, 0));		
		else
			accelDutyCycleCheckBox.setBackgroundColor(Color.argb(128, 255, 0, 0));

		accelDutyCycleCheckBox.setOnCheckedChangeListener(
				new OnCheckedChangeListener() {

					@Override
					public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
						// TODO Auto-generated method stub
						//means stop the previous mode
						//appState.getServiceController().stopAudioSensor(appState.enableAudioDutyCycling);

						/*try {
							Thread.sleep(1000);
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}*/

						if(isChecked){//means enabled
							appState.writeToPropertiesFile(appState.savedAudioSensorOn, appState.accelSensorOn, appState.savedLocationSensorOn, appState.savedRawSensorOn,appState.savedWifiSensorOn, appState.savedBluetoothSensorOn,
									appState.enableAccelDutyCycling, appState.enableAudioDutyCycling, appState.rawAccelOn);
							
							appState.getServiceController().stopAccelerometerSensor();
							appState.enableAccelDutyCycling = true;
							appState.accelSensorOn = true;
							
							accelDutyCycleCheckBox.setBackgroundColor(Color.argb(128, 0, 255, 0));
							new Thread(new Runnable() {
								public void run() {
									try {
										Thread.sleep(2000);
									} catch (InterruptedException e) {
										// TODO Auto-generated catch block
										e.printStackTrace();
									}
									appState.getServiceController().startAccelerometerSensor();

								}
							}).start();


						} else {
							appState.writeToPropertiesFile(appState.savedAudioSensorOn, appState.accelSensorOn, appState.savedLocationSensorOn, appState.savedRawSensorOn,appState.savedWifiSensorOn, appState.savedBluetoothSensorOn,
									appState.enableAccelDutyCycling, appState.enableAudioDutyCycling, appState.rawAccelOn);
							appState.getServiceController().stopAccelerometerSensor();
							appState.enableAccelDutyCycling = false;
							appState.accelSensorOn = true;//because we have to wait to start the sensor again. We just don't want to wait.
							
							accelDutyCycleCheckBox.setBackgroundColor(Color.argb(128, 255, 0, 0));
							new Thread(new Runnable() {
								public void run() {
									try {
										Thread.sleep(2000);
									} catch (InterruptedException e) {
										// TODO Auto-generated catch block
										e.printStackTrace();
									}
									appState.getServiceController().startAccelerometerSensor();
								}
							}).start();

						}


						//appState.getServiceController().startAudioSensor(appState.enableAudioDutyCycling);
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

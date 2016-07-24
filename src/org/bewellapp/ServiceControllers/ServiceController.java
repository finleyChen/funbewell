package org.bewellapp.ServiceControllers;

import java.io.File;

import org.bewellapp.Ml_Toolkit_Application;
import org.bewellapp.DaemonService.DaemonService;
import org.bewellapp.NTPTimeStampService.NTPTimeStampService;
import org.bewellapp.ServiceControllers.AccelerometerLib.AccelerometerService;
import org.bewellapp.ServiceControllers.AppLib.AppMonitorScreenStatusReceiver;
import org.bewellapp.ServiceControllers.AudioLib.AudioService;
import org.bewellapp.ServiceControllers.BestSleepLib.BestSleepService;
import org.bewellapp.ServiceControllers.BluetoothLib.BluetoothService;
import org.bewellapp.ServiceControllers.LocationLib.LocationService;
import org.bewellapp.ServiceControllers.UploadingLib.UploadService;
import org.bewellapp.ServiceControllers.UploadingLib.UploadingServiceIntelligent;
import org.bewellapp.ServiceControllers.UploadingLib.InfoObject.ConnectionType;
import org.bewellapp.ServiceControllers.WifiLib.WifiScanService;
import org.bewellapp.Storage.SDCardStorageService;

import edu.dartmouth.cs.funbewell.AwesomePipeline;

import android.content.Intent;
import android.util.Log;
import android.content.Context;
import android.content.IntentFilter;

public class ServiceController {
    private static final String TAG = "ServiceController";
    private final Ml_Toolkit_Application applicationCONTEXT;
    Intent accel_bindIntent;

    public ServiceController() {
	// CONTEXT = con;
	applicationCONTEXT = null;
    }

    public ServiceController(Context con) {
	applicationCONTEXT = (Ml_Toolkit_Application) con;
    }

    // public void startAccelerometerSensor(){
    public void startAccelerometerSensor() {
	Intent intent = new Intent(AccelerometerService.ACTION_FOREGROUND);
	intent.setClass(this.applicationCONTEXT, AccelerometerService.class);
	this.applicationCONTEXT.startService(intent);
	this.applicationCONTEXT.accelSensorOn = true;
    }

    public void startAppMonitor() {
	if (applicationCONTEXT.appMonitorScreenStatusReceiver == null) {
	    AppMonitorScreenStatusReceiver.applicationMonitoringEnabled
		    .set(true);
	    AppMonitorScreenStatusReceiver amssr = new AppMonitorScreenStatusReceiver();
	    applicationCONTEXT.appMonitorScreenStatusReceiver = amssr;
	    applicationCONTEXT.registerReceiver(amssr, new IntentFilter(
		    Intent.ACTION_SCREEN_ON));
	    applicationCONTEXT.registerReceiver(amssr, new IntentFilter(
		    Intent.ACTION_SCREEN_OFF));
	}
    }

    public void stopAppMonitor() {
	if (applicationCONTEXT.appMonitorScreenStatusReceiver != null) {
	    AppMonitorScreenStatusReceiver.applicationMonitoringEnabled
		    .set(false);
	    applicationCONTEXT
		    .unregisterReceiver(applicationCONTEXT.appMonitorScreenStatusReceiver);
	    applicationCONTEXT.appMonitorScreenStatusReceiver = null;
	}
    }

    public void startAudioSensor() {
	Intent intent = new Intent(AudioService.ACTION_FOREGROUND);
	intent.setClass(this.applicationCONTEXT, AudioService.class);
	this.applicationCONTEXT.startService(intent);
	// this.applicationCONTEXT.main_activity_Context.bindAudioService();
	this.applicationCONTEXT.audioSensorOn = true;
	// stopping the listener
	// stopService(new Intent(this, AudioService.class));
    }

    public void startLocationSensor() {
	Intent intent = new Intent(LocationService.ACTION_FOREGROUND);
	intent.setClass(this.applicationCONTEXT, LocationService.class);
	this.applicationCONTEXT.startService(intent);
	// this.applicationCONTEXT.main_activity_Context.bindLocationService();
	this.applicationCONTEXT.locationSensorOn = true;
    }

    private void makeOtherServicesForeground() {
	if (this.applicationCONTEXT.accelSensorOn == true)
	    this.applicationCONTEXT.acclerometerService
		    .callStartForegroundCompat();
	else if (this.applicationCONTEXT.audioSensorOn == true)
	    this.applicationCONTEXT.audioService.callStartForegroundCompat();
	else if (this.applicationCONTEXT.locationSensorOn == true)
	    this.applicationCONTEXT.locationService.callStartForegroundCompat();
	else if (this.applicationCONTEXT.wifiSensorOn == true)
	    this.applicationCONTEXT.wifiScanService.callStartForegroundCompat();
	else if (this.applicationCONTEXT.bluetoothSensorOn == true)
	    this.applicationCONTEXT.bluetoothService
		    .callStartForegroundCompat();
	else if (this.applicationCONTEXT.daemonService != null)
	    this.applicationCONTEXT.daemonService.callStartForegroundCompat();
	else if (this.applicationCONTEXT.nTPTimeStampService != null)
	    this.applicationCONTEXT.nTPTimeStampService
		    .callStartForegroundCompat();

    }

    public boolean areServiceUsingNotificationArea() {
	return (this.applicationCONTEXT.accelSensorOn == true)
		|| (this.applicationCONTEXT.audioSensorOn == true)
		|| (this.applicationCONTEXT.locationSensorOn == true)
		|| (this.applicationCONTEXT.wifiSensorOn == true)
		|| (this.applicationCONTEXT.bluetoothSensorOn == true)
		|| (this.applicationCONTEXT.daemonService != null)
		|| (this.applicationCONTEXT.nTPTimeStampService != null);
    }

    public void stopAccelerometerSensor() {

	Intent intent = new Intent(AccelerometerService.ACTION_FOREGROUND);
	intent.setClass(this.applicationCONTEXT, AccelerometerService.class);
	this.applicationCONTEXT.stopService(intent);
	this.applicationCONTEXT.accelSensorOn = false;

	makeOtherServicesForeground();
    }

    public void stopAudioSensor() {
	Intent intent = new Intent(AudioService.ACTION_FOREGROUND);
	intent.setClass(this.applicationCONTEXT, AudioService.class);
	this.applicationCONTEXT.stopService(intent);
	this.applicationCONTEXT.audioSensorOn = false;

	makeOtherServicesForeground();

    }

    public void stopNTPTimestampsService() {
	Intent intent = new Intent(NTPTimeStampService.ACTION_FOREGROUND);
	intent.setClass(this.applicationCONTEXT, NTPTimeStampService.class);
	this.applicationCONTEXT.stopService(intent);
	// this.applicationCONTEXT.locationSensorOn = false;

	makeOtherServicesForeground();
    }

    public void stopLocationSensor() {
	Intent intent = new Intent(LocationService.ACTION_FOREGROUND);
	intent.setClass(this.applicationCONTEXT, LocationService.class);
	this.applicationCONTEXT.stopService(intent);
	this.applicationCONTEXT.locationSensorOn = false;

	makeOtherServicesForeground();

    }

    public void stopWifiSensor() {
	// this.applicationCONTEXT.main_activity_Context.unbindService(this.applicationCONTEXT.main_activity_Context.aceelServConnection);
	// this.applicationCONTEXT.accelerometerSensor = false;

	Intent intent = new Intent(WifiScanService.ACTION_FOREGROUND);
	intent.setClass(this.applicationCONTEXT, WifiScanService.class);
	this.applicationCONTEXT.stopService(intent);
	this.applicationCONTEXT.wifiSensorOn = false;
	// this.applicationCONTEXT.main_activity_Context.unbindService(aceelServConnection);
	// this.applicationCONTEXT.main_activity_Context.accelServiceBinder =
	// null;

	makeOtherServicesForeground();

    }

    public void stopBluetoothSensor() {
	// this.applicationCONTEXT.main_activity_Context.unbindService(this.applicationCONTEXT.main_activity_Context.aceelServConnection);
	// this.applicationCONTEXT.accelerometerSensor = false;

	Intent intent = new Intent(BluetoothService.ACTION_FOREGROUND);
	intent.setClass(this.applicationCONTEXT, BluetoothService.class);
	this.applicationCONTEXT.stopService(intent);
	this.applicationCONTEXT.bluetoothSensorOn = false;
	// this.applicationCONTEXT.main_activity_Context.unbindService(aceelServConnection);
	// this.applicationCONTEXT.main_activity_Context.accelServiceBinder =
	// null;

	makeOtherServicesForeground();

    }

    public void startUploading(String db_path) {
	Intent intent = new Intent(this.applicationCONTEXT, UploadService.class);
	// use putextra to givev file names
	intent.putExtra("dbpath", db_path);
	this.applicationCONTEXT.startService(intent);

    }

    public void startSDCardStorageService(String db_path) {
	Intent intent = new Intent(this.applicationCONTEXT,
		SDCardStorageService.class);
	// use putextra to givev file names
	intent.putExtra("dbpath", db_path);
	this.applicationCONTEXT.startService(intent);
    }

    private long lastUploadTimestamp = 0;
    public void startUploadingIntellgent() {
	long now = System.currentTimeMillis();
	if(now - lastUploadTimestamp < 5 * 60 * 1000)
	{
	    return;
	}
	lastUploadTimestamp = now;
	
	boolean scoreFileExists;
	File scoreFile = applicationCONTEXT
		.getFileStreamPath(UploadingServiceIntelligent.BEWELL_UPLOAD_FILENAME);
	scoreFileExists = scoreFile.exists();

	if ((applicationCONTEXT.infoObj.isConnected() && scoreFileExists)
		|| (applicationCONTEXT.infoObj.charging_state != 0
			&& applicationCONTEXT.infoObj.wifi_status == ConnectionType.WIFI
			&& applicationCONTEXT.uploadRunning == false && applicationCONTEXT.fileList
			.size() > 0)) {
	    Intent intent = new Intent(
		    UploadingServiceIntelligent.ACTION_FOREGROUND);
	    intent.setClass(this.applicationCONTEXT,
		    UploadingServiceIntelligent.class);
	    this.applicationCONTEXT.startService(intent);
	}
    }

    public void stopUploadingIntellgent() {
	Intent intent = new Intent(
		UploadingServiceIntelligent.ACTION_FOREGROUND);
	intent.setClass(this.applicationCONTEXT,
		UploadingServiceIntelligent.class);
	this.applicationCONTEXT.stopService(intent);
    }

    public void startSleepService() {
	Intent intent = new Intent(BestSleepService.ACTION);
	intent.setClass(this.applicationCONTEXT, BestSleepService.class);
	this.applicationCONTEXT.startService(intent);

    }

    public void stopSleepService() {
	Intent intent = new Intent(BestSleepService.ACTION);
	intent.setClass(this.applicationCONTEXT, BestSleepService.class);
	this.applicationCONTEXT.stopService(intent);
    }

    public void startDaemonService() {
	Intent intent = new Intent(DaemonService.ACTION_FOREGROUND);
	intent.setClass(this.applicationCONTEXT, DaemonService.class);
	this.applicationCONTEXT.startService(intent);

	Log.i("DAEMON-SERVICE", "Running");
    }

    public void startMomentarySamplingService() {
	// TODO: re-enable momentary sampling
	// Intent intent = new
	// Intent(MomentarySamplingService.ACTION_FOREGROUND);
	// intent.setClass(this.applicationCONTEXT,
	// MomentarySamplingService.class);
	// this.applicationCONTEXT.startService(intent);
	//
	// Log.i("MomentarySamplingService","Running");
    }

    public void startNTPTimestampsService() {
	Intent intent = new Intent(NTPTimeStampService.ACTION_FOREGROUND);
	intent.setClass(this.applicationCONTEXT, NTPTimeStampService.class);
	this.applicationCONTEXT.startService(intent);
	Log.i("NTP-SERVICE", "Running");
    }

    public void startWifiService() {
	Intent intent = new Intent(WifiScanService.ACTION_FOREGROUND);
	intent.setClass(this.applicationCONTEXT, WifiScanService.class);
	this.applicationCONTEXT.startService(intent);
	this.applicationCONTEXT.wifiSensorOn = true;

	Log.i("WIFI-SERVICE", "Running");
    }

    public void startBluetoothService() {
	Intent intent = new Intent(BluetoothService.ACTION_FOREGROUND);
	intent.setClass(this.applicationCONTEXT, BluetoothService.class);
	this.applicationCONTEXT.startService(intent);
	this.applicationCONTEXT.bluetoothSensorOn = true;

	Log.i("BLUETOOTH-SERVICE", "Running");
    }

    public void startFunfService() {
	try {
	    Thread.sleep(60);
	} catch (Exception e) {
	}
	Intent i = new Intent(this.applicationCONTEXT.getApplicationContext(),
		AwesomePipeline.class);
	this.applicationCONTEXT.getApplicationContext().startService(i);

	Log.i("FUNF-PIPELINE", "Running");
    }
}
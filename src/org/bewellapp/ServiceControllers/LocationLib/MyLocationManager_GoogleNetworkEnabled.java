package org.bewellapp.ServiceControllers.LocationLib;

import java.util.Timer;
import java.util.TimerTask;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

public class MyLocationManager_GoogleNetworkEnabled extends MyLocationManager{

	private boolean gpsInOperation = false;
	private boolean networkBasedServiceInOperation = false;
	private Handler mHandler = new Handler();

	public MyLocationManager_GoogleNetworkEnabled(Context con) {
		super(con);
		// TODO Auto-generated constructor stub
	}

	public boolean getLocation(Context context, LocationResult result)
	{		
		//timer1=new Timer();
		//I use LocationResult callback class to pass location value from MyLocation to user code.
		locationResult=result;
		if(lm==null)
			lm = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);

		//exceptions will be thrown if provider is not permitted.
		try{gps_enabled=lm.isProviderEnabled(LocationManager.GPS_PROVIDER);}catch(Exception ex){}
		try{network_enabled=lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);}catch(Exception ex){}

		//don't start listeners if no provider is enabled
		//if(!gps_enabled && !network_enabled)
		//return false;

		Log.i("location Enabled", " " + gps_enabled + " " + network_enabled);

		if(gps_enabled){
			lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListenerGps);
			//start a timer and if the timer reaches its scheduled time then GPS operation failed
			//else GPS worked and the timer will not be functional
			//timer1=new Timer();
			//timer1.schedule(new GetLastLocation(), 40000);
			//timer1.schedule(new GetLastLocation_withNetwork(), gpsLocationRate);//wait for gpsLocationRate
			mHandler.removeCallbacks(mUpdateTimeTask);
			mHandler.postDelayed(mUpdateTimeTask, gpsLocationRate);
			
			//GPS in operation
			gpsInOperation = true;
			networkBasedServiceInOperation = false;
		}

		if(network_enabled && !gps_enabled){
			lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListenerNetwork);
			//start a timer and if the timer reaches its scheduled time then network operation failed
			//else network location worked and the timer will not be functional
			//timer1=new Timer();
			//timer1.schedule(new GetLastLocation(), 40000);
			//timer1.schedule(new GetLastLocation_withNetwork(), gpsLocationRate);//wait for gpsLocationRate
			mHandler.removeCallbacks(mUpdateTimeTask);
			mHandler.postDelayed(mUpdateTimeTask, gpsLocationRate);
			
			networkBasedServiceInOperation = true;
			gpsInOperation = false;
		}

		if(!gps_enabled && !network_enabled){
			getSkyhookLocation();
		}

		return true;
	}

	private Runnable mUpdateTimeTask = new Runnable() {
		//this will only run when there is no update from GPS or Network
		@Override
		public void run() {

			if(networkBasedServiceInOperation){
				lm.removeUpdates(locationListenerNetwork);
				getSkyhookLocation();
				networkBasedServiceInOperation = false;
			}
			else if(gpsInOperation)//that is GPS was operation and it failed
			{
				lm.removeUpdates(locationListenerGps);
				lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListenerNetwork);
				//start a timer and if the timer reaches its scheduled time then network operation failed
				//else network location worked and the timer will not be functional
				//timer1=new Timer();
				//timer1.schedule(new GetLastLocation(), 40000);
				//timer1.schedule(new GetLastLocation_withNetwork(), gpsLocationRate);//wait for gpsLocationRate
				mHandler.removeCallbacks(mUpdateTimeTask);
				mHandler.postDelayed(mUpdateTimeTask, gpsLocationRate);
				
				networkBasedServiceInOperation = true;
				gpsInOperation = false;
			}
			//else 
		}
	};
	
	LocationListener locationListenerGps = new LocationListener() {
		public void onLocationChanged(Location location) {
			//timer1.cancel();
			mHandler.removeCallbacks(mUpdateTimeTask);
			provider = "GPS";
			locationResult.gotLocation(location);
			Log.i("location", "GPS: " + location.getLatitude() + " " + location.getLongitude());	
			lm.removeUpdates(this);
			lm.removeUpdates(locationListenerGps);
			lm.removeUpdates(locationListenerNetwork);
		}
		public void onProviderDisabled(String provider) {}
		public void onProviderEnabled(String provider) {}
		public void onStatusChanged(String provider, int status, Bundle extras) {}
	};

	LocationListener locationListenerNetwork = new LocationListener() {
		public void onLocationChanged(Location location) {
			//timer1.cancel();
			mHandler.removeCallbacks(mUpdateTimeTask);
			provider = "Net";
			locationResult.gotLocation(location);
			Log.i("location", "Net: " + location.getLatitude() + " " + location.getLongitude());		
			lm.removeUpdates(this);
			lm.removeUpdates(locationListenerNetwork);
			lm.removeUpdates(locationListenerGps);
		}
		public void onProviderDisabled(String provider) {}
		public void onProviderEnabled(String provider) {}
		public void onStatusChanged(String provider, int status, Bundle extras) {}
	};
	
	/*
	 * = new Runnable() {
		public void run() {
			appState.location_text = "Scanning";
			myLocationManager.getLocation(LocationService.this, locationResult );
			//mHandler.postAtTime(this,1000*60*1);
			mHandler.removeCallbacks(mUpdateTimeTask);
			mHandler.postDelayed(mUpdateTimeTask, rateAccelSampling);
			//mHandler.postDelayed(mUpdateTimeTask, 1000*60*10);
			//mHandler.postDelayed(mUpdateTimeTask, 1000*30);
		}
	};
	 * 
	 */

}
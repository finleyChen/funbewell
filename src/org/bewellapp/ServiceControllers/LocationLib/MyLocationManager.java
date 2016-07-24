package org.bewellapp.ServiceControllers.LocationLib;

import java.util.Timer;
import java.util.TimerTask;

import com.skyhookwireless.wps.*;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;

public class MyLocationManager {
	Timer timer1;
	LocationManager lm;
	LocationResult locationResult;
	boolean gps_enabled=false;
	boolean network_enabled=false;
	public Context activity_con;
	public WPSLocation wps_location;
	public double[] my_location;
	public String provider; 
	public WPS wps; 
	//public WPSAuthentication auth;

	public double skyhookLat;
	public double skyhookLng;
	public boolean skyhookAvailable;


	//gps location durations
	protected final int gpsLocationRate = 1000*40;
	
	public MyLocationManager(Context con) {
		// TODO Auto-generated constructor stub
		this.activity_con = con;
		my_location= new double[2];
		provider = "Scanning";
		skyhookLat = 0;
		skyhookLng = 0;
		skyhookAvailable = false;
		//wps = new WPS(activity_con);
		//auth = new WPSAuthentication("mrshuva", "Dartmouth College");
	}

	public boolean getLocation(Context context, LocationResult result)
	{
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

		if(gps_enabled)
			lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListenerGps);
		
		//go for google based service now if gps is not enabled and network based service is enabled
		//if(network_enabled && !gps_enabled)
			//lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListenerNetwork);

		if(!gps_enabled && !network_enabled){
			getSkyhookLocation();
		}
		else{
			timer1=new Timer();
			//timer1.schedule(new GetLastLocation(), 40000);
			timer1.schedule(new GetLastLocation(), gpsLocationRate);
		}
		return true;
	}

	LocationListener locationListenerGps = new LocationListener() {
		public void onLocationChanged(Location location) {
			timer1.cancel();
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
			timer1.cancel();
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

	class GetLastLocation extends TimerTask {
		//this will only run when there is no update from GPS or Network
		@Override
		public void run() {
			getSkyhookLocation();
		}


	}

	protected void getSkyhookLocation()
	{
		lm.removeUpdates(locationListenerGps);
		lm.removeUpdates(locationListenerNetwork);

		/*
		//if GPS fails then use latest skyhook location
		if(my_location != null)				
			provider = "Skyhook";
		else
			provider = "none";
			//if(skyhookAvailable == true)


		locationResult.gotLocationn(my_location);
		 */




		// go for skyhook now
		wps = new WPS(activity_con);	
		WPSAuthentication auth = new WPSAuthentication("mrshuva", "Dartmouth College");


		WPSLocationCallback callback = new WPSLocationCallback()
		{
			// What the application should do after it's done
			public void done()
			{
				// after done() returns, you can make more WPS calls.
				Log.i("location Enabled", "done");
			}

			// What the application should do if an error occurs
			public WPSContinuation handleError(WPSReturnCode error)
			{
				//handleWPSError(error); // you'll implement handleWPSError()
				Log.i("location Enabled", "failed");
				// To retry the location call on error use WPS_CONTINUE,
				// otherwise return WPS_STOP
				my_location = null;
				provider = "No Location Service";
				locationResult.gotLocation(my_location);
				//runOnUiThread(locationTimer_Tick);



				//return WPSContinuation.WPS_STOP;
				return WPSContinuation.WPS_CONTINUE;


			}

			// Implements the actions using the location object
			public void handleWPSLocation(WPSLocation loc)
			{
				//Log.i("location Enabled", "Skyhook exexcuted");
				//location = new Location(l);
				//runOnUiThread(locationTimer_Tick);
				my_location= new double[2];
				my_location[0] = loc.getLatitude();
				my_location[1] = loc.getLongitude();
				provider = "Skyhook";
				locationResult.gotLocation(my_location);
				Log.i("location Enabled", "Skyhook exexcuted2");


				//Log.i("location Enabled", "failed");

				//WPSContinuation.WPS_STOP;
				// you'll implement printLocation()
				//printLocation(location.getLatitude(), location.getLongitude());
			}
		};

		// Call the location function with callback

		wps.getLocation(auth,
				WPSStreetAddressLookup.WPS_NO_STREET_ADDRESS_LOOKUP,
				callback);

		//wps.getPeriodicLocation(arg0, arg1, arg2, arg3, arg4)
		//commented because I don't want to rely on past location
		/*
		Location net_loc=null, gps_loc=null;
		if(gps_enabled)
			gps_loc=lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
		if(network_enabled)
			net_loc=lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

		//if there are both values use the latest one
		if(gps_loc!=null && net_loc!=null){
			if(gps_loc.getTime()>net_loc.getTime())
				locationResult.gotLocation(gps_loc);
			else
				locationResult.gotLocation(net_loc);
			return;
		}

		if(gps_loc!=null){
			locationResult.gotLocation(gps_loc);
			Log.i("location", "GPS: " + gps_loc.getLatitude() + " " + gps_loc.getLongitude());
			return;
		}
		if(net_loc!=null){
			locationResult.gotLocation(net_loc);
			Log.i("location", "Net: " + net_loc.getLatitude() + " " + net_loc.getLongitude());
			return;
		}
		locationResult.gotLocation(null);
		Log.i("location", "No location");
		 */
	}

	public static abstract class LocationResult{
		public abstract void gotLocation(Location location);
		public abstract void gotLocation(double[] location);
	}
}
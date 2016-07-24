package org.bewellapp.ServiceControllers.UploadingLib;

import org.bewellapp.Ml_Toolkit_Application;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.telephony.TelephonyManager;

public class InfoObject {

	public int battery_level;
	public int charging_state;
	public ConnectionType wifi_status;
	private Context appState;
	private Ml_Toolkit_Application mlobj;

	public InfoObject(Context appState, Ml_Toolkit_Application mlobj) {
		this.appState = appState;
		this.mlobj = mlobj;

		// start batttery receiver
		mlobj.registerReceiver(batteryReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));

		// start connection receiver
		IntentFilter intentFilter = new IntentFilter("android.net.conn.CONNECTIVITY_CHANGE");
		mlobj.registerReceiver(connectivityReceiver, intentFilter);

		battery_level = 0;
		charging_state = 0;
		wifi_status = ConnectionType.NONE;
	}

	public void updateBatteryCondition(int battery_level, int charging_state) {
		// textView.setText( "Battery level: "+ battery_level + "% Charging " +
		// charging_state);
		this.battery_level = battery_level;
		this.charging_state = charging_state;
	}

	public boolean isConnected() {
		NetworkInfo info = (NetworkInfo) ((ConnectivityManager) appState.getSystemService(Context.CONNECTIVITY_SERVICE))
				.getActiveNetworkInfo();

		if (info == null || !info.isConnected()) {
			return false;
		}
		if (info.isRoaming()) {
			// here is the roaming option you can change it if you want to
			// disable internet while roaming, just return false
			return false;
		}
		return true;
	}

	private final BroadcastReceiver batteryReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			int level = intent.getIntExtra("level", 0);
			int charging_state = intent.getIntExtra("plugged", 0);
			// Bundle charging_state = intent.getExtras();
			updateBatteryCondition(level, charging_state);
			
			startOrStopUploading();
		}
	};

	private final BroadcastReceiver connectivityReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {

			ConnectivityManager connec = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
			NetworkInfo info = connec.getActiveNetworkInfo();

			ConnectionType connType;
			if (info == null || !connec.getBackgroundDataSetting())
				connType = ConnectionType.NONE;

			else {
				int netType = info.getType();
				int netSubType = info.getSubtype();

				if (netType == ConnectivityManager.TYPE_WIFI)
					connType = ConnectionType.WIFI;
				else if (netSubType == ConnectivityManager.TYPE_MOBILE
						&& netSubType == TelephonyManager.NETWORK_TYPE_UMTS) // 3G
					connType = ConnectionType.MOBILE;
				else
					connType = ConnectionType.UNKNOWN;

			}

			wifi_status = connType;
			
			startOrStopUploading();
		}
	};
	
	private void startOrStopUploading() {
		/*
		 * stop uploading of huge files if we are not charging and not
		 * connected to a wifi network
		 */
		if (mlobj.uploadRunning == true && (charging_state == 0 || wifi_status != ConnectionType.WIFI)) {
			mlobj.getServiceController().stopUploadingIntellgent();
		} else if (isConnected()) {
			mlobj.getServiceController().startUploadingIntellgent();
		}
	}

	public enum ConnectionType {
		WIFI, MOBILE, UNKNOWN, NONE;
	}
}
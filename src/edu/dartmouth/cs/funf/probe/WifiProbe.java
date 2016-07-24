/**
 * Funf: Open Sensing Framework
 * Copyright (C) 2010-2011 Nadav Aharony, Wei Pan, Alex Pentland. 
 * Acknowledgments: Alan Gardner
 * Contact: nadav@media.mit.edu
 * 
 * This file is part of Funf.
 * 
 * Funf is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as 
 * published by the Free Software Foundation, either version 3 of 
 * the License, or (at your option) any later version. 
 * 
 * Funf is distributed in the hope that it will be useful, but 
 * WITHOUT ANY WARRANTY; without even the implied warranty of 
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  
 * See the GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public 
 * License along with Funf. If not, see <http://www.gnu.org/licenses/>.
 */
package edu.dartmouth.cs.funf.probe;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;
import android.os.Bundle;
import android.util.Log;
import edu.mit.media.funf.Utils;
import edu.mit.media.funf.probe.Probe;

public class WifiProbe extends Probe implements WiFiProbeKeys {

	public static final long DEFAULT_PERIOD = 60L * 5L;
	
	private static final String TAG = WifiProbe.class.getName();
	
	private WifiManager wifiManager;
	private int numberOfAttempts;
	private int previousWifiState;  // TODO: should this be persisted to disk?
	private BroadcastReceiver scanResultsReceiver;
	private WifiLock wifiLock;
	
	@Override
	public Parameter[] getAvailableParameters() {
		return new Parameter[] {
				new Parameter(Parameter.Builtin.PERIOD, DEFAULT_PERIOD),
				new Parameter(Parameter.Builtin.START, 0L),
				new Parameter(Parameter.Builtin.END, 0L)
		};
	}

	@Override
	public String[] getRequiredPermissions() {
		return new String[] {
			Manifest.permission.ACCESS_WIFI_STATE,
			Manifest.permission.CHANGE_WIFI_STATE,
		};
	}
	

	@Override
	public String[] getRequiredFeatures() {
		return new String[] {
				"android.hardware.wifi"
		};
	}
	
	@Override
	protected String getDisplayName() {
		return "Nearby Wifi Devices Probe";
	}

	@Override
	public void sendProbeData() {
		
		List<ScanResult> results = wifiManager.getScanResults();
		
		Set<String> set = new HashSet<String>();
		List<ScanResult> newList = new ArrayList<ScanResult>();
		   for (int i = 0; i<results.size();i++) {
			   String mac = results.get(i).BSSID;
			   String ssid = mac.substring(0,mac.length()-2);
		    if (set.add(ssid))
		     newList.add(results.get(i));
		   }
		
		if (newList != null) {
			for (int i = 0; i<newList.size();i++) {
				Bundle data = new Bundle();
				data.putString(BSSID, newList.get(i).BSSID);
				data.putString(SSID, newList.get(i).SSID);
				data.putString(CAPABILITY, newList.get(i).capabilities);
				data.putInt(FREQUENCY, newList.get(i).frequency);
				data.putInt(LEVEL, newList.get(i).level);
				sendProbeData(Utils.getTimestamp(), data);
			   }
		}
	}
	
	@Override
	protected void onEnable() {
		wifiManager = (WifiManager) this.getSystemService(Context.WIFI_SERVICE);
		numberOfAttempts = 0;
		scanResultsReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				if (WifiManager.SCAN_RESULTS_AVAILABLE_ACTION.equals(intent.getAction())) {
					sendProbeData();
					if (isRunning()) {
						stop();
					}
				}
			}
		};
		registerReceiver(scanResultsReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
	}
	
	@Override
	protected void onDisable() {
		unregisterReceiver(scanResultsReceiver);
	}

	@Override
	public void onRun(Bundle params) {
		acquireWifiLock();
		saveWifiStateAndRunScan();
	}
	
	private void saveWifiStateAndRunScan() {
		int state = wifiManager.getWifiState();
		if(state==WifiManager.WIFI_STATE_DISABLING ||state==WifiManager.WIFI_STATE_ENABLING){
			registerReceiver(new BroadcastReceiver() {
				@Override
				public void onReceive(Context ctx, Intent i) {
					if (WifiManager.WIFI_STATE_CHANGED_ACTION.equals(i.getAction())) {
						try {
							unregisterReceiver(this);  // TODO: sometimes this throws an IllegalArgumentException
							saveWifiStateAndRunScan();
						} catch (IllegalArgumentException e) {
							Log.e(TAG, "Unregistered WIFIE_STATE_CHANGED receiver more than once.");
						}
					}
				}
			}, new IntentFilter(WifiManager.WIFI_STATE_CHANGED_ACTION));
		} else {
			previousWifiState = state;
			runScan();
		}
	}
	
	private void loadPreviousWifiState() {
		// Enable wifi if previous sate was enabled, otherwise disable
		wifiManager.setWifiEnabled(previousWifiState == WifiManager.WIFI_STATE_ENABLED);
	}
	
	private void runScan() {
		numberOfAttempts += 1; 
		int state = wifiManager.getWifiState();
		if (state == WifiManager.WIFI_STATE_ENABLED) {
			boolean successfulStart = wifiManager.startScan();
			if (successfulStart) {
				Log.i(TAG, "WIFI scan started succesfully");
//				sendProbeData();
			} else {
				Log.e(TAG, "WIFI scan failed.");
			}
			numberOfAttempts = 0;
		} else if (numberOfAttempts <= 3) { 
			// Prevent infinite recursion by keeping track of number of attempts to change wifi state
			// TODO: investigate what is needed to keep Service alive while waiting for wifi state
			registerReceiver(new BroadcastReceiver() {
				@Override
				public void onReceive(Context ctx, Intent i) {
					if (WifiManager.WIFI_STATE_CHANGED_ACTION.equals(i.getAction())) {
						try {
							unregisterReceiver(this);
							runScan();
						} catch (IllegalArgumentException e) {
							// Not sure why, but sometimes this is not registered
							// Probably two intents at once
						}
					}
				}
			}, new IntentFilter(WifiManager.WIFI_STATE_CHANGED_ACTION));
			wifiManager.setWifiEnabled(true);
		} else {  // After 3 attempts stop trying
			// TODO: possibly send error
			stop();
		}
		
	}

	@Override
	public void onStop() {
		releaseWifiLock();
		loadPreviousWifiState();
	}

	private void acquireWifiLock() {
		wifiLock = wifiManager.createWifiLock(WifiManager.WIFI_MODE_SCAN_ONLY, TAG);
		wifiLock.setReferenceCounted(false);
		wifiLock.acquire();
	}
	
	private void releaseWifiLock() {
		if (wifiLock != null) {
			if (wifiLock.isHeld()) {
				wifiLock.release();
			}
			wifiLock = null;
		}
	}

}

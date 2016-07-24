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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Bundle;
import edu.mit.media.funf.Utils;
import edu.mit.media.funf.probe.Probe;

public class DebugProbe extends Probe implements DebugKeys {
	
	private BroadcastReceiver debugReceiver;
	private Boolean debugOn;
	public static final long DEFAULT_PERIOD = 60L * 30L;
	
	String index = "0";
	private int debug_counter = 0;
	private int charge_state = 0;
	public static final int repeat_time = 5;
	
	@Override
	public Parameter[] getAvailableParameters() {
		return new Parameter[] {
				new Parameter(Parameter.Builtin.PERIOD, DEFAULT_PERIOD),
				new Parameter(Parameter.Builtin.START, 0L),
				new Parameter(Parameter.Builtin.END, 0L)
		};
	}

	@Override
	public String[] getRequiredFeatures() {
		return new String[]{};
	}

	@Override
	public String[] getRequiredPermissions() {
		return new String[]{};
	}
	
	@Override
	protected String getDisplayName() {
		return "Debug On/Off State Probe";
	}

	@Override
	protected void onEnable() {
		debugOn = false;
		IntentFilter filter = new IntentFilter();
		filter.addAction(Intent.ACTION_BATTERY_CHANGED);

		debugReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				charge_state = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);

				if (charge_state == BatteryManager.BATTERY_PLUGGED_AC & debugOn == false) {
					debug_counter = 0;
					
				}else if (charge_state == BatteryManager.BATTERY_PLUGGED_USB & debugOn == false) {
					debug_counter = debug_counter + 1;
					if (debug_counter > repeat_time){
						debug_counter = 0;
						debugOn = true;
						sendProbeData();
					}
				} else if (charge_state == 0 & debugOn == true) {
					debugOn = false;
					sendProbeData();
				}
			}
		};
		getBaseContext().registerReceiver(debugReceiver, filter);
	}

	@Override
	protected void onDisable() {
		unregisterReceiver(debugReceiver);
	}


	@Override
	protected void onRun(Bundle params) {
		sendProbeData();
	}

	@Override
	protected void onStop() {
		// Only passive listener
	}

	@Override
	public void sendProbeData() {
		if (debugOn != null) {
			Bundle data = new Bundle();
			data.putBoolean(DEBUG_ON, debugOn);
			sendProbeData(Utils.getTimestamp(), data);
		}
	}
	

}

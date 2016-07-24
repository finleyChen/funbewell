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

import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.os.Bundle;
import edu.mit.media.funf.probe.SynchronousProbe;

public class RunningAppProcessInfoProbe extends SynchronousProbe implements RunningAppProcessKeys {
	
	@Override
	public String[] getRequiredPermissions() {
		return new String[] {
			android.Manifest.permission.GET_TASKS	
		};
	}

	@Override
	protected Bundle getData() {
		ActivityManager am = (ActivityManager)this.getApplicationContext().getSystemService(ACTIVITY_SERVICE);
		ArrayList<RunningAppProcessInfo> rp = (ArrayList)am.getRunningAppProcesses();
		int list_length = rp.size();
		RunningAppProcessInfo RunningProcess;
		ArrayList<String> RunningProcessArray = new ArrayList<String>(2);
		for (int i = 0; i<list_length; i++){
			RunningProcess = rp.get(i);
			RunningProcessArray.add(RunningProcess.processName);
		}
		
		
		Bundle data = new Bundle();
		data.putStringArrayList(RUNNING_PROCESS, RunningProcessArray);
		return data;
	}

	@Override
	protected long getDefaultPeriod() {
		return 30L;
	}
}

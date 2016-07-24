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

import java.text.Format;
import java.util.Calendar;

import android.database.Cursor;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.provider.CalendarContract.Calendars;
import android.text.format.DateFormat;
import edu.mit.media.funf.Utils;
import edu.mit.media.funf.probe.Probe;
import android.accounts.Account;
import android.accounts.AccountManager;

public class CalendarProbe extends Probe implements CalendarKeys {

	private Cursor mCursor = null;
	private static final String[] COLS = new String[]
			{ CalendarContract.Events.TITLE, CalendarContract.Events.DTSTART, CalendarContract.Events.DESCRIPTION, CalendarContract.Events.EVENT_LOCATION};
	
	String calendar_title;
	String calendar_description;
	String calendar_location;
	Long calendar_start;
	String calendar_date;
	String calendar_time;
	String current_date;
	int label;
	@Override
	public Parameter[] getAvailableParameters() {
		return new Parameter[] {
				new Parameter(Parameter.Builtin.PERIOD, 86400L),
		};
	}

	@Override
	public String[] getRequiredFeatures() {
		return new String[] {
				"android.hardware.bluetooth"
		};
	}

	@Override
	public String[] getRequiredPermissions() {
		return new String[] {
				android.Manifest.permission.WRITE_CALENDAR,
				android.Manifest.permission.READ_CALENDAR,	
		};
	}

	@Override
	protected String getDisplayName() {
		return "Calendar Probe";
	}

	@Override
	protected void onEnable() {

	}

	@Override
	protected void onDisable() {

	}

	@Override
	protected void onRun(Bundle params) {
		AccountManager accountManager =  AccountManager.get(getBaseContext()); 
		Account[] accounts = accountManager.getAccounts(); 
		String selection = "((" + Calendars.ACCOUNT_NAME + " = ?) AND (" 
				+ Calendars.ACCOUNT_TYPE + " = ?))";
		String possibleEmail = null;
		label = 0;
		for (Account account : accounts) {
		        possibleEmail = account.name;
		        String[] selectionArgs = new String[] {possibleEmail, "com.google"};
				mCursor = getContentResolver().query(
						CalendarContract.Events.CONTENT_URI, COLS, selection, selectionArgs, null);
				mCursor.moveToFirst();
				Format df = DateFormat.getDateFormat(this);
				Format tf = DateFormat.getTimeFormat(this);
				calendar_title = "N/A";
				calendar_start = 0L;
				Calendar cal = Calendar.getInstance();
				current_date = df.format(cal.getTime());
				try {
					while(true){
						calendar_title = mCursor.getString(0);
						calendar_start = mCursor.getLong(1);
						calendar_date = df.format(calendar_start);
						calendar_time = tf.format(calendar_start);
						calendar_description = mCursor.getString(2);
						calendar_location = mCursor.getString(3);
						if(calendar_date.equals(current_date)){
							label++;
							sendProbeData();
						}
						if (mCursor.isLast()){
							break;
						}else{
							mCursor.moveToNext();
						}
					}
				} catch (Exception e) {
					//ignore
				}
		}
		
	}


	@Override
	protected void onStop() {

	}


	@Override
	public void sendProbeData() {
		if (!calendar_title.equals("N/A")) {
			Bundle data = new Bundle();
			//			data.putString(TITLE, calendar_title);
			data.putString(DATE, calendar_date);
			data.putString(TIME, calendar_time);
			data.putInt(ACCOUNT_LABEL, label);
			//			data.putString(DESCRIPTION, calendar_description);
			//			data.putString(LOCATION, calendar_location);
			sendProbeData(Utils.getTimestamp(), data);
		}
	}

}

package org.bewellapp.ServiceControllers.AppLib;

import java.util.Calendar;
import java.util.concurrent.locks.ReentrantLock;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

public class AppStatsDBAdapter {

	private static final String TAG = "AppStatsDBAdapter";
	private static final ReentrantLock mLock = new ReentrantLock();

	public static final String TBL_PHONE_APPS_USAGE = "phone_usage";
	public static final String TBL_PHONE_TIME = "phone_timing";

	public static final String KEY_YEAR = "year";
	public static final String KEY_MONTH = "month";
	public static final String KEY_DAY = "day";
	public static final String KEY_HOUR = "hour";
	public static final String KEY_APP = "app";
	public static final String KEY_SECONDS = "seconds";

	public static final String KEY_PHONE_CALL = "phone_call";
	public static final String KEY_SOCIAL_APP = "social_app";
	public static final String KEY_OTHER_APP = "other_app";

	protected AppStatsDBHelper mDbHelper;
	protected Context mContext;
	protected SQLiteDatabase mDb;

	public AppStatsDBAdapter(Context context) {
		mContext = context;
	}

	public void addSecondsToApp(Calendar cal, String app, long numSeconds) {
		Log.d(TAG, String.format("Adding %d seconds to app %s", numSeconds, app));
		int year = cal.get(Calendar.YEAR);
		int month = cal.get(Calendar.MONTH) + 1;
		int day = cal.get(Calendar.DAY_OF_MONTH);
		int hour = cal.get(Calendar.HOUR_OF_DAY);
		String whereClause = String.format("year = %d and month = %d and day = %d and hour = %d and app = ?", year,
				month, day, hour);
		Cursor cursor = mDb.query(TBL_PHONE_APPS_USAGE, new String[] { KEY_SECONDS }, whereClause,
				new String[] { app }, null, null, null);
		int currSeconds = 0;
		boolean alreadyExists = false;
		if (cursor.moveToNext()) {
			currSeconds = cursor.getInt(0);
			alreadyExists = true;
		}
		cursor.close();
		currSeconds += numSeconds;
		if (alreadyExists) {
			ContentValues cv = new ContentValues();
			cv.put(KEY_SECONDS, currSeconds);
			int i = mDb.update(TBL_PHONE_APPS_USAGE, cv, whereClause, new String[] { app });
			Log.d(TAG, "Updated " + i + " rows");
		} else {
			String insertQuery = String.format(
					"insert into %s(year, month, day, hour, app, seconds) values (%d, %d, %d, %d, '%s', %d)",
					TBL_PHONE_APPS_USAGE, year, month, day, hour, app, currSeconds);
			mDb.execSQL(insertQuery);
		}
	}
	
	public void addSecondsToUsage(int year, int month, int day, PhoneUsageType put, long numSeconds) {
		Log.d(TAG, String.format("Adding %d seconds to usage %s", numSeconds, put));
		String whereClause = String.format("year = %d and month = %d and day = %d", year, month, day);
		String keyToUpdate = KEY_OTHER_APP;
		switch (put) {
		case USAGE_SOCIAL_APP:
			keyToUpdate = KEY_SOCIAL_APP;
			break;
		case USAGE_OTHER_APP:
			keyToUpdate = KEY_OTHER_APP;
			break;
		case USAGE_PHONE_CALL:
			keyToUpdate = KEY_PHONE_CALL;
		}
		Cursor cursor = mDb.query(TBL_PHONE_TIME, new String[] { keyToUpdate }, whereClause, null, null, null, null);
		int currSeconds = 0;
		boolean alreadyExists = false;
		if (cursor.moveToNext()) {
			boolean isNull = cursor.isNull(0);
			if (!isNull) {
				currSeconds = cursor.getInt(0);
			}
			alreadyExists = true;
		}
		cursor.close();
		currSeconds += numSeconds;
		if (alreadyExists) {
			ContentValues cv = new ContentValues();
			cv.put(keyToUpdate, currSeconds);
			mDb.update(TBL_PHONE_TIME, cv, whereClause, null);
		} else {
			String insertQuery = String.format("insert into %s(year, month, day, %s) values (%d, %d, %d, %d)",
					TBL_PHONE_TIME, keyToUpdate, year, month, day, currSeconds);
			mDb.execSQL(insertQuery);
		}
		cursor = mDb.query(TBL_PHONE_TIME, new String[] {"*"}, "1", null, null, null, null);
		while (cursor.moveToNext()) {
			UsageTimings u = UsageTimings.fromCursor(cursor);
			Log.d(TAG, u.toString());
		}
		cursor.close();
	}

	public void addSecondsToUsage(Calendar cal, PhoneUsageType put, long numSeconds) {
		int year = cal.get(Calendar.YEAR);
		int month = cal.get(Calendar.MONTH) + 1;
		int day = cal.get(Calendar.DAY_OF_MONTH);
		addSecondsToUsage(year, month, day, put, numSeconds);
	}

	public int deleteUsageStatsOlderThan(Calendar cal) {
		Log.d(TAG, String.format("Purging old usage timing stats"));
		int year = cal.get(Calendar.YEAR);
		int month = cal.get(Calendar.MONTH) + 1;
		int day = cal.get(Calendar.DAY_OF_MONTH);
		String whereClause = String.format("year <= %d and month <= %d and day < %d", year, month, day);
		int deleted = mDb.delete(TBL_PHONE_TIME, whereClause, null);
		return deleted;
	}

	public int deleteUsageStatsOlderThan(int year, int month, int day) {
		Log.d(TAG, String.format("Purging old usage timing stats"));
		String whereClause = String.format("year <= %d and month <= %d and day < %d", year, month, day);
		int deleted = mDb.delete(TBL_PHONE_TIME, whereClause, null);
		return deleted;
	}
	
	public int deleteUsageStatsOlderThanOrEqual(Calendar cal) {
		Log.d(TAG, String.format("Purging old usage timing stats"));
		int year = cal.get(Calendar.YEAR);
		int month = cal.get(Calendar.MONTH) + 1;
		int day = cal.get(Calendar.DAY_OF_MONTH);
		String whereClause = String.format("year <= %d and month <= %d and day <= %d", year, month, day);
		int deleted = mDb.delete(TBL_PHONE_TIME, whereClause, null);
		return deleted;
	}

	public int deleteUsageStatsOlderThanOrEqual(int year, int month, int day) {
		Log.d(TAG, String.format("Purging old usage timing stats"));
		String whereClause = String.format("year <= %d and month <= %d and day <=%d", year, month, day);
		int deleted = mDb.delete(TBL_PHONE_TIME, whereClause, null);
		return deleted;
	}
	
	public int deleteUsageStatsOlderThanOrEqual(UsageTimings ut) {
		return deleteUsageStatsOlderThanOrEqual(ut.year, ut.month, ut.day);
	}
	
	public boolean existsNewerStatData(int year, int month, int day) {
		String whereClause = String.format("year >= %d and month >= %d and day > %d", year, month, day);
		Cursor c = mDb.query(TBL_PHONE_TIME, new String[] { "*" }, whereClause, null, null, null, null);
		boolean newerdata = false;
		if (c.moveToNext()) {
			newerdata = true;
		}
		c.close();
		return newerdata;
	}
	
	public boolean existsNewerStatData(UsageTimings ut) {
		return existsNewerStatData(ut.year, ut.month, ut.day);
	}
	
	public int deleteUsageStatsOlderThan(UsageTimings ut) {
		return deleteUsageStatsOlderThan(ut.year, ut.month, ut.day);
	}

	public UsageTimings getOldestUsageTiming() {
		Cursor c = mDb.query(TBL_PHONE_TIME, new String[] { "*" }, null, null, null, null,
				"year asc, month asc, day asc");
		UsageTimings result = null;
		if (c.moveToNext()) {
			result = UsageTimings.fromCursor(c);
		}
		c.close();
		return result;
	}

	public AppStatsDBAdapter open() {
		mLock.lock();
		mDbHelper = new AppStatsDBHelper(mContext);
		mDb = mDbHelper.getWritableDatabase();
		return this;
	}

	public void close() {
		mDbHelper.close();
		mLock.unlock();
	}

	public static enum PhoneUsageType {
		USAGE_PHONE_CALL, USAGE_SOCIAL_APP, USAGE_OTHER_APP;
	}

	public static class UsageTimings {
		public int phone_calls;
		public int social_apps;
		public int other_apps;

		public int year;
		public int month;
		public int day;
		
		public static UsageTimings fromCursor(Cursor c) {
			UsageTimings result = new UsageTimings();
			result.year = c.getInt(0);
			result.month = c.getInt(1);
			result.day = c.getInt(2);
			result.phone_calls = 0;
			result.social_apps = 0;
			result.other_apps = 0;
			if (!c.isNull(3)) {
				result.phone_calls = c.getInt(3);
			}
			if (!c.isNull(4)) {
				result.social_apps = c.getInt(4);
			}
			if (!c.isNull(5)) {
				result.other_apps = c.getInt(5);
			}
			return result;
		}
		
		@Override
		public String toString() {
			String s = String.format("%d-%d-%d Phone calls: %d - Social apps: %d - Other apps: %d", year, month, day, phone_calls, social_apps, other_apps);
			return s;
		}
	}
}

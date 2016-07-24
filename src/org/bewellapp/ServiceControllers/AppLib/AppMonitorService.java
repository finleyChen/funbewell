package org.bewellapp.ServiceControllers.AppLib;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicBoolean;

import org.bewellapp.Ml_Toolkit_Application;
import org.bewellapp.ServiceControllers.AppLib.AppStatsDBAdapter.PhoneUsageType;
import org.bewellapp.Storage.ML_toolkit_object;

import android.app.ActivityManager;
import android.app.Service;
import android.app.ActivityManager.RunningTaskInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

public class AppMonitorService extends Service {

	public static final String START_ACTION = "org.bewellapp.ServiceControllers.AppLib.AppMonitorService.START";
	public static final String STOP_ACTION = "org.bewellapp.ServiceControllers.AppLibAppMonitorService.STOP";
	public static final int ACTIVITY_POLL_INTERVAL_MSEC = 2000;
	public static final int WRITE_TO_DB_MSEC_INTERVAL = 5000;

	private static final String LOGTAG = "ApplicationMonitoring";
	private static final AtomicBoolean sIsRunning = new AtomicBoolean(false);

	private AtomicBoolean mRunMonitor;
	private ActivityManager mActivityManager;
	private Thread mMonitoringThread;
	private Ml_Toolkit_Application appState;

	private Map<String, Integer> mAppUsageTime;

	public static final Set<String> social_apps;

	static {
		social_apps = new TreeSet<String>();
		social_apps.add("com.android.mms");
		social_apps.add("com.jb.gosms");
		social_apps.add("com.whatsapp");
		social_apps.add("com.google.android.email");
		social_apps.add("com.facebook.katana");
		social_apps.add("com.facebook.orca");
		social_apps.add("com.google.android.gm");
		social_apps.add("com.skype");
		social_apps.add("com.google.android.talk");
		social_apps.add("com.google.android.apps.googlevoice");
		social_apps.add("com.google.android.apps.plus");
		social_apps.add("com.twitter.android");
		social_apps.add("com.fsck.k9");
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	public static boolean isRunning() {
		return sIsRunning.get();
	}

	@Override
	public void onCreate() {
		Log.i(LOGTAG, "onCreate()");
		appState = (Ml_Toolkit_Application) getApplicationContext();
		try {
			sIsRunning.set(true);
			mRunMonitor = new AtomicBoolean(true);
			if (null == mMonitoringThread) {
				mMonitoringThread = new Thread(new TopActivityPoller(), "AppMonitorService monitoring thread");
				mMonitoringThread.start();
			}
			mAppUsageTime = new TreeMap<String, Integer>();
		} catch (Exception e) {
			Log.e(LOGTAG, e.getMessage());
			e.printStackTrace();
			stopSelf();
		}

	}

	@Override
	public void onDestroy() {
		mRunMonitor.set(false);
		mMonitoringThread.interrupt();
		try {
			mMonitoringThread.join(3000);
		} catch (InterruptedException e) {
			Toast.makeText(getApplicationContext(),
					String.format("Error while shutting down ActivityMonitor: %s", e.getLocalizedMessage()),
					Toast.LENGTH_SHORT).show();
		}
		saveMapToDb(new GregorianCalendar());
		mMonitoringThread = null;
		sIsRunning.set(false);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		// If we get killed, after returning from here, restart
		return START_STICKY;

	}

	/**
	 * Gets the foreground task.
	 * 
	 * @return The foreground task or null on exception.
	 */
	private RunningTaskInfo getForegroundTask() {
		ActivityManager.RunningTaskInfo result = null;
		List<ActivityManager.RunningTaskInfo> runningTasks = getActivityManager().getRunningTasks(1);
		if (runningTasks.size() > 0) {
			result = runningTasks.get(0);
		}
		return result;
	}

	private void addSeconds(String app, int seconds) {
		if (mAppUsageTime == null) {
			mAppUsageTime = new TreeMap<String, Integer>();
		}
		Integer oldSeconds = mAppUsageTime.get(app);
		if (oldSeconds == null) {
			mAppUsageTime.put(app, seconds);
		} else {
			mAppUsageTime.put(app, oldSeconds + seconds);
		}
	}

	private void addSocialSeconds(String app, int seconds) {
		if (social_apps.contains(app)) {
			SharedPreferences sp = getSharedPreferences(Ml_Toolkit_Application.SHARED_PREF, MODE_PRIVATE);
			int numsec = sp.getInt(Ml_Toolkit_Application.SP_SOCIAL_APPS_SECONDS, 0);
			numsec += seconds;
			SharedPreferences.Editor editor = sp.edit();
			editor.putInt(Ml_Toolkit_Application.SP_SOCIAL_APPS_SECONDS, numsec);
			editor.commit();
		}
	}

	private void saveMapToDb(Calendar time) {
		if (mAppUsageTime == null) {
			mAppUsageTime = new TreeMap<String, Integer>();
		}
		AppStatsDBAdapter appsDb = new AppStatsDBAdapter(this);
		appsDb.open();
		try {
			for (String app : mAppUsageTime.keySet()) {
				int numSec = mAppUsageTime.get(app);
				/*
				 * Uncomment the following line to enable fine-graned
				 * application usage monitoring.
				 */
				// appsDb.addSecondsToApp(time, app, numSec);

				PhoneUsageType put = PhoneUsageType.USAGE_OTHER_APP;
				if (social_apps.contains(app)) {
					put = PhoneUsageType.USAGE_SOCIAL_APP;
				}
				appsDb.addSecondsToUsage(time, put, numSec);
			}
		} finally {
			appsDb.close();
		}
		mAppUsageTime.clear();
	}

	private ActivityManager getActivityManager() {
		if (mActivityManager == null) {
			mActivityManager = (ActivityManager) getApplicationContext().getSystemService(Context.ACTIVITY_SERVICE);
		}
		return mActivityManager;
	}

	/**
	 * This thread polls for the top running activity.
	 */
	private class TopActivityPoller implements Runnable {

		Calendar startTime;

		@Override
		public void run() {
			startTime = new GregorianCalendar();
			// run while mRunMonitoring is true
			while (mRunMonitor.get()) {
				// get the foreground application and activity
				RunningTaskInfo fgApp = getForegroundTask();
				ComponentName fgAct = null;
				if (null != fgApp) {
					fgAct = fgApp.topActivity;
				}

				/*
				 * Log the foreground application and activity if: a) we
				 * successfully got the foreground application b) we
				 * successfully got the foreground activity
				 */
				if (fgApp != null && fgAct != null) {
					String activeApp = fgApp.baseActivity.getPackageName();
					String activeAct = fgAct.getClassName();

					long currentTimestamp = System.currentTimeMillis();
					String taskDumpString = String.format("ActiveApp:%s ActiveActivity:%s", activeApp, activeAct);
					ML_toolkit_object obj = appState.mMlToolkitObjectPool.borrowObject();
					Log.d(LOGTAG, taskDumpString);
					obj.setValues(currentTimestamp, 22, taskDumpString.getBytes());
					appState.ML_toolkit_buffer.insert(obj);

					addSeconds(activeApp, ACTIVITY_POLL_INTERVAL_MSEC / 1000);
					addSocialSeconds(activeApp, ACTIVITY_POLL_INTERVAL_MSEC / 1000);
				}

				GregorianCalendar now = new GregorianCalendar();
				if ((now.getTimeInMillis() - startTime.getTimeInMillis()) > WRITE_TO_DB_MSEC_INTERVAL) {
					saveMapToDb(startTime);
					startTime = now;
				}
				sleepForMillis(ACTIVITY_POLL_INTERVAL_MSEC);
			}
		}

		private void sleepForMillis(long duration) {
			long endTime = System.currentTimeMillis() + duration;
			while (System.currentTimeMillis() < endTime) {
				try {
					Thread.sleep(endTime - System.currentTimeMillis());
				} catch (InterruptedException e) {
					if (!mRunMonitor.get())
						break;
				}
			}
		}
	}
}

package org.bewellapp.ServiceControllers.UploadingLib;

//import android.os.PowerManager;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream; //import java.io.IOException;  
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import android.app.Service;
import android.widget.RemoteViews;
import android.os.IBinder;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.util.Log;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.GregorianCalendar;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.ContentBody;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.SingleClientConnManager;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.util.EntityUtils;
import org.bewellapp.Ml_Toolkit_Application;
import org.bewellapp.ServiceControllers.AppLib.AppStatsDBAdapter;
import org.bewellapp.ServiceControllers.AppLib.AppStatsDBAdapter.UsageTimings;
import org.bewellapp.ServiceControllers.UploadingLib.InfoObject.ConnectionType;
import org.bewellapp.wallpaper.WellnessSummaryActivity;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Days;
import org.json.JSONException;
import org.json.JSONObject;

//import com.example.android.apis.app.AccelerometerManager;

// Need the following import to get access to the app resources, since this
// class is in a sub-package.
import org.bewellapp.R;

public class UploadingServiceIntelligent extends Service {
	private static Context CONTEXT;

	@SuppressWarnings("rawtypes")
	private static final Class[] mStartForegroundSignature = new Class[] { int.class, Notification.class };
	@SuppressWarnings("rawtypes")
	private static final Class[] mStopForegroundSignature = new Class[] { boolean.class };

	public static final String ACTION_FOREGROUND = "org.bewellapp.ServiceControllers.UploadingLib.FOREGROUND";
	public static final String ACTION_BACKGROUND = "org.bewellapp.ServiceControllers.UploadingLib.BACKGROUND";

	private static NotificationManager mNM;
	private Method mStartForeground;
	private Method mStopForeground;

	private Object[] mStartForegroundArgs = new Object[2];
	private Object[] mStopForegroundArgs = new Object[1];

	public static boolean Foreground_on;

	public long no_of_records;
	public static int curr_no_of_records;
	public static boolean Activity_on;
	private static Notification notification;
	private RemoteViews contentView;
	// private RehearsalAudioRecorder ar;
	private Ml_Toolkit_Application appState;

	public static final String BEWELL_UPLOAD_FILENAME = "wellness_upload.txt";
	public static final String TAG = "UploadingService";
	public static final ReentrantLock scoreFileLock = new ReentrantLock();

	// binder
	// private final IBinder binder = new AudioBinder();

	// audio status
	public String inferred_audio_Status = "No inference available";
	private Thread t;
	private boolean stopUploadingForce = false;

	/** Called when the activity is first created. */
	@Override
	public void onCreate() {

		// screen will stay on during this section
		appState = (Ml_Toolkit_Application) getApplicationContext();

		no_of_records = 0;
		// curr_no_of_records = 0;
		CONTEXT = this;

		// for making sure while loop executes
		stopUploadingForce = false;

		mNM = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		try {
			mStartForeground = getClass().getMethod("startForeground", mStartForegroundSignature);
			mStopForeground = getClass().getMethod("stopForeground", mStopForegroundSignature);
		} catch (NoSuchMethodException e) {
			// Running on an older platform.
			mStartForeground = mStopForeground = null;
		}

		Activity_on = true;
		Foreground_on = true;
	}

	public static Context getContext() {
		return CONTEXT;
	}

	// This is the old onStart method that will be called on the pre-2.0
	// platform. On 2.0 or later we override onStartCommand() so this
	// method will not be called.
	@Override
	public void onStart(Intent intent, int startId) {
		handleCommand(intent);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		if (intent == null) {
			stopSelf();
			return START_NOT_STICKY;
		}

		handleCommand(intent);
		// We want this service to continue running until it is explicitly
		// stopped, so return sticky.
		// /return START_REDELIVER_INTENT;
		// return START_STICKY;
		return START_NOT_STICKY;
	}

	void handleCommand(Intent intent) {
		if (ACTION_FOREGROUND.equals(intent.getAction())) {

			contentView = new RemoteViews(getPackageName(), R.layout.notification_layout);
			notification = new Notification(R.drawable.notification_fish, getString(R.string.app_name), System.currentTimeMillis());

			notification.contentView = contentView;
			PendingIntent contentIntent = PendingIntent.getActivity(this, 0, new Intent(this,
					WellnessSummaryActivity.class), 0);

			notification.contentIntent = contentIntent;

			startForegroundCompat(R.string.CUSTOM_VIEW, notification);
			
			try {
				t = new Thread() {
					public void run() {
						if (!appState.uploadRunning) {
							appState.uploadRunning = true;
							try {
								startUploading();
							} finally {
								appState.uploadRunning = false;
							}
						}
					}

				};
				t.start();
			} catch (Exception ex) {
				ex.printStackTrace();
			}

		} else if (ACTION_BACKGROUND.equals(intent.getAction())) {
			stopForegroundCompat(R.string.foreground_service_started_aud);
			// stopForegroundCompat(2);

		}
	}

	/**
	 * This is a wrapper around the new startForeground method, using the older
	 * APIs if it is not available.
	 */
	void startForegroundCompat(int id, Notification notification) {
		// If we have the new startForeground API, then use it.
		Foreground_on = true;
		if (mStartForeground != null) {
			mStartForegroundArgs[0] = Integer.valueOf(id);
			mStartForegroundArgs[1] = notification;
			try {
				mStartForeground.invoke(this, mStartForegroundArgs);
			} catch (InvocationTargetException e) {
				// Should not happen.
				Log.w("ApiDemos", "Unable to invoke startForeground", e);
			} catch (IllegalAccessException e) {
				// Should not happen.
				Log.w("ApiDemos", "Unable to invoke startForeground", e);
			}
			return;
		}

		// Fall back on the old API.
//		setForeground(true);
//		mNM.notify(id, notification);
		Log.e("NOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOO", "NOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOO");
	}

	/**
	 * This is a wrapper around the new stopForeground method, using the older
	 * APIs if it is not available.
	 */
	void stopForegroundCompat(int id) {
		Foreground_on = false;
		// If we have the new stopForeground API, then use it.
		if (mStopForeground != null) {
			mStopForegroundArgs[0] = Boolean.TRUE;
			try {
				mStopForeground.invoke(this, mStopForegroundArgs);
			} catch (InvocationTargetException e) {
				// Should not happen.
				Log.w("ApiDemos", "Unable to invoke stopForeground", e);
			} catch (IllegalAccessException e) {
				// Should not happen.
				Log.w("ApiDemos", "Unable to invoke stopForeground", e);
			}
			return;
		}

		// Fall back on the old API. Note to cancel BEFORE changing the
		// foreground state, since we could be killed at that point.
//		mNM.cancel(id);
//		setForeground(false);
		Log.e("NOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOO", "NOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOO");
	}

	@Override
	public void onDestroy() {
		// Make sure our notification is gone.
		try {
			stopForegroundCompat(R.string.foreground_service_started_upload);
		} catch (Exception ex) {
		}
		// stopForegroundCompat(2);

		// for making sure while loop executes
		stopUploadingForce = true;
		appState.uploadRunning = false;
	}

	@Override
	public IBinder onBind(Intent intent) {
		// return binder;
		return null;
	}

	public void updateNotificationArea() {

		// Set the info for the views that show in the notification panel.
		/*
		 * PendingIntent contentIntent = PendingIntent.getActivity(this, 0, new
		 * Intent(this, org.bewellapp.main_activity.class), 0);
		 */
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
				new Intent(this, WellnessSummaryActivity.class), 0);

		// notification.setLatestEventInfo(this, "Audio Test ", text,
		// contentIntent);

		// Set the info for the views that show in the notification panel.

		// notification.setLatestEventInfo(this, "Uploading", text
		// + " " + no_of_records, contentIntent);

		/*
		 * SharedPreferences sp =
		 * getSharedPreferences(Ml_Toolkit_Application.SHARED_PREF,
		 * MODE_PRIVATE); if
		 * (sp.getBoolean(Ml_Toolkit_Application.SP_ISREGISTERED, false)) {
		 * notification.setLatestEventInfo(this, "Uploading", "Remaining " +
		 * appState.fileList.size() + "MB", contentIntent); } else {
		 * notification.setLatestEventInfo(this, "Cleaning up", "Remaining " +
		 * appState.fileList.size() + " data files", contentIntent); }
		 * 
		 * mNM.notify(R.string.foreground_service_started_upload, notification);
		 */

		contentView = new RemoteViews(getPackageName(), R.layout.notification_layout);
		notification.contentView = contentView;
		notification.contentIntent = contentIntent;

		mNM.notify(R.string.CUSTOM_VIEW, notification);

		// }
	}

	private void startUploading() {

		SharedPreferences sp = getSharedPreferences(Ml_Toolkit_Application.SHARED_PREF, MODE_PRIVATE);
		if (sp.getBoolean(Ml_Toolkit_Application.SP_ISREGISTERED, false)) {

			if (appState.infoObj.isConnected()) {
				uploadScores();
				uploadPhoneStats();
			}

			if (appState.infoObj.charging_state == 0 || appState.infoObj.wifi_status != ConnectionType.WIFI
					|| appState.fileList.size() == 0) {
				return;
			}

			appState.uploadRunning = true;

			do {
				// at least one file is there
				if (appState.SDCardMounted == true) {
					return;
				}
				
				
				File file = null;

				synchronized (appState.fileList) {
					if (appState.fileList.size() > 0) {
						file = new File(appState.fileList.get(0));
						if (!file.exists()) {
							try {
								appState.fileList.remove(0);
							} catch (Exception e) {
							}
							return;
						}
					} else {
						return;
					}
				}
				
				
				HttpClient httpclient = new HttpClientLooseSSL();
				String uploadUrl = sp.getString(Ml_Toolkit_Application.SP_WEB_UPLOAD_DB_URL, Ml_Toolkit_Application.DEFAULT_SP_UPLOAD_DB_URL);
				String completeUrl = String.format("%s/%s/%s", uploadUrl, getUsername(), getPassword());
				HttpPost httpPost = new HttpPost(completeUrl);
				FileBody bin = new FileBody(file);
				String filename = file.getName();
				try {
					StringBody comment = new StringBody("Filename: " + filename);

					MultipartEntity reqEntity = new MultipartEntity();
					reqEntity.addPart("data", bin);
					reqEntity.addPart("comment", comment);
					httpPost.setEntity(reqEntity);

					ResponseHandler<String> responseHandler = new BasicResponseHandler();
					String response = httpclient.execute(httpPost, responseHandler);
					JSONObject result = new JSONObject(response);
					//Log.i(TAG, result.toString());
					synchronized (appState.fileList) {
						if (appState.fileList.size() > 0) {
							// delete file from the list
							appState.fileList.remove(0);
							if(result.getString("result").equals("SUCCESS")) {
							    file.delete();
							}
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
					break;
				}

			} while (appState.fileList.size() != 0 && stopUploadingForce == false);
		} else {
//			Log.i(TAG, "User not registered: deleting score and data files");
//			try {
//				deleteFile(BEWELL_UPLOAD_FILENAME);
//			} catch (Exception e) {
//			}
//
//			AppStatsDBAdapter db = new AppStatsDBAdapter(this).open();
//			try {
//				db.deleteUsageStatsOlderThan(new GregorianCalendar());
//			} finally {
//				db.close();
//			}
//
//			for (String s : appState.fileList) {
//				File file = new File(s);
//				try {
//					file.delete();
//				} catch (Exception e) {
//				}
//			}
//			appState.fileList.clear();
		}

	}

	/**
	 * Uploads wellness score to the bewellapp website.
	 */
	private void uploadScores() {

		/*
		 * Check if the score file exists. If not, return.
		 */
		File scoreFile = getFileStreamPath(BEWELL_UPLOAD_FILENAME);
		if (!scoreFile.exists()) {
			return;
		}

		SharedPreferences sp = getSharedPreferences(Ml_Toolkit_Application.SHARED_PREF, MODE_PRIVATE);
		boolean uploadScores = sp.getBoolean(Ml_Toolkit_Application.SP_UPLOAD_SCORES, true);

		if (!uploadScores) {
			scoreFileLock.lock();
			try {
				deleteFile(BEWELL_UPLOAD_FILENAME);
			} catch (Exception e) {

			} finally {
				scoreFileLock.unlock();
			}
			return;
		}

		List<Integer> physical = new LinkedList<Integer>();
		List<Integer> social = new LinkedList<Integer>();
		List<Integer> sleep = new LinkedList<Integer>();
		List<Double> latitude = new LinkedList<Double>();
		List<Double> longitude = new LinkedList<Double>();
		List<Long> timemillis = new LinkedList<Long>();
		DateTimeZone timezone = DateTimeZone.getDefault();
		DateTime now = new DateTime();

		boolean corruptedFile = false;

		scoreFileLock.lock();
		try {
			FileInputStream fis = openFileInput(BEWELL_UPLOAD_FILENAME);
			DataInputStream dis = new DataInputStream(fis);
			while (true) {
				try {
					int physicalscore = dis.readInt();
					int socialscore = dis.readInt();
					int sleepscore = dis.readInt();
					double latitudepos = dis.readDouble();
					double longitudepos = dis.readDouble();
					long time = dis.readLong();
					DateTime target = new DateTime(time, timezone);
					int daysdiff = Days.daysBetween(now, target).getDays();
					if (daysdiff > 30 || now.isBefore(target)) {
						Log.w(TAG, String.format("Read timestamp %d: corrupted file", time));
						corruptedFile = true;
						break;
					}
					physical.add(physicalscore);
					social.add(socialscore);
					sleep.add(sleepscore);
					latitude.add(latitudepos);
					longitude.add(longitudepos);
					timemillis.add(time);
				} catch (IOException e) {
					break;
				}
			}
			dis.close();
			if (!corruptedFile
					&& (physical.size() == social.size() && social.size() == sleep.size()
							&& sleep.size() == timemillis.size() && timemillis.size() == longitude.size() && longitude
							.size() == latitude.size())) {
				corruptedFile = false;
			} else {
				corruptedFile = true;
			}

			if (corruptedFile == false) {
				while (physical.size() > 0) {
					int physicalscore = physical.get(0);
					int socialscore = social.get(0);
					int sleepscore = sleep.get(0);
					double latitudepos = latitude.get(0);
					double longitudepos = longitude.get(0);
					long time = timemillis.get(0);
					UploadResult result = uploadScore(physicalscore, socialscore, sleepscore, latitudepos,
							longitudepos, time);
					if (result == UploadResult.UNSUCCESSFUL_DELETE || result == UploadResult.SUCCESSFUL) {
						physical.remove(0);
						social.remove(0);
						sleep.remove(0);
						timemillis.remove(0);
						latitude.remove(0);
						longitude.remove(0);
					}
					if (result == UploadResult.UNSUCCESSFUL_DONOT_DELETE) {
						break;
					}
				}

				Log.d(TAG, "Resetting score file");
				deleteFile(BEWELL_UPLOAD_FILENAME);

				if (physical.size() > 0) {
					/* write data not uploaded back to file */
					FileOutputStream fos = openFileOutput(BEWELL_UPLOAD_FILENAME, MODE_PRIVATE);
					DataOutputStream dos = null;
					try {
						dos = new DataOutputStream(fos);
						for (int i = 0; i < physical.size(); i++) {
							dos.writeInt(physical.get(i));
							dos.writeInt(social.get(i));
							dos.writeInt(sleep.get(i));
							dos.writeDouble(latitude.get(i));
							dos.writeDouble(longitude.get(i));
							dos.writeLong(timemillis.get(i));
							dos.flush();
						}
					} finally {
						if (dos != null) {
							dos.close();
						}
					}
				}
			} else {
				throw new IOException("Score list sizes not coherent");
			}

		} catch (Exception e) {
			Log.e(TAG, "Detected error in the wellness upload file, deleting it.");
			Log.e(TAG, e.getMessage());
			deleteFile(BEWELL_UPLOAD_FILENAME);
		} finally {
			scoreFileLock.unlock();
		}
	}

	private void uploadPhoneStats() {
		AppStatsDBAdapter db = new AppStatsDBAdapter(this);
		db.open();
		boolean networkError = true;
		try {
			UsageTimings ut = null;
			do {
				ut = db.getOldestUsageTiming();
				if (ut != null) {
					UploadResult ur = uploadPhoneStats(ut.year, ut.month, ut.day, ut.phone_calls, ut.social_apps,
							ut.other_apps);
					switch (ur) {
					case SUCCESSFUL:
						if (db.existsNewerStatData(ut)) {
							db.deleteUsageStatsOlderThanOrEqual(ut);
						}
						networkError = false;
						break;
					case UNSUCCESSFUL_DELETE:
						if (db.existsNewerStatData(ut)) {
							db.deleteUsageStatsOlderThanOrEqual(ut);
						}
						networkError = false;
						break;
					case UNSUCCESSFUL_DONOT_DELETE:
						networkError = true;
					}
				}
			} while (ut != null && (networkError == false) && db.existsNewerStatData(ut));
		} finally {
			db.close();
		}
	}

	/**
	 * Uploads a single score to the bewellapp website.
	 * 
	 * @param physical
	 * @param social
	 * @param sleep
	 * @param timestamp
	 * @return
	 */
	private UploadResult uploadScore(int physical, int social, int sleep, double latitude, double longitude,
			long timestamp) {
		String timezone = DateTimeZone.getDefault().getID();
		String username = getUsername();
		String password = getPassword();
		Log.d(TAG, String.format(
				"Uploading score: physical %d, social %d, sleep %d, latitude %f, longitude %f, timestamp %d", physical,
				social, sleep, latitude, longitude, timestamp));
		String service = getSharedPreferences(Ml_Toolkit_Application.SHARED_PREF, MODE_PRIVATE).getString(
				Ml_Toolkit_Application.SP_WEB_SCORES_URL, Ml_Toolkit_Application.DEFAULT_SP_ADDSCORES_URL);
		JSONObject holder = new JSONObject();
		try {
			holder.put("email", username);
			holder.put("password", password);
			holder.put("timestamp", timestamp);
			holder.put("social", social);
			holder.put("physical", physical);
			holder.put("sleep", sleep);
			holder.put("latitude", latitude);
			holder.put("longitude", longitude);
			holder.put("timezone", timezone);
		} catch (JSONException e) {
			e.printStackTrace();
			return UploadResult.UNSUCCESSFUL_DELETE;
		}
		DefaultHttpClient httpClient = new DefaultHttpClient();
		HttpPost httpPost = new HttpPost(service);
		StringEntity se = null;
		try {
			se = new StringEntity(holder.toString());
			Log.i(TAG, "Sending score: " + holder.toString());
		} catch (UnsupportedEncodingException e) {
			return UploadResult.UNSUCCESSFUL_DELETE;
		}
		httpPost.setEntity(se);
		httpPost.setHeader("Accept", "application/json");
		httpPost.setHeader("Content-type", "application/json");
		ResponseHandler<String> responseHandler = new BasicResponseHandler();
		String response;
		try {
			response = httpClient.execute(httpPost, responseHandler);
		} catch (ClientProtocolException e) {
			e.printStackTrace();
			return UploadResult.UNSUCCESSFUL_DONOT_DELETE;
		} catch (IOException e) {
			return UploadResult.UNSUCCESSFUL_DONOT_DELETE;
		}
		JSONObject result;
		try {
			result = new JSONObject(response);
		} catch (JSONException e) {
			e.printStackTrace();
			return UploadResult.UNSUCCESSFUL_DONOT_DELETE;
		}
		Log.i(TAG, "Score upload result: " + result.toString());
		try {
			if (result.getString("result").equals("SUCCESS")) {
				return UploadResult.SUCCESSFUL;
			} else {
				return UploadResult.UNSUCCESSFUL_DELETE;
			}
		} catch (Exception e) {
			e.printStackTrace();
			return UploadResult.UNSUCCESSFUL_DELETE;
		}
	}

	private UploadResult uploadPhoneStats(int year, int month, int day, int phone_calls, int social_apps, int other_apps) {
		String timezone = DateTimeZone.getDefault().getID();
		String username = getUsername();
		String password = getPassword();
		String service = getSharedPreferences(Ml_Toolkit_Application.SHARED_PREF, MODE_PRIVATE).getString(
				Ml_Toolkit_Application.SP_WEB_PHONESTATS_URL, Ml_Toolkit_Application.DEFAULT_SP_ADDPHONESTATS_URL);
		JSONObject holder = new JSONObject();
		try {
			holder.put("email", username);
			holder.put("password", password);
			holder.put("timezone", timezone);
			holder.put("year", year);
			holder.put("month", month);
			holder.put("day", day);
			holder.put("phonecalls", phone_calls);
			holder.put("socialapps", social_apps);
			holder.put("otherapps", other_apps);
		} catch (JSONException e) {
			e.printStackTrace();
			return UploadResult.UNSUCCESSFUL_DELETE;
		}
		DefaultHttpClient httpClient = new DefaultHttpClient();
		HttpPost httpPost = new HttpPost(service);
		StringEntity se = null;
		try {
			se = new StringEntity(holder.toString());
			Log.i(TAG, "Sending score: " + holder.toString());
		} catch (UnsupportedEncodingException e) {
			return UploadResult.UNSUCCESSFUL_DELETE;
		}
		httpPost.setEntity(se);
		httpPost.setHeader("Accept", "application/json");
		httpPost.setHeader("Content-type", "application/json");
		ResponseHandler<String> responseHandler = new BasicResponseHandler();
		String response;
		try {
			response = httpClient.execute(httpPost, responseHandler);
		} catch (ClientProtocolException e) {
			e.printStackTrace();
			return UploadResult.UNSUCCESSFUL_DONOT_DELETE;
		} catch (IOException e) {
			return UploadResult.UNSUCCESSFUL_DONOT_DELETE;
		}
		JSONObject result;
		try {
			result = new JSONObject(response);
		} catch (JSONException e) {
			e.printStackTrace();
			return UploadResult.UNSUCCESSFUL_DONOT_DELETE;
		}
		Log.i(TAG, "Score upload result: " + result.toString());
		try {
			if (result.getString("result").equals("SUCCESS")) {
				return UploadResult.SUCCESSFUL;
			} else {
				return UploadResult.UNSUCCESSFUL_DELETE;
			}
		} catch (Exception e) {
			e.printStackTrace();
			return UploadResult.UNSUCCESSFUL_DELETE;
		}
	}

	private String getUsername() {
		SharedPreferences sp = getSharedPreferences(Ml_Toolkit_Application.SHARED_PREF, MODE_PRIVATE);
		return sp.getString(Ml_Toolkit_Application.SP_USERNAME, "john@sample.com");
	}

	private String getPassword() {
		SharedPreferences sp = getSharedPreferences(Ml_Toolkit_Application.SHARED_PREF, MODE_PRIVATE);
		return sp.getString(Ml_Toolkit_Application.SP_PASSWORD, "password");
	}

	private static enum UploadResult {
		SUCCESSFUL, UNSUCCESSFUL_DELETE, UNSUCCESSFUL_DONOT_DELETE;
	}

	private static class MySSLSocketFactory extends SSLSocketFactory {
		SSLContext sslContext = SSLContext.getInstance("SSL"); // or TLS

		public MySSLSocketFactory(KeyStore truststore) throws NoSuchAlgorithmException, KeyManagementException,
				KeyStoreException, UnrecoverableKeyException {
			super(truststore);

			TrustManager tm = new X509TrustManager() {

				@Override
				public X509Certificate[] getAcceptedIssuers() {
					return null;
				}

				@Override
				public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {

				}

				@Override
				public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
				}
			};

			sslContext.init(null, new TrustManager[] { tm }, null);
		}

		@Override
		public Socket createSocket(Socket socket, String host, int port, boolean autoClose) throws IOException,
				UnknownHostException {
			return sslContext.getSocketFactory().createSocket(socket, host, port, autoClose);
		}

		@Override
		public Socket createSocket() throws IOException {
			return sslContext.getSocketFactory().createSocket();
		}
	}

	private class HttpClientLooseSSL extends DefaultHttpClient {

		@Override
		protected ClientConnectionManager createClientConnectionManager() {
			SchemeRegistry registry = new SchemeRegistry();
			KeyStore trustStore = null;
			try {
				trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
			} catch (KeyStoreException e) {
				e.printStackTrace();
			}
			try {
				trustStore.load(null, null);
			} catch (IOException e) {
				e.printStackTrace();
			} catch (NoSuchAlgorithmException e) {
				e.printStackTrace();
			} catch (CertificateException e) {
				e.printStackTrace();
			}
			registry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
			// Register for port 443 our SSLSocketFactory with our keystore
			// to the ConnectionManager
			try {
				registry.register(new Scheme("https", new MySSLSocketFactory(trustStore), 443));
			} catch (NoSuchAlgorithmException e) {
				e.printStackTrace();
			} catch (KeyManagementException e) {
				e.printStackTrace();
			} catch (KeyStoreException e) {
				e.printStackTrace();
			} catch (UnrecoverableKeyException e) {
				e.printStackTrace();
			}
			return new SingleClientConnManager(getParams(), registry);
		}
	}
}

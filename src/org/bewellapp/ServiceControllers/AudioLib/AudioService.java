package org.bewellapp.ServiceControllers.AudioLib;

import android.app.Service;
import android.widget.RemoteViews;
import android.widget.Toast;
import android.media.AudioFormat;
import android.os.BatteryManager;
import android.os.Binder;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.util.Log;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.bewellapp.Ml_Toolkit_Application;

// Need the following import to get access to the app resources, since this
// class is in a sub-package.
import org.bewellapp.R;
import org.bewellapp.wallpaper.WellnessSummaryActivity;

import edu.cornell.audioProbe.AudioManager;
import edu.cornell.audioProbe.AudioManager.State;

public class AudioService extends Service {
    private static Context CONTEXT;

    @SuppressWarnings("rawtypes")
    private static final Class[] mStartForegroundSignature = new Class[] {
            int.class, Notification.class };
    @SuppressWarnings("rawtypes")
    private static final Class[] mStopForegroundSignature = new Class[] { boolean.class };

    public static final String ACTION_FOREGROUND = "org.bewellapp.ServiceControllers.AudioLib.FOREGROUND";
    public static final String ACTION_BACKGROUND = "org.bewellapp.ServiceControllers.AudioLib.BACKGROUND";

    private static NotificationManager mNM;
    private Method mStartForeground;
    private Method mStopForeground;

    private Object[] mStartForegroundArgs = new Object[2];
    private Object[] mStopForegroundArgs = new Object[1];

    public static boolean Foreground_on;

    // public long no_of_records;
    // public int curr_no_of_records;
    public static boolean Activity_on;
    private static Notification notification;
    private AudioManager ar;
    private Ml_Toolkit_Application appState;

    // binder
    private final IBinder binder = new AudioBinder();

    // audio status
    public String inferred_audio_Status = "Not Available";
    public String prev_inferred_audio_Status = "Not Available";
    private Thread t;

    private RemoteViews contentView;

    private Handler mHandler = new Handler();

    private BatteryReceiver mBatteryReceiver;

    /** Called when the activity is first created. */
    @Override
    public void onCreate() {

        // Register our receiver for the ACTION_SCREEN_OFF action. This will
        // make our receiver
        // code be called whenever the phone enters standby mode.
        // IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_OFF);
        // registerReceiver(AccelerometerManager.mReceiver, filter);

        // screen will stay on during this section
        appState = (Ml_Toolkit_Application) getApplicationContext();
        appState.audioService = this;
        // appState.audioServiceStarted = true;

        // no_of_records = 0;
        appState.audio_no_of_records = 0;
        // curr_no_of_records = 0;
        CONTEXT = this;

        mNM = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        try {
            mStartForeground = getClass().getMethod("startForeground",
                    mStartForegroundSignature);
            mStopForeground = getClass().getMethod("stopForeground",
                    mStopForegroundSignature);
        } catch (NoSuchMethodException e) {
            // Running on an older platform.
            mStartForeground = mStopForeground = null;
        }

        // Toast.makeText(this, "Audio Service Started ",
        // Toast.LENGTH_SHORT).show();

        Activity_on = true;
        Foreground_on = true;

        mBatteryReceiver = new BatteryReceiver();
        registerReceiver(mBatteryReceiver, new IntentFilter(
                Intent.ACTION_BATTERY_CHANGED));

        contentView = new RemoteViews(getPackageName(),
                R.layout.notification_layout);
    }

    public static Context getContext() {
        return CONTEXT;
    }

    private void startAudioManager() {
        ar = new AudioManager(appState, this, true,
                android.media.MediaRecorder.AudioSource.MIC, 8000,
                AudioFormat.CHANNEL_CONFIGURATION_MONO,
                AudioFormat.ENCODING_PCM_16BIT);

        try {
            // ar.setOutputFile("/sdcard/audio2.txt");
            // ar.prepare();
            // ar.start();

            t = new Thread() {
                public void run() {
                    // AccelerometerManager.startListening(accelServ);
                    startAudioRecording();
                }
            };
            // t.setPriority(Thread.NORM_PRIORITY+1);
            t.start();
        } catch (Exception ex) {
            ar = null;
            Toast.makeText(this, "Cannot create audio file \n" + ex.toString(),
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void startAudioRecording() {
        ar.setOutputFile(new File(Environment.getExternalStorageDirectory(),
                "audio3.txt").getAbsolutePath());
        ar.prepare();
        ar.start();

        //Log.e("AudioService","startAudioRecording() finished");
    }

    // This is the old onStart method that will be called on the pre-2.0
    // platform. On 2.0 or later we override onStartCommand() so this
    // method will not be called.
    @Override
    public void onStart(Intent intent, int startId) {
        handleCommand(intent);

        startAudioManager();
        // Toast.makeText(this, "Recording started", Toast.LENGTH_SHORT).show();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if (intent == null) {
            stopSelf();
            // appState.mlt.destroy();
            // return
            return START_NOT_STICKY;
        }

        handleCommand(intent);
        // starting the recording
        startAudioManager();

        // Toast.makeText(this, "Recording started", Toast.LENGTH_SHORT).show();

        // duty cycling code
        if (appState.enableAudioDutyCycling) {
            inDutyCycle = true;
            mHandler.removeCallbacks(mUpdateTimeTask);

            // sensing has started already so will need to stop it after sensing
            // interval now
            mHandler.postDelayed(mUpdateTimeTask,
                    appState.audioDutyCyclingSensingInterval.longValue());
        }

        // We want this service to continue running until it is explicitly
        // stopped, so return sticky.
        return START_STICKY;
        // return START_NOT_STICKY;
        // return START_REDELIVER_INTENT;
    }
    
    @SuppressWarnings("unused")
    private void doDutyCycleSchedule_old() {
        if (inDutyCycle) {
            try {

                // for debug
                // Toast.makeText(appState.audioService,
                // "AS: Battery level is: " +
                // mBatteryReceiver.currentEnergyLevel.toString() ,
                // Toast.LENGTH_LONG).show();

                // use here the battery manager
                // to calibrate duty cycling based on battery level

                // if (ar.intervalTotalCnt != 0 && ar.intervalVoiceCnt /
                // ar.intervalTotalCnt >= 0.5)
                if (mBatteryReceiver.currentEnergyLevel == ENERGYLEVEL.HIGH)
                // no duty-cycle
                {
                    appState.audioDutyCyclingRestartInterval
                            .set(appState.audioDutyCyclingSensingInterval
                                    .intValue());
                    // Log.d("DutyTag",
                    // "audioDutyCyclingRestartInterval is: " +
                    // appState.audioDutyCyclingRestartInterval.toString());
                }
                // else if (ar.intervalTotalCnt != 0 &&
                // ar.intervalSilenceCnt / ar.intervalTotalCnt >= 0.6)
                else if (mBatteryReceiver.currentEnergyLevel == ENERGYLEVEL.LOW)
                // heavy duty-cycle
                {
                    appState.audioDutyCyclingRestartInterval.set(5 * 60 * 1000);
                    // Log.d("DutyTag",
                    // "audioDutyCyclingRestartInterval is: " +
                    // appState.audioDutyCyclingRestartInterval.toString());
                } else
                // light duty-cycle
                {
                    appState.audioDutyCyclingRestartInterval.set(4 * 60 * 1000);
                    // Log.d("DutyTag",
                    // "audioDutyCyclingRestartInterval is: " +
                    // appState.audioDutyCyclingRestartInterval.toString());
                }
            } catch (Exception ex) {
                Log.d("DutyTag", ex.toString());
            }

            inDutyCycle = false;

            mHandler.removeCallbacks(mUpdateTimeTask);
            // sensing will stop now. restart after
            // "restartInterval-sensingTime"
            mHandler.postDelayed(
                    mUpdateTimeTask,
                    appState.audioDutyCyclingRestartInterval.longValue()
                            - appState.audioDutyCyclingSensingInterval
                                    .longValue() + 3000); // add 3 seconds
            // for this thread
            // to finish
            Log.d("DutyTag", "AS: audioDutyCyclingRestartInterval is: "
                    + appState.audioDutyCyclingRestartInterval.toString());
            Log.d("DutyTag", "AS: audioDutyCyclingSensingInterval is: "
                    + appState.audioDutyCyclingSensingInterval.toString());
            // Log.d("DutyTag", "AS: Battery level is: " +
            // mBatteryReceiver.currentEnergyLevel.toString());

            try {
                if (ar == null || ar.getManagerState() != State.RECORDING) {
                    // Toast.makeText(appState.audioService,
                    // "No audio file selected", Toast.LENGTH_SHORT).show();
                    return;
                }

                Log.d("AudioDutyCycle", "stop audio recording");
                ar.stopRecording();
                // ar.release();

                // Toast.makeText(appState.audioService,
                // "REcording stopped", Toast.LENGTH_SHORT).show();
            } catch (Exception ex) {
                Toast.makeText(appState.audioService,
                        "Failed to stop recording \n" + ex.toString(),
                        Toast.LENGTH_SHORT).show();
                Log.d("DutyTag", "Failed to stop recording \n" + ex.toString());
            }
            // ar = null;
            // ar = null;
            appState.audio_inference = "DutyCycling OFF";

        } else {
            mHandler.removeCallbacks(mUpdateTimeTask);
            // sensing will start soon. So, restart after
            // appState.audioDutyCyclingSensingInterval
            mHandler.postDelayed(mUpdateTimeTask,
                    appState.audioDutyCyclingSensingInterval.longValue());
            inDutyCycle = true;

            if (ar.getManagerState() != State.RECORDING) {
                Log.d("AudioDutyCycle", "restart audio manager");
                ar.startRecording();
            }
            // startAudioManager();
            // Toast.makeText(appState.audioService, "Recording started",
            // Toast.LENGTH_SHORT).show();

        }
    }

    // for inDutyCycle:
    // if(in conversation): sleep 1m
    // else: stop recording, sleep 3m
    // for !inDutyCycle:
    // start recording, sleep 1m
    final int inConversationDelay = 1 * 60 * 1000;
    final int inDutyCycleOff = 3 * 60 * 1000;

    // final int inConversationDelay = 10 * 1000;
    // final int inDutyCycleOff = 30 * 1000;
    private synchronized void doDutyCycleSchedule() {
        mHandler.removeCallbacks(mUpdateTimeTask);
        if (inDutyCycle) {
            if (ar != null && ar.isInConversation()) {
                mHandler.postDelayed(mUpdateTimeTask, inConversationDelay);
                Log.d("AudioDutyCycle", "AS: In Conversation, postponed for "
                        + inConversationDelay);
            } else {
                try {
                    if (ar != null && ar.getManagerState() == State.RECORDING) {

                        Log.d("AudioDutyCycle", "stop audio recording");
                        ar.stopRecording();
                    }
                } catch (Exception ex) {
                    Toast.makeText(appState.audioService,
                            "Failed to stop recording \n" + ex.toString(),
                            Toast.LENGTH_SHORT).show();
                    Log.d("AudioDutyCycle",
                            "Failed to stop recording \n" + ex.toString());
                }

                inDutyCycle = false;
                mHandler.postDelayed(mUpdateTimeTask, inDutyCycleOff); // add 3
                                                                       // seconds

                Log.d("AudioDutyCycle",
                        "AS: audioDutyCyclingRestartInterval is: "
                                + inDutyCycleOff);

                appState.audio_inference = "DutyCycling OFF";
            }
        } else {
            // sensing will start soon. So, restart after
            // appState.audioDutyCyclingSensingInterval
            if (ar.getManagerState() != State.RECORDING) {
                Log.d("AudioDutyCycle", "restart audio manager");
                ar.startRecording();
            }
            Log.d("AudioDutyCycle", "AS: audioDutyCyclingNextInterval is: "
                    + inConversationDelay);
            mHandler.postDelayed(mUpdateTimeTask, inConversationDelay);
            inDutyCycle = true;
        }
    }

    public boolean inDutyCycle;
    private Runnable mUpdateTimeTask = new Runnable() {
        public void run() {
            // appState.location_text = "Scanning";

            // locationAlreadyFound = false;

            // myLocationManager.getLocation(LocationService.this,
            // locationResult );
            // mHandler.postAtTime(this,1000*60*1);

            if (!appState.enableAudioDutyCycling
                    || appState.audioSensorOn == false) {
                mHandler.removeCallbacks(mUpdateTimeTask);
                // sensing has started already so will need to stop it after
                // sensing interval now
                mHandler.postDelayed(mUpdateTimeTask,
                        appState.audioDutyCyclingSensingInterval.longValue());

                inDutyCycle = false;

                return;
            }

            doDutyCycleSchedule();

            // mHandler.postDelayed(mUpdateTimeTask, 1000*60*10);
            // mHandler.postDelayed(mUpdateTimeTask, 1000*30);
        }
    };

    void handleCommand(Intent intent) {
        if (ACTION_FOREGROUND.equals(intent.getAction())) {

            if (mBatteryReceiver == null) {
                mBatteryReceiver = new BatteryReceiver();
                registerReceiver(mBatteryReceiver, new IntentFilter(
                        Intent.ACTION_BATTERY_CHANGED));
            }

            // In this sample, we'll use the same text for the ticker and the
            // expanded notification
            // String text = "Audio On:";//
            // getText(R.string.foreground_service_started);

            // notification = new
            // Notification(R.drawable.logo,"BeWell",System.currentTimeMillis());//(R.drawable.icon,
            // text, System.currentTimeMillis());
            notification = new Notification(R.drawable.notification_fish,
                    getString(R.string.app_name), System.currentTimeMillis());

            // contentView.setImageViewResource(R.id.status_icon,
            // R.drawable.icon);
            /*
             * contentView.setTextViewText(R.id.accel_text, "Accel On:" + " (" +
             * appState.accel_no_of_records + ")");
             * contentView.setTextColor(R.id.accel_text, Color.argb(128, 0, 115,
             * 0)); contentView.setTextViewText(R.id.audio_text, "Audio on:" +
             * " (" + appState.audio_no_of_records + ")");
             * contentView.setTextColor(R.id.audio_text, Color.argb(128, 0, 115,
             * 0)); contentView.setTextViewText(R.id.location_text,
             * "Location on:" + " (" + appState.location_text + ")");
             * contentView.setTextColor(R.id.location_text, Color.argb(128, 0,
             * 115, 0));
             */
            notification.contentView = contentView;
            /*
             * PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
             * new Intent(this, org.bewellapp.main_activity.class), 0);
             */
            PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                    new Intent(this, WellnessSummaryActivity.class), 0);

            notification.contentIntent = contentIntent;

            startForegroundCompat(R.string.CUSTOM_VIEW, notification);

        } else if (ACTION_BACKGROUND.equals(intent.getAction())) {
            stopForegroundCompat(R.string.foreground_service_started_aud);
            // stopForegroundCompat(2);

        }
    }

    public void callStartForegroundCompat() {
        startForegroundCompat(R.string.CUSTOM_VIEW, notification);
        updateNotificationArea();
    }

    /**
     * This is a wrapper around the new startForeground method, using the older
     * APIs if it is not available.
     */
    public void startForegroundCompat(int id, Notification notification) {
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
        // setForeground(true);
        // mNM.notify(id, notification);
        Log.e("NOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOO",
                "NOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOO");
    }

    /**
     * This is a wrapper around the new stopForeground method, using the older
     * APIs if it is not available.
     */
    public void stopForegroundCompat(int id) {
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
        // if
        // (!appState.getServiceController().areServiceUsingNotificationArea())
        // {
        // mNM.cancel(id);
        // }
        // setForeground(false);
    }

    @Override
    public void onDestroy() {

        unregisterReceiver(mBatteryReceiver);
        mHandler.removeCallbacks(mUpdateTimeTask);

        // Make sure our notification is gone.
        stopForegroundCompat(R.string.CUSTOM_VIEW);
        // stopForegroundCompat(2);

        try {
            if (ar != null) {
                ar.release();
                //Log.e("AudioService","onDestroy()");
            }
        } catch (Exception ex) {
            Toast.makeText(this, "Failed to stop recording \n" + ex.toString(),
                    Toast.LENGTH_SHORT).show();
        }
        ar = null;
        appState.audioService = null;
        // appState.audioServiceStarted = false;

    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    public class AudioBinder extends Binder {
        public AudioService getService() {
            return AudioService.this;
        }
    }

    public void updateNotificationArea() {
        // if (no_of_records % 100 == 0 && Foreground_on == true) {

        // String text = "No of samples";

        // Set the info for the views that show in the notification panel.

        try {
            /*
             * if(appState.accelSensorOn){
             * contentView.setTextViewText(R.id.accel_text, "Accel On:" + " (" +
             * appState.accel_no_of_records + ")");
             * contentView.setTextColor(R.id.accel_text, Color.argb(128, 0, 115,
             * 0)); } else { contentView.setTextViewText(R.id.accel_text,
             * "Accel Off"); contentView.setTextColor(R.id.accel_text,
             * Color.argb(128, 115, 0, 0)); }
             * 
             * contentView.setTextViewText(R.id.audio_text, "Audio On:" + " (" +
             * appState.audio_no_of_records + ")");
             * contentView.setTextColor(R.id.audio_text, Color.argb(128, 0, 115,
             * 0));
             * 
             * if(appState.locationSensorOn){
             * contentView.setTextViewText(R.id.location_text, "Location On:" +
             * " (" + appState.location_text + ")");
             * contentView.setTextColor(R.id.location_text, Color.argb(128, 0,
             * 115, 0)); } else {
             * contentView.setTextViewText(R.id.location_text, "Location off");
             * contentView.setTextColor(R.id.location_text, Color.argb(128, 115,
             * 0, 0)); }
             */

            notification.contentView = contentView;
            /*
             * PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
             * new Intent(this, org.bewellapp.main_activity.class), 0);
             */
            PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                    new Intent(this, WellnessSummaryActivity.class), 0);

            notification.contentIntent = contentIntent;

            // startForegroundCompat(R.string.CUSTOM_VIEW,notification);
            mNM.notify(R.string.CUSTOM_VIEW, notification);
        } catch (Exception e) {
            Log.e("AudioService", "Update Notification " + e.toString());
        }

        // }
    }

    private static enum ENERGYLEVEL {
        LOW, MEDIUM, HIGH;
    }

    private class BatteryReceiver extends BroadcastReceiver {

        protected ENERGYLEVEL currentEnergyLevel;
        private final float THRESH_ENERGY_LOW = 0.45f;
        private final float THRESH_ENERGY_MEDIUM = 0.85f;

        public BatteryReceiver() {
            currentEnergyLevel = ENERGYLEVEL.MEDIUM;
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            if (!intent.getAction().equals(Intent.ACTION_BATTERY_CHANGED)) {
                currentEnergyLevel = ENERGYLEVEL.MEDIUM;
                return;
            }
            int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            float scale = (float) intent.getIntExtra(
                    BatteryManager.EXTRA_SCALE, -1);
            int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);

            switch (status) {
            case BatteryManager.BATTERY_STATUS_FULL:
            case BatteryManager.BATTERY_STATUS_CHARGING:
                currentEnergyLevel = ENERGYLEVEL.HIGH;
                break;
            case BatteryManager.BATTERY_STATUS_NOT_CHARGING:
            case BatteryManager.BATTERY_STATUS_DISCHARGING: {
                float batteryLevel = level / scale;
                if (batteryLevel < THRESH_ENERGY_LOW) {
                    currentEnergyLevel = ENERGYLEVEL.LOW;
                } else if (batteryLevel < THRESH_ENERGY_MEDIUM) {
                    currentEnergyLevel = ENERGYLEVEL.MEDIUM;
                } else {
                    currentEnergyLevel = ENERGYLEVEL.HIGH;
                }
            }
                break;
            default:
                currentEnergyLevel = ENERGYLEVEL.MEDIUM;
            }
        }
    }
}

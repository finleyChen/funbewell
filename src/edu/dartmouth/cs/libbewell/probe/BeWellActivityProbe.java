package edu.dartmouth.cs.libbewell.probe;

import org.bewellapp.ServiceControllers.MessageNofitier;
import org.bewellapp.ServiceControllers.AccelerometerLib.AccelerometerEventReceiver;

import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;

import edu.dartmouth.cs.libbewell.probe.BeWellActivity.ActivityTypeKeys;
import edu.mit.media.funf.probe.Probe;

public class BeWellActivityProbe extends Probe implements MessageNofitier,
	ActivityTypeKeys {

    private AccelerometerEventReceiver activityReceiver = null;

    public BeWellActivityProbe() {
	activityReceiver = new AccelerometerEventReceiver(this);
    }

    @Override
    public void onNotifyNewMessage(Intent intent) {
	long timestamp = intent.getLongExtra("timestamp", -1);
	long duration = intent.getLongExtra("duration", -1);
	int actitivy = intent.getIntExtra("activityid", -1);
	if (timestamp == -1 || duration == -1 || actitivy == -1) {
	    // discard illegal data
	    return;
	}

	Bundle data = new Bundle();
	data.putInt(ActivityTypeKeys.ACTIVITY_TYPE, actitivy);
	data.putLong(ActivityTypeKeys.START_TIMESTAMP, timestamp);
	data.putLong(ActivityTypeKeys.DURATION, duration);
	Log.d("FunBeWellTEST", "activity:"+actitivy);
	sendProbeData(System.currentTimeMillis() / 1000, data);
    }

    @Override
    public String[] getRequiredPermissions() {
	return null;
    }

    @Override
    public String[] getRequiredFeatures() {
	return null;
    }

    @Override
    public Parameter[] getAvailableParameters() {
	return new Parameter[] {
		new Parameter(Parameter.Builtin.DURATION, 30L),
		new Parameter(Parameter.Builtin.PERIOD, 300L),
		new Parameter(Parameter.Builtin.START, 0L),
		new Parameter(Parameter.Builtin.END, 0L) };
    }

    @Override
    protected void onEnable() {
	IntentFilter acc_filter = new IntentFilter(
		AccelerometerEventReceiver.class.getName());

	registerReceiver(activityReceiver, acc_filter);
    }

    @Override
    protected void onRun(Bundle params) {

    }

    @Override
    protected void onStop() {
    }

    @Override
    protected void onDisable() {
	unregisterReceiver(activityReceiver);
    }

    @Override
    public void sendProbeData() {
    }

}

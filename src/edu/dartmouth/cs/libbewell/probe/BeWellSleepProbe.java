/**
 * 
 */
package edu.dartmouth.cs.libbewell.probe;

import org.bewellapp.Ml_Toolkit_Application;

import android.os.Bundle;
import edu.mit.media.funf.probe.Probe;

/**
 * @author coldray
 * 
 */
public class BeWellSleepProbe extends Probe implements BeWellSleepFeaturesKeys {

    private long lastSleepingDurationRecord_time = 0;

    /*
     * (non-Javadoc)
     * 
     * @see edu.mit.media.funf.probe.Probe#getRequiredPermissions()
     */
    @Override
    public String[] getRequiredPermissions() {
	return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.mit.media.funf.probe.Probe#getRequiredFeatures()
     */
    @Override
    public String[] getRequiredFeatures() {
	return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.mit.media.funf.probe.Probe#getAvailableParameters()
     */
    @Override
    public Parameter[] getAvailableParameters() {
	return new Parameter[] {
		new Parameter(Parameter.Builtin.DURATION, 30L),
		new Parameter(Parameter.Builtin.PERIOD, 300L),
		new Parameter(Parameter.Builtin.START, 0L),
		new Parameter(Parameter.Builtin.END, 0L) };
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.mit.media.funf.probe.Probe#onEnable()
     */
    @Override
    protected void onEnable() {

    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.mit.media.funf.probe.Probe#onRun(android.os.Bundle)
     */
    @Override
    protected void onRun(Bundle params) {
	Ml_Toolkit_Application appState = (Ml_Toolkit_Application) getApplicationContext();
	long sleep_record_time = appState.sleeping_duration_record_time;
	long duration = appState.amount_of_sleeping_duration;

	if (sleep_record_time > lastSleepingDurationRecord_time
		&& appState.sleeping_duration_record_time > 0) {
	    Bundle data = new Bundle();
	    data.putLong(RECORDTIMESTAMP, sleep_record_time);
	    data.putLong(SLEEP_DURATION, duration);
	    sendProbeData(System.currentTimeMillis() / 1000, data);
	    
	    lastSleepingDurationRecord_time = sleep_record_time;
	}
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.mit.media.funf.probe.Probe#onStop()
     */
    @Override
    protected void onStop() {
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.mit.media.funf.probe.Probe#onDisable()
     */
    @Override
    protected void onDisable() {
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.mit.media.funf.probe.Probe#sendProbeData()
     */
    @Override
    public void sendProbeData() {
    }

}

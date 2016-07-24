package edu.dartmouth.cs.libbewell.probe;

import org.bewellapp.ServiceControllers.MessageNofitier;
import org.bewellapp.ServiceControllers.AudioLib.AudioFeatureReceiver;

import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;

import edu.dartmouth.cs.libbewell.probe.AudioFeatures.BeWellAudioFeaturesKeys;
import edu.mit.media.funf.probe.Probe;

public class BeWellAudioFeatureProbe extends Probe implements MessageNofitier,
	BeWellAudioFeaturesKeys {

    private AudioFeatureReceiver audioReceiver = null;
    
    public BeWellAudioFeatureProbe()
    {
	audioReceiver = new AudioFeatureReceiver(this);
    }
    
    @Override
    protected String getDisplayName() {
        return "Conversation Probe";
    }
    
    @Override
    public void onNotifyNewMessage(Intent intent) {
	long start_ts = intent.getLongExtra("start_timestamp", -1);
	long end_ts = intent.getLongExtra("finish_timestamp", -1);
	
	Bundle data = new Bundle();
	data.putLong(CONVERSATION_START, start_ts);
	data.putLong(CONVERSATION_END, end_ts);
	
	Log.e("BeWellAudioFeatureProbe", "recieved a conversation:" + start_ts + "~" + end_ts);
	// Write out features
	sendProbeData(System.currentTimeMillis()/1000, data);
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
		new Parameter(Parameter.Builtin.PERIOD, 300L)};
    }

    @Override
    protected void onEnable() {
	IntentFilter audio_filter = new IntentFilter(
		AudioFeatureReceiver.class.getName());
	registerReceiver(audioReceiver, audio_filter);
    }

    @Override
    protected void onRun(Bundle params) {
    }

    @Override
    protected void onStop() {
    }

    @Override
    protected void onDisable() {
	unregisterReceiver(audioReceiver);
    }

    @Override
    public void sendProbeData() {

    }

}

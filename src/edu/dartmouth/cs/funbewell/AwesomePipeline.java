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
package edu.dartmouth.cs.funbewell;

import static edu.mit.media.funf.AsyncSharedPrefs.async;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;

import org.json.JSONException;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.StaticLayout;
import android.util.Log;
import edu.mit.media.funf.IOUtils;
import edu.mit.media.funf.Utils;
import edu.mit.media.funf.configured.ConfiguredPipeline;
import edu.mit.media.funf.configured.FunfConfig;
import edu.mit.media.funf.probe.Probe;
import edu.mit.media.funf.storage.BundleSerializer;
import edu.mit.media.funf.storage.UploadService;

public class AwesomePipeline extends ConfiguredPipeline {

    public static final String TAG = "FunfBGCollector";
    public static final String MAIN_CONFIG = "main_config";
    public static final String START_DATE_KEY = "START_DATE";
    public static Boolean config_change = false;

    public static final String ACTION_RUN_ONCE = "RUN_ONCE";
    public static final String RUN_ONCE_PROBE_NAME = "PROBE_NAME";

    @Override
    protected int getRedeliveryType(Intent intent) {
	return START_STICKY;
    }
    
    @Override
    public void onCreate() {
	super.onCreate();
	
	/*
	BroadcastReceiver configBroadcastReceiver = new BroadcastReceiver() 
    {
    	public void onReceive(Context context, Intent intent) 
    	{
    		Bundle extras = intent.getExtras();
    		Log.i("Adaptive_Config_reciver", "activityid = "+extras.getLong("activityid",-1L));
    		if (extras.getLong("activityid",-1L) == 0 & config_change)
    		{	
    			config_change = false;
    			Log.i("Adaptive_Config_reciver", "activityid = "+extras.getLong("activityid",-1L));
    			/*
    			String configString= "{\"dataRequests\":" +
    					"{" +
    					"\"edu.mit.media.funf.probe.builtin.LocationProbe\": [" +
    					"{ \"PERIOD\": " +
    					"1200" +
    					", \"DURATION\": 5 }," +
    					"{ \"PERIOD\": " +
    					"1200" +
    					", \"DURATION\": 5 }" +
    					"]," +
    					"\"edu.mit.media.funf.probe.builtin.WifiProbe\": [" +
    					"{ \"PERIOD\": " +
    					"1200" +
    					", \"DURATION\": 5 }," +
    					"]," +
    					"\"edu.mit.media.funf.probe.builtin.BluetoothProbe\": [" +
    					"{ \"PERIOD\": " +
    					"1200" +
    					", \"DURATION\": 5 }," +
    					"]}" +
    					"}";
    			updateSingleProbeConfig(configString);
    			
    		}else if ((extras.getLong("activityid",-1L) == 1 | extras.getLong("activityid",-1L) == 3) & !config_change){
    			config_change = true;
    			Log.i("Adaptive_Config_reciver", "activityid = "+ extras.getLong("activityid",-1L));
    			
    			String configString= "{" +
    					"\"edu.mit.media.funf.probe.builtin.LocationProbe\": [" +
    					"{ \"PERIOD\": " +
    					"300" +
    					", \"DURATION\": 5 }," +
    					"{ \"PERIOD\": " +
    					"300" +
    					", \"DURATION\": 5 }" +
    					"]," +
    					"\"edu.mit.media.funf.probe.builtin.WifiProbe\": [" +
    					"{ \"PERIOD\": " +
    					"300" +
    					", \"DURATION\": 5 }," +
    					"]," +
    					"\"edu.mit.media.funf.probe.builtin.BluetoothProbe\": [" +
    					"{ \"PERIOD\": " +
    					"300" +
    					", \"DURATION\": 5 }," +
    					"]}";
    			updateSingleProbeConfig(configString);
    			
    		}
    	} 
    };
    IntentFilter intentFilter = new IntentFilter( "edu.dartmouth.cs.funbewell.AwesomePipeline");
    registerReceiver( configBroadcastReceiver, intentFilter);
    */
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
	if(intent==null)
	{
	    intent = new Intent(getApplicationContext(),
			AwesomePipeline.class);
	}
        
	return super.onStartCommand(intent, flags, startId);
    }
    
    @Override
    protected void onHandleIntent(Intent intent) {
	if(intent == null || intent.getAction() == null)
	{
	    Log.e("AwesomePipeline", "null intent!");
	    return;
	}
	if (ACTION_RUN_ONCE.equals(intent.getAction())) {
	    String probeName = intent.getStringExtra(RUN_ONCE_PROBE_NAME);
	    runProbeOnceNow(probeName);
	} else {
	    super.onHandleIntent(intent);
	}
    }

    @Override
    public BundleSerializer getBundleSerializer() {
	return new BundleToJson();
    }

    public static class BundleToJson implements BundleSerializer {
	public String serialize(Bundle bundle) {
	    return JsonUtils.getGson().toJson(Utils.getValues(bundle));
	}

    }

    @Override
    public void onDataReceived(Bundle data) {
	super.onDataReceived(data);
	// Fill this in with extra behaviors on data received
    }

    @Override
    public void onStatusReceived(Probe.Status status) {
	super.onStatusReceived(status);
	// Fill this in with extra behaviors on status received
    }

    @Override
    public void onDetailsReceived(Probe.Details details) {
	super.onDetailsReceived(details);
	// Fill this in with extra behaviors on details received
    }

    public static boolean isEnabled(Context context) {
	return getSystemPrefs(context).getBoolean(ENABLED_KEY, true);
    }

    @Override
    public SharedPreferences getSystemPrefs() {
	return getSystemPrefs(this);
    }

    public static SharedPreferences getSystemPrefs(Context context) {
	return async(context.getSharedPreferences(
		AwesomePipeline.class.getName() + "_system", MODE_PRIVATE));
    }

    @Override
    public FunfConfig getConfig() {
	return getMainConfig(this);
    }

    @Override
    public Class<? extends UploadService> getUploadServiceClass() {
	return HttpUploadService.class;
    }
    
    static long lastLoadTime = 0;
    /**
     * Easy access to Funf config. As long as this service is running, changes
     * will be automatically picked up.
     * 
     * @param context
     * @return
     */
    public static FunfConfig getMainConfig(Context context) {
	FunfConfig config = getConfig(context, MAIN_CONFIG);
	long ts = System.currentTimeMillis();
	if (config.getName() == null || ts - lastLoadTime > 3600) {
	Log.e("_FUNF_","Reload funf config");
	    lastLoadTime = ts;
	    String jsonString = getStringFromAsset(context,
		    "default_config.json");
	    if (jsonString == null) {
		Log.e(TAG, "Error loading default config.  Using blank config.");
		jsonString = "{}";
	    }
	    try {
		config.edit().setAll(jsonString).commit();
	    } catch (JSONException e) {
		Log.e(TAG, "Error parsing default config", e);
	    }
	}
	
	return config;
    }

    public static String getStringFromAsset(Context context, String filename) {
	InputStream is = null;
	try {
	    is = context.getAssets().open(filename);
	    return IOUtils.inputStreamToString(is, Charset.defaultCharset()
		    .name());
	} catch (IOException e) {
	    Log.e(TAG, "Unable to read asset to string", e);
	    return null;
	} finally {
	    if (is != null) {
		try {
		    is.close();
		} catch (IOException e) {
		    Log.e(TAG, "Unable to close asset input stream", e);
		}
	    }
	}
    }

    public void runProbeOnceNow(final String probeName) {
	FunfConfig config = getMainConfig(this);
	ArrayList<Bundle> updatedRequests = new ArrayList<Bundle>();
	Bundle[] existingRequests = config.getDataRequests(probeName);
	if (existingRequests != null) {
	    for (Bundle existingRequest : existingRequests) {
		updatedRequests.add(existingRequest);
	    }
	}

	Bundle oneTimeRequest = new Bundle();
	oneTimeRequest.putLong(Probe.Parameter.Builtin.PERIOD.name, 0L);
	updatedRequests.add(oneTimeRequest);

	Intent request = new Intent(Probe.ACTION_REQUEST);
	request.setClassName(this, probeName);
	request.putExtra(Probe.CALLBACK_KEY, getCallback());
	request.putExtra(Probe.REQUESTS_KEY, updatedRequests);
	startService(request);
    }
    
}

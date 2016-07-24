package org.bewellapp.Storage;

import java.io.Serializable;


public class ML_toolkit_object implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	//int timestamp;
	public int datatype;
	public byte[] data;
	public long timestamp;
	public String inferred_results_or_label_info;
	public boolean isStart;
	
	//identifier for syncing data, feature, or labels
	public int sync_id; 
	
	public ML_toolkit_object() {
		this(-1, -1, null);
	}

	public ML_toolkit_object(long timestamp, int datatype, byte[] data)
	{
		//0 = audio, 1=accelerometer, 2=location, 3 = audio features, 4 = accel features,
		//5 = audio inference, 6 = accel inference, 7 = label_start , 8 = label_end, 9=wifi_scan_data 
		//10=bluetooth_data 
		//11-NTPtimestamps
		//12-Momentary Samples
		//13-sleep
		this.datatype = datatype;  
		this.data = data;
		this.timestamp = timestamp;
		this.inferred_results_or_label_info = null;
		this.isStart = false;//it doesn't matter because 
		this.sync_id = -1;
	}
	
	//for labels
	public ML_toolkit_object(long timestamp, int datatype, boolean isStart2, String str)
	{
		//0 = audio, 1=accelerometer, 2=location, 3 = audio features, 4 = accel features,
		//5 = audio inference, 6 = accel inference, 7 = label_start , 8 = label_end, 9=wifi_scan_data 
		//10=bluetooth_data 
		//11-NTPtimestamps
		//12-Momentary Samples
		//13-sleep
		this.datatype = datatype;  
		//this.data = data;
		this.inferred_results_or_label_info = str;
		this.data = null;
		this.timestamp = timestamp;
		this.isStart = isStart2;
		this.sync_id = -1;
	}

	
	public ML_toolkit_object(long timestamp, int datatype, byte[] data,int sync_id)
	{
		//0 = audio, 1=accelerometer, 2=location, 3 = audio features, 4 = accel features,
		//5 = audio inference, 6 = accel inference, 7 = label_start , 8 = label_end, 9=wifi_scan_data 
		//10=bluetooth_data 
		//11-NTPtimestamps
		//12-Momentary Samples
		//13-sleep
		this.datatype = datatype;  
		this.data = data;
		this.timestamp = timestamp;
		this.inferred_results_or_label_info = null;
		this.isStart = false;//it doesn't matter because 
		this.sync_id = sync_id;//sync with inference data
	}
	
	//for labels
	public ML_toolkit_object(long timestamp, int datatype, boolean isStart2, String str,int sync_id)
	{
		//0 = audio, 1=accelerometer, 2=location, 3 = audio features, 4 = accel features,
		//5 = audio inference, 6 = accel inference, 7 = label_start , 8 = label_end, 9=wifi_scan_data 
		//10=bluetooth_data 
		//11-NTPtimestamps
		//12-Momentary Samples
		//13-sleep
		this.datatype = datatype;  
		//this.data = data;
		this.inferred_results_or_label_info = str;
		this.data = null;
		this.timestamp = timestamp;
		this.isStart = isStart2;
		this.sync_id = sync_id;//sync with inference data
	}




	
	public ML_toolkit_object setValues(long timestamp, int datatype, byte[] data)
	{
		//0 = audio, 1=accelerometer, 2=location, 3 = audio features, 4 = accel features,
		//5 = audio inference, 6 = accel inference, 7 = label_start , 8 = label_end, 9=wifi_scan_data 
		//10=bluetooth_data 
		//11-NTPtimestamps
		//12-Momentary Samples
		//13-sleep
		//14-dark duration, 15-screen off duration, 16-shutdown duration, 17-phone charging duration
		//20-ground-truth sleep duration
		//21- BeWell scores
		//22- Application usage
		
		this.datatype = datatype;  
		this.data = data;
		this.timestamp = timestamp;
		this.inferred_results_or_label_info = null;
		this.isStart = false;//it doesn't matter because 
		this.sync_id = -1;
		return this;
	}
	
	//for labels
	public ML_toolkit_object setValues(long timestamp, int datatype, boolean isStart2, String str)
	{
		//0 = audio, 1=accelerometer, 2=location, 3 = audio features, 4 = accel features,
		//5 = audio inference, 6 = accel inference, 7 = label_start , 8 = label_end, 9=wifi_scan_data 
		//10=bluetooth_data 
		//11-NTPtimestamps
		//12-Momentary Samples
		//13-sleep
		this.datatype = datatype;  
		//this.data = data;
		this.inferred_results_or_label_info = str;
		this.data = null;
		this.timestamp = timestamp;
		this.isStart = isStart2;
		this.sync_id = -1;
		return this;
	}

	
	public ML_toolkit_object setValues(long timestamp, int datatype, byte[] data,int sync_id)
	{
		//0 = audio, 1=accelerometer, 2=location, 3 = audio features, 4 = accel features,
		//5 = audio inference, 6 = accel inference, 7 = label_start , 8 = label_end, 9=wifi_scan_data 
		//10=bluetooth_data 
		//11-NTPtimestamps
		//12-Momentary Samples
		this.datatype = datatype;  
		this.data = data;
		this.timestamp = timestamp;
		this.inferred_results_or_label_info = null;
		this.isStart = false;//it doesn't matter because 
		this.sync_id = sync_id;//sync with inference data
		return this;
	}
	
	//for labels
	public ML_toolkit_object setValues(long timestamp, int datatype, boolean isStart2, String str,int sync_id)
	{
		//0 = audio, 1=accelerometer, 2=location, 3 = audio features, 4 = accel features,
		//5 = audio inference, 6 = accel inference, 7 = label_start , 8 = label_end, 9=wifi_scan_data 
		//10=bluetooth_data 
		//11-NTPtimestamps
		//12-Momentary Samples
		//13-sleep
		this.datatype = datatype;  
		//this.data = data;
		this.inferred_results_or_label_info = str;
		this.data = null;
		this.timestamp = timestamp;
		this.isStart = isStart2;
		this.sync_id = sync_id;//sync with inference data
		return this;
	}
}
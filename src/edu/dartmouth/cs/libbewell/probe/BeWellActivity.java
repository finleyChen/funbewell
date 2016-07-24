package edu.dartmouth.cs.libbewell.probe;

import edu.mit.media.funf.probe.builtin.ProbeKeys.BaseProbeKeys;
import android.os.Bundle;

public class BeWellActivity {
	
	public int getActivityType() {
		return m_activityType;
	}
	public void setActivityType(int activityType) {
		this.m_activityType = activityType;
	}
	public long getStartTimestamp() {
		return m_startTimestamp;
	}
	public void setStartTimestamp(long startTimestamp) {
		this.m_startTimestamp = startTimestamp;
	}
	public long getDuration() {
		return m_duration;
	}
	public void setDuration(long duration) {
		this.m_duration = duration;
	}
	
	public Bundle toBundle()
	{
		Bundle data = new Bundle();
		data.putInt(ActivityTypeKeys.ACTIVITY_TYPE, m_activityType);
		data.putLong(ActivityTypeKeys.START_TIMESTAMP, m_startTimestamp);
		data.putLong(ActivityTypeKeys.DURATION, m_duration);
		
		return data;
	}
	
	private int m_activityType = -1;
	private long m_startTimestamp = -1;
	private long m_duration = -1;

	/**
	 * 
	 * @author Rui Wang
	 * 
	 */
	public static interface ActivityTypeKeys extends BaseProbeKeys {
		public static final String 
		ACTIVITY_TYPE = "ACTIVITY_TYPE",
		START_TIMESTAMP = "START_TIMESTAMP",
		DURATION = "DURATION";
	}
}

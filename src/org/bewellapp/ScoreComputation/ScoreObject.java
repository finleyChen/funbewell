package org.bewellapp.ScoreComputation;

import org.bewellapp.Ml_Toolkit_Application;

public class ScoreObject {
	
	public double sleepScorePrev = 0;
	public double sleepScoreCurrent = 0;
	public long sleepTimeInMillisecond = 0;
	public long sleepTimeRecordDataTime = 0;
	
	public double socialScorePrev = 0;
	public double socialScoreCurrent = 0;
	public long socialScoreAllActivityCount = 0;
	public long[] socialScoreDifferntActivityCount = null;
	
	public double physicalScorePrev = 0;
	public double physicalScoreCurrent = 0;
	public long physicalScoreAllActivityCount = 0;
	public long[] physicalScoreDifferntActivityCount = null;

	
	
	public ScoreObject()
	{
		//need to go for databases to store the values properly
		
		//0 = unknown, 1 = stationary, 2 = driving, 3 = walking, 4 = running,
		// 5 = cycling, 6 = error
		physicalScoreDifferntActivityCount =  new long[Ml_Toolkit_Application.NO_PHYSICAL_ACTIVITY];
		for(int i = 0; i < Ml_Toolkit_Application.NO_PHYSICAL_ACTIVITY ; i++)
			physicalScoreDifferntActivityCount[i] = 0;	
		
		
		//0 = silence, 1 = noise, 2 = voice, 3 = error
		socialScoreDifferntActivityCount = new long[Ml_Toolkit_Application.NO_VOICE_ACTIVITY];
		for(int i = 0; i < Ml_Toolkit_Application.NO_VOICE_ACTIVITY; i++)
			socialScoreDifferntActivityCount[i] = 0;
	}
	
}

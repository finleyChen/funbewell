package org.bewellapp.ServiceControllers.AccelerometerLib;

public interface SleepClassifier {
	
	public static long WINDOW_LENGTH_MS = 5 * 60 * 1000;

	public SleepState classify(AccelFeatures features);
}

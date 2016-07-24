package org.bewellapp.ServiceControllers.AccelerometerLib;

public class SignalEnergy {
	
	private double currEnergy = 0.0;
	
	public void increment(double d) {
		currEnergy += d*d;
	}
	
	public double getResult() {
		return currEnergy;
	}
	
	public void clear() {
		currEnergy = 0.0;
	}
}

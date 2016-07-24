/*
 *  MLToolKit
 *
 *  Created by Hong Lu on 07/11/2011.
 *
 */

package edu.dartmouth.cs.mltoolkit.processing;

public class AccFeatureExtraction {
	private final int frameSize;
	private final int samplingRate;
	private final int featureSize = 25;
	
	public int getFrameSize() {
		return frameSize;
	}
	
	public int getFeatureSize() {
		return featureSize;
	}
	
	public AccFeatureExtraction(int frameSize, int samplingRate){
		this.frameSize = frameSize;
		this.samplingRate = samplingRate;
	}
		
	static {
		System.loadLibrary("mltoolkit");
	}	
	
	public int getAccFeatures(double[] x,double[] y, double[] z, double[] features){
		return getAccFeature(x, y, z, samplingRate, frameSize, features);
	}
	/**
	 * 
	 * @param x
	 * @param y
	 * @param z
	 * @param samplingRate
	 * @param frameSize
	 * @param features
	 * @return
	 * 25 dimension accelerometer feature
	 * reference: The Jigsaw continuous sensing engine for mobile phone applications, Sensys 2010  
	 */
	//static public native double test(double[] tmp);
	//static public native double testArray(double[] tmp,double[] tmp2);
	static public native int getAccFeature(double[] x,double[] y, double[] z, int samplingRate, int frameSize,double[] features);
}

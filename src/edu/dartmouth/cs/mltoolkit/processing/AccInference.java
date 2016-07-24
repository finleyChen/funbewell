/*
 *  MLToolKit
 *
 *  Created by Hong Lu on 07/11/2011.
 *
 */

package edu.dartmouth.cs.mltoolkit.processing;

/**
 * @author hong
 *
 *  multivariate gaussian activity classifier
 *  reference: The Jigsaw continuous sensing engine for mobile phone applications, Sensys 2010  
 *
 */

public class AccInference {
	static {
		System.loadLibrary("mltoolkit");
	}	
	static public native int gaussianAccClassifier(double[] feature);
}

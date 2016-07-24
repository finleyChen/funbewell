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
 *  Decision tree based voice/non-voice classifier
 *  reference: The Jigsaw continuous sensing engine for mobile phone applications, Sensys 2010  
 *
 */


public class AudioInference {
	static public int dtAudioClassifier(double[] feature){

		if(feature[24] <80){
			return -1;
		} else {
			return tree(feature);
		}
	}
	
	static private int tree(double [] feature){
		int type = -1;
	    double spEntMean = feature[0];
		double spEntv = feature[1];
		double sfMean = feature[2];
		double sfv = feature[3];
		double rolloffMean = feature[4];
		double rolloffVariance = feature[5];
		double centroidMean = feature[6];
		double centroidVariance = feature[7];
		double bandwidthMean = feature[8];
		double bandwidthVariance = feature[9];
		double lowenergy = feature[10];
	    double mfcc1 = feature[11];
	    double mfcc2 = feature[12];
		double mfcc3 = feature[13];
		double mfcc4 = feature[14];
		double mfcc5 = feature[15];
		double mfcc6 = feature[16];
		double mfcc7 = feature[17];
		double mfcc8 = feature[18];
		double mfcc9 = feature[19];
		double mfcc10 = feature[20];
		double mfcc11 = feature[21];
		double mfcc12 = feature[22];
		double mfcc13 = feature[23];
		
		if(spEntMean<=0.251464){
			if(spEntMean<=0.196753){type = 0;
			}else{
				if(rolloffVariance<=918.092105){
					if(mfcc1<=-0.024296){type = 0;
					}else{
						if(centroidVariance<=719.760676){
							if(rolloffVariance<=87.186842){type = 0;
							}else{
								if(centroidMean<=47.763696){type = 0;
								}else{
									if(spEntv<=0.003908){type = 0;
									}else{type = 1;}
								}
							}
						}else{type = 0;}
					}
				}else{type = 0;}
			}
		}else{
			if(centroidMean<=82.228238){
				if(lowenergy<=0){
					if(rolloffVariance<=169.957895){type = 0;
					}else{
						if(centroidVariance<=41.017823){type = 0;
						}else{
							if(mfcc2<=-0.042565){
								if(mfcc1<=0.072543){type = 0;
								}else{type = 1;;}
							}else{type = 0;}
						}
					}
				}else{
					if(centroidMean<=20.324351){type = 0;
					}else{
						if(lowenergy<=9){
							if(mfcc6<=0.043434){
								if(centroidVariance<=34.666244){
									if(rolloffVariance<=311.2){type = 0;
									}else{type = 1;;}
								}else{
									if(mfcc5<=0.029696){
										if(lowenergy<=1){
											if(rolloffVariance<=128.197368){type = 0;
											}else{type = 1;;}
										}else{type = 1;;}
									}else{
										if(mfcc11<=-0.043327){type = 1;;
										}else{type = 0;}
									}
								}
							}else{
								if(mfcc6<=0.062939){type = 1;;
								}else{type = 0;}
							}
						}else{
							if(spEntv<=0.013754){type = 0;
							}else{
								if(centroidVariance<=548.542269){type = 1;;
								}else{type = 0;}
							}
						}
					}
				}
			}else{
				if(centroidMean<=98.867228){
					if(mfcc9<=0.013487){
						if(bandwidthMean<=48.620809){type = 0;
						}else{
							if(lowenergy<=8){
								if(mfcc1<=-0.041968){type = 0;
								}else{type = 1;;}
							}else{type = 0;}
						}
					}else{type = 0;}
				}else{type = 0;}
			}
		}
		return type;
	}
}

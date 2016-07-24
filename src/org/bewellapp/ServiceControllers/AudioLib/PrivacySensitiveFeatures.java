package org.bewellapp.ServiceControllers.AudioLib;


//import edu.dartmouth.java.lib.Complex;

public class PrivacySensitiveFeatures {

	public static Complex data[];

	// 
	private static double sum_full_data = 0;
	private static double sum_full_data_squared = 0;
	private static double mean_full_data = 0;

	//normalize the audio data
	public static void normalizeData(short[] audioData)
	{
		for(int i = 0; i<audioData.length; i++){
			sum_full_data = sum_full_data + audioData[i];
			sum_full_data_squared = sum_full_data_squared + audioData[i]*audioData[i];
		}
		
		mean_full_data = sum_full_data/audioData.length;
		
		for (int i = 0; i < audioData.length; i++) {
			//x[i] = new Complex(i, 0);
			data[i] = new Complex(audioData[i] - mean_full_data, 0);//zero mean the data
		}
	}
	
	public static double computeSpectralEntropy()
	{
		Complex[] hanningWindiowFrame = hanning(data.length,data);
		Complex[] temp = FFT.fft(hanningWindiowFrame);
		double[] spec = new double[data.length/2];
		double sum_spec = 0;
		for(int i = 0; i< data.length/2; i++){
			spec[i] = Math.sqrt(temp[i].re()*temp[i].re() + temp[i].im()*temp[i].im());
			sum_spec = sum_spec + spec[i];
		}
		
		//normalized spec
		double[] norm_spec = new double[data.length/2];
		double spectral_entropy = 0;
		for(int i = 0; i< data.length/2; i++){
			norm_spec[i] = spec[i]/(sum_spec + 0.00001);
			if(norm_spec[i] < 0.00001)
				norm_spec[i] = 0.00001;
			
			spectral_entropy = spectral_entropy - norm_spec[i]*Math.log(norm_spec[i]);	
		}
		//System.out.println("spectral_entropy: " + spectral_entropy);
		
		return spectral_entropy;
		
	}
	
	public static double compute_energy()
	{
		double energy = 0;
		
		for(int i = 0; i < data.length; i++)
			energy = energy + data[i].re()*data[i].re();
		
		return Math.sqrt(energy);
	}
	
	
	
	
	
	private static Complex[] hanning(int window, Complex[] full_data) {
		int w = 0;
		Complex h_wnd[] = new Complex[window]; //Hanning window

		//h_wnd[0] = new Complex(0,0);
		for (int i = 1; i <= window; i++) { //calculate the hanning window
			h_wnd[i-1] = new Complex(full_data[i-1].re()* 0.5 * (1 - Math.cos(2.0 * Math.PI * i / (window + 1))),0);
		}

		return h_wnd;
	}
	
}

package org.bewellapp.ServiceControllers.AudioLib;

import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.pool.BasePoolableObjectFactory;
import org.apache.commons.pool.PoolUtils;
import org.apache.commons.pool.impl.StackObjectPool;
import org.bewellapp.Ml_Toolkit_Application;
import org.bewellapp.ServiceControllers.AudioLib.AudioService;
import org.bewellapp.Storage.CircularBufferFeatExtractionInference;
import org.bewellapp.Storage.ML_toolkit_object;

//import edu.dartmouthcs.sensorlab.MLToolkitInterface;
//import edu.dartmouthcs.sensorlab.MLToolkitInterface.acc_config;
//import edu.dartmouthcs.sensorlab.MLToolkitInterfaceAudio;
//import edu.dartmouthcs.sensorlab.MLToolkitInterface.acc_config;

import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;
import edu.dartmouth.cs.mltoolkit.processing.AudioFeatureExtraction;
import edu.dartmouth.cs.mltoolkit.processing.AudioInference;
import edu.dartmouthcs.UtilLibs.MyDataTypeConverter;

public class RehearsalAudioRecorder
{

	public final int AUDIO_SAMPLES_REQUIRED=512; //also the freamesize
	public final int AUDIO_SAMPLERATE=8192;
	//private MLToolkitInterface mlt;
	//private MLToolkitInterfaceAudio mlt;
	private Ml_Toolkit_Application appState;

	public class audio_config{
		public int samplingRate;
		public int frameLength;
		public int windowLength;
		public int mfccLength;

		// void (*callback)(int)

		public audio_config(int sr, int fl, int wl, int ml){
			samplingRate = sr;
			frameLength = fl;
			windowLength = wl;
			mfccLength = ml;

		}		
	}


	/**
	 * INITIALIZING : recorder is initializing;
	 * READY : recorder has been initialized, recorder not yet started
	 * RECORDING : recording
	 * ERROR : reconstruction needed
	 * STOPPED: reset needed
	 */
	public enum State {INITIALIZING, READY, RECORDING, ERROR, STOPPED};

	public static final boolean RECORDING_UNCOMPRESSED = true;
	public static final boolean RECORDING_COMPRESSED = false;

	// Toggles uncompressed recording on/off; RECORDING_UNCOMPRESSED / RECORDING_COMPRESSED
	private boolean 		 rUncompressed;

	// Recorder used for uncompressed recording
	private AudioRecord 	 aRecorder = null;
	// Recorder used for compressed recording
	private MediaRecorder	 mRecorder = null;

	// Stores current amplitude (only in uncompressed mode)
	private int				 cAmplitude= 0;
	// Output file path
	private String			 fPath = null;

	// Recorder state; see State
	private State			 state;

	// Number of channels, sample rate, sample size(size in bits), buffer size, audio source, sample size(see AudioFormat)
	private short 			 nChannels;
	private int				 sRate;
	private short			 bSamples;
	private int				 bufferSize;
	private int				 aSource;
	private int				 aFormat;

	// Number of frames written to file on each output(only in uncompressed mode)
	private int				 framePeriod;

	// Buffer for output(only in uncompressed mode)
	//private byte[] 			 buffer;
	private short[] 			 buffer;

	// Number of bytes written to file after header(only in uncompressed mode)
	// after stop() is called, this size is written to the header/data chunk in the wave file
	private int				 payloadSize;
	private int 			 updateFlag;
	private static AudioService ASobj;
	private ML_toolkit_object AudioObject;

	//raw audio data buffer
	private final int rawAudioBufferSize = 1600;

	private final int MAXIMUM_NO_OF_INFERENCE_RESULT_BEFORE_ROLL_UP = rawAudioBufferSize/20;

	//variables for not recording audio samples
	public static final int NO_SAMPLES_TO_IGNORE_WHEN_VOICE = 2; //means 2 frames before and after
	public static final int AMOUNT_OF_BUFFERING_BEFORE_AUDIO_WRITE = 10; //no of inference results to buffer before the write happens

	public long tempTimestamp;
	
	// Pool of AudioData.
	private static final AudioDataPool mAudioDataPool = new AudioDataPool();
	private static final AtomicBoolean mRunQueuePopper = new AtomicBoolean(false);
	private static final AudioRawDataPool mAudioRawDataPool = new AudioRawDataPool();
	
	//audio buffer
	private static int buffersize;

	//Duty-cycle interval counters
	public int intervalSilenceCnt = 0;
	public int intervalVoiceCnt = 0;
	public int intervalTotalCnt = 0;

	/**
	 * 
	 * Returns the state of the recorder in a RehearsalAudioRecord.State typed object.
	 * Useful, as no exceptions are thrown.
	 * 
	 * @return recorder state
	 */
	public State getState()
	{
		return state;
	}

	/*
	 * 
	 * Method used for recording.
	 * 
	 */
	
	private int sync_id_counter = 0;
	public static class AudioData{
		public short data[];
		public long timestamp;
		public int sync_id;
		public AudioData(){
		}
		
		public AudioData setValues(short[] data, long timestamp,int sync_id){
			this.data = data;
			this.timestamp = timestamp;
			this.sync_id = sync_id;
			return this;
		}
	}
	
	private static final CircularBufferFeatExtractionInference<AudioData> cirBuffer = new CircularBufferFeatExtractionInference<RehearsalAudioRecorder.AudioData>(null, 100);
	
	//reader of data and feature extraction, inference thread
	public static class MyQueuePopper extends Thread {

		CircularBufferFeatExtractionInference<AudioData> obj;
		AudioFeatureExtraction af; 
		double[] audioFrameFeature;// = new double[af.getFrame_feature_size()];
		double [] audioWindowFeature;// = new double[af.getWindow_feature_size()];
		private AudioData audioFromQueueData;
		private Ml_Toolkit_Application appState;
		private ML_toolkit_object audio_features;
		private ML_toolkit_object audio_inference;
		private RehearsalAudioRecorder ar;
		
		public MyQueuePopper(CircularBufferFeatExtractionInference<AudioData>  obj, Ml_Toolkit_Application appState, RehearsalAudioRecorder ar)
		{
			super("RehearsalAudioQueuePopper");
			this.obj=obj;
			this.appState = appState;
			this.ar = ar;

			//feature computation
			//PrivacySensitiveFeatures.data = new Complex[AUDIO_SAMPLES_REQUIRED];
			af = new AudioFeatureExtraction(512,20,14,8000);
			audioFrameFeature = new double[af.getFrame_feature_size()];
			audioWindowFeature = new double[af.getWindow_feature_size()];
			
		}
		
		double rms(short[] a){
			double rms = 0;
			for (short x : a) {
				rms += x*x;
			}
			return Math.sqrt(rms/a.length);		
		}
		
		double mean(double[] a){
			double sum = 0;
			for (double x : a) {
				sum += x;
			}
			return sum/a.length;
		}

		protected void sendFeatures(long timestamp) {
		    double[] features = new double[audioWindowFeature.length];
		    System.arraycopy(audioWindowFeature, 0, features, 0, audioWindowFeature.length);
		    
		    //Intent intent = new Intent(AudioFeatureReceiver.class.getName());
		    
		    Intent intent = new Intent(
				AudioFeatureReceiver.class.getName());
		    intent.putExtra("timestamp", timestamp);
		    intent.putExtra("audio_features", features);
		    ASobj.sendBroadcast(intent);
		}
		
		@Override
		public void run() {
			// TODO Auto-generated method stub
			final int row = af.getWindow_length();
			final int col = af.getFrame_length();
			short[][] buffer = new short[row][col];
			double[] rms = new double[row];
			int index = 0;
			
			
			while(mRunQueuePopper.get()) {
				//q.put(i++);
				
				//Log.d("InferenceTAG", "Thread starteddddddddddddddddddddddddd" );
				audioFromQueueData = obj.deleteAndHandleData();
				if (audioFromQueueData == null) {
					// if there is no data it means that we've been interrupted: break the cycle
					break;
				}
				System.arraycopy(audioFromQueueData.data, 0, buffer[index%row], 0, audioFromQueueData.data.length);
				rms[index++%row] = rms(audioFromQueueData.data);
				
				if ((index >= row) && (index % row == 0)){
					if(mean(rms) > 80){
						for(int i = 0; i<row; i++)
						af.getFrameFeatures(buffer[i], audioFrameFeature);
					}
					else{
						af.setFrameIndex(0);
						appState.audio_inference = "silence";
						ar.intervalSilenceCnt++;
						ar.intervalTotalCnt++;
						appState.amount_of_different_voice_activity[0]++; 
						appState.amount_of_different_all_voice_activities++;
						//audio_inference =  new ML_toolkit_object(System.currentTimeMillis()+appState.timeOffset, 5, true, appState.audio_inference);
						//matching timestamp with raw data
						audio_inference =  appState.mMlToolkitObjectPool.borrowObject().setValues(audioFromQueueData.timestamp, 5, true, appState.audio_inference,audioFromQueueData.sync_id);
						appState.ML_toolkit_buffer.insert(audio_inference);
						//Log.d("InferenceTAG", "==========================slience detected===========================" );
						
						//audio_features =  new ML_toolkit_object(System.currentTimeMillis()+appState.timeOffset, 3, MyDataTypeConverter.toByta(audioWindowFeature));
						//no window feature is calculated, should not insert.
						//audio_features =  appState.mMlToolkitObjectPool.borrowObject().setValues(audioFromQueueData.timestamp, 3, MyDataTypeConverter.toByta(audioWindowFeature),audioFromQueueData.sync_id);
						//appState.ML_toolkit_buffer.insert(audio_features);
					}
										
				}
				
				
				//System.arraycopy(ShortAndDouble(audioFromQueueData.data), 0, PrivacySensitiveFeatures.data, 0, audioFromQueueData.data.length);
				//PrivacySensitiveFeatures.data = ShortAndDouble(audioFromQueueData.data);
				//Log.e(" Audio data " ," read " + audioFromQueueData.timestamp + " " + audioFromQueueData.data.length);
				//Log.e("inference results", "window data:"+Arrays.toString(audioFromQueueData.data));
				
				//PrivacySensitiveFeatures.normalizeData(audioFromQueueData.data);
				
				//Log.e(" Audio data " ," SE " + audioFromQueueData.timestamp + " " + PrivacySensitiveFeatures.computeSpectralEntropy());
				//Log.e(" Audio data " ," E " + audioFromQueueData.timestamp + " " + PrivacySensitiveFeatures.compute_energy());
  			    //Log.e("inference results", "frame feature:"+Arrays.toString(audioFrameFeature));
				//Log.d(TAG, Arrays.toString(audioFrameFeature));
				if ((af.getFrameIndex())>= af.getWindow_length()  && (af.getFrameIndex()) % af.getWindow_length() == 0){ 	
					//Arrays.fill(audioWindowFeature, 99);
					af.getWindowFeatures(audioWindowFeature);
					sendFeatures(System.currentTimeMillis()/1000);					
					//Log.e("inference results", "window feature:"+Arrays.toString(audioWindowFeature));
					//Log.e("inference results", "Audio Classification:"+AudioInference.dtAudioClassifier(audioWindowFeature));
					

					
					
					if(ASobj.inDutyCycle == false && appState.enableAudioDutyCycling == true)
					{	
						mAudioRawDataPool.returnObject(audioFromQueueData.data);
						mAudioDataPool.returnObject(audioFromQueueData);
						appState.audio_inference = "DutyCycling OFF";
						continue;
					}
					
					
					//voice and non-voice
					
					//0 = silence, 1 = noise, 2 = voice, 3 = error in "amount_of_different_voice_activity" array
					
					if(AudioInference.dtAudioClassifier(audioWindowFeature)==1){
						appState.audio_inference = "voice";
						ar.intervalVoiceCnt++;
						appState.amount_of_different_voice_activity[2]++;  
					}
					else if(AudioInference.dtAudioClassifier(audioWindowFeature)==0){
						appState.audio_inference = "noise";
						appState.amount_of_different_voice_activity[1]++; 
					}
					else if(AudioInference.dtAudioClassifier(audioWindowFeature)==-1){
						appState.audio_inference = "silence";
						ar.intervalSilenceCnt++;
						appState.amount_of_different_voice_activity[0]++; 
					}
					else{
						appState.audio_inference = "error";
						appState.amount_of_different_voice_activity[3]++; 
					}
					ar.intervalTotalCnt++;
					appState.amount_of_different_all_voice_activities++;
					
					
					//audio_inference =  new ML_toolkit_object(System.currentTimeMillis()+appState.timeOffset, 5, true, appState.audio_inference);
					
					//matching timestamp with raw data
					audio_inference =  appState.mMlToolkitObjectPool.borrowObject().setValues(audioFromQueueData.timestamp, 5, true, appState.audio_inference,audioFromQueueData.sync_id);
					appState.ML_toolkit_buffer.insert(audio_inference);
					
					
					//audio_features =  new ML_toolkit_object(System.currentTimeMillis()+appState.timeOffset, 3, MyDataTypeConverter.toByta(audioWindowFeature));
					audio_features =  appState.mMlToolkitObjectPool.borrowObject().setValues(audioFromQueueData.timestamp, 3, MyDataTypeConverter.toByta(audioWindowFeature),audioFromQueueData.sync_id);
					appState.ML_toolkit_buffer.insert(audio_features);
					
					af.setFrameIndex(0);
						
				}
				mAudioRawDataPool.returnObject(audioFromQueueData.data);
				mAudioDataPool.returnObject(audioFromQueueData);
				
				//debug code
				/*
				for(int i=0; i<audioFromQueueData.data.length;){
					Log.e(" Audio data " ," " + i + " " + audioFromQueueData.data[i] + " " + PrivacySensitiveFeatures.data[i]);
					i = i + 50;
				}
					//Log.e(" Audio data " ," " + i + " " + audioFromQueueData.data[i] + " ");
				*/
				
				
				
				//Log.e("InferenceTAG", "Got data " +  audioFromQueueData.data.length+ " " + audioFromQueueData.toString());
			}
		}
	}
	private static MyQueuePopper myQueuePopper;
	
	
	private AudioRecord.OnRecordPositionUpdateListener updateListener = new AudioRecord.OnRecordPositionUpdateListener()
	{
		public void onPeriodicNotification(AudioRecord recorder)
		{
			aRecorder.read(buffer, 0, buffer.length); // Fill buffer
			try
			{ 
				//fWriter.write(buffer); // Write buffer to file
				//fWriter.write(""+System.currentTimeMillis()+"\n");
				//flush is in the audio callback

				//fWriter.flush();
				//payloadSize += buffer.length;

				//System.arraycopy(buffer,0,normalize_temp_buffer,0,buffer.length);

				//for(int i = 0; i<512; i++)
				//buffer[i] = (short)Math.round(((double)buffer[i])*0.8);


				//no inference
				//appState.mlt.getAudioSample(buffer);


				//Log.i("RawAudioWrite" , "Raw Audio Sensing " + rawAudioIndexBufferWrite);
				tempTimestamp = System.currentTimeMillis()+appState.timeOffset;
				
				++sync_id_counter;//increment anyway, not dependent on whether raw audio recording is enabled or not. it will be needed for inference and feature syncing
				
				if(appState.rawAudioOn){
				//will roll back to the first one
					AudioObject =  appState.mMlToolkitObjectPool.borrowObject().setValues(tempTimestamp, 0, MyDataTypeConverter.toByta(buffer),(sync_id_counter)%16384);
					appState.ML_toolkit_buffer.insert(AudioObject); //inserting into the buffer
				}
				
				//always insert in the circular buffer for inference
				
				//Log.e("Debug", "Sensing "+sync_id_counter );
				cirBuffer.insert(mAudioDataPool.borrowObject().setValues(buffer,tempTimestamp,sync_id_counter%16384));
				buffer = mAudioRawDataPool.borrowObject();
				
				//else // buffer the data and wait for inference results
				//{

				//rawAudioBuffer[rawAudioIndexBufferWrite] = buffer;
				//System.arraycopy(buffer,0,rawAudioBuffer[rawAudioIndexBufferWrite],0,buffer.length);
				//rawAudioBufferTimestamps[rawAudioIndexBufferWrite] = System.currentTimeMillis()+appState.timeOffset;
				//rawAudioIndexBufferWrite = (rawAudioIndexBufferWrite+1)%rawAudioBufferSize;  

				//}


				payloadSize += buffer.length;

				updateFlag++; //means no of recods


				appState.audio_no_of_records = payloadSize;

				//updateFlag++;
				if (updateFlag%28125 == 1){
					//ASobj.no_of_records = payloadSize;

					ASobj.updateNotificationArea();
				}

				/*if (bSamples == 16)
				{
					for (int i=0; i<buffer.length/2; i++)
					{ // 16bit sample size
						short curSample = getShort(buffer[i*2], buffer[i*2+1]);
						if (curSample > cAmplitude)
						{ // Check amplitude
							cAmplitude = curSample;
						}
					}
				}
				else
				{ // 8bit sample size
					for (int i=0; i<buffer.length; i++)
					{
						if (buffer[i] > cAmplitude)
						{ // Check amplitude
							cAmplitude = buffer[i];
						}
					}
				}*/
			}
			catch (Exception e)
			{
				Log.e(RehearsalAudioRecorder.class.getName(), "Error occured in updateListener, recording is aborted" +e.toString());
				stop();
			}
		}

		public void onMarkerReached(AudioRecord recorder)
		{
			// NOT USED
		}
	};

	/** 
	 * 
	 * 
	 * Default constructor
	 * 
	 * Instantiates a new recorder, in case of compressed recording the parameters can be left as 0.
	 * In case of errors, no exception is thrown, but the state is set to ERROR
	 * 
	 */ 
	public RehearsalAudioRecorder(Ml_Toolkit_Application apppState, AudioService obj, boolean uncompressed, int audioSource, int sampleRate, int channelConfig,
			int audioFormat)
	{
		ASobj = obj;
		this.appState = apppState;
		try
		{
			rUncompressed = uncompressed;
			if (rUncompressed)
			{ // RECORDING_UNCOMPRESSED
				if (audioFormat == AudioFormat.ENCODING_PCM_16BIT)
				{
					bSamples = 16;
				}
				else
				{
					bSamples = 8;
				}

				if (channelConfig == AudioFormat.CHANNEL_CONFIGURATION_MONO)
				{
					nChannels = 1;
				}
				else
				{
					nChannels = 2;
				}

				aSource = audioSource;
				sRate   = sampleRate;
				aFormat = audioFormat;

				framePeriod = 512;//sampleRate * TIMER_INTERVAL / 1000;
				//bufferSize = framePeriod * 2 * bSamples * nChannels / 8;
				//bufferSize = framePeriod * 25 * bSamples * nChannels / 8;
				bufferSize = framePeriod * 100 * bSamples * nChannels / 8;
				
				Log.i("BUFFFFER SIZEEEEEEEE", "Buffer size" + bufferSize);
				if (bufferSize < AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat))
				{ // Check to make sure buffer size is not smaller than the smallest allowed one 
					bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat);
					// Set frame period and timer interval accordingly
					framePeriod = bufferSize / ( 2 * bSamples * nChannels / 8 );
					Log.w(RehearsalAudioRecorder.class.getName(), "Increasing buffer size to " + Integer.toString(bufferSize));
				}

				aRecorder = new AudioRecord(audioSource, sampleRate, channelConfig, audioFormat, bufferSize);
				if (aRecorder.getState() != AudioRecord.STATE_INITIALIZED)
					throw new Exception("AudioRecord initialization failed");
				aRecorder.setRecordPositionUpdateListener(updateListener);
				aRecorder.setPositionNotificationPeriod(framePeriod);
				this.updateFlag = 0;

				//setcallback for audio
				//appState.mlt.setAudioHandler(audioCallbackHandler);


				///Machine learning tool kit init
				//mlt = new MLToolkitInterfaceAudio(audioCallbackHandler); 
				//mlt = new MLToolkitInterface(audioCallbackHandler); 
				//edu.dartmouthcs.sensorlab.MLToolkitInterface.audio_config audio_Cfg = mlt.new audio_config(sampleRate,framePeriod, 20, 14);
				//edu.dartmouthcs.sensorlab.MLToolkitInterfaceAudio.audio_config audio_Cfg = mlt.new audio_config(sampleRate,framePeriod, 20, 14);
				//mlt.init(null, audio_Cfg, null);
				//mlt.init(acc_Cfg, audio_Cfg, null);


			} else
			{ // RECORDING_COMPRESSED
				mRecorder = new MediaRecorder();
				mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
				mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
				mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
			}
			cAmplitude = 0;
			fPath = null;
			state = State.INITIALIZING;
		} catch (Exception e)
		{
			if (e.getMessage() != null)
			{
				Log.e(RehearsalAudioRecorder.class.getName(), e.getMessage());
			}
			else
			{
				Log.e(RehearsalAudioRecorder.class.getName(), "Unknown error occured while initializing recording");
			}
			state = State.ERROR;
		}
	}

	
	public void setOutputFile(String argPath)
	{
		try
		{
			if (state == State.INITIALIZING)
			{
				fPath = argPath;
				if (!rUncompressed)
				{
					mRecorder.setOutputFile(fPath);
				}
			}
		}
		catch (Exception e)
		{
			if (e.getMessage() != null)
			{
				Log.e(RehearsalAudioRecorder.class.getName(), e.getMessage());
			}
			else
			{
				Log.e(RehearsalAudioRecorder.class.getName(), "Unknown error occured while setting output path");
			}
			state = State.ERROR;
		}
	}

	/**
	 * 
	 * Returns the largest amplitude sampled since the last call to this method.
	 * 
	 * @return returns the largest amplitude since the last call, or 0 when not in recording state. 
	 * 
	 */
	public int getMaxAmplitude()
	{
		if (state == State.RECORDING)
		{
			if (rUncompressed)
			{
				int result = cAmplitude;
				cAmplitude = 0;
				return result;
			}
			else
			{
				try
				{
					return mRecorder.getMaxAmplitude();
				}
				catch (IllegalStateException e)
				{
					return 0;
				}
			}
		}
		else
		{
			return 0;
		}
	}


	/**
	 * 
	 * Prepares the recorder for recording, in case the recorder is not in the INITIALIZING state and the file path was not set
	 * the recorder is set to the ERROR state, which makes a reconstruction necessary.
	 * In case uncompressed recording is toggled, the header of the wave file is written.
	 * In case of an exception, the state is changed to ERROR
	 * 	 
	 */
	public void prepare()
	{
		try
		{
			if (state == State.INITIALIZING)
			{
				if (rUncompressed)
				{
					if ((aRecorder.getState() == AudioRecord.STATE_INITIALIZED) & (fPath != null))
					{
						/*try{
							fOut = new FileOutputStream(fPath);
							fWriter = new  OutputStreamWriter(fOut);
						}
						catch(Exception ex){}*/

						/*
						// write file header

						fWriter = new RandomAccessFile(fPath, "rw");

						fWriter.setLength(0); // Set file length to 0, to prevent unexpected behavior in case the file already existed
						fWriter.writeBytes("RIFF");
						fWriter.writeInt(0); // Final file size not known yet, write 0 
						fWriter.writeBytes("WAVE");
						fWriter.writeBytes("fmt ");
						fWriter.writeInt(Integer.reverseBytes(16)); // Sub-chunk size, 16 for PCM
						fWriter.writeShort(Short.reverseBytes((short) 1)); // AudioFormat, 1 for PCM
						fWriter.writeShort(Short.reverseBytes(nChannels));// Number of channels, 1 for mono, 2 for stereo
						fWriter.writeInt(Integer.reverseBytes(sRate)); // Sample rate
						fWriter.writeInt(Integer.reverseBytes(sRate*bSamples*nChannels/8)); // Byte rate, SampleRate*NumberOfChannels*BitsPerSample/8
						fWriter.writeShort(Short.reverseBytes((short)(nChannels*bSamples/8))); // Block align, NumberOfChannels*BitsPerSample/8
						fWriter.writeShort(Short.reverseBytes(bSamples)); // Bits per sample
						fWriter.writeBytes("data");
						fWriter.writeInt(0); // Data chunk size not known yet, write 0
						 */

						//buffer = new byte[framePeriod*bSamples/8*nChannels];
						buffersize = framePeriod*bSamples/16*nChannels;
						buffer = mAudioRawDataPool.borrowObject();
						state = State.READY;
					}
					else
					{
						Log.e(RehearsalAudioRecorder.class.getName(), "prepare() method called on uninitialized recorder");
						state = State.ERROR;
					}
				}
				else
				{
					mRecorder.prepare();
					state = State.READY;
				}
			}
			else
			{
				Log.e(RehearsalAudioRecorder.class.getName(), "prepare() method called on illegal state");
				release();
				state = State.ERROR;
			}
		}
		catch(Exception e)
		{
			if (e.getMessage() != null)
			{
				Log.e(RehearsalAudioRecorder.class.getName(), e.getMessage());
			}
			else
			{
				Log.e(RehearsalAudioRecorder.class.getName(), "Unknown error occured in prepare()");
			}
			state = State.ERROR;
		}
	}

	/**
	 * 
	 * 
	 *  Releases the resources associated with this class, and removes the unnecessary files, when necessary
	 *  
	 */
	public void release()
	{
		//end the current state in the db
		//audio_inference =  new ML_toolkit_object(System.currentTimeMillis()+appState.timeOffset, 5, false, ASobj.inferred_audio_Status);
		//appState.ML_toolkit_buffer.insert(audio_inference);

		//audio_inference =  new ML_toolkit_object(System.currentTimeMillis()+appState.timeOffset, 5, true, "unknown");
		//appState.ML_toolkit_buffer.insert(audio_inference);
		
		
		appState.audio_release = true;
		if (state == State.RECORDING)
		{
			stop();
		}
		else
		{
			if ((state == State.READY) & (rUncompressed))
			{
				/*try
				{
					fWriter.close(); // Remove prepared file
				}
				catch (IOException e)
				{
					Log.e(RehearsalAudioRecorder.class.getName(), "I/O exception occured while closing output file");
				}
				(new File(fPath)).delete();*/
			}
		}

		if (rUncompressed)
		{
			if (aRecorder != null)
			{
				aRecorder.release();

			}
		}
		else
		{
			if (mRecorder != null)
			{
				mRecorder.release();
			}
		}
	}

	/**
	 * 
	 * 
	 * Resets the recorder to the INITIALIZING state, as if it was just created.
	 * In case the class was in RECORDING state, the recording is stopped.
	 * In case of exceptions the class is set to the ERROR state.
	 * 
	 */
	public void reset()
	{
		try
		{
			if (state != State.ERROR)
			{
				release();
				fPath = null; // Reset file path
				cAmplitude = 0; // Reset amplitude
				if (rUncompressed)
				{
					aRecorder = new AudioRecord(aSource, sRate, nChannels+1, aFormat, bufferSize);
				}
				else
				{
					mRecorder = new MediaRecorder();
					mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
					mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
					mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
				}
				state = State.INITIALIZING;
			}
		}
		catch (Exception e)
		{
			Log.e(RehearsalAudioRecorder.class.getName(), e.getMessage());
			state = State.ERROR;
		}
	}

	/**
	 * 
	 * 
	 * Starts the recording, and sets the state to RECORDING.
	 * Call after prepare().
	 * 
	 */
	public void start()
	{
		if (state == State.READY)
		{
			if (rUncompressed)
			{
				//start a new thread for reading audio stuff
				if (myQueuePopper == null) {
					mRunQueuePopper.set(true);
					myQueuePopper = new MyQueuePopper(cirBuffer, appState, this);
					myQueuePopper.start();
				}
				myQueuePopper.af.setFrameIndex(0);
				myQueuePopper.ar = this;
				payloadSize = 0;
				aRecorder.startRecording();
				aRecorder.read(buffer, 0, buffer.length);

				appState.audio_release = false;
			}
			else
			{
				mRecorder.start();
			}
			state = State.RECORDING;
		}
		else
		{
			Log.e(RehearsalAudioRecorder.class.getName(), "start() called on illegal state");
			state = State.ERROR;
		}
	}

	/**
	 * 
	 * 
	 *  Stops the recording, and sets the state to STOPPED.
	 * In case of further usage, a reset is needed.
	 * Also finalizes the wave file in case of uncompressed recording.
	 * 
	 */
	public void stop()
	{
		try{
		if (state == State.RECORDING)
		{
			if (rUncompressed)
			{
				aRecorder.stop();
			}
			else
			{
				mRecorder.stop();
			}
			state = State.STOPPED;
		}
		else
		{
			Log.e(RehearsalAudioRecorder.class.getName(), "stop() called on illegal state");
			state = State.ERROR;
		}}
		catch(Exception ex){}
	}


	public double[] ShortAndDouble(short[] array) {

		double[] doubleArray = new double[array.length];
		for (int i = 0; i < array.length; i++) {
			doubleArray[i] = (double)(array[i]); /// 32768;
		}
		
		return doubleArray;
	}
	
	private static class AudioDataFactory extends BasePoolableObjectFactory {
		public Object makeObject() {
			return new AudioData();
		}
	}

	private static class AudioDataPool extends StackObjectPool {
		public AudioDataPool() {
			super(new AudioDataFactory());
		}

		@Override
		public AudioData borrowObject() {
			try {
				return (AudioData) super.borrowObject();
			} catch (Exception e) {
				e.printStackTrace();
				return null;
			}
		}

		@Override
		public void returnObject(Object obj) {
			try {
				super.returnObject(obj);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		@Override
		public void close() {
			try {
				super.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	private static class AudioRawDataFactory extends BasePoolableObjectFactory {
		public Object makeObject() {
			return new short[buffersize];
		}
	}
	
	private static class AudioRawDataPool extends StackObjectPool {
		public AudioRawDataPool() {
			super(new AudioRawDataFactory());
		}

		@Override
		public short[] borrowObject() {
			try {
				return (short[]) super.borrowObject();
			} catch (Exception e) {
				e.printStackTrace();
				return null;
			}
		}

		@Override
		public void returnObject(Object obj) {
			try {
				super.returnObject(obj);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		@Override
		public void close() {
			try {
				super.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

}


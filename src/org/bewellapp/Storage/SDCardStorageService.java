package org.bewellapp.Storage;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.bewellapp.Ml_Toolkit_Application;


import android.app.IntentService;
import android.content.Intent;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;

public class SDCardStorageService extends IntentService{

	//public static final String UPLOAD_FILES = "org.bewellapp.ServiceControllers.UploadingLib.UploadService";

	//public UploadService(String name) {
	//super("worker");
	// TODO Auto-generated constructor stub
	//postData();
	//}
	static final int BUFF_SIZE = 100000;
	static final byte[] buffer = new byte[BUFF_SIZE];

	private static final String TAG = "XY_QUEUE_SD";
	private Ml_Toolkit_Application appState;
	private boolean inputFileDeleteFlag = true; 

	public String UploadFileLocation;
	public SDCardStorageService() {
		super("SD card storage Service");
		// TODO Auto-generated constructor stub
	}


	@Override
	public void onCreate() { 
		super.onCreate(); 
		appState = (Ml_Toolkit_Application)getApplicationContext();
		Log.i(TAG, "SD card copier started" );
	} 


	//@Override	
	//protected void onHandleIntent(Intent intent) {
	// TODO Auto-generated method stub
	//postData();
	//}

	private void postData(String db_path) {  


		Log.i(TAG, "db_path :" +  db_path);

		InputStream in = null;
		appState.currentOpenSDCardFile = null; 
		int sep =  db_path.lastIndexOf("/");;
		try {
			in = new FileInputStream(db_path);
			//sep 
			File currentSDcardFile = new File(new File(Environment.getExternalStorageDirectory(), "mltoolkit"), db_path.substring(sep + 1));
			appState.currentOpenSDCardFile = new FileOutputStream(currentSDcardFile);
			while (true) {
				//synchronized (buffer) {
				int amountRead = in.read(buffer);
				if (amountRead == -1) {	

					appState.currentOpenSDCardFile.close();
					appState.currentOpenSDCardFile = null;
					//add file to the upload list
					this.inputFileDeleteFlag = true;
					//write is done successfully ... so add to the list
					File currentFile = new File(new File(Environment.getExternalStorageDirectory(), "mltoolkit"), db_path.substring(sep + 1));
					String currentPath = currentFile.getAbsolutePath();
					appState.fileList.add(currentPath);
					Log.d(TAG, "file copied" );
					//try to upload files, if condition is available then upload will proceed
					appState.getServiceController().startUploadingIntellgent();	

					break;
				}
				appState.currentOpenSDCardFile.write(buffer, 0, amountRead); 
				//}
			} 
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			Log.e(TAG, "Error" + e.toString() );
			this.inputFileDeleteFlag = false;
			//means sd card is not available
		}catch (IOException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
			Log.e(TAG, "Error" + e.toString() );

			//in the middle of file writing file to SD card, SD card was mounted
			//do not delete the input file
			this.inputFileDeleteFlag = false;
		} 
		finally {
			if (in != null) {
				try {
					in.close();

					//delete the file from main memory to save space
					if(this.inputFileDeleteFlag){ 
						//means there was a SD card failure so don't delete the files
						//File f = new File(db_path);
						//f.delete();
					    	deleteDatabase(db_path);
						Log.e(TAG, "Error deleted:" + db_path);
					}					
					else //don't keep a file like that
					{
//						File f = new File(db_path);
//						f.delete();
						deleteDatabase(db_path);
						Log.e(TAG, "Error deleted:" + db_path);
					}

				} catch (IOException e) {
					// TODO Auto-generated catch block
					//e.printStackTrace();
				}
			}
			if (appState.currentOpenSDCardFile  != null) {
			}
		}
	}

	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	protected void onHandleIntent(Intent intent) {
		// TODO Auto-generated method stub
		//;
		Log.d(TAG, "Starting Copy" + intent.getStringExtra("dbpath"));
		postData(intent.getStringExtra("dbpath"));
	}  

}
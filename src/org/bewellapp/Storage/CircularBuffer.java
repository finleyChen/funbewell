package org.bewellapp.Storage;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import org.bewellapp.Ml_Toolkit_Application;
import org.bewellapp.CrashHandlers.CrashAutoStartUpService;

import edu.dartmouthcs.UtilLibs.MyUnSyncBufferedWriter;
import android.content.ContentValues;
import android.content.Context;
import android.util.Log;

public class CircularBuffer<T> {
	private int qMaxSize;// max queue size
	private int fp = 0;  // front pointer
	private int rp = 0;  // rear pointer
	private int qs = 0;  // size of queue
	private T[] q;    // actual queue
	private static Ml_Toolkit_Application appState;


	//thread to write in the database
	private Thread t; 

	private static final String TAG = "CircBuffer";	
	private String db_path;
	private List<T> tempList;
	private MyFileWriter myWriterThread;
	//ContentValues val; 

	@SuppressWarnings("unchecked")
	public CircularBuffer(Context context, int size) {
		qMaxSize = size;
		fp = 0;
		rp = 0;
		qs = 0;
		//q = new char[qMaxSize];
		q = (T[]) new Object[qMaxSize];
		appState = (Ml_Toolkit_Application) context;
		tempList = new ArrayList<T>();
		
		//start the writer thread
		myWriterThread = new MyFileWriter(this);
		myWriterThread.start();
	}

	public T delete() {
		if (!emptyq()) {

			//will not decrease size to avoid race condition
			qs--;


			fp = (fp + 1)%qMaxSize;
			return q[fp];
		}
		else {
			//System.err.println("Underflow");
			return null;
		}
	}

	public synchronized void insert(T c) {
		if (!fullq()) {
			qs++;
			rp = (rp + 1)%qMaxSize;
			q[rp] = c;


			//if(qs > 200)
			//{
			//	Log.d(TAG, "Exceeds 200 elements");
			//}

			//avoid race condition
			//only will start writing when buffer has 2000 elements
			if(qs == appState.writeAfterThisManyValues)
			{
				//writeDataInDatabase();
				Log.d(TAG, "Calling notifier thread" );
				notifyAll(); // start the write thread
			}


		}
		//else
		//System.err.println("Overflow\n");
	}


	private void writeDataInDatabase() {
		// TODO Auto-generated method stub
		//(qs > 200)
		/*
		t = new Thread() {
			public void run(){

				qs = qs - appState.writeAfterThisManyValues;
				Log.d(TAG, "start: " + appState.db_adapter.getDbSize());
				//AccelerometerManager.startListening(accelServ);
				//ContentValues val = new ContentValues();
				for(int i = 0; i < appState.writeAfterThisManyValues; ++i){


					//database code
					//val.clear();
					//val.put("_id", appState.database_primary_key_id++);
					//val.put("data", ML_toolkit_object.toBytes(delete()));
					//val.put("data", "mash");
					//abdval.put("name", "mash");
					//val.p
					//MyObject object = new MyObject(val);
					//if(appState.db_adapter.database_online != false)
						//appState.db_adapter.insertEntry(object);//inserting all the objects




				}
				Log.d(TAG, "end: " + appState.db_adapter.getDbSize() + " " );



				//if database size goes above certain size say 1 MB then upload
				if(appState.db_adapter.getDbSize() > appState.maximumDbSize/10)
				{
					db_path = appState.db_adapter.getPathOfDatabase();
					appState.db_adapter.close();
					appState.database_primary_key_id = 0;
					appState.db_adapter = new MyDBAdapter(appState,"myDatabase_" + MiscUtilityFunctions.now() + ".db");	
					appState.db_adapter.open();

					Log.d(TAG, "Current db: " + appState.db_adapter.getPathOfDatabase());
					appState.getServiceController().startUploading(db_path);
					//appState.getServiceController().
				}


			}
		};
		//t.setPriority(Thread.NORM_PRIORITY+1);
		t.start();
		 */

	}

	public boolean emptyq() {
		return qs == 0;
	}

	public boolean fullq() {
		return qs == qMaxSize;
	}

	public int getQSize() {
		return qs;
	}

	public void printq() {
		System.out.print("Size: " + qs +
				", rp: " + rp + ", fp: " + fp + ", q: ");
		for (int i = 0; i < qMaxSize; i++)
			System.out.print("q[" + i + "]=" 
					+ q[i] + "; ");
		System.out.println();
	}




	//wrtier thread
	public class MyFileWriter extends Thread {

		CircularBuffer<T> obj;

		public MyFileWriter(CircularBuffer<T>  obj)
		{
			this.obj=obj;
			//new Thread(this, "Producer").start();
		}

		@Override
		public void run() {
			// TODO Auto-generated method stub
			while(true) {
				//q.put(i++);
				//Log.d(TAG, "Thread starteddddddddddddddddddddddddd" );
				obj.writeToFile();
			}

		}

	}

	public synchronized void writeToFile() {
		
		//means that buffer doesn't yet have appState.writeAfterThisManyValues elements so sleep
		if(qs <= appState.writeAfterThisManyValues)
		{
			try {
				Log.d(TAG, "Thread going to sleep" );
				wait();
			} catch(InterruptedException e) {
				//System.out.println("InterruptedException caught");
			}
		}
		
		if(appState.db_adapter == null) {
			try {
				Thread.sleep(1500);
			} catch (InterruptedException e) {
			}
			appState.initializeDataBase();
			appState.db_adapter.open();
			return;
		}

		
		Log.d(TAG, "Writing filesssssssssssssssssssssssssssssssssssssssssssssssssssssssss" );
		
		
		//start writing into a list
		
		for(int i = 0; i < appState.writeAfterThisManyValues; ++i){
			//this.tempList.add(delete());
			//val = new ContentValues();
			//val.clear();
			//val.put("_id", appState.database_primary_key_id++);
			//val.put("data", ML_toolkit_object.toBytes(delete()));
			//val.put("data", "mash");
			//abdval.put("name", "mash");
			//val.p
			//MyObject object = new MyObject(val);
			if(appState.db_adapter.database_online != false) {
				T obj = delete();
				//add check code here!!!
				appState.db_adapter.insertMltObj(obj);//inserting all the objects
				appState.mMlToolkitObjectPool.returnObject(obj);
			}
		}
		//Log.d(TAG, "DB size: " + appState.db_adapter.getDbSize());

		try{
			//appState.dataOutputFile.writeObject(this.tempList);
			//appState.dataOutputFile.flush();
			//this.tempList.clear();//clear the object for future use
		}
		catch(Exception ex){}

		//check if the file is already oversized, if it is then change the filename
		
		//if((new File(appState.currentFileName).length()) > appState.maximumDbSize)
		//if((new File(appState.currentFileName).length()) > appState.maximumDbSize) ///22)
		long dbSize = appState.db_adapter.getDbSize();
		Log.d("DatabaseSize", "Databse size: " + dbSize);
		if (dbSize > appState.maximumDbSize)
		{
			try{
				
				Log.d(TAG, "Creating new filessssssssssssssssssssssssssssssssssssssss" );
				//appState.dataOutputFile.flush();
				//appState.dataOutputFile.close();
				//appState.getServiceController().startUploading(appState.currentFileName);
				//appState.currentFileName = "/SDcard/mltoolkit/MLTOOLKIT_FILE_"  + MiscUtilityFunctions.now() + ".txt";
				//OutputStream outFile = new FileOutputStream(appState.currentFileName);
				//appState.dataOutputFile = new ObjectOutputStream(new MyUnSyncBufferedWriter(outFile));
				
				db_path = appState.db_adapter.getPathOfDatabase();
				appState.db_adapter.close();
				appState.database_primary_key_id = 0;
				//appState.db_adapter = new MyDBAdapter(appState,"myDatabase_" + MiscUtilityFunctions.now() + ".db");	
				long my_time = java.lang.System.currentTimeMillis();
				my_time  = (long)(my_time /= 1000);
				appState.db_adapter = new MyDBAdapter(appState, my_time + "_" + appState.IMEI + ".dbr");
				//appState.db_adapter = new MyDBAdapter(appState, appState.IMEI + "_" + java.lang.System.currentTimeMillis() + ".dbr");
				appState.db_adapter.open();
				
				//copy the file to SD card
				//appState.fileList.add(db_path);
				appState.getServiceController().startSDCardStorageService(db_path);
			}
			catch(Exception ex){
				Log.d(TAG, ex.toString() );
			}
		}



	}
	
	
	
	
	//writer thread to write files into SD card
	/*public class MyFileWriterToSDcard extends Thread {

		CircularBuffer<T> obj;

		public MyFileWriterToSDcard(CircularBuffer<T>  obj)
		{
			this.obj=obj;
			//new Thread(this, "Producer").start();
		}

		@Override
		public void run() {
			// TODO Auto-generated method stub
			while(true) {
				//q.put(i++);
				Log.d(TAG, "Thread starteddddddddddddddddddddddddd" );
				obj.writeFileSDCard();
			}

		}

	}

	public void writeFileSDCard() {
		// TODO Auto-generated method stub
		try{
			InputStream inFile = new FileInputStream(db_path);
			in = new BufferedInputStream(inFile);
			OutputStream outFile = new FileOutputStream();
			MyUnSyncBufferedWriter myUnSyncBufferedWriter = new MyUnSyncBufferedWriter(outFile);
			
		}
		catch(Exception ex){}
	}*/
}
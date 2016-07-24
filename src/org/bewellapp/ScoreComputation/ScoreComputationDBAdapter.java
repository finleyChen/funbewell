package org.bewellapp.ScoreComputation;

//*******************************************************************
import java.io.File;

import edu.dartmouthcs.UtilLibs.MyDataTypeConverter;

//import org.bewellapp.MomentarySampling.MyMomentarySamplingDBAdapter.myDbHelper;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;


public class ScoreComputationDBAdapter {
	/**
	 * 
	 */
	//private static final long serialVersionUID = 1L;
	private String DATABASE_NAME;
	//private static final String DATABASE_TABLE = "sources";
	private static final int DATABASE_VERSION = 12;
	private static final long MAX_INSERT_PER_TRANSACTION = 1;
	private long insert_count;
	public boolean database_online;


	public static final String BeWellScores_ID = "_id";
	public static final String DATE_time = "DATE_time";
	public static final String Score_type = "Score_type";
	public static final String PrevScore = "PrevScore";
	public static final String CurrentScore = "CurrentScore";
	public static final String NoAllActivity  = "NoAllActivity";
	public static final String NoDifferentActivity = "NoDifferentActivity";

	private static final String CREATE_TABLE_BeWellScores_TABLE_NAME = "CREATE TABLE IF NOT EXISTS " +
	"WellbeingScoreDb(_id INTEGER PRIMARY KEY ASC AUTOINCREMENT, " +
	"DATE_time DATETIME, Score_type TEXT, PrevScore FLOAT, CurrentScore FLOAT,"  +
	"NoAllActivity INTEGER, NoDifferentActivity BLOB);";

	public static final String BeWellScores_TABLE_NAME = "WellbeingScoreDb";
	
	public static final String BeWellDuration_ID = "_id";
	//public static final String DATE_time = "DATE_time";
	public static final String Duration_type = "Duration_type";
	public static final String Duration = "Duration";
	//create duration table
	private static final String CREATE_TABLE_BeWellDurations_TABLE_NAME = "CREATE TABLE IF NOT EXISTS " +
	"WellbeingDurationDb(_id INTEGER PRIMARY KEY ASC AUTOINCREMENT, " +
	"DATE_time DATETIME, Duration_type INTEGER, Duration FLOAT);";
	
	public static final String BeWellDuration_TABLE_NAME = "WellbeingDurationDb";
	

	// Variable to hold the database instance
	private SQLiteDatabase db;
	// Context of the application using the database.
	private final Context context;
	// Database open/upgrade helper
	private myDbHelper dbHelper;

	public ScoreComputationDBAdapter(Context _context) {
		context = _context;
		dbHelper = new myDbHelper(context, DATABASE_NAME, null,
				DATABASE_VERSION);
		insert_count = 0;

	}

	public ScoreComputationDBAdapter(Context _context, String database_name) {
		context = _context;

		this.DATABASE_NAME = database_name;
		dbHelper = new myDbHelper(context, DATABASE_NAME, null,
				DATABASE_VERSION);
	}

	public ScoreComputationDBAdapter open() throws SQLException {
		db = dbHelper.getWritableDatabase();
		database_online = true;
		return this;
	}

	public SQLiteDatabase getHandle() { return db; }

	public void close() { 
		database_online = false;
		if(db.inTransaction()){
			db.setTransactionSuccessful();
			db.endTransaction();		
		}
		insert_count=0;
		db.close();
	}

	public long insertEntryBeWellDuration(long sampling_time, int duration_type, double duration){		

		ContentValues value = new ContentValues();
		value.put("DATE_time", sampling_time);
		value.put("Duration_type", duration_type);
		value.put("Duration", duration);
		
		db.execSQL(CREATE_TABLE_BeWellDurations_TABLE_NAME);

		return insertEntry(BeWellDuration_TABLE_NAME, value);

	}
	
	public long insertEntryBeWellScore(long score_calculation_sampling_time, String score_type, 
			double prevScore, double currentScore, long no_of_all_activities, byte[] no_of_different_activities){		

		ContentValues value = new ContentValues();
		value.put("DATE_time", score_calculation_sampling_time);
		value.put("Score_type", score_type);
		value.put("PrevScore", prevScore);
		value.put("CurrentScore", currentScore);
		value.put("NoAllActivity", no_of_all_activities);
		value.put("NoDifferentActivity", no_of_different_activities);
		return insertEntry(BeWellScores_TABLE_NAME, value);

	}


	private long insertEntry(String table_name, ContentValues value) {
		// TODO: Create a new ContentValues to represent my row
		// and insert it into the database.

		long index = 0;


		try{
			if (insert_count == 0) db.beginTransaction();

			index = (int)db.insert(table_name, null, value);
			insert_count++;

			//Log.d("XY", "Insersion count "+insert_count);
			if(insert_count == MAX_INSERT_PER_TRANSACTION  ) {
				Log.d("XY", "trasaction sucessful");

				db.setTransactionSuccessful();
			}
		}catch (SQLException e) {
		} finally {
			if(insert_count == MAX_INSERT_PER_TRANSACTION  ){ 
				insert_count=0;
				db.endTransaction();
			}
		}

		return index;
	}




	public int removeAllEntries() {
		return db.delete(BeWellScores_TABLE_NAME, null, null);

	}
	
	public int removeAllDurationEntries() {
		return db.delete(BeWellDuration_TABLE_NAME, null, null);

	}

	public long getDbSize()
	{
		File file = new File(this.getPathOfDatabase());
		return file.length();
	}

	private static class myDbHelper extends SQLiteOpenHelper {

		public myDbHelper(Context context, String name, CursorFactory factory,
				int version) {
			super(context, name, factory, version);
		}

		// Called when no database exists in disk and the helper class needs
		// to create a new one.
		@Override
		public void onCreate(SQLiteDatabase _db) {
			_db.execSQL(CREATE_TABLE_BeWellScores_TABLE_NAME);
			_db.execSQL(CREATE_TABLE_BeWellDurations_TABLE_NAME);
		}

		// Called when there is a database version mismatch meaning that the
		// version
		// of the database on disk needs to be upgraded to the current version.
		@Override
		public void onUpgrade(SQLiteDatabase _db, int _oldVersion,
				int _newVersion) {
			Log.w("TaskDBAdapter", "Upgrading from version " + _oldVersion
					+ " to " + _newVersion
					+ ", which will destroy all old data");


			_db.execSQL("DROP TABLE IF EXISTS " + BeWellScores_TABLE_NAME);	// correct?
			_db.execSQL("DROP TABLE IF EXISTS " + BeWellDuration_TABLE_NAME);	

			onCreate(_db);
		}
	}

	public String getPathOfDatabase()
	{
		return db.getPath();
	}


	public static ScoreObject getBeWellScores(ScoreComputationDBAdapter db_adapter)
	{
		String   table = ScoreComputationDBAdapter.BeWellScores_TABLE_NAME;
		String[] columns = {ScoreComputationDBAdapter.BeWellScores_ID, ScoreComputationDBAdapter.DATE_time,ScoreComputationDBAdapter.Score_type,
				ScoreComputationDBAdapter.PrevScore,ScoreComputationDBAdapter.CurrentScore, ScoreComputationDBAdapter.NoAllActivity, ScoreComputationDBAdapter.NoDifferentActivity};

		ScoreObject scoreObject = new ScoreObject();//defaults are set to zero	


		/* Column indices */

		//social scores		
		Cursor c = db_adapter.getHandle().query(table, columns, 
				ScoreComputationDBAdapter.Score_type + " = 'Social'", 
				null, null, null, ScoreComputationDBAdapter.DATE_time + " DESC");

		int prevScoreIndex = c.getColumnIndex(ScoreComputationDBAdapter.PrevScore);
		int currentScoreIndex = c.getColumnIndex("CurrentScore");//(ScoreComputationDBAdapter.CurrentScore);
		int noAllActivityIndex = c.getColumnIndex(ScoreComputationDBAdapter.NoAllActivity);
		int noDifferentActivityIndex = c.getColumnIndex(ScoreComputationDBAdapter.NoDifferentActivity);
		int datatime_index = c.getColumnIndex(ScoreComputationDBAdapter.DATE_time);
		Log.e("Number of entries ",""+c.getCount() + " " + prevScoreIndex + " " + currentScoreIndex + " " + noAllActivityIndex);

		if (c != null) {
			/* Check if at least one Result was returned. */
			c.moveToFirst();
			if (c.getCount() > 0) {
				Log.e("Number of entries ",""+c.getCount() + " " + prevScoreIndex + " " + currentScoreIndex + " " + noAllActivityIndex);   
				scoreObject.socialScoreCurrent = c.getDouble(currentScoreIndex);
				scoreObject.socialScorePrev = c.getDouble(prevScoreIndex);
				scoreObject.socialScoreAllActivityCount = c.getLong(noAllActivityIndex);
				scoreObject.socialScoreDifferntActivityCount = MyDataTypeConverter.toLongA(c.getBlob(noDifferentActivityIndex));
			}			
		}


		//physical health	
		c = db_adapter.getHandle().query(table, columns, 
				ScoreComputationDBAdapter.Score_type + " = 'Physical'", 
				null, null, null, ScoreComputationDBAdapter.DATE_time + " DESC");


		if (c != null) {
			/* Check if at least one Result was returned. */
			c.moveToFirst();
			if (c.getCount() > 0) {

				scoreObject.physicalScoreCurrent = c.getDouble(currentScoreIndex);
				scoreObject.physicalScorePrev = c.getDouble(prevScoreIndex);
				scoreObject.physicalScoreAllActivityCount = c.getLong(noAllActivityIndex);
				scoreObject.physicalScoreDifferntActivityCount = MyDataTypeConverter.toLongA(c.getBlob(noDifferentActivityIndex));
			}			
		}


		//sleep scores		
		c = db_adapter.getHandle().query(table, columns, 
				ScoreComputationDBAdapter.Score_type + " = 'Sleep'", 
				null, null, null, ScoreComputationDBAdapter.DATE_time + " DESC");


		if (c != null) {
			/* Check if at least one Result was returned. */
			c.moveToFirst();
			if (c.getCount() > 0) {

				scoreObject.sleepScoreCurrent = c.getDouble(currentScoreIndex);
				scoreObject.sleepScorePrev = c.getDouble(prevScoreIndex);

				//in score computation I will store the number of number of minutes of  sleep 
				//in NoAllActivity  
				scoreObject.sleepTimeInMillisecond = c.getLong(noAllActivityIndex);
				scoreObject.sleepTimeRecordDataTime = c.getLong(datatime_index);
			}			
		}






		return scoreObject;
	}

	public static double[] getAllDurations(ScoreComputationDBAdapter db_adapter)
	//get duration of stationary and silence
	{
		double[] results = {0, 0, 0, 0, 0, 0};	//6 types of durations

		//
		//Phone states
		//
		String   durationTable = ScoreComputationDBAdapter.BeWellDuration_TABLE_NAME;
		/* Column indices */
		String[] durationcolumns = {ScoreComputationDBAdapter.BeWellDuration_ID, ScoreComputationDBAdapter.DATE_time,
				ScoreComputationDBAdapter.Duration_type, ScoreComputationDBAdapter.Duration};
		
		//darkDuration
		Cursor c = db_adapter.getHandle().query(durationTable, durationcolumns, 
				ScoreComputationDBAdapter.Duration_type + " = 14",  
				null, null, null, ScoreComputationDBAdapter.Duration + " DESC");
		int valueIndex = c.getColumnIndex(ScoreComputationDBAdapter.Duration);
		
		if (c != null) {
			/* Check if at least one Result was returned. */
			c.moveToFirst();
			if (c.getCount() > 0) {
				double darkDuration = c.getDouble(valueIndex); 
				Log.d("BestSleep", "darkDuration: " + Double.toString(darkDuration));
				results[0] = darkDuration;
			}	
		}
		
		//lockDuration
		c = db_adapter.getHandle().query(durationTable, durationcolumns, 
				ScoreComputationDBAdapter.Duration_type + " = 15",  
				null, null, null, ScoreComputationDBAdapter.Duration + " DESC");
		
		if (c != null) {
			/* Check if at least one Result was returned. */
			c.moveToFirst();
			if (c.getCount() > 0) {
				double lockDuration = c.getDouble(valueIndex); 
				Log.d("BestSleep", "lockDuration: " + Double.toString(lockDuration));
				results[1] = lockDuration;
			}	
		}
		
		//offDuration
		c = db_adapter.getHandle().query(durationTable, durationcolumns, 
				ScoreComputationDBAdapter.Duration_type + " = 16",  
				null, null, null, ScoreComputationDBAdapter.Duration + " DESC");
		
		if (c != null) {
			/* Check if at least one Result was returned. */
			c.moveToFirst();
			if (c.getCount() > 0) {
				double offDuration = c.getDouble(valueIndex); 
				Log.d("BestSleep", "offDuration: " + Double.toString(offDuration));
				results[2] = offDuration;
			}	
		}
		
		//chargingDuration
		c = db_adapter.getHandle().query(durationTable, durationcolumns, 
				ScoreComputationDBAdapter.Duration_type + " = 17",  
				null, null, null, ScoreComputationDBAdapter.Duration + " DESC");
		
		if (c != null) {
			/* Check if at least one Result was returned. */
			c.moveToFirst();
			if (c.getCount() > 0) {
				double chargingDuration = c.getDouble(valueIndex); 
				Log.d("BestSleep", "chargingDuration: " + Double.toString(chargingDuration));
				results[3] = chargingDuration;
			}	
		}
		
		//
		//Stationary, Silence
		//
		long currentMillis = System.currentTimeMillis();
		long yesterdayMillis = currentMillis - 10 * 60 * 60 * 1000;	//yesterday's currentMillis (actually 10 hours ago)

		String bewellTable = ScoreComputationDBAdapter.BeWellScores_TABLE_NAME;
		/* Column indices */
		String[] bewellColumns = {ScoreComputationDBAdapter.BeWellScores_ID, ScoreComputationDBAdapter.DATE_time,ScoreComputationDBAdapter.Score_type,
				ScoreComputationDBAdapter.PrevScore,ScoreComputationDBAdapter.CurrentScore, ScoreComputationDBAdapter.NoAllActivity, ScoreComputationDBAdapter.NoDifferentActivity};

		//stationary counts		
		double today = 0;
		c = db_adapter.getHandle().query(bewellTable, bewellColumns, 
				ScoreComputationDBAdapter.Score_type + " = 'Physical'",  
				null, null, null, ScoreComputationDBAdapter.DATE_time + " DESC");
		int noDifferentActivityIndex = c.getColumnIndex(ScoreComputationDBAdapter.NoDifferentActivity);
		if (c != null) {
			/* Check if at least one Result was returned. */
			c.moveToFirst();
			if (c.getCount() > 0) {
				
				long []physicalScoreDifferntActivityCount = MyDataTypeConverter.toLongA(c.getBlob(noDifferentActivityIndex));
				today = physicalScoreDifferntActivityCount[1] * 2;
			}	
		}
		double yesterday = 0;
		c = db_adapter.getHandle().query(bewellTable, bewellColumns, 
				ScoreComputationDBAdapter.Score_type + " = 'Physical'" + " AND " + ScoreComputationDBAdapter.DATE_time + " < " + Long.toString(yesterdayMillis), 
				null, null, null, ScoreComputationDBAdapter.DATE_time + " DESC");
		if (c != null) {
			/* Check if at least one Result was returned. */
			c.moveToFirst();
			if (c.getCount() > 0) {
				  
				long []physicalScoreDifferntActivityCount = MyDataTypeConverter.toLongA(c.getBlob(noDifferentActivityIndex));
				yesterday = physicalScoreDifferntActivityCount[1] * 2;
			}	
		}
		
		results[4] = (today - yesterday) / 3600;	//unit hours
		Log.d("BestSleep", "stationaryDuration: " + Double.toString(results[4]));
		//Log.d("BestSleep", "yersterday: " + Double.toString(yesterday) + " today: " + Double.toString(today));

		//silence counts		
		today = 0;
		c = db_adapter.getHandle().query(bewellTable, bewellColumns, 
				ScoreComputationDBAdapter.Score_type + " = 'Social'",  
				null, null, null, ScoreComputationDBAdapter.DATE_time + " DESC");
		if (c != null) {
			/* Check if at least one Result was returned. */
			c.moveToFirst();
			if (c.getCount() > 0) {
				
				long []physicalScoreDifferntActivityCount = MyDataTypeConverter.toLongA(c.getBlob(noDifferentActivityIndex));
				today = physicalScoreDifferntActivityCount[0] * 1.28;
			}	
		}
		yesterday = 0;
		c = db_adapter.getHandle().query(bewellTable, bewellColumns, 
				ScoreComputationDBAdapter.Score_type + " = 'Social'" + " AND " + ScoreComputationDBAdapter.DATE_time + " < " + Long.toString(yesterdayMillis), 
				null, null, null, ScoreComputationDBAdapter.DATE_time + " DESC");
		if (c != null) {
			/* Check if at least one Result was returned. */
			c.moveToFirst();
			if (c.getCount() > 0) {
				  
				long []physicalScoreDifferntActivityCount = MyDataTypeConverter.toLongA(c.getBlob(noDifferentActivityIndex));
				yesterday = physicalScoreDifferntActivityCount[0] * 1.28;
			}	
		}
		
		results[5] = (today - yesterday) / 3600;	//unit hours
		Log.d("BestSleep", "silenceDuration: " + Double.toString(results[5]));
		
		return results;
	}
	
	

}

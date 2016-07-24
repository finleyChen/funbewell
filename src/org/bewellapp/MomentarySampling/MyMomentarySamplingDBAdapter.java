package org.bewellapp.MomentarySampling;



//*******************************************************************
import java.io.File;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

//import org.bewellapp.Storage;

public class MyMomentarySamplingDBAdapter{
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

	
	public static final String MomentarySamples_ID = "_id";
	public static final String DATE_time = "DATE_time";
	public static final String Start_time = "Start_time";
	public static final String End_time = "End_time";
	public static final String Stress_level = "Stress_level";
	public static final String Label_value_  = "Label_value_";
	public static final String Inferred_status_ = "Inferred_status_";
	
	private static final String CREATE_TABLE_MomentarySamples_TABLE_NAME = "CREATE TABLE IF NOT EXISTS " +
	"MomentarySamplingDb(_id INTEGER PRIMARY KEY ASC AUTOINCREMENT, " +
	"DATE_time DATETIME, Start_time DATETIME, End_time DATETIME, Stress_level INTEGER,LOCATION_ TEXT,Label_value_ TEXT,Inferred_status_ TEXT);";

	
	public static final String MomentarySamples_TABLE_NAME = "MomentarySamplingDb";

	// Variable to hold the database instance
	private SQLiteDatabase db;
	// Context of the application using the database.
	private final Context context;
	// Database open/upgrade helper
	private myDbHelper dbHelper;

	public MyMomentarySamplingDBAdapter(Context _context) {
		context = _context;
		dbHelper = new myDbHelper(context, DATABASE_NAME, null,
				DATABASE_VERSION);
		insert_count = 0;

	}

	public MyMomentarySamplingDBAdapter(Context _context, String database_name) {
		context = _context;

		this.DATABASE_NAME = database_name;
		dbHelper = new myDbHelper(context, DATABASE_NAME, null,
				DATABASE_VERSION);
	}

	public MyMomentarySamplingDBAdapter open() throws SQLException {
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

	
	//"CREATE TABLE IF NOT EXISTS " +
	//"location(location_time DATETIME ASC, location_is_start BOOLEAN, location_lat FLOAT, location_lon FLOAT);";
	
	
	//"CREATE TABLE IF NOT EXISTS " +
	//"MomentarySamplingDb(MomentarySamples_ID INTEGER PRIMARY KEY ASC AUTOINCREMENT, " +
	//"DATE_time DATETIME, Start_time DATETIME, End_time DATETIME, Stress_level INTEGER,LOCATION_ TEXT);";

	public long insertEntryMomertarySampling(long momentary_sampling_time, long momentary_sampling_time_start, 
			long momentary_sampling_time_end, int stress_level, String location_, String label_value_, String inferrred_status_){		

		ContentValues value = new ContentValues();
		value.put("DATE_time", momentary_sampling_time);
		value.put("Start_time", momentary_sampling_time_start);
		value.put("End_time", momentary_sampling_time_end);
		value.put("Stress_level", stress_level);
		value.put("LOCATION_", location_);
		value.put("Label_value_", label_value_);
		value.put("Inferred_status_", inferrred_status_);
		return insertEntry(MomentarySamples_TABLE_NAME, value);

	}

	public void updateEntryMomertarySampling(long momentary_sampling_time, long momentary_sampling_time_start, 
			long momentary_sampling_time_end, int stress_level, String location_, String label_value_, String inferrred_status_, long rowId){		

		ContentValues value = new ContentValues();
		value.put("DATE_time", momentary_sampling_time);
		value.put("Start_time", momentary_sampling_time_start);
		value.put("End_time", momentary_sampling_time_end);
		value.put("Stress_level", stress_level);
		value.put("LOCATION_", location_);
		value.put("Label_value_", label_value_);
		value.put("Inferred_status_", inferrred_status_);
		updateEntry(MomentarySamples_TABLE_NAME, value,rowId);

	}
	
	private long updateEntry(String table_name, ContentValues value, long rowId) {
		// TODO: Create a new ContentValues to represent my row
		// and insert it into the database.

		long index = 0;


		try{
			if (insert_count == 0) db.beginTransaction();

			index = (int)db.update(table_name, value, MomentarySamples_ID + "=" + rowId, null);
			insert_count++;

			Log.i("UPDATEEEEEE", "Update completeddddddddddddd");
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
		return db.delete(MomentarySamples_TABLE_NAME, null, null);

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
			_db.execSQL(CREATE_TABLE_MomentarySamples_TABLE_NAME);
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


			_db.execSQL("DROP TABLE IF EXISTS " + MomentarySamples_TABLE_NAME);	// correct?

			onCreate(_db);
		}
	}

	public String getPathOfDatabase()
	{
		return db.getPath();
	}
	
	public static int getAvailableCount(MyMomentarySamplingDBAdapter db_adapter)
	{
		String   table = MyMomentarySamplingDBAdapter.MomentarySamples_TABLE_NAME;
		String[] columns = {MyMomentarySamplingDBAdapter.MomentarySamples_ID, MyMomentarySamplingDBAdapter.DATE_time,MyMomentarySamplingDBAdapter.Start_time,
				MyMomentarySamplingDBAdapter.End_time,MyMomentarySamplingDBAdapter.Stress_level, "LOCATION_", MyMomentarySamplingDBAdapter.Label_value_,
				MyMomentarySamplingDBAdapter.Inferred_status_};
		Cursor c = db_adapter.getHandle().query(table, columns, 
				MyMomentarySamplingDBAdapter.DATE_time + ">" + (System.currentTimeMillis() - momentary_list.how_many_hours *60*60*1000) +
				" and " + MyMomentarySamplingDBAdapter.Label_value_ + " <> 'Available'", 
				null, null, null, MyMomentarySamplingDBAdapter.DATE_time + " DESC");
		return c.getCount();
		
	}


}
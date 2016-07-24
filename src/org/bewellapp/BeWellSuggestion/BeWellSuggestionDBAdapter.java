package org.bewellapp.BeWellSuggestion;


import java.io.File;
import java.util.Random;

import org.bewellapp.ScoreComputation.ScoreComputationDBAdapter;
import org.bewellapp.ScoreComputation.ScoreObject;

import edu.dartmouthcs.UtilLibs.MyDataTypeConverter;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.util.Log;


public class BeWellSuggestionDBAdapter {
	/*
	 * DataBase Adapter for BeWell Suggestion
	 */

	private String DATABASE_NAME;
	private static final int DATABASE_VERSION = 12;
	private static final long MAX_INSERT_PER_TRANSACTION = 1;
	private long insert_count;
	public boolean database_online;

	public static final String BeWellSuggestions_ID = "_id";
	public static final String Suggestion_type = "Suggestion_type";
	public static final String Suggestion = "Suggestion";

	private static final String CREATE_TABLE_BeWellSuggestions_TABLE_NAME = "CREATE TABLE IF NOT EXISTS " +
	"WellbeingSuggestionDb(_id INTEGER PRIMARY KEY ASC AUTOINCREMENT, " +
	"Suggestion_type TEXT, Suggestion TEXT);";

	public static final String BeWellSuggestions_TABLE_NAME = "WellbeingSuggestionDb";

	// Variable to hold the database instance
	private SQLiteDatabase db;
	// Context of the application using the database.
	private final Context context;
	// Database open/upgrade helper
	private myDbHelper dbHelper;
	
	private static final String TAG = "BeWellSuggestionService";

	//constructor
	public BeWellSuggestionDBAdapter(Context _context, String database_name) {
		context = _context;

		this.DATABASE_NAME = database_name;
		dbHelper = new myDbHelper(context, DATABASE_NAME, null,
				DATABASE_VERSION);
	}

	public BeWellSuggestionDBAdapter open() throws SQLException {
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


	public long insertEntryBeWellSuggestion(String suggestion_type, String suggestion){		

		ContentValues value = new ContentValues();
		value.put("Suggestion_type", suggestion_type);
		value.put("Suggestion", suggestion);

		return insertEntry(BeWellSuggestions_TABLE_NAME, value);

	}

	private long insertEntry(String table_name, ContentValues value) {
		// TODO: Create a new ContentValues to represent my row
		// and insert it into the database.

		long index = 0;


		try{
			if (insert_count == 0) db.beginTransaction();

			index = (int)db.insert(table_name, null, value);
			insert_count++;

			if(insert_count == MAX_INSERT_PER_TRANSACTION  ) {
				//Log.d("XY", "Insersion count "+insert_count);
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
		return db.delete(BeWellSuggestions_TABLE_NAME, null, null);

	}

	public long getDbSize()
	{
		File file = new File(this.getPathOfDatabase());
		return file.length();
	}

	public String getPathOfDatabase()
	{
		return db.getPath();
	}


	public static String getBeWellSuggestions(BeWellSuggestionDBAdapter db_adapter, String type)
	{
		String table = BeWellSuggestionDBAdapter.BeWellSuggestions_TABLE_NAME;
		String[] columns = {BeWellSuggestionDBAdapter.BeWellSuggestions_ID, BeWellSuggestionDBAdapter.Suggestion_type, BeWellSuggestionDBAdapter.Suggestion};
		String result = "";

		/* Column indices */	
		Cursor c = db_adapter.getHandle().query(table, columns, 
				BeWellSuggestionDBAdapter.Suggestion_type + " = " + "\"" + type + "\"", 
				null, null, null, null);

		int suggestionIndex = c.getColumnIndex(BeWellSuggestionDBAdapter.Suggestion);
	
		if (c != null) {
			int cnt = c.getCount();
			Log.d(TAG, "Number of entries " + cnt);
			
			long seed = System.currentTimeMillis();
			Random generator = new Random(seed);
			int position = Math.abs(generator.nextInt()) % cnt;
			Log.d(TAG, "Position is " + Integer.toString(position));
			
			boolean moved = c.moveToPosition(position);
			if (moved) {
				result = c.getString(suggestionIndex);
			}			
			else{// if position is not valid
				c.moveToFirst();
				result = c.getString(suggestionIndex);
			}
		}


		return result;
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
			_db.execSQL(CREATE_TABLE_BeWellSuggestions_TABLE_NAME);
		}

		// Called when there is a database version mismatch meaning that the
		// version
		// of the database on disk needs to be upgraded to the current version.
		@Override
		public void onUpgrade(SQLiteDatabase _db, int _oldVersion,
				int _newVersion) {
			Log.w("SuggestionDBAdapter", "Upgrading from version " + _oldVersion
					+ " to " + _newVersion
					+ ", which will destroy all old data");

			_db.execSQL("DROP TABLE IF EXISTS " + BeWellSuggestions_TABLE_NAME);	

			onCreate(_db);
		}
	}

}

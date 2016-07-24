package org.bewellapp.ServiceControllers.AppLib;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;

import static org.bewellapp.ServiceControllers.AppLib.AppStatsDBAdapter.*;

public class AppStatsDBHelper extends SQLiteOpenHelper {
	
	private static final String APPSTATS_DB_NAME = "bewell_appstats_db";
	private static final int DB_VERSION = 1;
	
	private static final String DB_CREATE_PHONE_USAGE_TABLE = "create table %s (YEAR INT NOT NULL, MONTH INT NOT NULL, DAY INT NOT NULL, HOUR INT NOT NULL, APP TEXT, SECONDS INT, PRIMARY KEY (YEAR, MONTH, DAY, HOUR, APP))";
	private static final String DB_CREATE_PHONE_TIME_TABLE = "create table %s (YEAR INT NOT NULL, MONTH INT NOT NULL, DAY INT NOT NULL, PHONE_CALL INT, SOCIAL_APP INT, OTHER_APP INT , PRIMARY KEY (YEAR, MONTH, DAY))";

	public AppStatsDBHelper(Context context, String name, CursorFactory factory, int version) {
		super(context, name, factory, version);
	}
	
	public AppStatsDBHelper(Context context) {
		super(context, APPSTATS_DB_NAME, null, DB_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		createTable(db, TBL_PHONE_APPS_USAGE, DB_CREATE_PHONE_USAGE_TABLE);
		createTable(db, TBL_PHONE_TIME, DB_CREATE_PHONE_TIME_TABLE);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		String drop = String.format("DROP TABLE IF EXISTS %s", TBL_PHONE_APPS_USAGE);
		db.execSQL(drop);
		drop = String.format("DROP TABLE IF EXISTS %s", TBL_PHONE_TIME);
		db.execSQL(drop);
		onCreate(db);		
	}
	
	private void createTable(SQLiteDatabase db, String tablename, String createString) {
		if (tablename == null || db == null) {
			throw new IllegalArgumentException();
		}
		String dml = String.format(createString, tablename);
		db.execSQL(dml);
	}

}

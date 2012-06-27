package com.jfs.funkmachine2000;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {

	private static final String DATABASE_NAME = "dj_jfs";
	private static final int DATABASE_VERSION = 4;

	public DatabaseHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL("CREATE  TABLE \"midi\" (\"_id\" INTEGER PRIMARY KEY "
				+ " AUTOINCREMENT  NOT NULL  UNIQUE , \"file\" BLOB, "
				+ "\"date\" DATETIME DEFAULT CURRENT_TIMESTAMP, "
				+ "\"string\" TEXT NOT NULL);");
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		db.execSQL("DROP TABLE IF EXISTS \"midi\"");
		onCreate(db);
	}

}

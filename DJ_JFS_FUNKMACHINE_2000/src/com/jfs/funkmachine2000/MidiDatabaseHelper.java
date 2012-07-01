/**
 * 
 */
package com.jfs.funkmachine2000;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;


/**
 * 
 * @author Jan
 * 
 */
public class MidiDatabaseHelper {

	private SQLiteDatabase db;
	private DatabaseHelper dbHelper;
	private String[] allColumns = { "_id", "file", "date", "string" };

	public MidiDatabaseHelper(Context context) {
		this.dbHelper = new DatabaseHelper(context);
	}

	public void open() throws SQLException {
		this.db = dbHelper.getWritableDatabase();
	}

	public void close() {
		dbHelper.close();
	}

	public Midi insertMidi(byte[] midi, String string) {
		ContentValues values = new ContentValues();
		values.put("file", midi);
		values.put("string", string);
		return selectMidi(this.db.insert("midi", null, values));
	}

	public void deleteMidi(long id) {
		db.delete("midi", "_id = ?", new String[] { String.valueOf(id) });
	}

	public List<Midi> fetchAll() {
		List<Midi> comments = new ArrayList<Midi>();
		Cursor cursor = db.query("midi", new String[] { "_id", "date" }, null,
				null, null, null, null);
		cursor.moveToFirst();
		while (!cursor.isAfterLast()) {
			comments.add(getMidi(cursor));
			cursor.move(1);
		}
		cursor.close();
		return comments;
	}

	public Midi selectMidi(long id) {
		System.out.println("adasas");
		Cursor cursor = db.query("midi", new String[] { "_id", "file", "date",
				"string" }, "_id = ?", new String[] { String.valueOf(id) },
				null, null, null, "1");
		cursor.moveToFirst();
		Midi midi = new Midi();
		midi.setId(cursor.getLong(0)).setFile(cursor.getBlob(1))
				.setDate(cursor.getString(2)).setString(cursor.getString(3));
		cursor.close();
		return midi;
	}

	public Midi getMidi(Cursor cursor) {
		Midi midi = new Midi();
		return midi.setId(cursor.getLong(0)).setDate(cursor.getString(1));
	}
}

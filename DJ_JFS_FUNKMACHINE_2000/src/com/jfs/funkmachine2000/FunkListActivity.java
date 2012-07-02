package com.jfs.funkmachine2000;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

/**
 * Shows all the previously created songs and calls the PlayMidiActivity onclick.
 * @author Jan
 *
 */
public class FunkListActivity extends ListActivity {

	private MidiDatabaseHelper dbHelper;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.funklist);

		dbHelper = new MidiDatabaseHelper(this);
		dbHelper.open();

		ArrayAdapter<Midi> adapter = new ArrayAdapter<Midi>(this,
				android.R.layout.simple_list_item_1, dbHelper.fetchAll());
		dbHelper.close();
		setListAdapter(adapter);
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		ArrayAdapter<Midi> adapter = (ArrayAdapter<Midi>) getListAdapter();
		dbHelper.open();
		Midi midi = dbHelper.selectMidi(((Midi) adapter.getItem(position))
				.getId());
		dbHelper.close();
		Intent intent = new Intent(this, PlayMidiActivity.class);
		intent.putExtra(PlayMidiActivity.MIDI_ID, midi.getId());
		startActivity(intent);
	}

}

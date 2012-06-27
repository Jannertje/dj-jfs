package com.jfs.funkmachine2000;

import java.io.File;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

/**
 * The about activity shows a small description of the app and how to use it.
 * 
 * @author Floris
 * 
 */
public class PlayMidiActivity extends Activity {
	public static final String MIDI_ID = "com.jfs.funkmachine2000.MIDIID";
	private long midiID;
	private MidiPlayer player;
	private Bitmap warpBitmap;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.playmidi);

		Intent intent = getIntent();
		midiID = intent.getLongExtra(MIDI_ID, -1L);

		if (midiID == -1L)
			finish();

		File imgFile = new File(getFilesDir() + "/warped/warpedImage"+midiID+".jpg");
		if (imgFile.exists()) {

			warpBitmap = BitmapFactory.decodeFile(imgFile
					.getAbsolutePath());

			ImageView warpedImage = (ImageView) findViewById(R.id.warpedImageView);
			warpedImage.setVisibility(View.VISIBLE);
			warpedImage.setImageBitmap(warpBitmap);
		}
		
		MidiDatabaseHelper helper = new MidiDatabaseHelper(this);
		helper.open();
		Midi toPlay = helper.selectMidi(midiID);
		helper.close();
		player = new MidiPlayer(toPlay.getFile(), this);
		player.playMidi();
	}

	@Override
	public void onPause() {
		super.onPause();
		player.stopMidi();
	}

	@Override
	public void onStop() {
		super.onStop();
		player.stopMidi();
	}

}

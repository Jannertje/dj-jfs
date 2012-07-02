package com.jfs.funkmachine2000;

import java.io.File;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;

/**
 * The PlayMidiActivity plays a previously created midi and shows the
 * corresponding image, if available.
 * 
 * @author Jan
 * 
 */
public class PlayMidiActivity extends Activity {
	public static final String MIDI_ID = "com.jfs.funkmachine2000.MIDIID";
	private long midiID;
	private MidiPlayer player;
	private Bitmap warpBitmap;
	private boolean formatted;
	private String imgpath;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.playmidi);

		Intent intent = getIntent();
		midiID = intent.getLongExtra(MIDI_ID, -1L);

		if (midiID == -1L)
			finish();

		MidiDatabaseHelper helper = new MidiDatabaseHelper(this);
		helper.open();
		Midi toPlay = helper.selectMidi(midiID);
		helper.close();

		player = new MidiPlayer(toPlay.getFile(), this);
		player.playMidi();

		formatted = false;

		File imgFile = new File(getFilesDir() + "/warped/warpedImage" + midiID
				+ ".jpg");
		imgpath = imgFile.getAbsolutePath();
		if (imgFile.exists()) {

			warpBitmap = BitmapFactory.decodeFile(imgpath);

			ChessboardImageView warpedImage = (ChessboardImageView) findViewById(R.id.warpedImageViewPlay);
			warpedImage.setVisibility(View.VISIBLE);
			warpedImage.init(warpBitmap, toPlay.getString(), formatted);
			findViewById(R.id.changeViewButtonPlay).setVisibility(View.VISIBLE);
		}
	}

	public void changeView(View view) {
		Button changeView = (Button) view;
		ChessboardImageView warpedImage = (ChessboardImageView) findViewById(R.id.warpedImageViewPlay);

		formatted = warpedImage.swap();

		if (formatted) {
			changeView.setText(R.string.changeviewback);
		} else {
			changeView.setText(R.string.changeview);
		}
	}

	public void openWarpedImage(View view) {
		String extpath = Environment.getExternalStorageDirectory()
				+ "/DJ_JFS_FunkMachine/imageWarped.jpg";
		if (!formatted) {
			FunkFileManager.copyfile(imgpath,extpath);
			Intent intent = new Intent();
			intent.setAction(android.content.Intent.ACTION_VIEW);
			intent.setDataAndType(Uri.fromFile(new File(extpath)), "image/jpg");
			startActivity(intent);
		}
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

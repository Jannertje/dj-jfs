package com.jfs.funkmachine2000;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import android.content.Context;
import android.media.MediaPlayer;

public class MidiPlayer {
	
	static MediaPlayer player;
	byte[] midiFile;
	Context context;
	
	public MidiPlayer(byte[] midi, Context context) {
		if (player == null) player = new MediaPlayer();
		this.context = context;
		this.midiFile = midi;
	}
	
	public void stopMidi() {
		if (player != null) player.stop();
	}
	
	public void playMidi() {
	    try {
	        // create temp file that will hold byte array
	        File temp= File.createTempFile("midi", "mid", context.getCacheDir());
	        temp.deleteOnExit();
	        FileOutputStream fos = new FileOutputStream(temp);
	        fos.write(midiFile);
	        fos.close();

	        player.reset();

	        FileInputStream fis = new FileInputStream(temp);
	        player.setDataSource(fis.getFD());

	        player.prepare();
	        player.setLooping(true);
	        player.start();
	    } catch (IOException e) {
	    	e.printStackTrace();
	    }
	}
	
}

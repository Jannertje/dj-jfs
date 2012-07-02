	package com.jfs.funkmachine2000;

import android.app.Activity;
import android.os.Bundle;

/**
 * The about activity shows a small description of the app and how to use it.
 * TODO: extend the about activity with all the chessboard instructions.
 * 
 * @author Floris
 * 
 */
public class AboutActivity extends Activity {
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.about);
	}
}

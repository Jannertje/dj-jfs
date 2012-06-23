package com.jfs.funkmachine2000;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.TextView;

public class ProcessImageActivity extends Activity {
	private TextView processText;
	private Thread closeThread;
	private NativeChessboardTask chessTask;

	private String imagePath;
	private String imageFile;
	private boolean error;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		error = false;
		super.onCreate(savedInstanceState);
		setContentView(R.layout.process);

		Intent intent = getIntent();
		imagePath = intent.getStringExtra(FunkMachineActivity.IMAGE_PATH);
		imageFile = intent.getStringExtra(FunkMachineActivity.IMAGE_FILE);

		processText = (TextView) findViewById(R.id.progressText);
		processText.setText(imagePath + "/" + imageFile);

		chessTask = new NativeChessboardTask();
		chessTask.execute(imagePath + "/" + imageFile);

		closeThread = new Thread() {
			@Override
			public void run() {
				try {
					synchronized (this) {
						wait(1000);
					}
				} catch (InterruptedException ex) {

				}
				finish();
			}
		};
	}

	@Override
	public void onBackPressed() {
		if(error) finish();
	}

	@Override
	public void onStop() {
		super.onStop();

		chessTask.cancel(true);
	}

	static {
		System.loadLibrary("chessboard");
	}

	public native String detectChessboardFromImage(String imgpath,
			String imgfile, int squareSize, int hueTolerance,
			int cannyThreshold1, int cannyThreshold2);

	private class NativeChessboardTask extends
			AsyncTask<String, Integer, String> {
		@Override
		protected String doInBackground(String... imagepath) {
			String nativeResult = detectChessboardFromImage(imagePath,
					imageFile, DefaultOCVSettings.SQUARE_SIZE,
					DefaultOCVSettings.HUE_TOLERANCE,
					DefaultOCVSettings.CANNY_THRESHOLD_1,
					DefaultOCVSettings.CANNY_THRESHOLD_2);
			return nativeResult;
		}

		protected void onPostExecute(String result) {
			if(result.charAt(0)=='e') {
				processText.setText(result);
				error = true;
			} else {
				processText.setText("done");
				closeThread.start();
			}
		}
	}
}

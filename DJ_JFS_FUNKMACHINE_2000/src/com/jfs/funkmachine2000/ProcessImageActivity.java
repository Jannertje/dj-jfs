package com.jfs.funkmachine2000;

import java.io.File;
import java.io.IOException;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * The ProcessImageActivity class makes a Native call to process the user's
 * image. It also shows the progress of that native call to the user. The back
 * button is temporarily disabled so the user won't close the Activity while the
 * native call is made. When the Native Call has executed without error, it will
 * wait 1 second and close the activity.
 * 
 * @author Floris, Jan
 * 
 */
public class ProcessImageActivity extends Activity {
	private TextView progressText;
	private TextView processText;
	private Thread closeThread;
	private NativeChessboardTask chessTask;
	private SharedPreferences sharedPrefs;

	private String imagePath;
	private String imageFile;

	private Bitmap warpBitmap;
	private Bitmap formattedBitmap;
	private String outputString;

	private boolean completed;
	private boolean formatted;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.process);

		Intent intent = getIntent();
		imagePath = intent.getStringExtra(FunkMachineActivity.IMAGE_PATH);
		imageFile = intent.getStringExtra(FunkMachineActivity.IMAGE_FILE);

		progressText = (TextView) findViewById(R.id.progressText);
		progressText.setText(imagePath + "/" + imageFile);
		processText = (TextView) findViewById(R.id.processText);

		sharedPrefs = PreferenceManager
				.getDefaultSharedPreferences(getBaseContext());

		completed = false;
		formatted = false;
	}

	@Override
	public void onBackPressed() {
		if (completed)
			finish();
	}

	@Override
	public void onStop() {
		super.onStop();

		chessTask.cancel(true);
	}

	@Override
	public void onSaveInstanceState(Bundle outstate) {
		super.onSaveInstanceState(outstate);
		outstate.putBoolean("completed", completed);
	}

	@Override
	public void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		completed = savedInstanceState.getBoolean("completed", false);
	}

	@Override
	public void onResume() {
		super.onResume();
		chessTask = new NativeChessboardTask();
		if (!completed) {
			chessTask.execute(imagePath + "/" + imageFile);
		}
	}

	/**
	 * Runs when the user presses the change view button. Should toggle between
	 * formatted and unformatted image
	 * 
	 * @param view
	 */
	public void changeView(View view) {
		Button changeView = (Button) view;
		ChessboardImageView warpedImage = (ChessboardImageView) findViewById(R.id.warpedImageView);
		
		formatted = warpedImage.swap();
		
		if (formatted) {
			changeView.setText(R.string.changeviewback);
		} else {
			changeView.setText(R.string.changeview);
		}
	}

	/**
	 * Allows the user to take a closer look at the warped image by calling the
	 * ACTION_VIEW Intent on the image path.
	 * 
	 * @param view
	 */
	public void openWarpedImage(View view) {
		if (!formatted) {
			File warpedImage = new File(imagePath + "/imageWarped.jpg");
			Intent intent = new Intent();
			intent.setAction(android.content.Intent.ACTION_VIEW);
			intent.setDataAndType(Uri.fromFile(warpedImage), "image/jpg");
			startActivity(intent);
		}
	}

	/**
	 * Writes the image to private storage, and saves the midi to the SQLite
	 * database.
	 * 
	 * @param view
	 */
	public void saveMusic(View view) {

		SharedPreferences sharedPrefs = PreferenceManager
				.getDefaultSharedPreferences(this);

		File file = new File(getCacheDir(), "midi.mid");
		StringToMidi helper = new StringToMidi(outputString,
				Integer.parseInt(sharedPrefs.getString("bpm", "120")),
				Integer.parseInt(sharedPrefs.getString("rootnote", "0"))
						+ 12
						* (Integer.parseInt(sharedPrefs.getString("rootoctave",
								"4")) + 1));
		helper.makeMidi(file);

		try {
			MidiDatabaseHelper dbHelper = new MidiDatabaseHelper(this);
			dbHelper.open();
			Midi toPlay = dbHelper.insertMidi(
					FunkFileManager.getBytesFromFile(file), outputString);
			dbHelper.close();

			if (sharedPrefs.getBoolean("saveImage", false)) {
				FunkFileManager.saveBitmap(warpBitmap,
						"warpedImage" + toPlay.getId(), this);
			}

			Intent intent = new Intent(this, PlayMidiActivity.class);
			intent.putExtra(PlayMidiActivity.MIDI_ID, toPlay.getId());
			startActivity(intent);
			
			finish();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Shows a Toast message (useful for debugging)
	 * 
	 */
	public void showToast(CharSequence message) {
		Context context = getApplicationContext();
		int duration = Toast.LENGTH_LONG;

		Toast toast = Toast.makeText(context, message, duration);
		toast.show();
	}

	static {
		System.loadLibrary("chessboard");
	}

	/**
	 * The native method that processes the image. Please note that the content
	 * of this method is already compiled to /libs/armeabi-v7a/libchessboard.so.
	 * 
	 * @param imgfolder
	 *            The path to the image to process, without the filename and
	 *            final '/'
	 * @param imgfile
	 *            The filename of the image to process.
	 * @param squareSize
	 *            The size in pixels of one side of one square on the
	 *            chessboard. The chessboard will be perspective warped to this
	 *            size. The warped image will be used to detect the color blobs
	 *            on the chessboard and will be saved to warpedImage.jpg under
	 *            imgpath.
	 * @param hueTolerance
	 *            An int value in the range 0-255. Color blobs will be regarded
	 *            as equal when their hue values differ less than this tolerance
	 *            value.
	 * @param saturationTolerance
	 *            Color blobs will be regarded as equal when their saturation
	 *            values differs less than this tolerance value.
	 * @param valueTolerance
	 *            An int value in the range 0-255. Color blobs will be regarded
	 *            as equal when their HSV value differs less than this tolerance
	 *            value.
	 * @param cannyThreshold1
	 *            The first threshold argument of the Canny function. See the
	 *            OpenCV docs for more info.
	 * @param cannyThreshold2
	 *            The second threshold argument of the Canny function. See the
	 *            OpenCV docs for more info.
	 * @param adaptiveThreshold
	 *            Boolean that sets the adaptive threshold parameter for the
	 *            findChessboardCorners function. See the OpenCV docs for more
	 *            info
	 * @param normalizeImage
	 *            Boolean that sets the normalize image parameter for the
	 *            findChessboardCorners function. See the OpenCV docs for more
	 *            info
	 * @param filterQuads
	 *            Boolean that sets the filter quads parameter for the
	 *            findChessboardCorners function. See the OpenCV docs for more
	 *            info
	 * @param fastCheck
	 *            Boolean that sets the fast-check parameter for the
	 *            findChessboardCorners function. See the OpenCV docs for more
	 *            info
	 * @param nsquaresx
	 *            Number of columns on the chessboard
	 * @param nsquaresy
	 *            Number of rows on the chessboard
	 * @return A String that holds the detected colors. It is formatted as
	 *         follows: <br>
	 *         nrows,mcols:alphabet:row1col1,row2col1, ... , row1coln, row2coln
	 *         ... <br>
	 *         For example: <br>
	 *         3,3:abcd:ab,cd,a,b,c,dd,,d <br>
	 *         In case of an error in the chessboard detection, the function
	 *         will return: <br>
	 *         e:description of error
	 */
	public native String readChessboardImage(String imgfolder, String imgfile,
			int squareSize, int hueTolerance, int saturationTolerance,
			int valueTolerance, int cannyThreshold1, int cannyThreshold2,
			boolean adaptiveThreshold, boolean normalizeImage,
			boolean filterQuads, boolean fastCheck, int nsquaresx, int nsquaresy);

	/**
	 * An AsyncTask to do the Native Call. This is to ensure that the UI doesn't
	 * freeze and the progress is visible to the user while making the call.
	 * 
	 * @author Floris
	 * 
	 */
	private class NativeChessboardTask extends
			AsyncTask<String, Integer, String> {
		@Override
		protected String doInBackground(String... imagepath) {
			String nativeResult = readChessboardImage(
					imagePath,
					imageFile,
					Integer.parseInt(sharedPrefs.getString("squareSize", "100")),
					Integer.parseInt(sharedPrefs
							.getString("hueTolerance", "20")), Integer
							.parseInt(sharedPrefs.getString(
									"saturationTolerance", "30")), Integer
							.parseInt(sharedPrefs.getString("valueTolerance",
									"400")), Integer.parseInt(sharedPrefs
							.getString("cannyThreshold1", "30")), Integer
							.parseInt(sharedPrefs.getString("cannyThreshold2",
									"90")), sharedPrefs.getBoolean(
							"adaptiveThreshold", false), sharedPrefs
							.getBoolean("normalizeImage", false), sharedPrefs
							.getBoolean("fastCheck", true), sharedPrefs
							.getBoolean("filterQuads", false), Integer
							.parseInt(sharedPrefs.getString("nsquaresx", "8")),
					Integer.parseInt(sharedPrefs.getString("nsquaresy", "8")));

			return nativeResult;
		}

		protected void onPostExecute(String result) {
			completed = true;
			if (result.charAt(0) == 'e') {
				processText.setText(result.substring(2));
				processText.setTextColor(Color.RED);

			} else {
				processText.setVisibility(View.GONE);
				progressText.setVisibility(View.GONE);

				File imgFile = new File(imagePath + "/imageWarped.jpg");
				if (imgFile.exists()) {

					warpBitmap = BitmapFactory.decodeFile(imgFile
							.getAbsolutePath());

					ChessboardImageView warpedImage = (ChessboardImageView) findViewById(R.id.warpedImageView);
					warpedImage.setVisibility(View.VISIBLE);
					warpedImage.init(warpBitmap, result, false);

					Button changeView = (Button) findViewById(R.id.changeViewButton);
					changeView.setVisibility(View.VISIBLE);

					Button saveButton = (Button) findViewById(R.id.saveFunkButton);
					saveButton.setVisibility(View.VISIBLE);
				}

				outputString = result;

			}
		}
	}
}

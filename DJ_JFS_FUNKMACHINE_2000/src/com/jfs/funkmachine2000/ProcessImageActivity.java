package com.jfs.funkmachine2000;

import java.io.File;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * The ProcessImageActivity class makes a Native call to process the user's
 * image. It also shows the progress of that native call to the user. The back
 * button is temporarily disabled so the user won't close the Activity while the
 * native call is made. When the Native Call has executed without error, it will
 * wait 1 second and close the activity.
 * 
 * @author Floris
 * 
 */
public class ProcessImageActivity extends Activity {
	private TextView processText;
	private Thread closeThread;
	private NativeChessboardTask chessTask;
	private SharedPreferences sharedPrefs;

	private String imagePath;
	private String imageFile;
	private boolean error;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.process);

		error = false;

		Intent intent = getIntent();
		imagePath = intent.getStringExtra(FunkMachineActivity.IMAGE_PATH);
		imageFile = intent.getStringExtra(FunkMachineActivity.IMAGE_FILE);

		processText = (TextView) findViewById(R.id.progressText);
		processText.setText(imagePath + "/" + imageFile);

		chessTask = new NativeChessboardTask();
		chessTask.execute(imagePath + "/" + imageFile);

		sharedPrefs = getSharedPreferences("preferences", Context.MODE_PRIVATE);
	}

	@Override
	public void onBackPressed() {
		finish();
		if (error)
			finish();
	}

	@Override
	public void onStop() {
		super.onStop();

		chessTask.cancel(true);
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
	 *            as equal when they differ less than this tolerance value.
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
			int squareSize, int hueTolerance, int cannyThreshold1,
			int cannyThreshold2, boolean adaptiveThreshold,
			boolean normalizeImage, boolean filterQuads, boolean fastCheck,
			int nsquaresx, int nsquaresy);

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

			// TODO: Add normalized image to native code
			String nativeResult = readChessboardImage(imagePath, imageFile,
					sharedPrefs.getInt("squareSize", 100),
					sharedPrefs.getInt("hueTolerance", 20),
					sharedPrefs.getInt("cannyThreshold1", 50),
					sharedPrefs.getInt("cannyThreshold2", 100),
					sharedPrefs.getBoolean("adaptiveThreshold", false),
					sharedPrefs.getBoolean("normalizeImage", false),
					sharedPrefs.getBoolean("fastCheck", true),
					sharedPrefs.getBoolean("filterQuads", false),
					sharedPrefs.getInt("nsquaresx", 8),
					sharedPrefs.getInt("nsquaresy", 8));
			return nativeResult;
		}

		protected void onPostExecute(String result) {
			if (result.charAt(0) == 'e') {
				processText.setText(result);
				error = true;
			} else {
				processText.setText("done");
				
				File imgFile = new  File(imagePath + "/imageWarped.jpg");
				if(imgFile.exists()){

				    Bitmap myBitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());

				    ImageView warpedImage = (ImageView) findViewById(R.id.warpedImageView);
				    warpedImage.setImageBitmap(myBitmap);

				}

				Intent intent = new Intent(Intent.ACTION_SEND);
				intent.setType("text/plain");
				intent.putExtra(Intent.EXTRA_TEXT, result);
				startActivity(Intent.createChooser(intent, "Share with"));

			}
		}
	}
}

package com.jfs.funkmachine2000;

import java.io.File;
import java.io.IOException;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Main activity. Called when you open the app.
 * 
 * @author Floris, Jan, Daan, Joran, Sander
 * 
 */
public class FunkMachineActivity extends Activity {
	public final static String IMAGE_PATH = "com.jfs.funkmachine2000.IMAGEPATH";
	public final static String IMAGE_FILE = "com.jfs.funkmachine2000.IMAGEFILE";

	private final static int TAKE_PICTURE = 1;
	private final static int SELECT_IMAGE = 2;
	
	public static final String TAG = "FunkMachine";

	private Uri outputFileUri;
	private TextView processButton;
	private String imagePath;
	private String imageFile;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		imagePath = Environment.getExternalStorageDirectory()
				+ "/DJ_JFS_FunkMachine";
		imageFile = "capturedImage.jpg";

	}

	/**
	 * Called when the user presses the "Process image" button.
	 * 
	 * @param view
	 */
	public void processImage(View view) {
		Intent intent = new Intent(this, ProcessImageActivity.class);
		intent.putExtra(IMAGE_PATH, imagePath);
		intent.putExtra(IMAGE_FILE, imageFile);

		processButton = (TextView) findViewById(R.id.processbutton);
		startActivity(intent);
	}

	/**
	 * Called when the user presses the "Choose image from gallery" button
	 * 
	 * @param view
	 */
	public void browseImage(View view) {
		startActivityForResult(new Intent(Intent.ACTION_PICK,
				android.provider.MediaStore.Images.Media.INTERNAL_CONTENT_URI),
				SELECT_IMAGE);

	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == SELECT_IMAGE)
			if (resultCode == Activity.RESULT_OK) {
				Uri selectedImage = data.getData();
				String realPath = getRealPathFromURI(selectedImage);
				File file = new File(realPath);
				imagePath = file.getParent();
				imageFile = file.getName();
				processButton = (TextView) findViewById(R.id.processbutton);
				processButton.setText("Process " + realPath);
			}
		if (requestCode == TAKE_PICTURE)
			if (resultCode == Activity.RESULT_OK) {
				processButton = (TextView) findViewById(R.id.processbutton);
				processButton.setText("Process captured image");
				imagePath = Environment.getExternalStorageDirectory()
						+ "/DJ_JFS_FunkMachine";
				imageFile = "capturedImage.jpg";
			}
	}

	/**
	 * Used to transform the Uri into a File Path, because C++ doesn't
	 * understand Uri.
	 * 
	 * @param contentUri
	 *            The Uri to transform
	 * @return The File Path of the content Uri
	 */
	public String getRealPathFromURI(Uri contentUri) {
		String[] proj = { MediaStore.Images.Media.DATA };
		Cursor cursor = managedQuery(contentUri, proj, null, null, null);
		int column_index = cursor
				.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
		cursor.moveToFirst();
		return cursor.getString(column_index);
	}

	/**
	 * Called when the user presses the "Capture New Image" button
	 * 
	 * @param view
	 */
	public void takePhoto(View view) {
		try {
			FunkFileManager.createFolderNoMedia(Environment.getExternalStorageDirectory()
				+ "/DJ_JFS_FunkMachine");
		} catch (IOException e) {
			showToast("Unable to create folder "+Environment.getExternalStorageDirectory()
					+ "/DJ_JFS_FunkMachine");
			return;
		}

		File f = new File(Environment.getExternalStorageDirectory()
				+ "/DJ_JFS_FunkMachine", "capturedImage.jpg");

		Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

		outputFileUri = Uri.fromFile(f);
		intent.putExtra(MediaStore.EXTRA_OUTPUT, outputFileUri);
		startActivityForResult(intent, TAKE_PICTURE);
	}

	/**
	 * Called when the user presses the "Settings" button
	 * 
	 * @param view
	 */
	public void openSettings(View view) {
		startActivity(new Intent(this, SettingsActivity.class));
	}

	/**
	 * Called when the user presses the "FunkList" button
	 * 
	 * @param view
	 */
	public void openFunkList(View view) {
		startActivity(new Intent(this, FunkListActivity.class));
	}

	/**
	 * Called when the user presses the "About" button
	 * 
	 * @param view
	 */
	public void openAbout(View view) {
		startActivity(new Intent(this, AboutActivity.class));
	}
	
	/**
	 * Shows a Toast message (useful for debugging)
	 */
	public void showToast (CharSequence message) {
		Context context = getApplicationContext();
		int duration = Toast.LENGTH_LONG;
		
		Toast toast = Toast.makeText(context, message, duration);
		toast.show();
	}
}
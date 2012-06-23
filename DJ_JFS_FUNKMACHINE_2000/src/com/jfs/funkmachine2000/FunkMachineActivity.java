package com.jfs.funkmachine2000;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.TextView;

public class FunkMachineActivity extends Activity {
	public final static String IMAGE_PATH = "com.jfs.funkmachine2000.IMAGEPATH";
	public final static String IMAGE_FILE = "com.jfs.funkmachine2000.IMAGEFILE";

	private final static int TAKE_PICTURE = 1;
	private final static int SELECT_IMAGE = 2;

	private Uri outputFileUri;
	private TextView picUsed;
	private String imagePath;
	private String imageFile;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		imagePath = getFilesDir().getPath();
		imageFile = "capturedImage.jpg";
	}

	public void processImage(View view) {
		Intent intent = new Intent(this, ProcessImageActivity.class);
		intent.putExtra(IMAGE_PATH, imagePath);
		intent.putExtra(IMAGE_FILE, imageFile);

		picUsed = (TextView) findViewById(R.id.picused);
		startActivity(intent);
	}

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
				picUsed = (TextView) findViewById(R.id.picused);
				picUsed.setText("Using " + realPath);
			}

		if (requestCode == TAKE_PICTURE)
			if (resultCode == Activity.RESULT_OK) {
				picUsed = (TextView) findViewById(R.id.picused);
				picUsed.setText("Using captured picture");
				imagePath = getFilesDir().getPath();
				imageFile = "capturedImage.jpg";
			}
	}
	
	public String getRealPathFromURI(Uri contentUri) {
        String[] proj = { MediaStore.Images.Media.DATA };
        Cursor cursor = managedQuery(contentUri, proj, null, null, null);
        int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();
        return cursor.getString(column_index);
    }
	
	public void takePhoto(View view) {
		//Remove if exists, the file MUST be created using the lines below
		File f = new File(getFilesDir(), "capturedImage.jpg");
		f.delete();
		//Create new file
		try {
			FileOutputStream fos = openFileOutput("capturedImage.jpg", Context.MODE_WORLD_WRITEABLE);
			fos.close();
		} catch(Exception e) {
			//
		}
		//Get reference to the file
		f = new File(getFilesDir(), "capturedImage.jpg");
		
		Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

		outputFileUri = Uri.fromFile(f);
		intent.putExtra(MediaStore.EXTRA_OUTPUT, outputFileUri);
		startActivityForResult(intent, TAKE_PICTURE);
	}
}
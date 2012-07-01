package com.jfs.funkmachine2000;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import android.content.Context;
import android.graphics.Bitmap;
import android.widget.Toast;

/**
 * Class for handling file input/ouput
 * 
 * @author Floris, Jan
 * 
 */
public class FunkFileManager {

	/**
	 * Creates a new folder (if it doesn't already exist) and adds a .nomedia
	 * file to prevent that the media scanner adds the images or midi files to
	 * the gallery.
	 * 
	 * @param dir
	 *            The path to the new folder
	 * @return A boolean indicating if the creation was successful
	 * 
	 */
	public static void createFolderNoMedia(String dir) throws IOException {
		File folder = new File(dir);
		if (!folder.isDirectory())
			folder.mkdir();
		File nomedia = new File(dir, ".nomedia");
		if (!nomedia.isFile())
			nomedia.createNewFile();
	}
	
	/**
	 * 
	 * Attempts to write a bitmap image to a jpeg file.
	 * 
	 * @param bmp The bitmap object to write
	 * @param filename The full path for the file to write
	 * @param context The context used to store the file
	 * @return A boolean indicating if the write was successful
	 */
	public static boolean saveBitmap (Bitmap bmp, String filename, Context context) {
		try {
			createFolderNoMedia(context.getFilesDir() + "/warped");
			FileOutputStream out = new FileOutputStream(context.getFilesDir() + "/warped/"+filename+".jpg");
			bmp.compress(Bitmap.CompressFormat.JPEG, 90, out);
			showToast("Saved image in: " + context.getFilesDir()
					+ "/warped. ", context);
		} catch (Exception e) {
			showToast("Unable to create image in: " + context.getFilesDir()
					+ "/warped. Image not saved.", context);
			return false;
		}
		return true;
	}
	
	/**
	 * Read a byte array from file
	 * @param file The file object to read
	 * @return The read byte array
	 * @throws IOException When the file couldn't be read somehow.
	 */
	public static byte[] getBytesFromFile(File file) throws IOException {
		InputStream is = new FileInputStream(file);

		// Get the size of the file
		long length = file.length();

		// You cannot create an array using a long type.
		// It needs to be an int type.
		// Before converting to an int type, check
		// to ensure that file is not larger than Integer.MAX_VALUE.
		if (length > Integer.MAX_VALUE) {
			// File is too large
		}

		// Create the byte array to hold the data
		byte[] bytes = new byte[(int) length];

		// Read in the bytes
		int offset = 0;
		int numRead = 0;
		while (offset < bytes.length
				&& (numRead = is.read(bytes, offset, bytes.length - offset)) >= 0) {
			offset += numRead;
		}

		// Ensure all the bytes have been read in
		if (offset < bytes.length) {
			throw new IOException("Could not completely read file "
					+ file.getName());
		}

		// Close the input stream and return bytes
		is.close();
		return bytes;
	}
	
	/**
	 * Shows a Toast message (useful for debugging)
	 * 
	 */
	public static void showToast(CharSequence message, Context context) {
		Context appcontext = context.getApplicationContext();
		int duration = Toast.LENGTH_LONG;

		Toast toast = Toast.makeText(appcontext, message, duration);
		toast.show();
	}
}

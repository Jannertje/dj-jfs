package com.jfs.funkmachine2000;

import java.io.File;
import java.io.IOException;

/**
 * Class for handling file input/ouput
 * 
 * @author Floris
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
}

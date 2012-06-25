package com.jfs.funkmachine2000;

/**
 * PLEASE NOTE: THIS CLASS IS NOT USED IN THE FINAL APP
 * ITS JUST TO GENERATE HEADER FILES
 * @author floris
 *
 */
public class NativeMethod {
	public native String detectChessboardFromImage(String imgfolder,
			String imgfile, int squareSize, int hueTolerance,
			int cannyThreshold1, int cannyThreshold2, boolean adaptiveThreshold,
			boolean normalizeImage, boolean filterQuads, boolean fastCheck,
			int nsquaresx, int nsquaresy);
}

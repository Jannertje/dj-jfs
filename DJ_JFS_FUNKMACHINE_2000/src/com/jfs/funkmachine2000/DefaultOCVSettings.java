package com.jfs.funkmachine2000;

/**
 * This class contains the default settings for the chessboard detection. It
 * will be used to initialize variables in the settings menu that will
 * ultimately feed the parameters in the native call.
 * 
 * @author Floris
 * 
 */
public class DefaultOCVSettings {
	public static final int SQUARE_SIZE = 100;
	public static final int HUE_TOLERANCE = 20;
	public static final int CANNY_THRESHOLD_1 = 50;
	public static final int CANNY_THRESHOLD_2 = 100;
}

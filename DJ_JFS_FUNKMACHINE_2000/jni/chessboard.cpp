#include "jni.h"
#include <android/log.h>
#include <opencv2/core/core.hpp>
#include <opencv2/calib3d/calib3d.hpp>
#include <opencv2/imgproc/imgproc.hpp>
#include <opencv2/highgui/highgui.hpp>

#include <string>
#include <sstream>
#include <iostream>

#define APPNAME "FUNKMACHINE"

using namespace std;
using namespace cv;

extern "C" {
JNIEXPORT jstring JNICALL Java_com_jfs_funkmachine2000_ProcessImageActivity_readChessboardImage(
		JNIEnv * enc, jobject obj, jstring jimageFolder, jstring jimageFile,
		jint jsquareSize, jint jhueTolerance, jint jcannyThreshold1,
		jint jcannyThreshold2, jboolean adaptiveThreshold,
		jboolean normalizeImage, jboolean filterQuads, jboolean fastCheck,
		jint nsquaresx, jint nsquaresy);
}

int findChessboardConfig(int at, int ni, int fq, int fc) {
	return at * CALIB_CB_ADAPTIVE_THRESH + ni * CALIB_CB_NORMALIZE_IMAGE
			+ fq * CALIB_CB_FILTER_QUADS + fc * CALIB_CB_FAST_CHECK;
}

Mat detectChessboardFromImage(Mat img, int squareSize, int nsquaresx,
		int nsquaresy, bool adaptiveThreshold, bool normalizeImage,
		bool filterQuads, bool fastCheck) {

	// Number of corners in the interior of the chessboard
	Size patternsize(nsquaresx - 1, nsquaresy - 1);

	// Create coordinate arrays for the warp perspective and findChessboardCorners output
	Point2f destinationCorners[4], outerCorners[4];
	vector < Point2f > corners;

	// Detect the chessboard corners
	bool patternfound = findChessboardCorners(img, patternsize, corners,
			findChessboardConfig(adaptiveThreshold, normalizeImage, filterQuads,
					fastCheck));

	if (!patternfound)
		return Mat();

	// Set coordinates for warp
	outerCorners[0] = corners[0];
	outerCorners[1] = corners[6];
	outerCorners[2] = corners[42];
	outerCorners[3] = corners[48];
	destinationCorners[0] = Point2f(squareSize * 7, squareSize);
	destinationCorners[1] = Point2f(squareSize * 7, squareSize * 7);
	destinationCorners[2] = Point2f(squareSize, squareSize);
	destinationCorners[3] = Point2f(squareSize, squareSize * 7);

	Mat pTransform = getPerspectiveTransform(outerCorners, destinationCorners);
	return pTransform;
}

string detectColors(Mat img, unsigned int nsquaresx, unsigned int nsquaresy,
		string imgfolder, Mat pTransform, unsigned int squareSize,
		unsigned int hueTolerance, int cannyThreshold1, int cannyThreshold2) {

	// Create a new image and write the trandsformed source image
	Size warpedSize = Size(squareSize * 8, squareSize * 8);
	Mat warped = Mat(warpedSize, CV_8UC3);
	warpPerspective(img, warped, pTransform, warpedSize);

	// Create a HSV copy for easy color reading
	Mat warpedHSV;
	cvtColor(warped, warpedHSV, CV_BGR2HSV, 0);

	// Detect edges with Canny
	Mat cannyOutput;
	Canny(warped, cannyOutput, cannyThreshold1, cannyThreshold2, 3);

	// Find the contours from the detected edges
	vector < vector<Point> > contours;
	vector < Vec4i > hierarchy;
	findContours(cannyOutput, contours, hierarchy, CV_RETR_CCOMP,
			CV_CHAIN_APPROX_SIMPLE);

	// Number of colors detected so far
	unsigned int ncolors = 0;
	unsigned const int maxcolors = 15;
	// Arrays holding the color hue values and color indexes
	int colors[maxcolors];
	int contourColors[maxcolors];

	// Array containing all chess fields strings
	stringstream chessboard[nsquaresx * nsquaresy];
	// Stream containing all the color labels
	stringstream labelStream;

	bool found;
	Rect bounding;
	double area;
	Vec3b sampleColor;
	char colorLabel;
	stringstream temps;
	unsigned int i, j, row, col, x, y, colorDiff;

	// Loop through the contours
	for (i = 0; i < contours.size(); i++) {
		// Select each contour only once
		if (hierarchy[i][3] != -1) {
			area = contourArea(contours[i]);
			bounding = boundingRect(contours[i]);

			// Center points
			x = bounding.x + bounding.width / 2;
			y = bounding.y + bounding.height / 2;

			// Filter chessboard squares
			if (bounding.width < squareSize * 0.8
					&& bounding.height < squareSize * 0.8) {
				// Filter blobs contours that are too thin or small
				if (area > max(bounding.area() * 0.2, (double) 10)) {
					// Get the HSV array at the center pixel of the shape
					sampleColor = warpedHSV.at < Vec3b > (y, x);


					// Check if the sampleColor or similar color is found before
					found = false;
					for (j = 0; j < ncolors; j++) {
						colorDiff = abs(sampleColor[0] - colors[j]);
						if (colorDiff < hueTolerance
								|| colorDiff > 180 - hueTolerance) {
							contourColors[i] = j;
							found = true;
							break;
						}
					}

					// If not found, add to the colors array
					if (!found) {
						if (ncolors == maxcolors) {
							return "e: too many colors";
						}
						colors[ncolors] = sampleColor[0];
						contourColors[i] = ncolors;
						colorLabel = 'a' + ncolors;
						labelStream << colorLabel;
						ncolors++;
					}

					// Add labels to the warped image
					colorLabel = 'a' + contourColors[i];
					temps.str(std::string());
					temps.clear();
					temps << colorLabel;

					putText(warped, temps.str(), Point(x, y),
							FONT_HERSHEY_PLAIN, 1, Scalar(0, 0, 0), 2, CV_AA,
							false);
					putText(warped, temps.str(), Point(x, y),
							FONT_HERSHEY_PLAIN, 1, Scalar(255, 255, 255), 1,
							CV_AA, false);

					// Determine in which chessboard field the contour resides
					row = y / squareSize;
					col = x / squareSize;
					chessboard[col * nsquaresx + row] << colorLabel;
				}
			}
		}
	}

	// Create the output string
	stringstream outputstream;
	outputstream << nsquaresx;
	outputstream << ',';
	outputstream << nsquaresy;
	outputstream << ':';
	outputstream << labelStream.str();
	outputstream << ':';
	for (i = 0; i < nsquaresx; i++) {
		for (j = 0; j < nsquaresy; j++) {
			if (i != 0 || j != 0)
				outputstream << ',';
			outputstream << chessboard[i * nsquaresx + j].str();
		}
	}

	string imageWarped = imgfolder + "/imageWarped.jpg";
	imwrite(imageWarped, warped);

	return outputstream.str();
}

JNIEXPORT jstring JNICALL Java_com_jfs_funkmachine2000_ProcessImageActivity_readChessboardImage(
		JNIEnv * env, jobject obj, jstring jimageFolder, jstring jimageFile,
		jint jsquareSize, jint jhueTolerance, jint jcannyThreshold1,
		jint jcannyThreshold2, jboolean adaptiveThreshold,
		jboolean normalizeImage, jboolean filterQuads, jboolean fastCheck,
		jint nsquaresx, jint nsquaresy) {
	jboolean isCopy;
	const char *imageFolder = env->GetStringUTFChars(jimageFolder, &isCopy);
	const char *imageFile = env->GetStringUTFChars(jimageFile, &isCopy);

	// Create folder and filepath string
	stringstream pathbuilder;
	pathbuilder << imageFolder;
	string imgfolder = pathbuilder.str();
	pathbuilder << "/";
	pathbuilder << imageFile;
	string imgfile = pathbuilder.str();


	// Read an image to matrix
	Mat img = imread(imgfile, CV_LOAD_IMAGE_COLOR);
	if (img.data == NULL) {
		string rval = "e: Unable to read image " + imgfile;
		return env->NewStringUTF(rval.c_str());
	}

	Mat pTransform = detectChessboardFromImage(img, (int) jsquareSize,
			(int) nsquaresx, (int) nsquaresy, (bool) adaptiveThreshold,
			(bool) normalizeImage, (bool) filterQuads, (bool) fastCheck);

	if (pTransform.data == NULL) {
		string rval = "e: Chessboard not found";
		return env->NewStringUTF(rval.c_str());
	}

	String outputString = detectColors(img, (unsigned int) nsquaresx, (unsigned int) nsquaresy,
			imgfolder, pTransform, (unsigned int) jsquareSize, (unsigned int) jhueTolerance,
			(int) jcannyThreshold1, (int) jcannyThreshold2);
	env->ReleaseStringUTFChars(jimageFolder, imageFolder);
	env->ReleaseStringUTFChars(jimageFile, imageFile);

	const char *output = outputString.c_str();
	return env->NewStringUTF(output);

}

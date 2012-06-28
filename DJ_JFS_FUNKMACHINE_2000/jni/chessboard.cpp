#include "jni.h"
#include <android/log.h>
#include <opencv2/core/core.hpp>
#include <opencv2/calib3d/calib3d.hpp>
#include <opencv2/imgproc/imgproc.hpp>
#include <opencv2/highgui/highgui.hpp>

#include <string>
#include <sstream>
#include <iostream>
#include "orb.h"

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

Mat detectChessboardFromImage(Mat &img, int squareSize, int nsquaresx,
		int nsquaresy, bool adaptiveThreshold, bool normalizeImage,
		bool filterQuads, bool fastCheck) {

	// Number of corners in the interior of the chessboard
	Size patternsize(nsquaresx - 1, nsquaresy - 1);

	// Create coordinate arrays for the warp perspective and findChessboardCorners output
	Point2f destinationCorners[4], outerCorners[4];
	vector<Point2f> corners;

	// Detect the chessboard corners
	bool patternfound = findChessboardCorners(img, patternsize, corners,
			findChessboardConfig(adaptiveThreshold, false, filterQuads,
					fastCheck));

	if (!patternfound)
		return Mat();

	cornerSubPix(img, corners, Size(8, 8), Size(-1, -1),
			TermCriteria(CV_TERMCRIT_EPS + CV_TERMCRIT_ITER, 30, 0.1));

	// Set coordinates for warp
	outerCorners[0] = corners[6];
	outerCorners[1] = corners[48];
	outerCorners[2] = corners[0];
	outerCorners[3] = corners[42];
	destinationCorners[0] = Point2f(squareSize * 7, squareSize);
	destinationCorners[1] = Point2f(squareSize * 7, squareSize * 7);
	destinationCorners[2] = Point2f(squareSize, squareSize);
	destinationCorners[3] = Point2f(squareSize, squareSize * 7);

	Mat pTransform = getPerspectiveTransform(outerCorners, destinationCorners);
	return pTransform;
}



double distanceBetween(Point p1, Point p2) {
	return sqrt((p1.x - p2.x) * (p1.x - p2.x) + (p1.y - p2.y) * (p1.y - p2.y));
}

string detectColors(Mat &img, unsigned int nsquaresx, unsigned int nsquaresy,
		string imgfolder, Mat pTransform, unsigned int squareSize,
		unsigned int hueTolerance, int cannyThreshold1, int cannyThreshold2) {

	__android_log_print(ANDROID_LOG_INFO, APPNAME,  "start detectColors...");
	// Create a new image and write the trandsformed source image
	Size warpedSize = Size(squareSize * nsquaresx, squareSize * nsquaresy);
	Mat warped = Mat(warpedSize, CV_8U);
	Mat warpedf = Mat(warpedSize, CV_32F);
	warpPerspective(img, warped, pTransform, warpedSize);
	warpPerspective(img, warpedf, pTransform, warpedSize);

	// Normalize image
	Mat normalized;
	normalize(warped, normalized, 0, 255, CV_MINMAX);

	// Shift image
	Mat warpedShifted;
	pyrMeanShiftFiltering(normalized, warpedShifted, 10, 13, 1);
	normalized.release();



	// Create a HSV copy for easy color reading
	Mat warpedHSV;
	cvtColor(warpedf, warpedHSV, CV_BGR2HSV, 0);

	// Detect edges with Canny
	Mat cannyOutput;
	Canny(warpedShifted, cannyOutput, cannyThreshold1, cannyThreshold2, 3);
	warpedShifted.release();

	// Find the contours from the detected edges
	vector<vector<Point> > contours;
	vector<Vec4i> hierarchy;

	findContours(cannyOutput, contours, hierarchy, CV_RETR_CCOMP,
			CV_CHAIN_APPROX_SIMPLE);

	// Stream containing all the color labels
	stringstream labelStream;

	unsigned int i, j, x, y, row, col;
	double area;
	bool innerOK;
	Rect bounding;
	vector<int> structuredContours[nsquaresx * nsquaresy];
	vector<Point> hull;
	vector<Point> foundContourCenters;
	vector< vector < Point > > hulls;
	Point center;
	bool center_ok;

	// Loop through the contours
	for (i = 0; i < contours.size(); i++) {
		innerOK = false;
		if (hierarchy[i][2] == -1) {
			innerOK = true;
		} else {
			area = contourArea(contours[hierarchy[i][2]]);
			if (area < 30) {
				innerOK = true;
			}
		}
		if (innerOK) {
			bounding = boundingRect(contours[i]);

			// Center points
			x = bounding.x + bounding.width / 2;
			y = bounding.y + bounding.height / 2;

			// Filter chessboard squares
			if (bounding.width < squareSize * 0.8
					&& bounding.height < squareSize * 0.8) {
				convexHull(contours[i], hull);
				area = contourArea(hull);
				// Filter blobs contours that are too thin or small
				if (area > 120 && bounding.width > 8 && bounding.height > 8) {
					hulls.clear();
					hulls.push_back(contours[i]);
					drawContours(warped, hulls, 0, Scalar(0,0,255));
					// Avoid doubles
					center = Point(x, y);
					center_ok = true;
					for (j = 0; j < foundContourCenters.size(); j++) {
						if (distanceBetween(center, foundContourCenters[j]) < 10) {
							center_ok = false;
						}
					}
					if (center_ok) {
						// Determine in which chessboard field the contour resides
						row = y / squareSize;
						col = x / squareSize;
						// Add the contour to a new vector at the correct position
						structuredContours[col * nsquaresx + row].push_back(i);
						foundContourCenters.push_back(Point(x, y));
					}
				}
			}
		}
	}

	// Number of colors detected so far
	unsigned int ncolors = 0;
	unsigned const int maxcolors = 15;
	// Arrays holding the color hue values and color indexes
	int colors[maxcolors][2];
	int contourColors[maxcolors];

	// Loop through chessboard squares
	orblist *l;
	unsigned int s, hueDiff, valueDiff, satDiff;
	char colorLabel;
	int sampleColor[2];
	bool found;
	stringstream temps;

	// Create the chessboard string
	stringstream chessboardstream;

	for (s = 0; s < nsquaresx * nsquaresy; s++) {
		l = init_orblist(structuredContours[s].size(),
				squareSize * (s / nsquaresx), squareSize * (s % nsquaresx),
				squareSize, squareSize);

		for (i = 0; i < structuredContours[s].size(); i++) {
			bounding = boundingRect(contours[structuredContours[s][i]]);

			// Center points
			x = bounding.x + bounding.width / 2;
			y = bounding.y + bounding.height / 2;

			// Sample the hue at the center 9 pixels
			sampleColor[0] = warpedHSV.at<Vec3b>((int) y , (int) x )[0];
			/*sampleColor[0] += warpedHSV.at<Vec3b>((int) y + 1, (int) x)[0];
			sampleColor[0] += warpedHSV.at<Vec3b>((int) y + 1, (int) x + 1)[0];
			sampleColor[0] += warpedHSV.at<Vec3b>((int) y - 1, (int) x - 1)[0];
			sampleColor[0] += warpedHSV.at<Vec3b>((int) y - 1, (int) x)[0];
			sampleColor[0] += warpedHSV.at<Vec3b>((int) y - 1, (int) x + 1)[0];
			sampleColor[0] += warpedHSV.at<Vec3b>((int) y, (int) x - 1)[0];
			sampleColor[0] += warpedHSV.at<Vec3b>((int) y + 1, (int) x)[0];
			sampleColor[0] += warpedHSV.at<Vec3b>((int) y, (int) x + 1)[0];
			sampleColor[0] /= 9;*/

			// Sample the hue at the center 9 pixels
			sampleColor[1] = warpedHSV.at<Vec3b>((int) y , (int) x)[1];
			/*sampleColor[1] += warpedHSV.at<Vec3b>((int) y + 1, (int) x)[2];
			sampleColor[1] += warpedHSV.at<Vec3b>((int) y + 1, (int) x + 1)[2];
			sampleColor[1] += warpedHSV.at<Vec3b>((int) y - 1, (int) x - 1)[2];
			sampleColor[1] += warpedHSV.at<Vec3b>((int) y - 1, (int) x)[2];
			sampleColor[1] += warpedHSV.at<Vec3b>((int) y - 1, (int) x + 1)[2];
			sampleColor[1] += warpedHSV.at<Vec3b>((int) y, (int) x - 1)[2];
			sampleColor[1] += warpedHSV.at<Vec3b>((int) y + 1, (int) x)[2];
			sampleColor[1] += warpedHSV.at<Vec3b>((int) y, (int) x + 1)[2];
			sampleColor[1] /= 9;*/


			sampleColor[2] = warpedHSV.at<Vec3b>((int) y, (int) x )[2];

			// Check if the sampleColor or similar color is found before
			found = false;
			for (j = 0; j < ncolors; j++) {
				hueDiff = abs(sampleColor[0] - colors[j][0]);
				satDiff = abs(sampleColor[1] - colors[j][1]);
				valueDiff = abs(sampleColor[2] - colors[j][2]);
				if (hueDiff < hueTolerance
						|| hueDiff > 180 - hueTolerance) {
					if(valueDiff < 500) {
						if(satDiff< 30) {
							contourColors[i] = j;
							found = true;
							break;
						}
					}
				}
			}
			// If not found, add to the colors array
			if (!found) {
				if (ncolors == maxcolors) {
					return "e: too many colors";
				}
				colors[ncolors][0] = sampleColor[0];
				colors[ncolors][1] = sampleColor[1];
				colors[ncolors][2] = sampleColor[2];
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

			putText(warped, temps.str(), Point(x, y), FONT_HERSHEY_PLAIN, 1,
					Scalar(0, 0, 0), 3, CV_AA, false);
			putText(warped, temps.str(), Point(x, y), FONT_HERSHEY_PLAIN, 1,
					Scalar(255, 255, 255), 1, CV_AA, false);

			add_orb(l, i, x, y, colorLabel);
		}

		sort_orblist(l);

		if (s != 0)
			chessboardstream << ',';

		int p;
		for (p = 0; p < l->num; p++) {
			chessboardstream << l->at[p]->v;
		}

		free_orblist(l);
	}

	// Create the output string
	stringstream outputstream;
	outputstream << nsquaresx;
	outputstream << ',';
	outputstream << nsquaresy;
	outputstream << ':';
	outputstream << labelStream.str();
	outputstream << ':';
	outputstream << chessboardstream.str();

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
	Mat grayimg = imread(imgfile, CV_LOAD_IMAGE_GRAYSCALE);
	if (img.data == NULL || grayimg.data == NULL) {
		string rval = "e: Unable to read image " + imgfile;
		return env->NewStringUTF(rval.c_str());
	}

	Mat pTransform;


	if((bool) normalizeImage) {
		Mat stretched_img;
		normalize(grayimg, stretched_img, 0, 255, CV_MINMAX);
		grayimg.release();
		Mat blur_img;
		blur(stretched_img, blur_img, Size(3, 3));
		stretched_img.release();

		pTransform = detectChessboardFromImage(blur_img, (int) jsquareSize,
				(int) nsquaresx, (int) nsquaresy, (bool) adaptiveThreshold,
				(bool) normalizeImage, (bool) filterQuads, (bool) fastCheck);
	} else {
		pTransform = detectChessboardFromImage(img, (int) jsquareSize,
						(int) nsquaresx, (int) nsquaresy, (bool) adaptiveThreshold,
						(bool) normalizeImage, (bool) filterQuads, (bool) fastCheck);
	}

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

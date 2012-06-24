#include <jni.h>

#include <opencv2/core/core.hpp>
#include <opencv2/calib3d/calib3d.hpp>
#include <opencv2/imgproc/imgproc.hpp>
#include <opencv2/highgui/highgui.hpp>

#include <stdio.h>
#include <string.h>

using namespace std;
using namespace cv;

extern "C" {
JNIEXPORT jstring JNICALL Java_com_jfs_funkmachine2000_ProcessImageActivity_detectChessboardFromImage(
		JNIEnv * env, jclass obj, jstring imgpath, jstring imgfile, jint jsquareSize,
		jint jhueTolerance, jint jcannyThreshold1, jint jcannyThreshold2);
}

JNIEXPORT jstring JNICALL Java_com_jfs_funkmachine2000_ProcessImageActivity_detectChessboardFromImage(
		JNIEnv * env, jclass obj, jstring imgpath, jstring imgfile, jint jsquareSize,
		jint jhueTolerance, jint jcannyThreshold1, jint jcannyThreshold2) {


	jboolean isCopy;
	const char *imagePathConst = env->GetStringUTFChars(imgpath, &isCopy);
	const char *imageFileName = env->GetStringUTFChars(imgfile, &isCopy);
	char imagePath[strlen(imagePathConst) + 10];
	strcpy(imagePath, imagePathConst);
	char imageFile[strlen(imagePathConst) + strlen(imageFileName) + 10];
	strcpy(imageFile, imagePath);
	strcat(imageFile, "/");
	strcat(imageFile, imageFileName);

    env->ReleaseStringUTFChars(imgpath, imagePathConst);
    env->ReleaseStringUTFChars(imgfile, imageFileName);

    int squareSize = (int) jsquareSize;
    int hueTolerance = (int) jhueTolerance;
    int cannyThreshold1 = (int) jcannyThreshold1;
    int cannyThreshold2 = (int) jcannyThreshold2;

	// Number of corners in the interior of the chessboard
	Size patternsize(7, 7);
	// Read a greyscale and color copy
	Mat img = imread(imageFile, CV_LOAD_IMAGE_COLOR);
	Mat grayimg = imread(imageFile, CV_LOAD_IMAGE_GRAYSCALE);

	if (img.data == NULL) {
		char rval[30+strlen(imageFile)];
		strcpy(rval,"e: Unable to read image ");
		strcat(rval, imageFile);
		return env->NewStringUTF(rval);
	}

	// This is where the coordinates for the warpPerspective go
	Point2f destinationCorners[4], outerCorners[4];
	// This is where the findChessBoardCorners output goes
	vector < Point2f > corners;

	bool patternfound = findChessboardCorners(img, patternsize, corners,
			CALIB_CB_FAST_CHECK + CALIB_CB_NORMALIZE_IMAGE);

	if (patternfound) {
		cornerSubPix(grayimg, corners, Size(11, 11), Size(-1, -1),
				TermCriteria(CV_TERMCRIT_EPS + CV_TERMCRIT_ITER, 30, 0.1));
	} else {
		char rval[] = "e: Chessboard not found";
		return env->NewStringUTF(rval);
	}

	// Draw the detected corners
	drawChessboardCorners(grayimg, patternsize, Mat(corners), patternfound);

	// Set coordinates for warp
	outerCorners[0] = corners[0];
	outerCorners[1] = corners[6];
	outerCorners[2] = corners[42];
	outerCorners[3] = corners[48];
	destinationCorners[0] = Point2f(squareSize * 7, squareSize);
	destinationCorners[1] = Point2f(squareSize * 7, squareSize * 7);
	destinationCorners[2] = Point2f(squareSize, squareSize);
	destinationCorners[3] = Point2f(squareSize, squareSize * 7);

	// Get the matrix for the warp transformation
	Mat ptrans = getPerspectiveTransform(outerCorners, destinationCorners);

	// Create a new image and wCV_32SC1rite the transformed source image
	Size warpedsize = Size(squareSize * 8, squareSize * 8);
	Mat warped = Mat(warpedsize, CV_8UC3);
	warpPerspective(img, warped, ptrans, warpedsize);

	// Create a HSV copy for easy color reading
	Mat warpedHSV;
	cvtColor(warped, warpedHSV, CV_BGR2HSV, 0);

	// Detect edges with Canny
	Mat canny_output;
	Canny(warped, canny_output, cannyThreshold1, cannyThreshold2, 3);

	// Find the contours from the detected edges
	vector < vector<Point> > contours;
	vector < Vec4i > hierarchy;
	findContours(canny_output, contours, hierarchy, CV_RETR_CCOMP,
			CV_CHAIN_APPROX_SIMPLE);

	// Number of colors detected so far
	unsigned int ncolors = 0;
	// Arrays holding the color hue values and the color indexes for the contours
	int colors[20];
	Vec3b colorsrgb[20];
	int contourColors[contours.size()];

	// Array containing all chess fields
	stringstream chessboard[8 * 8];

	bool found;
	double area;
	char colorlabel;
	Rect bounding;
	Vec3b sampleColor;
	stringstream tempss;
	string temps;
	unsigned int i = 0, j, row, col, x, y, nfieldcolors, subx, suby;

	// String stream containing all color labels
	stringstream labelstream;

	// Loop through the contours
	for (; i < contours.size(); i++) {
		if (hierarchy[i][3] != -1) {
			area = contourArea(contours[i]);
			bounding = boundingRect(contours[i]);

			// Center points
			x = bounding.x + bounding.width / 2;
			y = bounding.y + bounding.height / 2;

			// Check if the contour is valid based on area and bounding properties
			if (area < squareSize * squareSize * 0.75
					&& area > max(bounding.area() * 0.2, (double) 10)) {
				// Get the HSV array at the center pixel of the shape
				sampleColor = warpedHSV.at < Vec3b > ((int) y, (int) x);

				// Check if the sampleColor or similar color is found before
				j = 0;
				found = false;
				for (; j < ncolors; j++) {
					if (abs(sampleColor[0] - colors[j]) < hueTolerance
							|| abs(sampleColor[0] - colors[j])
									> 180 - hueTolerance) {
						contourColors[i] = j;
						found = true;
						break;
					}
				}
				// If not found, add to the colors array
				if (!found) {
					colors[ncolors] = sampleColor[0];
					colorsrgb[ncolors] = warped.at < Vec3b > ((int) y, (int) x);
					contourColors[i] = ncolors;
					colorlabel = 'a' + ncolors;
					labelstream << colorlabel;
					ncolors++;
					if (ncolors > 20) {
						// TODO: Make error
						break;
					}
				}

				// Add labels to the warped image
				colorlabel = 'a' + contourColors[i];
				tempss.clear();
				tempss << colorlabel;
				tempss >> temps;
				putText(warped, temps, Point(x, y), FONT_HERSHEY_PLAIN, 1,
						Scalar(0, 0, 0), 2, CV_AA, false);
				putText(warped, temps, Point(x, y), FONT_HERSHEY_PLAIN, 1,
						Scalar(255, 255, 255), 1, CV_AA, false);

				// Determine in which chessboard field the contour resides
				row = y / squareSize;
				col = x / squareSize;
				chessboard[col * 8 + row] << colorlabel;
			}
		}
	}

	// Create the output string
	stringstream outputstream;
	outputstream << "8,8:";
	outputstream << labelstream;
	outputstream << ":";
	for (i = 0; i < 8; i++) {
		for (j = 0; j < 8; j++) {
			if (i != 0 || j != 0)
				outputstream << ',';
			outputstream << chessboard[i * 8 + j].str();
		}
	}

	String output;
	outputstream >> output;
	const char *outputstring = output.c_str();

	// Write the warped image to file
	char imageWarped[200];
	strcpy(imageWarped, imagePath);
	strcat(imageWarped, "/imageWarped.jpg");
	imwrite(imageWarped, warped);

	return env->NewStringUTF(outputstring);
}

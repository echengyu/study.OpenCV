package edu.neu.mhealth.debug;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import edu.neu.mhealth.debug.helper.Global;

import android.os.Bundle;
import android.app.Activity;
import android.util.Log;
import android.view.Menu;
import android.view.SurfaceView;
import android.view.WindowManager;

public class CameraActivity extends Activity implements CvCameraViewListener2 {

	/*Basic Variables*/
	private final String TAG = Global.APP_LOG_TAG;
	private final int HOUGH_LINE_COUNT = 20;
	
	private List<Point> points1 = new ArrayList<Point>();
	private List<Point> points2 = new ArrayList<Point>();
	
	/*OpenCv Variables*/
	private Mat mRgba;
	private Mat mGray;
	private Mat lines;
	private CameraBridgeViewBase mOpenCvCameraView;
	private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
		@Override
		public void onManagerConnected(int status) {
			switch (status) {
				case LoaderCallbackInterface.SUCCESS: {
					Log.i(TAG, "OpenCV loaded successfully");
					mOpenCvCameraView.enableView();
				}
				break;
				default: {
					super.onManagerConnected(status);
				}
				break;
			}
		}
	};
	
	/*
	 *   Activity Callbacks
	 *   
	 **/
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		setContentView(R.layout.activity_camera);
		mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.HellpOpenCvView);
		mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
		mOpenCvCameraView.setCvCameraViewListener(this);
	}

	@Override
	protected void onPause() {
		super.onPause();
		if (mOpenCvCameraView != null) {
			mOpenCvCameraView.disableView();
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_6, this, mLoaderCallback);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (mOpenCvCameraView != null) {
			mOpenCvCameraView.disableView();
		}
	}

	/*
	 *   Opencv Callbacks
	 *   
	 **/
	@Override
	public void onCameraViewStarted(int width, int height) {
		lines = new Mat();
	}

	@Override
	public void onCameraViewStopped() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
		mRgba = inputFrame.rgba();
		mGray = inputFrame.gray();
		
		// 目前螢幕解析度
		int rows = (int) inputFrame.rgba().size().height;
        int cols = (int) inputFrame.rgba().size().width;       
        Log.e("sizeRgba", "("+rows+", "+cols+")");
		
		// 二值化
		Imgproc.cvtColor(mRgba, mRgba, Imgproc.COLOR_RGBA2GRAY, 0);
		
		// 高斯濾波器
		Imgproc.GaussianBlur(mRgba, mRgba, new Size(3, 3), 6);
		
		// 邊緣偵測
		Imgproc.Canny(mRgba, mRgba, 220, 180);	
		
		// 蝕刻
		Imgproc.erode(mRgba, mRgba, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(1, 1)));
		
		// 膨脹
        Imgproc.dilate(mRgba, mRgba, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(4, 4)));
        
        // 霍夫找線
        Imgproc.HoughLinesP(mRgba, lines, 1, Math.PI/180, 180, 80, 0);		

        for (int x = 0; x < lines.cols() && x < HOUGH_LINE_COUNT; x++) {
        	double[] vec = lines.get(0, x);

        	if(vec!=null) {
        		double x1 = vec[0],
        		       y1 = vec[1],
        		       x2 = vec[2],
        		       y2 = vec[3];

        		if(x%2 == 1) {
        			points1.add(new Point(x1, y1));
        		} else {
        			points2.add(new Point(x2, y2));
        		}
        	}
        }
		
//		Log.e("lines.cols()", String.valueOf(lines.cols()));
//		Log.e("points1", points1.toString());
//		Log.e("points2", points2.toString());		
		
		if(points1.size() != 0 && points2.size() != 0){			
			if(Math.abs(points1.get(0).x - points2.get(0).x) > (points2.get(0).x / 5)){
				Core.rectangle(inputFrame.rgba(), points1.get(0),  points2.get(0), new Scalar(255, 127, 63, 255), 10);
				
				
			}else{
				Core.trace(inputFrame.rgba());
			}
		}else{
			Core.trace(inputFrame.rgba());
		}
		
	    points1.removeAll(points1);
	    points2.removeAll(points2);
		
//        if(points1.size() == 4 && points2.size() == 4){
//      	  
//      	  double pointX = Math.abs(points1.get(0).x - points2.get(0).x);
//      	  double pointY = Math.abs(points1.get(0).y - points2.get(0).y);
//      	  
//      	  Point point0 = new Point(points1.get(0).x, points1.get(0).y);
//          Point point1 = new Point((points1.get(0).x + pointX), points1.get(0).y);
//          Point point2 = new Point((points1.get(0).x + pointX), (points1.get(0).y + pointY));
//          Point point3 = new Point(points1.get(0).x, (points1.get(0).y + pointY));
//  	  	       	          
////          Point point0 = points1.get(0);
////          Point point1 = points1.get(1);
////          Point point2 = points1.get(2);
////          Point point3 = points1.get(3);
//          
////          Point point0 = new Point(100, 100);
////          Point point1 = new Point(400, 100);
////          Point point2 = new Point(400, 400);
////          Point point3 = new Point(100, 400);
//    
//          Core.line(mRgba, point0, point1, new Scalar(255, 127, 63, 255), 10);
//          Core.line(mRgba, point1, point2, new Scalar(255, 127, 63, 255), 10);
//          Core.line(mRgba, point2, point3, new Scalar(255, 127, 63, 255), 10);
//          Core.line(mRgba, point3, point0, new Scalar(255, 127, 63, 255), 10);
//          
//          Core.line(mGray, point0, point1, new Scalar(255, 127, 63, 255), 10);
//          Core.line(mGray, point1, point2, new Scalar(255, 127, 63, 255), 10);
//          Core.line(mGray, point2, point3, new Scalar(255, 127, 63, 255), 10);
//          Core.line(mGray, point3, point0, new Scalar(255, 127, 63, 255), 10);
//          
//          points1.removeAll(points1);
//          points2.removeAll(points2);
//        }
        

		
//		Log.e("points", points1.toString());
		
		return mRgba;
//		return mGray;
	}

	
}

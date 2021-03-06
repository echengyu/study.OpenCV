package edu.neu.mhealth.debug;

import java.util.ArrayList;
import java.util.List;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.highgui.Highgui;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgproc.Moments;


import android.os.Bundle;
import android.app.Activity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceView;
import android.view.WindowManager;

public class CameraActivity extends Activity implements CvCameraViewListener2 {


	protected static final String TAG = "why?";
	private List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
	private Mat hierarchy;
	private Mat mIntermediateMat;
	private MatOfPoint2f approxCurve;

	/*OpenCv Variables*/
	private Mat mRgba;
	private Mat mGray;
	
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
	protected void onResume() {
		super.onResume();
		OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_6, this, mLoaderCallback);
	}

	@Override
	protected void onPause() {
		super.onPause();
		if (mOpenCvCameraView != null) {
			mOpenCvCameraView.disableView();
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (mOpenCvCameraView != null) {
			mOpenCvCameraView.disableView();
		}
	}

	@Override
	public void onCameraViewStarted(int width, int height) {
		mRgba = new Mat(height, width, CvType.CV_8UC4);
		mIntermediateMat = new Mat(height, width, CvType.CV_8UC4);
		mGray = new Mat(height, width, CvType.CV_8UC1);
		hierarchy = new Mat();
	}

	@Override
	public void onCameraViewStopped() {
		mRgba.release();
		mGray.release();
		mIntermediateMat.release();
		hierarchy.release();
	}

	public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
		mRgba = inputFrame.rgba();
		Point resolutionPoint = new Point(inputFrame.rgba().width(), inputFrame.rgba().height());

		// 二值化
		Imgproc.cvtColor(mRgba, mRgba, Imgproc.COLOR_RGBA2GRAY, 0);

		// 高斯濾波器
		Imgproc.GaussianBlur(mRgba, mRgba, new Size(3, 3), 6);

		// 邊緣偵測
		Imgproc.Canny(mRgba, mRgba, 360, 180);

		// 蝕刻
		Imgproc.erode(mRgba, mRgba, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(1, 1)));

		// 膨脹
		Imgproc.dilate(mRgba, mRgba, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(4, 4)));

		contours = new ArrayList<MatOfPoint>();
		hierarchy = new Mat();

		// 找影像輪廓
		Imgproc.findContours(mRgba, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE, new Point(0, 0));
		hierarchy.release();

//		// 劃出輪廓線
//		Imgproc.drawContours(inputFrame.rgba(), contours, -1, new Scalar(255, 127, 63, 255));
		
		if(contours.size() != 0 &&contours.size() < 500){
			
			// 劃出輪廓線
			Imgproc.drawContours(inputFrame.rgba(), contours, -1, new Scalar(255, 255, 0, 255));

//	        Rect touchedRect = new Rect();
//	        Scalar mBlobColorRgba = new Scalar(255);
//
//	        touchedRect.x = 10;
//	        touchedRect.y = 10;
//
//	        touchedRect.width = 100;
//	        touchedRect.height = 100;
//
//	        Mat touchedRegionRgba = mRgba.submat(touchedRect);
//
//	        Mat touchedRegionHsv = new Mat();
//	        Imgproc.cvtColor(touchedRegionRgba, touchedRegionHsv, Imgproc.COLOR_RGB2HSV_FULL);
//
//	        // Calculate average color of touched region
//	        Scalar mBlobColorHsv = Core.sumElems(touchedRegionHsv);
//	        int pointCount = touchedRect.width*touchedRect.height;
//	        for (int i = 0; i < mBlobColorHsv.val.length; i++)
//	            mBlobColorHsv.val[i] /= pointCount;
//
//	        mBlobColorRgba = converScalarHsv2Rgba(mBlobColorHsv);
//
//	        Log.i(TAG, "Touched rgba color: (" + mBlobColorRgba.val[0] + ", " + mBlobColorRgba.val[1] +
//	                ", " + mBlobColorRgba.val[2] + ", " + mBlobColorRgba.val[3] + ")");
//
//	        mDetector.setHsvColor(mBlobColorHsv);
	        	        
	        
	        //For each contour found
	        approxCurve = new MatOfPoint2f();
	        for (int i=0; i<contours.size(); i++)
	        {
	            //Convert contours(i) from MatOfPoint to MatOfPoint2f
	            MatOfPoint2f contour2f = new MatOfPoint2f( contours.get(i).toArray() );	            
//	            Log.e("contour2f", contour2f.toString());
	            
	            //Processing on mMOP2f1 which is in type MatOfPoint2f
	            double approxDistance = Imgproc.arcLength(contour2f, true)*0.02;
	            Log.e("approxDistance", String.valueOf(approxDistance));
	            
	            Imgproc.approxPolyDP(contour2f, approxCurve, approxDistance, true);

	            //Convert back to MatOfPoint
	            MatOfPoint points = new MatOfPoint( approxCurve.toArray() );

	            // Get bounding rect of contour
	            Rect rect = Imgproc.boundingRect(points);

	             // draw enclosing rectangle (all same color, but you could use variable i to make them unique)
//	            Core.rectangle(mRgba, new Point(rect.x,rect.y), new Point(rect.x+rect.width,rect.y+rect.height), new Scalar(0, 255, 0, 255), 2); 
	        }
	        
//			List<Moments> mu = new ArrayList<Moments>(contours.size());
//		    for (int i = 0; i < contours.size(); i++) {
//		        mu.add(i, Imgproc.moments(contours.get(i), false));
//		        Moments p = mu.get(i);
//		        int x = (int) (p.get_m10() / p.get_m00());
//		        int y = (int) (p.get_m01() / p.get_m00());
//		        Log.e("sizeRgba", "("+x+", "+y+")");
//		        Core.putText(mRgba, String.valueOf(i+1), new Point(x, y), 1, 1, new Scalar(255, 127, 0, 255), 2);
//		    };

			
//			for(int i=0; i<contours.size(); i++){				
//				int rows = (int) contours.get(i).size().height + 20;
//		        int cols = (int) contours.get(i).size().width + 60;       
//		        Log.e("sizeRgba", "("+rows+", "+cols+")");
//		        Core.putText(mRgba, String.valueOf(i+1), new Point(rows, cols), 1, 1, new Scalar(255, 255, 0, 255), 2);
//			}
			
		}else{
			Core.trace(inputFrame.rgba());
		}

//		Log.e("contours", String.valueOf(contours.size()));
//		Log.e("contours", String.valueOf(contours.get(0)));
		
//		Imgproc.convexHull(points, hull);
		
//		for(int i=0; i<contours.size(); i++){
//			// 目前螢幕解析度
//			int rows = (int) contours.get(i).size().height + 20;
//	        int cols = (int) contours.get(i).size().width + 60;       
//	        Log.e("sizeRgba", "("+rows+", "+cols+")");
//	        Core.putText(mRgba, String.valueOf(contours.size()), new Point(rows, cols), 3, 1, new Scalar(0, 255, 0, 255), 2);
//		}
		
//		// 目前螢幕解析度
//		int rows = (int) contours.get(0).size().height + 20;
//        int cols = (int) contours.get(0).size().width + 60;       
//        Log.e("sizeRgba", "("+rows+", "+cols+")");
//        Core.putText(mRgba, String.valueOf(contours.size()), new Point(rows, cols), 3, 1, new Scalar(0, 255, 0, 255), 2);
		
		
		Core.putText(mRgba, String.valueOf(contours.size()), new Point(10, 30), 3, 1, new Scalar(255, 0, 0, 255), 2);
//		Core.putText(mRgba, String.valueOf(contours.get(0)), new Point(30, 550), 1, 1, new Scalar(0, 255, 0, 255), 1);
		

		return mRgba;
	}
	
    private Scalar converScalarHsv2Rgba(Scalar hsvColor) {
        Mat pointMatRgba = new Mat();
        Mat pointMatHsv = new Mat(1, 1, CvType.CV_8UC3, hsvColor);
        Imgproc.cvtColor(pointMatHsv, pointMatRgba, Imgproc.COLOR_HSV2RGB_FULL, 4);

        return new Scalar(pointMatRgba.get(0, 0));
    }
}

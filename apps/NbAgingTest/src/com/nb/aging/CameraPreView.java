package com.nb.aging;

import android.content.Context;
import android.hardware.Camera;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;

public class CameraPreView extends SurfaceView implements SurfaceHolder.Callback {
	SurfaceHolder mHolder;
	private android.hardware.Camera mCamera = null;
	int mZoom = 0;
	int mViewFinderWidth;
	int mViewFinderHeight;

	int cameraId = 0;

	Context mContext;
       
       private final AutoFocusCallback mAutoFocusCallback = 
        new AutoFocusCallback();
       private static final int FOCUS_NOT_STARTED = 0;
       private static final int FOCUSING = 1;
       private static final int FOCUS_SUCCESS = 2;
       private static final int FOCUS_FAIL = 3;
       protected int mFocusState = FOCUS_NOT_STARTED;
       private ToneGenerator mFocusToneGenerator;
     
	private String TAG = "CameraPreView";

	CameraPreView(Context context) {
		super(context);
		mContext = context;

		mHolder = getHolder();
		mHolder.addCallback(this);
		mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
	}

	CameraPreView(Context context, int scale) {
		this(context);
		mZoom = scale;
	}

	CameraPreView(Context context, int scale, int camera) {
		this(context, scale);
		cameraId = camera;
		Log.e(TAG, "cameraId: "+cameraId);
	}
       
       private final class AutoFocusCallback
       implements android.hardware.Camera.AutoFocusCallback {
               public void onAutoFocus(
                               boolean focused, android.hardware.Camera camera) {

                       if (mFocusState == FOCUSING){
                               ToneGenerator tg = mFocusToneGenerator;

                               if (tg != null) {
                                       tg.startTone(ToneGenerator.TONE_PROP_BEEP2);
                               }

                               if (focused) {
                                       mFocusState = FOCUS_SUCCESS;
                               } else {
                                       mFocusState = FOCUS_FAIL;
                               }
                       }
               }
       }
       
    private void initializeFocusTone() {
        // Initialize focus tone generator.
        try {
            mFocusToneGenerator = new ToneGenerator(
                    AudioManager.STREAM_SYSTEM, 100);
        } catch (Throwable ex) {
            Log.w(TAG, "Exception caught while creating tone generator: ", ex);
            mFocusToneGenerator = null;
        }
    }
   
	public void surfaceCreated(SurfaceHolder holder) {
		// The Surface has been created, acquire the camera and tell it
		// where
		// to draw.
		try {
			if (cameraId == 0) {
				mCamera = Camera.open();
			} else {
				if (cameraId < Camera.getNumberOfCameras()) {
					mCamera = Camera.open(cameraId);
					CameraPreView.setCameraDisplayOrientation(mContext, cameraId,
							mCamera);
				}
			}
		} catch (Exception e) {
			Log.e("MMI Test", "can't open camera ");
			return;
		}
               
                initializeFocusTone();
               
		/*
		 * try { mCamera.setPreviewDisplay(holder); }catch (IOException
		 * exception) { //add a toast when camera open. Toast.makeText(mContext,
		 * "there is an error on open camera.", Toast.LENGTH_LONG).show();
		 * mCamera.release(); mCamera = null; // TODO: add more exception
		 * handling logic here }catch(Exception ex){ Toast.makeText(mContext,
		 * "there is an error on open camera.", Toast.LENGTH_LONG).show(); }
		 */
	}
         
         
          public void doAutoFocus(){
               mFocusState = FOCUSING;
               mCamera.autoFocus(mAutoFocusCallback);
          }
       
          public void doCancelFocus(){
               mCamera.cancelAutoFocus();
          }
         
	public static void setCameraDisplayOrientation(Context context,
			int cameraId, Camera camera) {
		// See android.hardware.Camera.setCameraDisplayOrientation for
		// documentation.
		Camera.CameraInfo info = new Camera.CameraInfo();
		Camera.getCameraInfo(cameraId, info);
		int degrees = getDisplayRotation(context);
		int result;
		if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
			//result = (info.orientation + degrees) % 360;
			result = (info.orientation - degrees + 180) % 360;
		} else {
			result = (info.orientation - degrees + 360) % 360;
		}
		camera.setDisplayOrientation(result);
	}

	public static int getDisplayRotation(Context context) {
		int rotation = ((WindowManager) (context
				.getSystemService(Context.WINDOW_SERVICE))).getDefaultDisplay()
				.getRotation();
		switch (rotation) {
		case Surface.ROTATION_0:
			return 0;
		case Surface.ROTATION_90:
			return 90;
		case Surface.ROTATION_180:
			return 180;
		case Surface.ROTATION_270:
			return 270;
		}
		return 0;
	}

	public void surfaceDestroyed(SurfaceHolder holder) {
		// Surface will be destroyed when we return, so stop the preview.
		// Because the CameraDevice object is not a shared resource,
		// it's very
		// important to release it when the activity is paused.
		if ((mCamera != null) && mCamera.previewEnabled()) {
			try {
				int mCameraEnabled = 0;
				Camera.Parameters mParameters = mCamera.getParameters();
				mParameters.set("camera_enabled",
						String.valueOf(mCameraEnabled));
				mCamera.setParameters(mParameters);
				if (mCamera.previewEnabled())
					mCamera.stopPreview();
				mCamera.release();

			} catch (Exception e) {
				Log.e("MMI Test", "can't stop preview ");
			}
			mCamera = null;
		}
	}

	public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
		// Now that the size is known, set up the camera parameters and
		// begin
		// the preview.
		if (mCamera != null) {
			try {
				mCamera.setPreviewDisplay(holder);
			} catch (Exception ex) {
				Log.e(TAG, "setPreview failed");
			}

			mViewFinderWidth = w;
			mViewFinderHeight = h;
			setCameraParameters();

			try {
				Log.v(TAG, "startPreview");
				mCamera.startPreview();
				if(cameraId == 0) {
				    doAutoFocus();
				}

			} catch (Throwable ex) {
				Log.d(TAG, "startPreview failed", ex);
			}
		}
	}

	public void setZoom(int z) {
		Camera.Parameters mParameters = mCamera.getParameters();
		if (z < 0) {
			z = 0;
		} else if (z > 60) {
			z = 60;
		}
		Log.d(TAG, "set zoom value to " + z);
		mParameters.set("zoom", String.valueOf(z));
		mCamera.setParameters(mParameters);
	}

	private void setCameraParameters() {
		Log.v(TAG, "setCameraParameters");

		final String ANDROID_QUALITY = "jpeg-quality";
		final String THUNDERST_TIMESTAMP = "thunderst_timestamp";
		final String THUNDERST_NIGHTMODE = "thunderst_nightmode";
		final String ANDROID_EFFECT = "effect";
		final String ANDROID_FLICKER_ADJ = "antibanding";
		final String PARM_PICTURE_SIZE = "picture-size";

		final String BRIGHTNESS = "luma-adaptation";
		final String WHITEBALANCE = "whitebalance";

		Camera.Parameters mParameters = mCamera.getParameters();
		DisplayMetrics mDisplayMetrics = mContext.getResources().getDisplayMetrics();
		/*mParameters.setPreviewSize(mViewFinderWidth, mViewFinderHeight);
		Log.e(TAG, "setCameraParameter: mViewFinderWidth: " + mViewFinderWidth
				+ " mViewFinderHeight: " + mViewFinderHeight);

		// if we don't set camera enabled, the HAL won't enable the camera
		// device
		// and we MUST disable camera when we stop camera device
		int mCameraEnabled = 1;
		mParameters.set("camera_enabled", String.valueOf(mCameraEnabled));

		// to prevent auto clockwise rotation of 90 degree
		// mParameters.set("orientation", "portrait");
		// mParameters.set("rotation", 180);
		// SurfaceHolder.getSurface();
		// Surface.setOrientation(Display.DEFAULT_DISPLAY, Surface.ROTATION_90);
		// mParameters.set("orientation", "landscape");
		// mParameters.set("rotation", 90);
		// mParameters.setRotation(90);
		mParameters.set("disp-rotate", 1);

		// Set picture size parameter.
		//String pictureSize = "800x600";
		//String pictureSize = "640x480";
		//mParameters.set(PARM_PICTURE_SIZE, pictureSize);
		DisplayMetrics mDisplayMetrics = mContext.getResources().getDisplayMetrics();
		mParameters.setPreviewSize(mDisplayMetrics.widthPixels, mDisplayMetrics.heightPixels);

		// Set zoom
		int currentZoomValue = 0;
		mParameters.set("zoom", String.valueOf(currentZoomValue));

		// Set resolution
		//mParameters.set(PARM_PICTURE_SIZE, "1600x1200");
		mCamera.setParameters(mParameters);

		// Set whitebalance
		String whiteBalance = "auto";
		mParameters.set(WHITEBALANCE, whiteBalance);
		mCamera.setParameters(mParameters);

		// Set brightness
		int brightness = 4;
		mParameters.set(BRIGHTNESS, brightness);

		// Set the MyCameraSettings' settings to camera device
		
		 * Quality 0:high 1:normal 2:basic
		 
		String camera_quality = "1";
		String quality = "90";
		mParameters.set(ANDROID_QUALITY, quality);

		
		 * Night mode 1:night mode 0:not night mode
		 
		boolean nightmode = false;
		mParameters.set(THUNDERST_NIGHTMODE, nightmode ? "1" : "0");

		
		 * timestamp 1: with timestamp 0: without timestamp
		 
		boolean timestamp = true;
		mParameters.set(THUNDERST_TIMESTAMP, timestamp ? "1" : "0");

		
		 * effect 0: None 1: Grayscale 2: Negative 3: Sepia
		 
		String effect = "0";
		mParameters.set(ANDROID_EFFECT, effect);

		
		 * flicker adjustment 0: auto 1: 50 HZ 2: 60 HZ
		 
		String flicker = "0";
		mParameters.set(ANDROID_FLICKER_ADJ, flicker);
		mParameters.setFocusMode("auto");*/

		//mParameters = getParameters(0);
		Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(cameraId, info);
        if (info.facing != Camera.CameraInfo.CAMERA_FACING_FRONT) {
            mParameters.set("zoom", "0");
            mParameters.set("whitebalance", "auto");
            mParameters.set("afeng-min-focus-step", "0");
            mParameters.set("rotation", "90");
            mParameters.set("jpeg-thumbnail-quality", "100");
            mParameters.set("preview-format", "yuv420p");
            mParameters.set("preview-frame-rate", "120");
            mParameters.set("jpeg-thumbnail-width", "160");
            mParameters.set("preview-fps-range", "5000,60000");
            mParameters.set("auto-whitebalance-lock", "false");
            mParameters.set("max-num-focus-areas", "1");
            mParameters.set("brightness_value", "0");
            mParameters.set("picture-format", "jpeg");
            mParameters.set("afeng-max-focus-step", "1023");
            mParameters.set("zoom-supported", "true");
            mParameters.set("scene-mode", "auto");
            mParameters.set("jpeg-quality", "90");
            mParameters.set("focus-fs-fi", "0");
            mParameters.set("focus-fs-fi-min", "0");
            mParameters.set("brightness", "middle");
            mParameters.set("jpeg-thumbnail-height", "128");
            mParameters.set("smooth-zoom-supported", "true");
            mParameters.set("auto-whitebalance-lock-supported", "true");
            mParameters.set("focus-areas", "(0,0,0,0,0)");
            mParameters.set("max-zoom", "10");
            mParameters.set("focus-distances", "0.95,1.9,Infinity");
            mParameters.set("stereo-image-refocus", "off");
            mParameters.set("focus-fs-fi-max", "65535");
            mParameters.setPictureSize(mDisplayMetrics.widthPixels, mDisplayMetrics.heightPixels);
            mParameters.setPreviewSize(mDisplayMetrics.widthPixels, mDisplayMetrics.heightPixels);
        } else {
            mParameters.set("zoom", "0");
            mParameters.set("whitebalance", "auto");
            mParameters.set("afeng-min-focus-step", "0");
            mParameters.set("rotation", "270");
            mParameters.set("jpeg-thumbnail-quality", "100");
            mParameters.set("preview-format", "yuv420p");
            mParameters.set("preview-frame-rate", "120");
            mParameters.set("jpeg-thumbnail-width", "160");
            mParameters.set("auto-whitebalance-lock", "false");
            mParameters.set("preview-fps-range", "5000,60000");
            mParameters.set("antibanding", "auto");
            mParameters.set("max-num-focus-areas", "1");
            mParameters.set("brightness_value", "0");
            mParameters.set("edge", "middle");
            mParameters.set("picture-format", "jpeg");
            mParameters.set("afeng-max-focus-step", "1023");
            mParameters.set("zoom-supported", "true");
            mParameters.set("scene-mode", "auto");
            mParameters.set("jpeg-quality", "90");
            mParameters.set("focus-fs-fi", "0");
            mParameters.set("focus-fs-fi-min", "0");
            mParameters.set("zoom-ratios", "100,114,132,151,174,200,229,263,303,348,400");
            mParameters.set("brightness", "middle");
            mParameters.set("jpeg-thumbnail-height", "128");
            mParameters.set("auto-whitebalance-lock-supported", "true");
            mParameters.set("focus-areas", "(0,0,0,0,0)");
            mParameters.set("max-zoom", "10");
            mParameters.set("focus-distances", "0.95,1.9,Infinity");
            mParameters.set("stereo-image-refocus", "off");
            mParameters.set("focus-fs-fi-max", "65535");
            mParameters.setPictureSize(mDisplayMetrics.heightPixels, mDisplayMetrics.widthPixels);
            mParameters.setPreviewSize(mDisplayMetrics.heightPixels, mDisplayMetrics.widthPixels);
        }
		try {
			mCamera.setParameters(mParameters);
		} catch (IllegalArgumentException e) {
			Log.e(TAG, "set Parameters error!!!");
			e.printStackTrace();
		}
	}
	
}


package com.droidipc;

import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.util.Log;
import android.view.Menu;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Camera.Size;
import android.view.Display;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

class cFpsCalc
{
	long timeMs;
	int frmCnt;
	int fps;
	
	cFpsCalc()
	{
		timeMs=System.currentTimeMillis();
		frmCnt=0;
		fps=0;
	}
}

public class MainActivityIPC extends Activity
{
	private CamView mCamView;
	private Button buttonTakePic;
	private Button buttonRec;
	private TextView textLog;
	private AutoFocusCB camAFCB;
	
	private SurfaceView viewPalyback;
	SurfaceHolder playbackHld;
	palybackViewCB playViewCB;
	
	final int MSG_TIMER_FPS=1;
	final int FPS_INTVAL=500;
	

	cFpsCalc fpsCalc;

    public native String  stringFromJNI();
    
    static {
        System.loadLibrary("ffmpeg-jni");
    }
    
	/*
	 * private SurfaceHolder camSurfHolder = null; private SurfaceView camSurf =
	 * null; static  private
	 * 
	    android:background="@android:color/transparent"
	 */

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		requestWindowFeature(Window.FEATURE_NO_TITLE);
		super.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);// FLAG_FULLSCREEN
		super.getWindow().addFlags(
				WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		setContentView(R.layout.activity_main_activity_ipc);
		
		textLog = (TextView) findViewById(R.id.textViewLog);
		FrameLayout framePreview = (FrameLayout) findViewById(R.id.frameViewCam);
		
		mCamView = new CamView(this, textLog);
		framePreview.addView(mCamView);

		viewPalyback=(SurfaceView)findViewById(R.id.SurfaceViewPlayback);
		
		playbackHld=viewPalyback.getHolder();
		playViewCB=new palybackViewCB();
		playbackHld.addCallback(playViewCB);

		//textLog.setText(stringFromJNI());
		//textLog.setText("Wid:"+mCamView.prevSize.width+" height:"+mCamView.prevSize.height);
		
		fpsCalc = new cFpsCalc();
		
		final Handler mHandler = new Handler()
		{
			public void handleMessage(Message msg)
			{
				if(msg.what==MSG_TIMER_FPS)
				{
					textLog.setText("FPS:"+fpsCalc.fps);

				}
				super.handleMessage(msg);
			}
		};
		
		TimerTask task = new TimerTask()
		{
			public void run()
			{
				Message message = new Message();
				message.what = MSG_TIMER_FPS;      
				mHandler.sendMessage(message);
				playViewCB.drawBit();
			}
		};
		
		Timer timer = new Timer(true);
		timer.schedule(task,FPS_INTVAL, FPS_INTVAL);
		
		mCamView.mCamera.setPreviewCallback(new Camera.PreviewCallback()
		{
			@Override
			public void onPreviewFrame(byte[] data, Camera cam)
			{
				long curTimems=System.currentTimeMillis();
				fpsCalc.frmCnt++;
				if((curTimems-fpsCalc.timeMs)>FPS_INTVAL)
				{
					fpsCalc.fps=(int) (1000*fpsCalc.frmCnt/(curTimems-fpsCalc.timeMs));
					fpsCalc.frmCnt=0;
					fpsCalc.timeMs=curTimems;
					//textLog.setText("FPS:"+fpsCalc.fps);

					cam.stopPreview();

				}
			}
			
		});
		
		//mCamView.mCamera.

		mCamView.setOnClickListener(new View.OnClickListener()
		{
			public void onClick(View v)
			{
				mCamView.mCamera.autoFocus(null);
			}
		});

		buttonTakePic = (Button) findViewById(R.id.buttonTakePic);
		buttonTakePic.setOnClickListener(new View.OnClickListener()
		{
			public void onClick(View v)
			{
				camAFCB = new AutoFocusCB();
				mCamView.mCamera.autoFocus(camAFCB);
			}
		});

		buttonRec = (Button) findViewById(R.id.buttonRec);
		buttonRec.setOnClickListener(new View.OnClickListener()
		{
			public void onClick(View v)
			{
				mCamView.recOnAction(buttonRec);
			}
		});
		
		Log.i("DroidIPC", "onCreate");

		// Create our Preview view and set it as the content of our activity.
		
		
	}

	public class AutoFocusCB implements AutoFocusCallback
	{
		@Override
		public void onAutoFocus(boolean success, Camera camera)
		{
			// TODO Auto-generated method stub
			if (success)
			{
				PictureCallback mPicture = new CameraPictureCallBack();
				camera.takePicture(null, null, null, mPicture);
			}
		}
	}

	public class CameraPictureCallBack implements PictureCallback
	{

		public void onPictureTaken(byte[] data, Camera camera)
		{
			try
			{
				String timeStamp = new SimpleDateFormat("yyyy_MMdd_HHmmss").format(new Date());
				File file = new File(Environment.getExternalStorageDirectory()
				+ File.separator + "PIC_" + timeStamp + ".jpg");
				
				FileOutputStream fos = new FileOutputStream(file);
				fos.write(data);
				fos.close();
				camera.stopPreview();
				camera.startPreview();
			} catch (Exception e)
			{
				Log.e(INPUT_METHOD_SERVICE, e.toString());
			}
		}
	}
	

	
}

class palybackViewCB implements SurfaceHolder.Callback
{
	SurfaceHolder hld;
	int color=0;
	
	public void drawBit()
	{
		Canvas playbackCvs;
		playbackCvs=hld.lockCanvas();
		playbackCvs.drawColor(Color.rgb(color+=20, 0, 0));
		hld.unlockCanvasAndPost(playbackCvs);
	}
	
	
	
	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height)
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void surfaceCreated(SurfaceHolder playbackHld)
	{
		// TODO Auto-generated method stub
		hld=playbackHld;
		
		Canvas playbackCvs;
		playbackCvs=playbackHld.lockCanvas();
		playbackCvs.drawColor(Color.rgb(255, 0, 0));
		playbackHld.unlockCanvasAndPost(playbackCvs);
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder)
	{
		// TODO Auto-generated method stub
		
	}
	
}
// ----------------------------------------------------------------------

class CamView extends SurfaceView implements SurfaceHolder.Callback
{
	SurfaceHolder mHolder;
	MediaRecorder mMediaRec;
	
	TextView textLog;
	
	Camera mCamera;
	Camera.Size picSize;
	Camera.Size prevSize;

	private File mediaFile;

	CamView(Context context, TextView txtLog)
	{
		super(context);
		
		textLog=txtLog;
		
		mCamera = Camera.open();
		Camera.Parameters parameters = mCamera.getParameters();
		List<Camera.Size> PictureSizes=parameters.getSupportedPictureSizes();
		
		picSize=PictureSizes.get(0);
		for(int i=0; i<PictureSizes.size(); i++)
		{
			
			if(picSize.width<PictureSizes.get(i).width)
			{
				picSize.width=PictureSizes.get(i).width;
				picSize.height=PictureSizes.get(i).height;
			}
			
			Log.i("PictureSizes", "width:"+PictureSizes.get(i).width+" height:"+PictureSizes.get(i).height); 
		}
		
		List<Camera.Size> PreviewSizes=parameters.getSupportedPreviewSizes();
		prevSize=PreviewSizes.get(0);
		for(int i=0; i<PreviewSizes.size(); i++)
		{
			textLog.setText(textLog.getText()+" "+PreviewSizes.get(i).width+"*"+PreviewSizes.get(i).height+" ");
			textLog.getText();
			if(prevSize.width<PreviewSizes.get(i).width)
			{
				prevSize.width=PreviewSizes.get(i).width;
				prevSize.height=PreviewSizes.get(i).height;
			}
			
			Log.i("PreviewSizes", "width:"+PreviewSizes.get(i).width+" height:"+PreviewSizes.get(i).height); 
		}
		//Log.i("PreviewSizes", "width:"+prevSize.width+" height:"+prevSize.height); 
		
		
		parameters.setPictureFormat(PixelFormat.JPEG);
		parameters.setPictureSize(prevSize.width, prevSize.height);//(picSize.width, picSize.height);
		parameters.setPreviewSize(prevSize.width, prevSize.height);
		parameters.setJpegQuality(100);
		//parameters.get
		mCamera.setParameters(parameters);
		
		// Install a SurfaceHolder.Callback so we get notified when the
		// underlying surface is created and destroyed.
		mHolder = this.getHolder();
		mHolder.addCallback(this);
		mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		
	}
	
	

	private void releaseMediaRecorder()
	{
		if (mMediaRec != null)
		{
			mMediaRec.reset(); // clear recorder configuration
			mMediaRec.release(); // release the recorder object
			mMediaRec = null;
			mCamera.lock(); // lock camera for later use
			mCamera.startPreview();
		}
	}

	public boolean prepareRec()
	{
		mMediaRec = new MediaRecorder();
		mCamera.stopPreview();
		mCamera.unlock();

		mMediaRec.setCamera(mCamera);
		mMediaRec.setVideoSource(MediaRecorder.VideoSource.CAMERA);
		mMediaRec.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
		mMediaRec.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
		mMediaRec.setVideoEncoder(MediaRecorder.VideoEncoder.MPEG_4_SP);
		mMediaRec.setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT);
		mMediaRec.setVideoSize(prevSize.width, prevSize.height);
		mMediaRec.setVideoFrameRate(25);

		if (!(Environment.getExternalStorageState()
				.equals(Environment.MEDIA_MOUNTED)))
		{
			return false;
		}

		String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss")
				.format(new Date());
		mediaFile = new File(Environment.getExternalStorageDirectory()
				+ File.separator + "VID_" + timeStamp + ".mp4");
		mMediaRec.setOutputFile(mediaFile.toString());

		mMediaRec.setPreviewDisplay(getHolder().getSurface());

		try
		{
			mMediaRec.prepare();
		}
		catch(IllegalStateException e)
		{
			Log.d(VIEW_LOG_TAG,
					"IllegalStateException preparing MediaRecorder: "
							+ e.getMessage());
			releaseMediaRecorder();
			return false;
		}
		catch(IOException e)
		{
			Log.d(VIEW_LOG_TAG,
					"IOException preparing MediaRecorder: " + e.getMessage());
			releaseMediaRecorder();
			return false;
		}
		return true;

	}

	private boolean isRecording = false;

	public void recOnAction(Button btn)
	{
		if (isRecording)
		{/**/
			// stop recording and release camera
			mMediaRec.stop(); // stop the recording
			releaseMediaRecorder(); // release the MediaRecorder object

			// inform the user that recording has stopped
			btn.setText(R.string.record);
			isRecording = false;
		} else
		{
			// initialize video camera
			if (prepareRec())
			{
				// Camera is available and unlocked, MediaRecorder is prepared,
				// // now you can start recording
				mMediaRec.start();

				// inform the user that recording has started
				btn.setText(R.string.stop);
				isRecording = true;
			} else
			{
				// prepare didn't work, release the camera
				releaseMediaRecorder();
				// inform user
			}
		}
	}

	public void surfaceCreated(SurfaceHolder holder)
	{
		// The Surface has been created, acquire the camera and tell it where
		// to draw.
		// mCamera = Camera.open();
		try
		{
			mCamera.setPreviewDisplay(holder);
			mCamera.startPreview();
		} catch (IOException exception)
		{
			mCamera.release();
			mCamera = null;
			// TODO: add more exception handling logic here
		}
	}

	public void surfaceDestroyed(SurfaceHolder holder)
	{
		// Surface will be destroyed when we return, so stop the preview.
		// Because the CameraDevice object is not a shared resource, it's very
		// important to release it when the activity is paused.
		if (mCamera != null)
		{
			mCamera.setPreviewCallback(null);
			mCamera.stopPreview();
			mCamera.release(); // release the camera for other applications
			mCamera = null;
		}
	}

	public void surfaceChanged(SurfaceHolder holder, int format, int w, int h)
	{
		// Now that the size is known, set up the camera parameters and begin
		// the preview.
		Camera.Parameters parameters = mCamera.getParameters();
		parameters.setPreviewSize(w, h);
		Log.i("DroidIPC", "surfaceChanged format:"+format+" size:"+w+","+h); 
		mCamera.setParameters(parameters);
		mCamera.startPreview();
	}
}
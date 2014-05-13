package com.droidipc;

import android.media.MediaRecorder;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.util.Log;
import android.view.Menu;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
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
import java.io.InputStream;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import com.droidipc.MainActivityIPC.CamPreviewCB;

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
	private TextView tvFps;
	private TextView tvIp;
	/*
	private SurfaceView viewPalyback;
	SurfaceHolder playbackHld;
	PlaybackViewCB playViewCB;
	*/
	final int MSG_TIMER_FPS=1;
	final int FPS_INTVAL=500;
	

	private cFpsCalc fpsCalc;

    //public native String  stringFromJNI();
    
    /*
    static {
        System.loadLibrary("ffmpeg-jni");
    }
    */

    private static final int DLG_CAM_ERROR = 1;
    private static final int DLG_NO_STORAGE = 2;
    
    protected Dialog onCreateDialog(int dlgID) 
    {
        if(dlgID==DLG_CAM_ERROR)
        {
        	return new AlertDialog.Builder(this)
            .setTitle("相机故障 ")
            .setMessage("无法打开摄像头!点击\"确定\"退出程序...")
            .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                	MainActivityIPC.this.finish();
                }
            })
            .create();
        }
        else if(dlgID==DLG_NO_STORAGE)
        {
        	return new AlertDialog.Builder(this)
            .setTitle("存储器故障 ")
            .setMessage("SD卡不存在或未挂载")
            .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                	dialog.cancel();
                }
            })
            .create();
        }
        
        return null;
    }

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		Log.i("DroidIPC", "onCreate");

		requestWindowFeature(Window.FEATURE_NO_TITLE);
		super.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);// FLAG_FULLSCREEN
		super.getWindow().addFlags(
				WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		setContentView(R.layout.activity_main_activity_ipc);
		
		tvFps = (TextView)findViewById(R.id.tvFps);
		tvIp=(TextView)findViewById(R.id.tvIp);
		tvIp.setText(getLocalIp()+"   "+getWifiMac()); 
		
		initCam();
		
		initServer();
	}
	
	private void initServer()
	{
		File webDir=getWebDir();
		File index=getWebFile("index.html");
		if(index!=null)
		{
			try
			{
				HttpServer server=new HttpServer(8080, webDir);
			} catch (IOException e)
			{
				e.printStackTrace();
			}
		}
	}
	
	private void initCam()
	{

		FrameLayout framePreview = (FrameLayout) findViewById(R.id.frameViewCam);
		
		mCamView = new CamView(this, new CamPreviewCB());
		if(mCamView.mCamera==null)
		{
			showDialog(DLG_CAM_ERROR);
		}
		else
		{
			framePreview.addView(mCamView);

			/*
			viewPalyback=(SurfaceView)findViewById(R.id.SurfaceViewPlayback);
			playbackHld=viewPalyback.getHolder();
			
			viewPalyback.setZOrderOnTop(true);
			playbackHld.setFormat(PixelFormat.TRANSPARENT);
			
			playViewCB=new PlaybackViewCB(viewPalyback);
			playbackHld.addCallback(playViewCB);
			*/

			
			//tvFps.setText(stringFromJNI());
			//tvFps.setText("Wid:"+mCamView.prevSize.width+" height:"+mCamView.prevSize.height);
			
			fpsCalc = new cFpsCalc();
			
			final Handler mHandler = new Handler()
			{
				public void handleMessage(Message msg)
				{
					if(msg.what==MSG_TIMER_FPS)
					{
						tvFps.setText("FPS:"+fpsCalc.fps);

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
				}
			};
			
			Timer timer = new Timer(true);
			timer.schedule(task,FPS_INTVAL, FPS_INTVAL);
			
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
					if(getAppDir()!=null&&!mCamView.isRecording)
					{
						mCamView.mCamera.autoFocus(new TakePicAfterFocus());
					}
				}
			});

			buttonRec = (Button) findViewById(R.id.buttonRec);
			buttonRec.setOnClickListener(new View.OnClickListener()
			{
				public void onClick(View v)
				{
					mCamView.recOnAction(buttonRec, getAppDir());
				}
			});
			
		}

	}
	
	public class CamPreviewCB implements Camera.PreviewCallback
	{

		@Override
		public void onPreviewFrame(byte[] data, Camera camera)
		{
			// TODO Auto-generated method stub

			long curTimems=System.currentTimeMillis();
			fpsCalc.frmCnt++;
			if((curTimems-fpsCalc.timeMs)>FPS_INTVAL)
			{
				fpsCalc.fps=(int) (1000*fpsCalc.frmCnt/(curTimems-fpsCalc.timeMs));
				fpsCalc.frmCnt=0;
				fpsCalc.timeMs=curTimems;
				//tvFps.setText("FPS:"+fpsCalc.fps);

			}
			//playViewCB.drawBit(data, mCamView.mCamera);
		}
		
	}
	
	public class TakePicAfterFocus implements AutoFocusCallback
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
				File appDir=getAppDir();
				if(appDir!=null)
				{
					String timeStamp = new SimpleDateFormat("yyyy_MMdd_HHmmss").format(new Date());
					File pic = new File(appDir, "PIC_" + timeStamp + ".jpg");
					Log.i("onPictureTaken", pic.toString());
					FileOutputStream fos = new FileOutputStream(pic);
					fos.write(data);
					fos.close();
				}
				else
				{
					Log.w("onPictureTaken","File Fail");
				}
				camera.stopPreview();

				camera.setPreviewCallback(mCamView.previewCallBack);
				camera.startPreview();
				
			} catch (Exception e)
			{
				Log.e("PictureCallback", e.toString());
			}
		}
	}
	
	public File getAppDir()
	{
		File dir=null;
		
		//外部存储器存在
		if(Environment.getExternalStorageState()
		.equals(android.os.Environment.MEDIA_MOUNTED))
		{
			dir=new File(Environment.getExternalStorageDirectory() +
								File.separator + getString(R.string.app_name));
			if(!dir.exists())
			{
				dir.mkdir();
			}
			Log.i("getAppDir", "Dir:"+dir.toString());
		}
		else
		{
			showDialog(DLG_NO_STORAGE);
		}
		
		return dir;
	}
	
	public File getWebDir()
	{
		File dir=null;
		
		File appDir=getAppDir();
		if(appDir!=null)
		{
			dir=new File(appDir, "WebFile");
			if(!dir.exists())
			{
				dir.mkdir();
			}
		}
		return dir;
	}
	
	public File getWebFile(String file)
	{
		File webFile=null; 
		File webDir=getWebDir();
		String logTag="HtmlToSD";
		if(webDir!=null)
		{
			webFile=new File(webDir, file);
			//if(!webFile.exists())
			{
				try
				{
					InputStream rawIn = getResources().openRawResource(R.raw.index);
					FileOutputStream fos = new FileOutputStream(webFile);
					
			        int     length;
			        byte[] buffer = new byte[1024*32];
			        while((length = rawIn.read(buffer)) != -1)
			        {
			            fos.write(buffer,0,length);
			        }
			        rawIn.close();
			        fos.close();
				}
				catch(IOException ioe)
				{
					Log.e(logTag, ioe.toString());
				}

			}
		}
		Log.i(logTag, webFile.toString());
		return webFile;
	}
	
	public String getLocalIp()
	{
	    try
	    {
	        for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces();
	        		en.hasMoreElements();)
	        {
	            NetworkInterface intf = en.nextElement();
	            for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); 
	            		enumIpAddr.hasMoreElements();)
	            {
	                InetAddress inetAddress = enumIpAddr.nextElement();
	                if (!inetAddress.isLoopbackAddress() && !inetAddress.isLinkLocalAddress())
	                {
	                    return inetAddress.getHostAddress().toString();
	                }
	            }
	        }
	    } catch (SocketException ex) {
	        Log.e("getLocalIpAddress", ex.toString());
	    }
	    return null;
	}
	
    public String getWifiMac() { 
    	try
    	{
	        WifiManager wifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);    
	        WifiInfo info = wifi.getConnectionInfo();    
	        return info.getMacAddress().toUpperCase();    
    	}
    	catch(Exception ex)
    	{
    		Log.e("getWifiMac", ex.toString());
    	}
    	return null;
    }    
	
}

	
// ----------------------------------------------------------------------

class CamView extends SurfaceView implements SurfaceHolder.Callback
{
	SurfaceHolder mHolder;
	MediaRecorder mMediaRec;
	
	CamPreviewCB previewCallBack;
	
	Camera mCamera;
	Camera.Size picSize;
	Camera.Size prevSize;


	CamView(Context context, CamPreviewCB camPreviewCB)
	{
		super(context);
		
		previewCallBack=camPreviewCB;
		
		mCamera = Camera.open(0);
		if(mCamera!=null)
		{
			Camera.Parameters parameters = mCamera.getParameters();
			
			//查找最大预览尺寸
			List<Camera.Size> PreviewSizes=parameters.getSupportedPreviewSizes();
			prevSize=PreviewSizes.get(0);
			for(int i=0; i<PreviewSizes.size(); i++)
			{
				if(prevSize.width<PreviewSizes.get(i).width)
				{
					prevSize.width=PreviewSizes.get(i).width;
					prevSize.height=PreviewSizes.get(i).height;
				}
				
				Log.i("PreviewSizes", "width:"+PreviewSizes.get(i).width+" height:"+PreviewSizes.get(i).height); 
			}
			//Log.i("PreviewSizes", "width:"+prevSize.width+" height:"+prevSize.height); 
			
			//查找与最大预览尺寸最接近的照片尺寸
			List<Camera.Size> PictureSizes=parameters.getSupportedPictureSizes();
			picSize=PictureSizes.get(0);
			int widDiff=Math.abs(picSize.width-prevSize.width);
			for(int i=0; i<PictureSizes.size(); i++)
			{
				int curDiff=Math.abs(PictureSizes.get(i).width-prevSize.width);
				if(widDiff>curDiff)
				{
					picSize.width=PictureSizes.get(i).width;
					picSize.height=PictureSizes.get(i).height;
					widDiff=curDiff;
				}
				
				Log.i("PictureSizes", "width:"+PictureSizes.get(i).width+" height:"+PictureSizes.get(i).height); 
			}
			Log.i("PictureSizes Used", "width:"+picSize.width+" height:"+picSize.height); 
			
			
			parameters.setPictureFormat(PixelFormat.JPEG);
			parameters.setPictureSize(picSize.width, picSize.height);//(picSize.width, picSize.height);
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
	}
	

	private void releaseMediaRecorder()
	{
		if (mMediaRec != null)
		{
			mMediaRec.reset(); // clear recorder configuration
			mMediaRec.release(); // release the recorder object
			mMediaRec = null;
			mCamera.lock(); // lock camera for later use

			mCamera.setPreviewCallback(previewCallBack);
			mCamera.startPreview();
		}
	}

	public boolean prepareRec(File vidFile)
	{
		if(vidFile==null)
		{
			return false;
		}
		
		mMediaRec = new MediaRecorder();
		mCamera.stopPreview();
		mCamera.unlock();

		mMediaRec.setCamera(mCamera);
		mMediaRec.setVideoSource(MediaRecorder.VideoSource.CAMERA);
		mMediaRec.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
		mMediaRec.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
		mMediaRec.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
		mMediaRec.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
		mMediaRec.setVideoSize(prevSize.width, prevSize.height);
		mMediaRec.setVideoFrameRate(25);

		mMediaRec.setOutputFile(vidFile.toString());

		mMediaRec.setPreviewDisplay(getHolder().getSurface());

		try
		{
			mMediaRec.prepare();
		}
		catch(IllegalStateException e)
		{
			Log.d("prepareRec",
				  "IllegalStateException preparing MediaRecorder: "
							+ e.getMessage());
			releaseMediaRecorder();
			return false;
		}
		catch(IOException e)
		{
			Log.d("prepareRec",
					"IOException preparing MediaRecorder: " + e.getMessage());
			releaseMediaRecorder();
			return false;
		}
		return true;

	}

	public boolean isRecording = false;
	
	public boolean startRec(File vidFile)
	{
		// initialize video camera
		if (prepareRec(vidFile))
		{
			mMediaRec.start();
			return true;
		}else
		{
			// prepare didn't work, release the camera
			releaseMediaRecorder();
			return false;
		}
	}
	
	public void stopRec()
	{
		// stop recording and release camera
		mMediaRec.stop(); // stop the recording
		releaseMediaRecorder(); // release the MediaRecorder object
	}

	public void recOnAction(Button btn, File vidFile)
	{
		if (isRecording)
		{/**/
			// stop recording and release camera
			mMediaRec.stop(); // stop the recording
			releaseMediaRecorder(); // release the MediaRecorder object

			// inform the user that recording has stopped
			btn.setText(R.string.record);
			isRecording = false;
		}
		else if(vidFile!=null)
		{
			
			// initialize video camera
			if (prepareRec(vidFile))
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
		if(mCamera != null)
		{
			try
			{
				mCamera.setPreviewDisplay(holder);
			} catch (IOException exception)
			{
				mCamera.release();
				mCamera = null;
				// TODO: add more exception handling logic here
			}
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
		
		if(mCamera!=null)
		{
			mCamera.setPreviewCallback(previewCallBack);
			Camera.Parameters parameters = mCamera.getParameters();
			parameters.setPreviewSize(w, h);
			Log.i("DroidIPC", "surfaceChanged format:"+format+" size:"+w+","+h); 
			mCamera.setParameters(parameters);
			mCamera.startPreview();
		}
		
	}
}
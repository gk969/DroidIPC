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
import android.view.ViewGroup.LayoutParams;
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
	private boolean isRecording;
	
	
	private Button buttonTakePic;
	private Button buttonRec;
	private TextView tvFps;
	private TextView tvIp;
	
	Timer timCam;
	
	private enum MSG{TIMER_FPS};
	
	final int FPS_INTVAL=500;
	final int HTTP_PORT=8080;
	
	hldMsg mMsgHld;

	HttpServer httpSvr;
	
	private cFpsCalc fpsCalc;

    private enum DLG{CAM_ERROR, NO_STORAGE, WIFI_LOST, STORAGE_ERROR};
    
    public Dialog sysFaultAlert(String title, String desc, final boolean exit)
    {
    	return new AlertDialog.Builder(this)
        .setTitle("哎呀 "+title+"粗问题了")
        .setMessage(desc+"!"+(exit?",点击\"确定\"退出程序...":""))
        .setPositiveButton("确定", new DialogInterface.OnClickListener(){
            public void onClick(DialogInterface dialog, int whichButton){
            	if(exit){
            		MainActivityIPC.this.finish();
            	}
            }
        })
        .create();
    }
    
    @Override
    protected Dialog onCreateDialog(int dlgId) 
    {
        DLG dlg=DLG.values()[dlgId];
    	switch(dlg)
        {
        	case CAM_ERROR:
        	{
        		return sysFaultAlert("相机", "无法打开摄像头", true);
        	}
        	case NO_STORAGE:
        	{
        		return sysFaultAlert("存储器", "SD卡不存在或未挂载", true);
        	}
        	case WIFI_LOST:
        	{
        		return sysFaultAlert("WIFI", "WIFI未连接", true);
        	}
        	case STORAGE_ERROR:
        	{
        		return sysFaultAlert("存储器", "SD卡文件系统出错或或未连接", true);
        	}
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

		NetIf wifi=getWifi();
		if(wifi==null)
		{
			showDialog(DLG.WIFI_LOST.ordinal());
		}
		else
		{
			tvIp.setText(wifi.aip+"   "+wifi.amac); 
			
			initCam();
			
			initServer();
		}
	}
	
	protected void onDestroy()
	{
		super.onDestroy();
		Log.i("onDestroy", "onDestroy");
		if(timCam!=null)
		{
			timCam.cancel();
		}
		
		if(httpSvr!=null)
		{
			httpSvr.close();
		}
	}
	
	private void initServer()
	{
		File webDir=getWebDir();
		if(webFileToSD())
		{
			try
			{
				httpSvr=new HttpServer(HTTP_PORT, webDir);
			} catch (IOException e)
			{
				e.printStackTrace();
			}
		}
	}
	
	private class hldMsg extends Handler
	{
		@Override
		public void handleMessage(Message msg)
		{
			MSG msgId=MSG.values()[msg.what];
			switch(msgId)
			{
				case TIMER_FPS:
				{
					tvFps.setText("FPS:"+fpsCalc.fps);
					break;
				}
				
				default:
					break;
			}
			super.handleMessage(msg);
		}
		
		public void sendMsg(final int msg)
		{
			Message CMsg = new Message();
			CMsg.what = msg;
			this.sendMessage(CMsg);
		}
	}
	
	
	private void initCam()
	{
		FrameLayout framePreview = (FrameLayout) findViewById(R.id.frameViewCam);
		
		mCamView = new CamView(this, new CamPreviewCB());
		if(mCamView.mCamera==null)
		{
			showDialog(DLG.CAM_ERROR.ordinal());
		}
		else
		{
			Display display =getWindowManager().getDefaultDisplay();
			
			Log.i("initCam", "screen:"+display.getWidth()+" "+display.getHeight());
			Log.i("initCam", "mCamView:"+mCamView.ipcSize.width+" "+mCamView.ipcSize.height);
			
			mCamView.mHolder.setFixedSize(mCamView.ipcSize.width*display.getHeight()/
										mCamView.ipcSize.height, display.getHeight());
			framePreview.addView(mCamView);
			
			fpsCalc = new cFpsCalc();
			
			mMsgHld = new hldMsg();
			
			timCam = new Timer(true);
			timCam.schedule(new TimerTask()
			{
				@Override
				public void run()
				{
					mMsgHld.sendMsg(MSG.TIMER_FPS.ordinal());
				}
			},FPS_INTVAL, FPS_INTVAL);
			
			
			
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
					if(getAppDir()!=null&&!isRecording)
					{
						mCamView.mCamera.autoFocus(new TakePicAfterFocus());
					}
				}
			});

			isRecording=false;
			buttonRec = (Button) findViewById(R.id.buttonRec);
			buttonRec.setOnClickListener(new View.OnClickListener()
			{
				public void onClick(View v)
				{
					if (isRecording)
					{
						mCamView.stopRec();
						buttonRec.setText(R.string.record);
						isRecording = false;
					}
					else
					{
						File appDir;
						appDir=getAppDir();
						if(appDir!=null)
						{
							String timeStamp = new SimpleDateFormat("yyyy_MMdd_HHmmss").format(new Date());
							File vidFile = new File(appDir, "VID_" + timeStamp + ".MP4");
							// initialize video camera
							if (mCamView.startRec(vidFile))
							{
								buttonRec.setText(R.string.stop);
								isRecording = true;
							}
						}
					}
				}
			});
			
		}

	}
	
	public class CamPreviewCB implements Camera.PreviewCallback
	{

		@Override
		public void onPreviewFrame(byte[] data, Camera camera)
		{
			long curTimems=System.currentTimeMillis();
			fpsCalc.frmCnt++;
			if((curTimems-fpsCalc.timeMs)>FPS_INTVAL)
			{
				fpsCalc.fps=(int) (1000*fpsCalc.frmCnt/(curTimems-fpsCalc.timeMs));
				fpsCalc.frmCnt=0;
				fpsCalc.timeMs=curTimems;
			}
			
			if(httpSvr!=null)
			{
				httpSvr.synImg.setImgData(data, mCamView.ipcSize.width, 
						mCamView.ipcSize.height);
			}
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
				Log.i("getAppDir", "Dir:"+dir.toString()+" Not Exist!");
				dir.mkdir();
				if(!dir.exists())
				{
					showDialog(DLG.STORAGE_ERROR.ordinal());
				}
			}
			else
			{
				Log.i("getAppDir", "Dir:"+dir.toString()+" Already Exist!");
			}
		}
		else
		{
			showDialog(DLG.NO_STORAGE.ordinal());
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
	
	public boolean webFileToSD()
	{
		File webFile=null; 
		File webDir=getWebDir();
		
		if(webDir==null)
		{
			return false;
		}

		int[] webFileRawId={R.raw.index, R.raw.proc};
		String[] webFileName={"index.html", "proc.js"};
		final int webFileNum=webFileRawId.length;
		if(webFileNum!=webFileName.length)
		{
			return false;
		}
		
		for(int i=0; i<webFileNum; i++)
		{
			webFile=new File(webDir, webFileName[i]);
			Log.i("webFileToSD", webFile.toString());
			try
			{
				InputStream rawIn = getResources().openRawResource(webFileRawId[i]);
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
				Log.e("webFileToSD", ioe.toString());
				return false;
			}
		}
		return true;
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
	
	public class NetIf
	{
		String aip;
		int nip;
		String amac;

	    public String ip_ntoa(int ipInt) 
	    {
	    	if(ipInt!=0)
	    	{
	    		return new StringBuilder().append((ipInt & 0xff)).append('.')
	                .append((ipInt >> 8) & 0xff).append('.').append(
	                        (ipInt >> 16) & 0xff).append('.').append((ipInt >> 24) & 0xff)
	                .toString();
	    	}
	    	return null;
	    }
	    
	    public boolean setIp(int ip)
	    {
	    	if(ip==0)
	    	{
	    		return false;
	    	}
	    	
	    	nip=ip;
	    	aip=ip_ntoa(ip);
	    	return true;
	    }
	}
	
    public NetIf getWifi() 
    {
    	NetIf wifi=new NetIf();
    	try
    	{
	        WifiManager mWifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);    
	        WifiInfo info = mWifi.getConnectionInfo();
	        int ip=info.getIpAddress();
	        if(ip==0)
	        {
	        	return null;
	        }
	        
	        wifi.setIp(ip);
	        Log.i("getWifiIP", wifi.aip);
	        wifi.amac=info.getMacAddress().toUpperCase();
	        return wifi;
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
	final static int IPC_WIDTH=640;
	Camera.Size ipcSize;


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
			
			//查找与宽度IPC_WIDTH相近的预览尺寸，作为IPC传输的默认尺寸。
			ipcSize=PreviewSizes.get(0);
			widDiff=Math.abs(picSize.width-IPC_WIDTH);
			for(int i=0; i<PreviewSizes.size(); i++)
			{
				int curDiff=Math.abs(PreviewSizes.get(i).width-IPC_WIDTH);
				if(widDiff>curDiff)
				{
					ipcSize.width=PreviewSizes.get(i).width;
					ipcSize.height=PreviewSizes.get(i).height;
					widDiff=curDiff;
				}
			}
			Log.i("IPC sizes Used", "width:"+ipcSize.width+" height:"+ipcSize.height); 
			
			
			parameters.setPictureFormat(PixelFormat.JPEG);
			parameters.setPictureSize(picSize.width, picSize.height);//(picSize.width, picSize.height);
			parameters.setPreviewSize(ipcSize.width, ipcSize.height);
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

		mMediaRec.setPreviewDisplay(mHolder.getSurface());

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
	
	public boolean startRec(File vidFile)
	{
		// initialize video camera
		Log.i("startRec","startRec");
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
		if(mMediaRec!=null)
		{
			// stop recording and release camera
			mMediaRec.stop(); // stop the recording
			releaseMediaRecorder(); // release the MediaRecorder object
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
		stopRec();
		
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
			/*
			Camera.Parameters parameters = mCamera.getParameters();
			parameters.setPreviewSize(w, h);
			Log.i("DroidIPC", "surfaceChanged format:"+format+" size:"+w+","+h); 
			mCamera.setParameters(parameters);
			*/
			mCamera.startPreview();
		}
		
	}
}
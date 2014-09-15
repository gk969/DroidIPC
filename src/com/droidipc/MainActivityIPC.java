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
import android.text.Editable;
import android.util.Log;
import android.view.Menu;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.graphics.drawable.BitmapDrawable;
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

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import org.json.JSONException;
import org.json.JSONObject;

import com.droidipc.MainActivityIPC.CamPreviewCB;

class cFpsCalc
{
	long timeMs;
	int frmCnt;
	int fps;

	cFpsCalc()
	{
		timeMs = System.currentTimeMillis();
		frmCnt = 0;
		fps = 0;
	}
}

public class MainActivityIPC extends Activity
{
	private static final String LOG_TAG = "MainActivityIPC";
	
	private CamView mCamView;
	private boolean isRecording;

	private Button buttonTakePic;
	private Button buttonRec;
	private Button btLogin;
	private Button btSwitchCam;
	private Button btSlowExpose;
	private Button btExposeTime;
	
	
	private TextView tvFps;
	private TextView tvIp;
	private TextView tvLoginMsg;
	private TextView tvExposeTim;

	Timer timCam;

	static final private int GET_CODE = 0;

	
	public static final int MSG_TIMER_FPS = 0;
	public static final int MSG_HTTP_LOGIN_SUCCESS = 1;
	public static final int MSG_HTTP_LOGIN_LINK_FAIL = 2;
	public static final int MSG_HTTP_LOGIN_AUTH_FAIL = 3;
	public static final int MSG_EXPOSE_TIM = 4;

	final int FPS_INTVAL = 500;
	final int EXPOSE_TIM_INTVAL=1000;
	
	final int HTTP_PORT = 9693;

	
	private int slowExposeState;

	private final int SE_IDLE=0;
	private final int SE_EXPOSING=1;
	private final int SE_DISPLAY=2;
	
	private ExposeTimRunnable exposeTimRunnable;
	private int slowExposeTim;
	private int[] slowExposeBuf;
	private int slowExposeFrameCnt;
	
	
	private hldMsg mMsgHld;

	HttpServer httpSvr;

	public static HttpDdnsClient httpDdnsClient;

	private cFpsCalc fpsCalc;

	private enum DLG
	{
		CAM_ERROR, NO_STORAGE, WIFI_LOST, STORAGE_ERROR, EXPOSE_TIME_SEL
	};

	private final CharSequence exposeTimeList[]={"2��", "4��", "8��", "16��", "32��", "64��", "128��", "256��"};
	
	SharedPreferences spMain;
	
	
	public Dialog sysFaultAlert(String title, String desc, final boolean exit)
	{
		return new AlertDialog.Builder(this).setTitle("��ѽ " + title + "��������")
				.setMessage(desc + "!" + (exit ? ",���\"ȷ��\"�˳�����..." : ""))
				.setPositiveButton(R.string.alert_dialog_ok, new DialogInterface.OnClickListener()
				{
					public void onClick(DialogInterface dialog, int whichButton)
					{
						if (exit)
						{
							MainActivityIPC.this.finish();
						}
					}
				}).create();
	}

	@Override
	protected Dialog onCreateDialog(int dlgId)
	{
		DLG dlg = DLG.values()[dlgId];
		switch (dlg)
		{
			case CAM_ERROR:
			{
				return sysFaultAlert("���", "�޷�������ͷ", true);
			}
			case NO_STORAGE:
			{
				return sysFaultAlert("�洢��", "SD�������ڻ�δ����", true);
			}
			case WIFI_LOST:
			{
				return sysFaultAlert("WIFI", "WIFIδ����", true);
			}
			case STORAGE_ERROR:
			{
				return sysFaultAlert("�洢��", "SD���ļ�ϵͳ������δ����", true);
			}
			
			case EXPOSE_TIME_SEL:
			{
	            return new AlertDialog.Builder(this)
	            .setTitle("ѡ���ع�ʱ��")
	            .setItems(exposeTimeList, new DialogInterface.OnClickListener() {
	                public void onClick(DialogInterface dialog, int whichButton) {
	
	                    /* User clicked on a radio button do some stuff */
	        			Log.i(LOG_TAG, "whichButton:"+whichButton);
	        			
	            		Editor editor = spMain.edit();
	                    editor.putInt("exposeTime", whichButton);
	                    editor.commit();
	                    
	                    setExposeTimDisp();
	                }
	            })
	           .create();
			}
	
		}
		return null;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		Log.i(LOG_TAG, "onCreate");

		super.getWindow().addFlags(
				WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		setContentView(R.layout.activity_main_activity_ipc);

		tvFps = (TextView) findViewById(R.id.tvFps);

		NetIf wifi = getWifi();
		if (wifi == null)
		{
			showDialog(DLG.WIFI_LOST.ordinal());
			return;
		}

		tvIp = (TextView) findViewById(R.id.tvIp);
		tvIp.setText(wifi.ipStr + ":" + HTTP_PORT + "   " + wifi.macStr);

		tvLoginMsg = (TextView) findViewById(R.id.textViewMainLoginMsg);

		tvExposeTim=(TextView)findViewById(R.id.textViewExposeTim);
		
		if(camInit())
		{
			fpsCalc = new cFpsCalc();

			mMsgHld = new hldMsg();

			timCam = new Timer(true);
			timCam.schedule(new TimerTask()
			{
				@Override
				public void run()
				{
					Message mainMsg = new Message();
					mainMsg.what = MSG_TIMER_FPS;
					mMsgHld.sendMessage(mainMsg);
				}
			}, FPS_INTVAL, FPS_INTVAL);
			
			viewListenerInit();
			
			httpServerInit();
	
			httpDdnsClient = new HttpDdnsClient();
			
			spMain=getSharedPreferences("spMain", 0);
			setExposeTimDisp();
			slowExposeState=SE_IDLE;
			exposeTimRunnable=new ExposeTimRunnable();
			new Thread(exposeTimRunnable).start();
		}
	}

	private class ExposeTimRunnable extends Thread
	{
		public void run()
		{
			while(slowExposeState==SE_EXPOSING)
			{
				Message mainMsg = new Message();
				mainMsg.what = MSG_EXPOSE_TIM;
				mMsgHld.sendMessage(mainMsg);
				
				try
				{
					sleep(EXPOSE_TIM_INTVAL);
				} catch (InterruptedException e)
				{
					e.printStackTrace();
				}
			}
		}
	}
	
	private void setExposeTimDisp()
	{
		btSlowExpose.setText((2<<spMain.getInt("exposeTime", 0))+"s "+getString(R.string.slowExpose));
	}
	
	private synchronized byte[] getOrAddNv21Buf(byte[] srcData, boolean isRead)
	{
		if(isRead)
		{
			byte[] nv21=new byte[slowExposeBuf.length];
			for(int i=0; i<nv21.length; i++)
			{
				nv21[i]=(byte) (slowExposeBuf[i]/slowExposeFrameCnt);
			}
			return nv21;
		}
		

		if(slowExposeFrameCnt<2)
		{
			for(int i=0; i<srcData.length; i++)
			{
				slowExposeBuf[i]+=srcData[i];
			}
			slowExposeFrameCnt++;
		}
		
		return null;
	}
	
	private Bitmap slowExposeConvert()
	{

		Log.i(LOG_TAG, "slowExposeFrameCnt:"+slowExposeFrameCnt);
		
		byte[] nv21=getOrAddNv21Buf(null, true);
		
		ByteArrayOutputStream jpgStream=new ByteArrayOutputStream();
		
		YuvImage yuv=new YuvImage(nv21, ImageFormat.NV21, 
				mCamView.prevSize.width, mCamView.prevSize.height, null);
		try
		{
			jpgStream.reset();
			long tim=System.currentTimeMillis();
			yuv.compressToJpeg(new Rect(0, 0, mCamView.prevSize.width, mCamView.prevSize.height),
                    100, jpgStream);
			
			tim=System.currentTimeMillis()-tim;
			Log.i(LOG_TAG, "compressToJpeg time:"+tim+"ms size:"+
					jpgStream.size()+" "+(jpgStream.size()/1024)+"KB");
		}catch(Exception e)
		{
			e.printStackTrace();
		}
		
		File appDir = getAppDir();
		byte[] jpgArray=jpgStream.toByteArray();
		if (appDir != null)
		{
			try
			{
				String timeStamp = new SimpleDateFormat("yyyy_MMdd_HHmmss")
						.format(new Date());
				File pic = new File(appDir, "SE_ " + timeStamp + ".jpg");
				Log.i(LOG_TAG, pic.toString());
				FileOutputStream fos = new FileOutputStream(pic);
				fos.write(jpgArray);
				fos.close();
				
			} catch(IOException e)
			{
				e.printStackTrace();
			}
		}
		
		InputStream is=new ByteArrayInputStream(jpgArray);
		return BitmapFactory.decodeStream(is);
	}
	
	private void stopSlowExpose()
	{
		
		Bitmap bmp=slowExposeConvert();
		if(bmp!=null)
		{
			slowExposeState=SE_DISPLAY;
			
			mCamView.closeCamera();
			
			SurfaceHolder hld=mCamView.getHolder();
			Canvas playbackCvs=hld.lockCanvas();
			playbackCvs.drawBitmap(bmp, 0, 0, null);
			hld.unlockCanvasAndPost(playbackCvs);
		}
		else
		{
			slowExposeState=SE_IDLE;
		}
			
		setExposeTimDisp();
		tvExposeTim.setText("");
		btExposeTime.setEnabled(true);
		btSwitchCam.setEnabled(true);
	}
	
	protected void onStart()
	{
		super.onStart();
		Log.i(LOG_TAG, "onStart");
	}

	protected void onResume()
	{
		super.onResume();
		Log.i(LOG_TAG, "onResume");

		if(httpDdnsClient!=null)
		{
			httpDdnsClient.setMsgHandler(mMsgHld);
		}
	}

	protected void onPause()
	{
		super.onPause();
		Log.i(LOG_TAG, "onPause");

	}

	protected void onStop()
	{
		super.onStop();
		Log.i(LOG_TAG, "onStop");
	}

	protected void onDestroy()
	{
		super.onDestroy();
		Log.i(LOG_TAG, "onDestroy");
		if (timCam != null)
		{
			timCam.cancel();
		}

		if (httpSvr != null)
		{
			httpSvr.close();
		}

		if (httpDdnsClient != null)
		{
			httpDdnsClient.stop();
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{

		if (requestCode == GET_CODE)
		{
			if (resultCode == RESULT_CANCELED)
			{
				Log.i(LOG_TAG, "cancelled!");
				tvLoginMsg.setText("");
			} else
			{
				if (data != null)
				{
					String retStr = data.getAction();
					Log.i(LOG_TAG, retStr);
					tvLoginMsg.setText(retStr);
				}
			}
		}
	}

	private void httpServerInit()
	{
		File webDir = getWebDir();
		try
		{
			httpSvr = new HttpServer(this, HTTP_PORT, webDir);
		} catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	private class hldMsg extends Handler
	{
		@Override
		public void handleMessage(Message msg)
		{
			switch (msg.what)
			{
			case MSG_TIMER_FPS:
			{
				tvFps.setText("FPS:" + fpsCalc.fps);
				break;
			}

			case MSG_HTTP_LOGIN_SUCCESS:
			{
				String jsonStr = (String) (msg.obj);
				Log.i(LOG_TAG, "httpRecv:" + jsonStr);

				JSONObject object;
				try
				{
					object = new JSONObject(jsonStr);
					String ipcAddr = object.getString("ipc_addr");
					tvLoginMsg.setText("��¼�ɹ�������IP��" + ipcAddr + "�Ѵ����������");

				} catch (JSONException e)
				{
					e.printStackTrace();
				}

				break;
			}

			case MSG_HTTP_LOGIN_AUTH_FAIL:
			{
				Log.i(LOG_TAG, "Server Login Auth Fail!");
				tvLoginMsg.setText("��¼ʧ�ܣ��û������������");
				break;
			}

			case MSG_HTTP_LOGIN_LINK_FAIL:
			{
				Log.i(LOG_TAG, "Server Login Link Fail!");
				tvLoginMsg.setText("��¼ʧ�ܣ��޷����ӷ�������");
				break;
			}
			
			case MSG_EXPOSE_TIM:
			{
				tvExposeTim.setText(String.valueOf(slowExposeTim));

				Log.i(LOG_TAG, "Slow Expose "+slowExposeTim);
				
				if(slowExposeTim!=0)
				{
					slowExposeTim--;
				}
				else
				{
					stopSlowExpose();
				}
				break;
			}

			default:
				break;
			}
			super.handleMessage(msg);
		}

	}

	private boolean camInit()
	{
		FrameLayout framePreview = (FrameLayout) findViewById(R.id.frameViewCam);

		mCamView = new CamView(this, new CamPreviewCB(), 
				getWindowManager().getDefaultDisplay());
		if (mCamView.mCamera == null)
		{
			showDialog(DLG.CAM_ERROR.ordinal());
			return false;
		}
	
		framePreview.addView(mCamView);

		return true;
	}
	
	private void viewListenerInit()
	{

		mCamView.setOnClickListener(new View.OnClickListener()
		{
			public void onClick(View v)
			{
				if(slowExposeState==SE_DISPLAY)
				{
					slowExposeState=SE_IDLE;
					mCamView.openCamera(mCamView.camIndex);
					mCamView.StartPreview();
				}
				else
				{
					mCamView.mCamera.autoFocus(null);
				}
			}
		});

		buttonTakePic = (Button) findViewById(R.id.buttonTakePic);
		buttonTakePic.setOnClickListener(new View.OnClickListener()
		{
			public void onClick(View v)
			{
				if (getAppDir() != null && !isRecording)
				{
					mCamView.mCamera.autoFocus(new TakePicAfterFocus());
				}
			}
		});

		isRecording = false;
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
				} else
				{
					File appDir;
					appDir = getAppDir();
					if (appDir != null)
					{
						String timeStamp = new SimpleDateFormat(
								"yyyy_MMdd_HHmmss").format(new Date());
						File vidFile = new File(appDir, "VID_" + timeStamp
								+ ".MP4");
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

		btLogin = (Button) findViewById(R.id.buttonLogin);
		btLogin.setOnClickListener(new View.OnClickListener()
		{
			public void onClick(View v)
			{
				Intent intent = new Intent(MainActivityIPC.this,
						ActivityLogin.class);
				// startActivity(intent);//ֱ���л�Activity�����շ��ؽ��
				startActivityForResult(intent, GET_CODE);
			}
		});

		btSwitchCam=(Button)findViewById(R.id.buttonSwitchCam);
		btSwitchCam.setOnClickListener(new View.OnClickListener()
		{
			
			@Override
			public void onClick(View v)
			{
				mCamView.switchCam();
			}
		});
		
		btSlowExpose=(Button)findViewById(R.id.buttonSlowExpose);
		btSlowExpose.setOnClickListener(new View.OnClickListener()
		{
			
			@Override
			public void onClick(View v)
			{
				//��ʼ����
				if(slowExposeState==SE_IDLE)
				{
					slowExposeState=SE_EXPOSING;
					btSlowExpose.setText(getString(R.string.stopSlowExpose));

					slowExposeTim=2<<spMain.getInt("exposeTime", 0);
					slowExposeFrameCnt=0;
					new Thread(exposeTimRunnable).start();
					
					btExposeTime.setEnabled(false);
					btSwitchCam.setEnabled(false);
				}
				//ֹͣ����
				else if(slowExposeState==SE_EXPOSING)
				{
					stopSlowExpose();
				}
			}
		});
		
		btExposeTime=(Button)findViewById(R.id.buttonExposeTime);
		btExposeTime.setOnClickListener(new View.OnClickListener()
		{
			
			@Override
			public void onClick(View v)
			{
				showDialog(DLG.EXPOSE_TIME_SEL.ordinal());
			}
		});
		
	}

	public class CamPreviewCB implements Camera.PreviewCallback
	{

		@Override
		public void onPreviewFrame(byte[] data, Camera camera)
		{
			long curTimems = System.currentTimeMillis();
			fpsCalc.frmCnt++;
			if ((curTimems - fpsCalc.timeMs) > FPS_INTVAL)
			{
				fpsCalc.fps = (int) (1000 * fpsCalc.frmCnt / (curTimems - fpsCalc.timeMs));
				fpsCalc.frmCnt = 0;
				fpsCalc.timeMs = curTimems;
			}

			if (httpSvr != null)
			{
				httpSvr.synImg.setImgData(data, mCamView.prevSize.width,
						mCamView.prevSize.height);
			}
			
			if(slowExposeState==SE_EXPOSING)
			{
				if(slowExposeBuf==null)
				{
					slowExposeBuf=new int[data.length];
				}
				
				getOrAddNv21Buf(data, false);
				
			}
		}
	}

	public class TakePicAfterFocus implements AutoFocusCallback
	{
		@Override
		public void onAutoFocus(boolean success, Camera camera)
		{
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
				File appDir = getAppDir();
				if (appDir != null)
				{
					String timeStamp = new SimpleDateFormat("yyyy_MMdd_HHmmss")
							.format(new Date());
					File pic = new File(appDir, "PIC_" + timeStamp + ".jpg");
					Log.i("onPictureTaken", pic.toString());
					FileOutputStream fos = new FileOutputStream(pic);
					fos.write(data);
					fos.close();
				} else
				{
					Log.w("onPictureTaken", "File Fail");
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
		File dir = null;

		// �ⲿ�洢������
		if (Environment.getExternalStorageState().equals(
				android.os.Environment.MEDIA_MOUNTED))
		{
			dir = new File(Environment.getExternalStorageDirectory()
					+ File.separator + getString(R.string.app_name));
			if (!dir.exists())
			{
				Log.i("getAppDir", "Dir:" + dir.toString() + " Not Exist!");
				dir.mkdir();
				if (!dir.exists())
				{
					showDialog(DLG.STORAGE_ERROR.ordinal());
				}
			} else
			{
				Log.i("getAppDir", "Dir:" + dir.toString() + " Already Exist!");
			}
		} else
		{
			showDialog(DLG.NO_STORAGE.ordinal());
		}

		return dir;
	}

	public File getWebDir()
	{
		File dir = null;

		File appDir = getAppDir();
		if (appDir != null)
		{
			dir = new File(appDir, "WebFile");
			if (!dir.exists())
			{
				dir.mkdir();
			}
		}
		return dir;
	}

	public String getLocalIp()
	{
		try
		{
			for (Enumeration<NetworkInterface> en = NetworkInterface
					.getNetworkInterfaces(); en.hasMoreElements();)
			{
				NetworkInterface intf = en.nextElement();
				for (Enumeration<InetAddress> enumIpAddr = intf
						.getInetAddresses(); enumIpAddr.hasMoreElements();)
				{
					InetAddress inetAddress = enumIpAddr.nextElement();
					if (!inetAddress.isLoopbackAddress()
							&& !inetAddress.isLinkLocalAddress())
					{
						return inetAddress.getHostAddress().toString();
					}
				}
			}
		} catch (SocketException ex)
		{
			Log.e(LOG_TAG, ex.toString());
		}
		return null;
	}

	public class NetIf
	{
		String ipStr;
		int ipInt;
		String macStr;

		public String ip_ntoa(int ipInt)
		{
			if (ipInt != 0)
			{
				return new StringBuilder().append((ipInt & 0xff)).append('.')
						.append((ipInt >> 8) & 0xff).append('.')
						.append((ipInt >> 16) & 0xff).append('.')
						.append((ipInt >> 24) & 0xff).toString();
			}
			return null;
		}

		public boolean setIp(int ip)
		{
			if (ip == 0)
			{
				return false;
			}

			ipInt = ip;
			ipStr = ip_ntoa(ip);
			return true;
		}
	}

	public NetIf getWifi()
	{
		NetIf wifi = new NetIf();
		try
		{
			WifiManager mWifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);
			WifiInfo info = mWifi.getConnectionInfo();
			int ip = info.getIpAddress();
			if (ip == 0)
			{
				return null;
			}

			wifi.setIp(ip);
			Log.i(LOG_TAG, wifi.ipStr);
			wifi.macStr = info.getMacAddress().toUpperCase();
			return wifi;
		} catch (Exception ex)
		{
			Log.e(LOG_TAG, ex.toString());
		}
		return null;
	}

}

class SurfaceMask extends SurfaceView implements SurfaceHolder.Callback
{

	public SurfaceMask(Context context)
	{
		super(context);
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height)
	{
		
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder)
	{
		
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder)
	{
		
	}
	
}

class CamView extends SurfaceView implements SurfaceHolder.Callback
{
	SurfaceHolder mHolder;
	MediaRecorder mMediaRec;

	CamPreviewCB previewCallBack;
	
	SharedPreferences spCamOpened;
	
	Display Container;

	Camera mCamera;
	Camera.Size picSize;
	Camera.Size prevSize;
	
	int camIndex;

	private static final int IPC_BEST_WIDTH = 640;
	private static final String LOG_TAG = "CamView";

	CamView(Context context, CamPreviewCB camPreviewCB, Display camContainer)
	{
		super(context);
		
		previewCallBack = camPreviewCB;
		Container=camContainer;
		
        spCamOpened = context.getSharedPreferences("spCamOpened", 0);
        camIndex=spCamOpened.getInt("camIndex", 0);
        
		mHolder = this.getHolder();
		mHolder.addCallback(this);
		mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        
		openCamera(camIndex);
	}
	
	public void switchCam()
	{
		closeCamera();
		
		if(camIndex<(Camera.getNumberOfCameras()-1))
		{
			camIndex++;
		}
		else
		{
			camIndex=0;
		}
		
		if(!openCamera(camIndex))
		{
			camIndex=0;
			openCamera(0);
		}

		Editor editor = spCamOpened.edit();
        editor.putInt("camIndex", camIndex);
        editor.commit();
        
        StartPreview();
	}

	public void StartPreview()
	{
		try
		{
			mCamera.setPreviewDisplay(mHolder);
		} catch (IOException e)
		{
			e.printStackTrace();
		}
		mCamera.setPreviewCallback(previewCallBack);
		mCamera.startPreview();
	}
	
	public boolean openCamera(int cam)
	{
		mCamera = Camera.open(cam);
		if (mCamera == null)
		{
			return false;
		}
		
		Camera.Parameters parameters = mCamera.getParameters();
		
		Log.i(LOG_TAG,"HorizontalViewAngle:"+parameters.getHorizontalViewAngle());
		Log.i(LOG_TAG,"VerticalViewAngle:"+parameters.getVerticalViewAngle());

		// �������Ԥ���ߴ�
		List<Camera.Size> PreviewSizes = parameters
				.getSupportedPreviewSizes();
		prevSize = PreviewSizes.get(0);
		for (int i = 0; i < PreviewSizes.size(); i++)
		{
			if (prevSize.width < PreviewSizes.get(i).width)
			{
				prevSize.width = PreviewSizes.get(i).width;
				prevSize.height = PreviewSizes.get(i).height;
			}

			Log.i(LOG_TAG, "PreviewSizes width:" + PreviewSizes.get(i).width
					+ " height:" + PreviewSizes.get(i).height);
		}
		// Log.i("PreviewSizes",
		// "width:"+prevSize.width+" height:"+prevSize.height);

		// ���������Ԥ���ߴ���ӽ�����Ƭ�ߴ�
		List<Camera.Size> PictureSizes = parameters
				.getSupportedPictureSizes();
		picSize = PictureSizes.get(0);
		int widDiff = Math.abs(picSize.width - prevSize.width);
		for (int i = 0; i < PictureSizes.size(); i++)
		{
			int curDiff = Math.abs(PictureSizes.get(i).width
					- prevSize.width);
			if (widDiff > curDiff)
			{
				picSize.width = PictureSizes.get(i).width;
				picSize.height = PictureSizes.get(i).height;
				widDiff = curDiff;
			}

			Log.i(LOG_TAG, "PictureSizes width:" + PictureSizes.get(i).width
					+ " height:" + PictureSizes.get(i).height);
		}
		Log.i(LOG_TAG, "PictureSizes Used width:" + picSize.width + " height:"
				+ picSize.height);

		parameters.setPictureFormat(PixelFormat.JPEG);
		parameters.setPictureSize(picSize.width, picSize.height);// (picSize.width,

		parameters.setPreviewSize(prevSize.width, prevSize.height);
		parameters.setJpegQuality(100);
		// parameters.get
		mCamera.setParameters(parameters);

		Log.i(LOG_TAG,
				"screen:" + Container.getWidth() + " " + Container.getHeight());
		Log.i(LOG_TAG, "mCamView:" + prevSize.width + " "
				+ prevSize.height);


		mHolder.setFixedSize(prevSize.width * Container.getHeight()/
				prevSize.height, Container.getHeight());
		
		return true;
	}
	
	public void closeCamera()
	{
		stopRec();

		if (mCamera != null)
		{
			mCamera.setPreviewCallback(null);
			mCamera.stopPreview();
			mCamera.release(); // release the camera for other applications
			mCamera = null;
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
		if (vidFile == null)
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
		} catch (IllegalStateException e)
		{
			Log.d(LOG_TAG,
					"prepareRec IllegalStateException preparing MediaRecorder: "
							+ e.getMessage());
			releaseMediaRecorder();
			return false;
		} catch (IOException e)
		{
			Log.d(LOG_TAG,
					"prepareRec IOException preparing MediaRecorder: " + e.getMessage());
			releaseMediaRecorder();
			return false;
		}
		return true;

	}

	public boolean startRec(File vidFile)
	{
		// initialize video camera
		Log.i(LOG_TAG, "startRec");
		if (prepareRec(vidFile))
		{
			mMediaRec.start();
			return true;
		} else
		{
			// prepare didn't work, release the camera
			releaseMediaRecorder();
			return false;
		}
	}

	public void stopRec()
	{
		if (mMediaRec != null)
		{
			// stop recording and release camera
			mMediaRec.stop(); // stop the recording
			releaseMediaRecorder(); // release the MediaRecorder object
		}
	}

	public void surfaceCreated(SurfaceHolder holder)
	{
		Log.i(LOG_TAG, "surfaceCreated");
		if (mCamera == null)
		{
			openCamera(camIndex);
		}
		// The Surface has been created, acquire the camera and tell it where
		// to draw.
		if (mCamera != null)
		{
			try
			{
				mCamera.setPreviewDisplay(holder);
			} catch (IOException exception)
			{
				mCamera.release();
				mCamera = null;
			}
		}
	}

	public void surfaceDestroyed(SurfaceHolder holder)
	{
		Log.i(LOG_TAG, "surfaceDestroyed");
		closeCamera();
	}

	public void surfaceChanged(SurfaceHolder holder, int format, int w, int h)
	{
		// Now that the size is known, set up the camera parameters and begin
		// the preview.
		Log.i(LOG_TAG, "surfaceChanged");

		if (mCamera != null)
		{
			mCamera.setPreviewCallback(previewCallBack);
			mCamera.startPreview();
		}

	}
}
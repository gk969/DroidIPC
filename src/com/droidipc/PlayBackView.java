package com.droidipc;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.hardware.Camera;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;




class PlaybackViewCB implements SurfaceHolder.Callback
{
	private SurfaceHolder hld;
	private int color=0;
	private boolean surfaceIsValid=false;
	private Bitmap mBitmap;
	private SurfaceView mView;
	
	
	private int[] picSize;
	private int[] rgb;
	final int isrcWid=0;
	final int isrcHei=1;
	final int itop=2;
	final int ileft=3;
	final int itarWid=4;
	final int itarHei=5;
	
	private static native boolean  OnPreviewFrame(byte[] nv21, int[] rgb, int[] picSize);
    
    public PlaybackViewCB(SurfaceView view)
    {
    	mView=view;
    	picSize=new int[6];
    }
	
	public void drawBit(byte[] nv21, Camera camera)
	{
		if(surfaceIsValid)
		{
			Camera.Size prevSize=camera.getParameters().getPreviewSize();
			picSize[isrcWid]=prevSize.width;
			picSize[isrcHei]=prevSize.height;
	    	picSize[itop]=picSize[isrcHei]/2;
	    	picSize[ileft]=0;
			//Log.i("PlaybackViewCB", "frame data size:"+data.length); 
	    	
			if(OnPreviewFrame(nv21, rgb, picSize))
			{
				//Log.i("PlaybackViewCB", "rgb length:"+rgb.length); 
				mBitmap.setPixels(rgb, 0, picSize[itarWid], 0, 0, picSize[itarWid], picSize[itarHei]);
				if(mBitmap!=null)
				{
					Canvas playbackCvs;
					playbackCvs=hld.lockCanvas();
					//playbackCvs.drawColor(Color.rgb(color+=20, 0, 0));
					playbackCvs.drawBitmap(mBitmap, 0, 0, null);
					hld.unlockCanvasAndPost(playbackCvs);
				}
			}
			
		}
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
		surfaceIsValid=true;
		
		int width=mView.getWidth()&0xFFFFFFFE;
		int height=mView.getHeight()&0xFFFFFFFE;
		picSize[itarWid]=width;
		picSize[itarHei]=height;
		rgb=new int[width*height];
    	mBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
    	Log.i("palyback View Size", "width:"+width+" height:"+height); 
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder)
	{
		// TODO Auto-generated method stub
		surfaceIsValid=false;
	}
	
}

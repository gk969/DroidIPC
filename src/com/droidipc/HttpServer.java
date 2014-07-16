package com.droidipc;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

import com.droidipc.NanoHTTPD.Response;

import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.util.Log;

public class HttpServer extends NanoHTTPD
{
    private File homeDir;
    final String LOG_TAG="HttpServer";
    
    NV21toJpgThread compressThread;
    
    ByteArrayOutputStream imgStream;
    
    boolean imgStreamInUse=false;
    boolean imgDataInUse=false;
    
    private byte[] imgData;
    int imgWidth;
    int imgHeight;
    
	public HttpServer(int port, File webDir) throws IOException
	{
		super(port, webDir);
		homeDir=webDir;
		imgData=null;
		imgStream =new ByteArrayOutputStream();
		
		compressThread = new NV21toJpgThread();
		compressThread.setDaemon(true);
		compressThread.start();
	}
	
	public class NV21toJpgThread extends Thread
	{
		public void run()
		{
			while(true)
			{
				Log.i("compressThread", "NV21 to Jpg");

	        	long tim=System.currentTimeMillis();
	        	NV21toJpgStream();
				tim=System.currentTimeMillis()-tim;
				Log.i(LOG_TAG, "NV21toJpgStream "+tim+"ms");
	        	
				pause();
			}
		}
		

	    public synchronized void pause()
	    {
	        try
	        {
	            wait();
	        }
	        catch(InterruptedException ie)
	        {
	            ie.printStackTrace();
	        }
	    }
	    
	    public synchronized void awake()
	    {
	        notifyAll();
	    }
	}
	
	public void close()
	{
		this.stop();
	}
	
	private class Cimg
	{
		byte[] data;
		int width;
		int height;
		
		private boolean dataReady;
		private boolean streamReady;
		
		public Cimg()
		{
			dataReady=false;
			streamReady=false;
		}
		
		public synchronized boolean dataIsReady()
		{
			return dataReady;
		}
		
		public synchronized boolean streamIsReady()
		{
			return streamReady;
		}
		
		public synchronized void setDataReady(boolean status)
		{
			dataReady=status;
		}

		public synchronized void setStreamReady(boolean status)
		{
			streamReady=status;
		}
	}
	
	public class CimgPool
	{
		Cimg[] img;
		
		static final int IMG_POOL_SIZE=10;
		
		public CimgPool()
		{
			img=new Cimg[IMG_POOL_SIZE];
			
			for(int i=0; i<IMG_POOL_SIZE; i++)
			{
				img[i]=new Cimg();
			}
		}
		
		public void setImgData(byte[] srcData, int width, int height)
		{
			for(int i=0; i<IMG_POOL_SIZE; i++)
			{
				if(!img[i].dataIsReady())
				{
					img[i].data=srcData;
					img[i].width=width;
					img[i].height=height;
					img[i].setDataReady(true);
				}
			}
		}
		
		public int getReadyStream()
		{
			int i;
			for(i=0; i<IMG_POOL_SIZE; i++)
			{
				if(img[i].dataIsReady())
				{
					break;
				}
			}
			YuvImage yuv=new YuvImage(imgData, ImageFormat.NV21, 
									  imgWidth, imgHeight, null);
			
			try
			{
				imgStreamInUse=true;
				imgStream.reset();
				long tim=System.currentTimeMillis();
				yuv.compressToJpeg(new Rect(0, 0, imgWidth, imgHeight),
	                    60, imgStream);
				imgStreamInUse=false;
				//this.notifyAll();
				tim=System.currentTimeMillis()-tim;
				Log.i(LOG_TAG, "compressToJpeg time:"+tim+"ms size:"+
					  imgStream.size()+" "+(imgStream.size()/1024)+"KB");
			}catch(Exception e)
			{
				e.printStackTrace();
			}
			
			
		}
	}
	
	public void setImgData(byte[] data, int imgWid, int imgHei)
	{
		if((!imgDataInUse)&&(data!=null))
		{
			imgData=data;
			
			imgWidth=imgWid;
			imgHeight=imgHei;
		}
		
	}
	
	private void NV21toJpgStream()
	{
		if(imgData==null)
		{
			return;
		}
		
		imgDataInUse=true;
		YuvImage yuv=new YuvImage(imgData, ImageFormat.NV21, 
								  imgWidth, imgHeight, null);
		imgDataInUse=false;
		try
		{
			imgStreamInUse=true;
			imgStream.reset();
			long tim=System.currentTimeMillis();
			yuv.compressToJpeg(new Rect(0, 0, imgWidth, imgHeight),
                    60, imgStream);
			imgStreamInUse=false;
			//this.notifyAll();
			tim=System.currentTimeMillis()-tim;
			Log.i(LOG_TAG, "compressToJpeg time:"+tim+"ms size:"+
				  imgStream.size()+" "+(imgStream.size()/1024)+"KB");
		}catch(Exception e)
		{
			e.printStackTrace();
		}
		
	}

	@Override
	public Response serve( String uri, String method, Properties header, Properties parms, Properties files ) {
        Log.i(LOG_TAG, "serve "+ method + " '" + uri + "' ");
        
        String target=uri.toString().substring(1);
        
        Log.i(LOG_TAG, "target "+target);
        
        int len=target.length();
        if(len>7)
        {
	        if(target.substring(0, 3).equals("pic")&&
	           target.substring(len-4, len).equals(".jpg"))
	        {
	        	
	        	long tim=System.currentTimeMillis();
	        	NV21toJpgStream();
				tim=System.currentTimeMillis()-tim;
				Log.i(LOG_TAG, "NV21toJpgStream "+tim+"ms");
	        	
	        	String mime = "image/jpeg";
				Response res = new Response( HTTP_OK, mime, 
						new ByteArrayInputStream(imgStream.toByteArray()));
				res.addHeader( "Content-Length", "" + imgStream.size());
				res.addHeader( "Accept-Ranges", "bytes");
				return res;
	        }
        }
        return serveFile( uri, header, homeDir, true); 
	}
	
	@Override
	public void serveDone(Response r)
	{
		Log.i(LOG_TAG, "serveDone status:"+r.status+
			" mimeType:"+r.mimeType+" header:"+r.header.toString());
	}
}

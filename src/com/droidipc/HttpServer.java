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
    
    Cimg yuvImg;
    
	public HttpServer(int port, File webDir) throws IOException
	{
		super(port, webDir);
		homeDir=webDir;
		yuvImg=new Cimg();
	}
	
	public void close()
	{
		this.stop();
	}
	
	public class Cimg
	{
		byte[] data;
		int width;
		int height;
		
		public void setImg(byte[] imgDat, int wid, int hei)
		{
			data=imgDat;
			width=wid;
			height=hei;
		}
	}
	
	private synchronized Cimg readOrWriteImgData(Cimg img, boolean isRead)
	{
		if(!isRead)
		{
			yuvImg=img;
		}
		return yuvImg;
	}
	
	public void setImgData(byte[] data, int width, int height)
	{
		Cimg img=new Cimg();
		img.setImg(data, width, height);
		readOrWriteImgData(img, false);
	}
	
	public Cimg getImgdata()
	{
		return readOrWriteImgData(yuvImg, true);
	}
	
	private ByteArrayOutputStream NV21toJpgStream(Cimg img)
	{
		if(img.data==null)
			return null;
		
		ByteArrayOutputStream imgStream=new ByteArrayOutputStream();
		
		YuvImage yuv=new YuvImage(img.data, ImageFormat.NV21, 
								  img.width, img.height, null);
		try
		{
			imgStream.reset();
			long tim=System.currentTimeMillis();
			yuv.compressToJpeg(new Rect(0, 0, img.width, img.height),
                    60, imgStream);
			
			tim=System.currentTimeMillis()-tim;
			Log.i(LOG_TAG, "compressToJpeg time:"+tim+"ms size:"+
				  imgStream.size()+" "+(imgStream.size()/1024)+"KB");
		}catch(Exception e)
		{
			e.printStackTrace();
		}
		
		return imgStream;
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
	        	ByteArrayOutputStream imgStream=NV21toJpgStream(getImgdata());
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

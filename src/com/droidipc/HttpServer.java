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
    
    
    private byte[] imgData;
    ByteArrayOutputStream imgStream;
    
    boolean imgDataInUse=false;
    int imgWidth;
    int imgHeight;
    
	public HttpServer(int port, File webDir) throws IOException
	{
		super(port, webDir);
		homeDir=webDir;
		imgData=null;
		imgStream =new ByteArrayOutputStream();
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
		
		//imgFile = new File(homeDir, fileName);
		
		
		imgDataInUse=true;
		YuvImage yuv=new YuvImage(imgData, ImageFormat.NV21, 
								  imgWidth, imgHeight, null);
		imgDataInUse=false;
		try
		{
			imgStream.reset();
			long tim=System.currentTimeMillis();
			yuv.compressToJpeg(new Rect(0, 0, imgWidth, imgHeight),
                    90, imgStream);
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
				//res.addHeader( "ETag", etag);
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

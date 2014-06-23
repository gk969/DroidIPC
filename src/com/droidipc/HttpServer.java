package com.droidipc;

import java.io.File;
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
    private File imgFile;
    
    boolean imgDataInUse=false;
    boolean imgFileInServe=false;
    int imgWidth;
    int imgHeight;
    
	public HttpServer(int port, File webDir) throws IOException
	{
		super(port, webDir);
		homeDir=webDir;
	}
	
	public void setImgData(byte[] data, int imgWid, int imgHei)
	{
		if(!imgDataInUse)
		{
			imgData=data;
			
			imgWidth=imgWid;
			imgHeight=imgHei;
		}
		
	}
	
	private void NV21toJpgFile(String fileName)
	{
		imgFile = new File(homeDir, fileName);
		
		imgDataInUse=true;
		YuvImage yuv=new YuvImage(imgData, ImageFormat.NV21, imgWidth, imgHeight, null);
		imgDataInUse=false;
		Log.i(LOG_TAG, "NV21toJpgFile "+fileName);
		try
		{
			FileOutputStream fos = new FileOutputStream(imgFile);
			yuv.compressToJpeg(new Rect(0, 0, imgWidth, imgHeight),
                    90, fos);
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
        if(target.length()>7)
        {
	        if(target.substring(0, 3).equals("pic"))
	        {
	        	NV21toJpgFile(target);
	        	imgFileInServe=true;
	        }
        }
        return serveFile( uri, header, homeDir, true); 
	}
	
	@Override
	public void serveDone(Response r)
	{
		Log.i(LOG_TAG, "serveDone status:"+r.status+
					" mimeType:"+r.mimeType+" header:"+r.header.toString());
		
		if(imgFileInServe)
		{
			if(imgFile.isFile() && imgFile.exists())
			{
				imgFile.delete();
			}
			imgFileInServe=false;
		}
	}
}

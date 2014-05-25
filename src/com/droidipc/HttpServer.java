package com.droidipc;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import com.droidipc.NanoHTTPD.Response;

import android.util.Log;

public class HttpServer extends NanoHTTPD
{
    private File homeDir;
    public  String newVidNm;
    final String LOG_TAG="HttpServer";
	
	public HttpServer(int port, File webDir) throws IOException
	{
		super(port, webDir);
		homeDir=webDir;
		newVidNm="none";
	}

	@Override
	public Response serve( String uri, String method, Properties header, Properties parms, Properties files ) {
        Log.i(LOG_TAG, "serve "+ method + " '" + uri + "' ");
        
        if(uri.toString().substring(1).equals("newVid"))
        {
        	Log.i(LOG_TAG, "serve newVid");
        	return new Response(HTTP_OK, MIME_HTML, newVidNm);
        }
        return serveFile( uri, header, homeDir, true ); 
	}
	
	@Override
	public void serveDone(Response r)
	{
		Log.i(LOG_TAG, "serveDone status:"+r.status+
					" mimeType:"+r.mimeType+" header:"+r.header.toString());
	}
}

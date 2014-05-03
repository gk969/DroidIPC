package com.droidipc;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import android.util.Log;

public class HttpServer extends NanoHTTPD
{
    private File homeDir;
	
	public HttpServer(int port, File webDir) throws IOException
	{
		super(port, webDir);
		homeDir=webDir;
	}
	
	public Response serve( String uri, String method, Properties header, Properties parms, Properties files ) {
        Log.i("HttpServer", method + " '" + uri + "' " );
        return serveFile( uri, header, homeDir, true ); 
	}
}

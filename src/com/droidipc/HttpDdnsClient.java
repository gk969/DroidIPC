package com.droidipc;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import android.util.Log;

public class HttpDdnsClient
{
	private static final int linkInterval=5000;
	private static final String LOG_TAG="HttpDdnsClient";
	private boolean threadRunning=true;
	
	public HttpDdnsClient()
	{
		ServerLinkThread serverLinkThread=new ServerLinkThread();
		serverLinkThread.setDaemon(true);
		serverLinkThread.start();
	}

	public class ServerLinkThread extends Thread
	{
		public void run()
		{
			while(threadRunning)
			{
				Log.i(LOG_TAG, "thread");
				try
				{
					sleep(linkInterval);
					sendLinkMsg();
				} catch (InterruptedException e)
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}
	
	public void stopServerLinkThread()
	{
		threadRunning=false;
	}
	
	private void sendLinkMsg()
	{
		final String LOG_TAG="httpDdnsClientInit";
		
		Log.i(LOG_TAG, LOG_TAG+" start");
		
		try
		{
			
			URL url=new URL("http://gk969.com");
			HttpURLConnection urlConn=(HttpURLConnection)url.openConnection();
			urlConn.setChunkedStreamingMode(0);
			urlConn.setConnectTimeout(5000);
			urlConn.setRequestMethod("GET");
			urlConn.setReadTimeout(1000);
			urlConn.setDoOutput(true);
			urlConn.setDoInput(true);
			urlConn.setUseCaches(false);
			urlConn.setRequestProperty("Content-Type","text/html");
			
			
			if(urlConn.getResponseCode()==200)
			{
				InputStreamReader in = new InputStreamReader(urlConn.getInputStream());
				BufferedReader buffer = new BufferedReader(in);
                String inputLine = null;
                
                while (((inputLine = buffer.readLine()) != null))
                {
                	Log.i(LOG_TAG, inputLine);
                }
	            
                in.close();
			}
			
            urlConn.disconnect(); 
			
		} catch (MalformedURLException e)
		{
			e.printStackTrace();
		} catch (IOException e)
		{
			e.printStackTrace();
		}
		
		
	}
	
}
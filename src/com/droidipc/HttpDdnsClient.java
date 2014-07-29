package com.droidipc;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;

import android.util.Log;

public class HttpDdnsClient
{
	private static final int LINK_INTERVAL=10000;
	private static final String LOG_TAG="HttpDdnsClient";
	private boolean threadRunning=true;
	private ServerLinkThread serverLinkThread;
	
	private String userName;
	private String passWord;
	
	public HttpDdnsClient()
	{
		serverLinkThread=new ServerLinkThread();
		serverLinkThread.setDaemon(true);
		linkServer("gk969", "23dd34s33");
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
					sendLinkMsg();
					sleep(LINK_INTERVAL);
				} catch (InterruptedException e)
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}
	
	public void linkServer(String user, String pwd)
	{
		userName=user;
		passWord=pwd;
		
		threadRunning=true;
		serverLinkThread.start();
	}
	
	public void stop()
	{
		threadRunning=false;
	}
	
	private void sendLinkMsg()
	{
		final String LOG_TAG="httpDdnsClientInit";
		
		Log.i(LOG_TAG, LOG_TAG+" start");
		
		try
		{
			//URL url=new URL("http://192.168.0.101");
			URL url=new URL("http://gk969.com/wp-login.php");
			HttpURLConnection urlConn=(HttpURLConnection)url.openConnection();
			
			String postAuthStr="log="+userName+"&pwd="+passWord+"&wp-submit=%E7%99%BB%E5%BD%95&redirect_to=http%3A%2F%2Fgk969.com%2Fwp-admin%2F&testcookie=1";
			try
			{
				//urlConn.setChunkedStreamingMode(500);
				urlConn.setConnectTimeout(5000);
				urlConn.setReadTimeout(1000);
				urlConn.setDoOutput(true);
				urlConn.setDoInput(true);
				urlConn.setUseCaches(false);
				urlConn.setRequestProperty("Content-Type","application/x-www-form-urlencoded");
				urlConn.setRequestProperty("Content-Length", String.valueOf(postAuthStr.length()));
				urlConn.setRequestProperty("Charset", "UTF-8");
				
				OutputStream out = urlConn.getOutputStream();
				out.write(postAuthStr.getBytes());
				out.close();
				
				if(urlConn.getResponseCode()==200)
				{
					int hdrpos=0;
					String hdr;
					
					Map<String, List<String>> header=urlConn.getHeaderFields();
					List<String> cookie=header.get("Set-Cookie");
					
					Log.i(LOG_TAG, cookie.toString());
					
					/*
					for (String key : header.keySet())
					{
						Log.i(LOG_TAG, key+": "+header.get(key));
					}
					
					//Log.i(LOG_TAG, header.toString());
					
					Log.i(LOG_TAG, urlConn.getHeaderField("Set-Cookie"));
					
					while((hdr=urlConn.getHeaderField(hdrpos))!=null)
					{
						Log.i(LOG_TAG, hdr);
						hdrpos++;
					}
					*/
					
					/*
					InputStreamReader in = new InputStreamReader(urlConn.getInputStream());
					BufferedReader buffer = new BufferedReader(in);
	                String inputLine = null;
	                
	                int lineCnt=0;
	                while (((inputLine = buffer.readLine()) != null)&&(lineCnt<50))
	                {
	                	Log.i(LOG_TAG, inputLine);
	                	lineCnt++;
	                }
		            
	                in.close();
	                */
				}
				
				
			}
			finally
			{
				if(urlConn!=null)urlConn.disconnect(); 
			}
			
		} catch (MalformedURLException e)
		{
			e.printStackTrace();
		} catch (IOException e)
		{
			e.printStackTrace();
		}
		
		
	}
	
}
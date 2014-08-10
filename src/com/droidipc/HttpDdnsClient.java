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

import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class HttpDdnsClient
{
	private static final int LINK_INTERVAL=10000;
	private static final String LOG_TAG="HttpDdnsClient";
	private boolean threadRunning;
	private ServerLinkThread serverLinkThread;
	
	private String userName;
	private String passWord;
	private Handler msgHandler;
	
	public HttpDdnsClient()
	{
		serverLinkThread=new ServerLinkThread();
		serverLinkThread.setDaemon(true);
		threadRunning=false;
	}
	
	
	/********************** 对外接口 *********************/
	
	public void setAuth(String user, String pwd)
	{
		userName=user;
		passWord=pwd;
	}
	
	public void setMsgHandler(Handler handler)
	{
		msgHandler=handler;
	}
	
	public void start()
	{
		threadRunning=true;
		if(!serverLinkThread.isAlive())
		{
			serverLinkThread.start();
		}
	}
	
	public void stop()
	{
		threadRunning=false;
	}
	
	
	
	/********************* 私有成员 *******************/
	
	private class ServerLinkThread extends Thread
	{
		public void run()
		{
			while(threadRunning)
			{
				Log.i(LOG_TAG, "thread");
				try
				{
					linkAndLogin();
					sleep(LINK_INTERVAL);
				} catch (InterruptedException e)
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}
	
	private void linkAndLogin()
	{
		String cookie=linkServer();
		Message msg = new Message();
		if(cookie!=null)
		{
			//通过返回的Cookie判断是否登陆成功
			if(cookie.indexOf("wordpress_logged_in")>=0)
			{
				String retJson=ipcLogin(parseCookie(cookie));
				
				//未返回正常JSON包，服务器连接超时或出错
				if(retJson==null)
				{
					Log.e(LOG_TAG, "Return JSON Error!"+retJson);
					msg.what = MainActivityIPC.MSG_HTTP_LOGIN_LINK_FAIL;
				}
				else
				{
					msg.what = MainActivityIPC.MSG_HTTP_LOGIN_SUCCESS;
					msg.obj=retJson;
				}
			}
			//登录失败，用户名或密码错误
			else
			{
				msg.what = MainActivityIPC.MSG_HTTP_LOGIN_AUTH_FAIL;
			}
		}
		else
		{
			msg.what = MainActivityIPC.MSG_HTTP_LOGIN_LINK_FAIL;
		}
		
		msgHandler.sendMessage(msg);
	}
	
	private String linkServer()
	{
		String cookieStr=null;
		
		Log.i(LOG_TAG, "linkServer start");
		
		try
		{
			URL url=new URL("http://gk969.com/wp-login.php");
			HttpURLConnection urlConn=(HttpURLConnection)url.openConnection();
			
			String postAuthStr="log="+userName+"&pwd="+passWord+"&wp-submit=%E7%99%BB%E5%BD%95&redirect_to=http%3A%2F%2Fgk969.com%2Fwp-admin%2F&testcookie=1";
			try
			{
				urlConn.setConnectTimeout(10000);
				urlConn.setReadTimeout(5000);
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
					Map<String, List<String>> header=urlConn.getHeaderFields();
					List<String> cookie=header.get("Set-Cookie");
					
					cookieStr=cookie.toString();
					Log.i(LOG_TAG, "cookie:"+cookieStr);
					
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
		
		return cookieStr;
	}
	
	private String parseCookie(String cookieIn)
	{
		String cookieOut="";
		String oneCookie;
		
		cookieIn=cookieIn.substring(1);
		
		int offset=0;
		
		while(true)
		{
			int step=cookieIn.indexOf("; ", offset);
			if(step>=0)
			{
				step+=2;
				oneCookie=cookieIn.substring(offset, step);
				Log.i(LOG_TAG, oneCookie);
				offset=step;
				cookieOut+=oneCookie;
				
				step=cookieIn.indexOf(", ", offset);
				if(step>=0)
				{
					offset=step+=2;
				}
				else
				{
					break;
				}
			}
			else
			{
				break;
			}
			
		}
		cookieOut=cookieOut.substring(0, cookieOut.length()-2);
		Log.i(LOG_TAG, cookieOut);
		return cookieOut;
	}
	
	private String ipcLogin(String cookie)
	{
		Log.i(LOG_TAG, "ipcLogin start");
		String retJson=null;
		try
		{
			URL url=new URL("http://gk969.com/ipc/droidipc.php?=ipc-login");
			HttpURLConnection urlConn=(HttpURLConnection)url.openConnection();
			InputStreamReader in=null;
			BufferedReader inBuffer=null;
			try
			{
				urlConn.setConnectTimeout(5000);
				urlConn.setReadTimeout(2000);
				urlConn.setDoInput(true);
				urlConn.setUseCaches(false);
				urlConn.setRequestProperty("Cookie",cookie);
				
				if(urlConn.getResponseCode()==200)
				{
					in = new InputStreamReader(urlConn.getInputStream());
					inBuffer = new BufferedReader(in);
					
					retJson=inBuffer.readLine();
					Log.i(LOG_TAG, retJson);
				}
			}
			finally
			{
				if(urlConn!=null)urlConn.disconnect(); 
				if(in!=null)in.close();
				if(inBuffer!=null)inBuffer.close();
			}
			
		} catch (MalformedURLException e)
		{
			e.printStackTrace();
		} catch (IOException e)
		{
			e.printStackTrace();
		}
		
		return retJson;
	}


	
}
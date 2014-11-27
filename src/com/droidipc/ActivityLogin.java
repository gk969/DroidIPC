package com.droidipc;

import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class ActivityLogin extends Activity
{
	private static final String LOG_TAG="ActivityLogin";
	
	private TextView textViewUser;
	private TextView textViewPassword;
	private TextView tvLoginMsg;
	
	private String user;
	private String password;
	
	private Handler hldLoginMsg;
	
    @Override
	protected void onCreate(Bundle savedInstanceState)
    {
        // Be sure to call the super class.
        super.onCreate(savedInstanceState);

        
        setContentView(R.layout.login);
        
        tvLoginMsg=(TextView)findViewById(R.id.textViewLoginMsg);
		textViewUser=(TextView)findViewById(R.id.editTextUser);
		textViewPassword=(TextView)findViewById(R.id.editTextPassword);
		
		loadUser();
		
        hldLoginMsg=new Handler(){
    		@Override
    		public void handleMessage(Message msg)
    		{
    			switch(msg.what)
    			{
    				case MainActivityIPC.MSG_HTTP_LOGIN_SUCCESS:
    				{
    					String jsonStr=(String)(msg.obj);
    					Log.i(LOG_TAG, "httpRecv:"+jsonStr);
    					
    					JSONObject object;
    					try
						{
    						saveUser(user, password);
    						
							object = new JSONObject(jsonStr);
	    					String ipcAddr = object.getString("ipc_addr");
	    					setResult(RESULT_OK, (new Intent()).setAction(
	    							"登录成功！外网IP："+ipcAddr+"已存入服务器。"));
	    					
	    					finish();
						} catch (JSONException e)
						{
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
    					
    					
    					break;
    				}

    				case MainActivityIPC.MSG_HTTP_LOGIN_AUTH_FAIL:
    				{
    					Log.i(LOG_TAG, "Server Login Auth Fail!");
    					tvLoginMsg.setText("登录失败，用户名或密码错误！");
    					break;
    				}

    				case MainActivityIPC.MSG_HTTP_LOGIN_LINK_FAIL:
    				{
    					Log.i(LOG_TAG, "Server Login Link Fail!");
    					tvLoginMsg.setText("登录失败，无法连接服务器！");
    					break;
    				}
    				default:
    					break;
    			}
    			super.handleMessage(msg);
    		}
    	};
        
    	
        Button btStartLogin=(Button)findViewById(R.id.buttonStartLogin);
        btStartLogin.setOnClickListener(new View.OnClickListener()
		{
			public void onClick(View v)
			{
				tvLoginMsg.setText("正在连接到gk969.com,请稍候…");
				
				user=textViewUser.getText().toString();
				password=textViewPassword.getText().toString();
				MainActivityIPC.ipcLogin(user, password, hldLoginMsg);
			}
		});
    }
    

	static class DatabaseHelper extends SQLiteOpenHelper
	{
		DatabaseHelper(Context context)
		{
			super(context, "auth.db", null, 1);
		}

		@Override
		public void onCreate(SQLiteDatabase db)
		{
			// TODO 创建数据库后，对数据库的操作
			db.execSQL("CREATE TABLE user "
					+ "(name varchar(20) not null , password varchar(60) not null );"); 
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
		{
			// TODO 更改数据库版本的操作
		}

		@Override
		public void onOpen(SQLiteDatabase db)
		{
			super.onOpen(db);
			// TODO 每次成功打开数据库后首先被执行
		}
	}

	private void saveUser(String name, String passWord)
	{
		SQLiteDatabase db = null;
		try
		{
			try
			{
				db=(new DatabaseHelper(getBaseContext())).getWritableDatabase();
				
				//多用户
				/*
				Cursor cursor = db.rawQuery("select * from user where name=?",new String[]{name});
				if(cursor.moveToFirst())
				{
				    String oldPwd = cursor.getString(cursor.getColumnIndex("password"));
				    Log.i(LOG_TAG, name+" Exist password:"+oldPwd);
				    if(passWord!=oldPwd)
				    {
					    db.execSQL("update [user] set password = '"+passWord+"' where name='"+name+"'");
					    Log.i(LOG_TAG, name+" New password:"+passWord);
				    }
				}
				else
				{
					db.execSQL("insert into user(name,password) values ('"+name+"','"+passWord+"');");
				    Log.i(LOG_TAG, "New Usre "+name+":"+passWord);
				}
				*/
				
				//单用户
				db.execSQL("delete from user");
				db.execSQL("insert into user(name,password) values ('"+name+"','"+passWord+"');");
			    Log.i(LOG_TAG, "User "+name+":"+passWord);
			}
			finally
			{
				if(db!=null)db.close();
			}
		}
		catch(SQLiteException e)
		{
			Log.i(LOG_TAG, e.toString());
		}
	}
	
    private void loadUser()
    {
    	SQLiteDatabase db = null;
		try
		{
			try
			{
				db=(new DatabaseHelper(getBaseContext())).getWritableDatabase();
				
				Cursor cursor = db.rawQuery("select * from user", null);
				if(cursor.moveToFirst())
				{
					textViewUser.setText(cursor.getString(cursor.getColumnIndex("name")));
					textViewPassword.setText(cursor.getString(cursor.getColumnIndex("password")));
				}
				
			}
			finally
			{
				if(db!=null)db.close();
			}
		}
		catch(SQLiteException e)
		{
			Log.i(LOG_TAG, e.toString());
		}
    }
    
}
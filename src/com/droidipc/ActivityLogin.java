package com.droidipc;

import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
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
							object = new JSONObject(jsonStr);
	    					String ipcAddr = object.getString("ipc_addr");
	    					setResult(RESULT_OK, (new Intent()).setAction(
	    							"��¼�ɹ�������IP��"+ipcAddr+"�Ѵ����������"));
	    					
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
    					tvLoginMsg.setText("��¼ʧ�ܣ��û������������");
    					break;
    				}

    				case MainActivityIPC.MSG_HTTP_LOGIN_LINK_FAIL:
    				{
    					Log.i(LOG_TAG, "Server Login Link Fail!");
    					tvLoginMsg.setText("��¼ʧ�ܣ��޷����ӷ�������");
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
				tvLoginMsg.setText("�������ӵ�gk969.com,���Ժ�");
				MainActivityIPC.httpDdnsClient.setAuth(textViewUser.getText().toString(), 
						textViewPassword.getText().toString());
				MainActivityIPC.httpDdnsClient.setMsgHandler(hldLoginMsg);
				MainActivityIPC.httpDdnsClient.start();
			}
		});
    }
}
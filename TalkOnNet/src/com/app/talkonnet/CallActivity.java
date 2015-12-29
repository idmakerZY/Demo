package com.app.talkonnet;

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

public class CallActivity extends Activity {

	public static Socket s;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.call_activity);
		TextView tx=(TextView)findViewById(R.id.textViewCall);
		tx.setText("正在连接");
		
		Intent intentCall=getIntent();
		final String toIp=intentCall.getStringExtra("ip");
		new Thread(new Runnable() {
			
			@Override
			public void run() {
				// TODO Auto-generated method stub
				//连接目标端，得到socket
				try {
					Log.d("tag", toIp);
					s=new Socket(toIp, ConstValue.port);
					Thread.sleep(1000);
					//可以在子线程里跳转activity
					Intent intentTalk=new Intent(CallActivity.this, TalkActivity.class);
					intentTalk.putExtra("comefrom", "callactivity");
					startActivity(intentTalk);
				} catch (UnknownHostException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				
			}
		}).start();
	}

}

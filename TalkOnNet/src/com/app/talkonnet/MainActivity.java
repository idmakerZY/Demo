package com.app.talkonnet;

import java.net.Socket;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.app.Activity;
import android.content.Intent;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class MainActivity extends Activity {

	Handler handler;
	public static ListenConnect lt;//�������ʵ��,��¶�����������activity����
	EditText etIp;
	Button btnlink;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        etIp=(EditText)findViewById(R.id.editTextIp);
        btnlink=(Button)findViewById(R.id.buttonLink);
        //final String toIp=etIp.getText().toString();
        btnlink.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub
				String toIp=etIp.getText().toString();
				Toast.makeText(getBaseContext(), toIp, Toast.LENGTH_SHORT).show();
				Intent intentCall=new Intent(getBaseContext(), CallActivity.class);
				intentCall.putExtra("ip", toIp);
				startActivity(intentCall);
			}
		});
        handler=new Handler(){

			@Override
			public void handleMessage(Message msg) {
				// TODO Auto-generated method stub
				if(msg.what==ConstValue.newlink)
				{
					//Socket s=(Socket)msg.obj;//Ҫ��obj��Ҫʵ�������л��ӿڵ���Ķ���Ļ����Ͳ���ListenConnect��getS()����
					//��ת��talk_activity
					Intent intentTalk=new Intent(getBaseContext(), TalkActivity.class);
					intentTalk.putExtra("comefrom", "mainactivity");
					startActivity(intentTalk);
				}
				super.handleMessage(msg);
			}
        	
        };
        
        if(lt==null)
        {
	       lt=new ListenConnect(handler);
	       Thread listen=new Thread(lt);
	       listen.start();
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }
    
}
